# Semantic Search + RAG

A small Spring Boot 4 / Java 21 demo that:

1. Embeds text **locally** with a sentence-transformers ONNX model (no embedding API key needed).
2. Stores those embeddings in **Pinecone** (a managed vector database).
3. Uses **Google Gemini** to write short, grounded answers from the most relevant indexed snippets (RAG — retrieval-augmented generation).
4. Ships with a tiny static HTML/JS frontend at `/` so you can index docs and ask questions from the browser.

---

## Quick start on Replit

The project is already wired up. Just press **Run** (or restart the `Start application` workflow). The preview pane will open the UI at port 5000.

You'll need three secrets configured in Replit Secrets:

| Secret | What it is | Where to get it |
| --- | --- | --- |
| `PINECONE_API_KEY` | Pinecone API key | https://app.pinecone.io |
| `PINECONE_INDEX_URL` | Full index host URL, e.g. `https://my-index-xxxx.svc.aped-...pinecone.io` | Pinecone console → your index → "Connect" |
| `GEMINI_API_KEY` | Google AI Studio key | https://aistudio.google.com/apikey |

The Pinecone index **must** be:
- **Dimension: 384** (matches `all-MiniLM-L6-v2`)
- **Metric: cosine**
- **Type: Dense**

The API key and the index must belong to the **same Pinecone project**, otherwise Pinecone returns 401/403.

---

## Using the app

Open the preview and you'll see two cards:

- **Add Document** — paste a sentence or short paragraph and click *Add to index*. The text is embedded locally and upserted into Pinecone.
- **Ask a question** — type a question, click *Ask*. The app retrieves the top 3 similar snippets and asks Gemini to answer using only those. The answer is shown by itself; click *Show sources* to see which snippets were used.

**Tip:** semantic search works best when each indexed document is **one focused fact**. Indexing one giant note leads to messier retrieval than indexing many small ones.

---

## REST API

All endpoints accept and return JSON. The body shape is `{"text": "..."}` for every endpoint that takes input.

| Method | Path | Body | Returns |
| --- | --- | --- | --- |
| `POST` | `/api/add` | `{"text": "..."}` | `{success, documentId, text}` |
| `POST` | `/api/search` | `{"text": "..."}` | `{success, query, result, score}` — top-1 nearest neighbor + cosine score |
| `POST` | `/api/ask` | `{"text": "..."}` | `{answer, sources: [{text, score}, ...]}` — RAG answer + the snippets it used |
| `POST` | `/api/health` | — | `{status: "ok"}` |

Example:

```bash
curl -X POST http://localhost:5000/api/add \
  -H 'Content-Type: application/json' \
  -d '{"text":"Spring Boot simplifies Java enterprise development"}'

curl -X POST http://localhost:5000/api/ask \
  -H 'Content-Type: application/json' \
  -d '{"text":"What framework helps build Java apps?"}'
```

Error responses use sensible HTTP codes:
- `400` — empty/invalid input
- `502` — Pinecone or Gemini call failed (with a sanitized message; e.g. `"Gemini free-tier quota reached. Please wait ~1 minute and try again."`)
- `500` — anything else unexpected

---

## How it works

```
Browser UI (static HTML/JS at /)
        │
        ▼
SearchController  ──┬── /api/add    ─► SemanticSearchService.indexDocument
                    │                       └─ EmbeddingModel.embed (local ONNX)
                    │                       └─ PineconeRepository.upsert
                    │
                    ├── /api/search ─► SemanticSearchService.searchWithScore
                    │                       └─ PineconeRepository.queryTopOne
                    │
                    └── /api/ask    ─► SemanticSearchService.searchTopK (top 3)
                                          └─ PineconeRepository.queryTopK
                                       │
                                       ▼
                                    Build context → GeminiClient.generate
                                       │
                                       ▼
                                  {answer, sources}
```

### Embeddings (local, free)

Spring AI's `spring-ai-starter-model-transformers` runs **`sentence-transformers/all-MiniLM-L6-v2`** through ONNX Runtime inside the JVM. No API key, no network calls. The model is downloaded once on first run and cached. Output is a 384-dim vector.

### Retrieval (Pinecone)

`PineconeRepository` calls Pinecone's REST API directly with `RestTemplate`:
- `POST {index-url}/vectors/upsert` to index
- `POST {index-url}/query` to retrieve top-K matches with metadata + cosine scores

Backend failures throw a typed `PineconeException` so the controller can return a real 502 instead of pretending "no docs found."

### Answer synthesis (Gemini)

`GeminiClient` is a minimal REST client for Google's Generative Language API (`models/{model}:generateContent`). The API key is sent in the `x-goog-api-key` header (never in the URL) so it can't leak via exception messages or logs.

The `/api/ask` endpoint:
1. Embeds the question locally.
2. Pulls the top 3 matches from Pinecone.
3. Drops matches whose score is more than 0.15 below the best one (and below an absolute floor of 0.30) — keeps weak/irrelevant snippets out of the prompt.
4. Caps each snippet at 1500 chars so one long document can't dominate.
5. Sends Gemini a strict prompt: *answer in one short sentence using only the snippets; if the answer isn't there, reply "I don't know."*

Default model is `gemini-flash-latest`. You can list models available to your key with:

```bash
curl "https://generativelanguage.googleapis.com/v1beta/models?key=$GEMINI_API_KEY"
```

Gemini's free tier is 5 requests/minute on this model — if you hit it, the UI shows *"Gemini free-tier quota reached. Please wait ~1 minute and try again."*

---

## Project layout

```
src/main/java/com/example/demo/
├── DemoApplication.java
├── config/
│   └── RestTemplateConfig.java          # RestTemplate bean (10s connect / 30s read timeouts)
├── controller/
│   └── SearchController.java            # /api/add, /api/search, /api/ask, /api/health
├── model/
│   ├── Document.java
│   ├── AddRequest.java
│   ├── SearchRequest.java
│   ├── SearchResponse.java
│   ├── PineconeVector.java
│   └── PineconeQueryResult.java
├── repository/
│   └── PineconeRepository.java          # upsert, queryTopOne, queryTopK
└── service/
    ├── SemanticSearchService.java       # local embedding + orchestration
    └── GeminiClient.java                # Generative Language API client

src/main/resources/
├── application.properties               # all config (port, secrets, model)
└── static/
    └── index.html                       # the browser UI
```

---

## Configuration (`src/main/resources/application.properties`)

| Property | Purpose |
| --- | --- |
| `server.port=5000`, `server.address=0.0.0.0` | Required for the Replit preview |
| `pinecone.api-key=${PINECONE_API_KEY}` | Pinecone secret |
| `pinecone.index-url=${PINECONE_INDEX_URL}` | Pinecone index host (must be 384-dim cosine) |
| `gemini.api-key=${GEMINI_API_KEY}` | Gemini secret |
| `gemini.model=gemini-flash-latest` | Reliable default; older `gemini-1.5-flash` / `gemini-2.0-flash` may not be enabled on every key |
| `spring.ai.embedding.transformer.onnx.model-output-name=last_hidden_state` | Required for `all-MiniLM-L6-v2` |

All secrets come from Replit Secrets — nothing sensitive is committed to source.

---

## Build & run

System packages: `jdk21`, `maven` (provided via Nix on Replit).

```bash
mvn -DskipTests package
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

The `Start application` workflow runs:

```bash
mvn -q -DskipTests package && java -jar target/demo-0.0.1-SNAPSHOT.jar
```

on port 5000.

---

## Tech stack

- Java 21
- Spring Boot 4.0.5
- Spring AI 2.0.0-M4 (`spring-ai-starter-model-transformers`, ONNX Runtime)
- Sentence Transformers `all-MiniLM-L6-v2` (384-dim)
- Pinecone (managed vector DB) — REST API via `RestTemplate`
- Google Gemini (`gemini-flash-latest`) — REST API via `RestTemplate`
- Jackson for JSON
- Vanilla HTML/CSS/JS frontend

---

## Notes / gotchas

- Spring Boot 4 requires Java 21.
- Pinecone returns HTTP 400 *"Vector dimension X does not match the dimension of the index Y"* if your index isn't 384-dim — re-create the index with the right dimension.
- Pinecone returns 401/403 if the API key and index URL are from different Pinecone projects.
- Gemini free tier: **5 requests/minute** on `gemini-flash-latest`. The UI surfaces this clearly.
- API keys are never embedded in URLs (Gemini key uses the `x-goog-api-key` header) and exception messages returned to the client are sanitized.
