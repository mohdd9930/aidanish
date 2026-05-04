# Semantic Search + RAG (Spring Boot + Local ONNX Embeddings + Pinecone + Gemini)

A Java 21 / Spring Boot 4 backend that indexes text documents as **local ONNX embeddings** (no API key required for embeddings), stores/queries them in Pinecone, and uses **Google Gemini** to write natural-language answers from the retrieved snippets (RAG). A small static HTML/JS frontend is served at `/` so the app is usable directly from the Replit preview.

## Architecture

- `com.example.demo.DemoApplication` – Spring Boot entry point
- `controller/SearchController` – REST endpoints under `/api`
  - `POST /api/add` `{text}` → indexes a document
  - `POST /api/upload` (multipart `file`) → extracts text via Apache Tika (PDF/DOC/DOCX/TXT/RTF/ODT/HTML), chunks (~600 chars, 80 overlap), embeds + upserts each chunk with metadata `{text, filename, chunkIndex}`
  - `POST /api/search` `{text}` → returns the single most similar stored document with its cosine score
  - `POST /api/ask` `{text}` → RAG: retrieves top-3 similar chunks, asks Gemini to answer using only those, returns `{answer, sources:[{text, score, filename?}]}`
  - `POST /api/health` → liveness check
- `service/SemanticSearchService` – generates embeddings via Spring AI `EmbeddingModel` and orchestrates upsert/query (`searchWithScore` for top-1, `searchTopK` for RAG, `addFileChunks` for multi-chunk file uploads)
- `service/FileTextExtractor` – Apache Tika `AutoDetectParser` wrapper; throws `ExtractionException` (→ 400) for unreadable files
- `service/TextChunker` – word-boundary-aware text splitter (`CHUNK_SIZE=600`, `OVERLAP=80`)
- `service/GeminiClient` – minimal REST client for Google's Generative Language API (`generateContent`)
- `repository/PineconeRepository` – calls Pinecone REST API (`/vectors/upsert`, `/query`) with `RestTemplate`; `queryTopK` returns the full match list
- `config/RestTemplateConfig` – `RestTemplate` bean with 10s connect / 30s read timeouts
- `src/main/resources/static/index.html` – minimal UI (Add Document / Ask a question) calling the REST API; sources are hidden behind a "Show sources" toggle so only the answer is shown by default

## Embedding model

Uses Spring AI's `spring-ai-starter-model-transformers` starter, which runs **`sentence-transformers/all-MiniLM-L6-v2`** locally via ONNX Runtime.

- Output dimension: **384**
- No API key, no network calls for embeddings
- The model is downloaded on first run and cached
- `spring.ai.embedding.transformer.onnx.model-output-name=last_hidden_state` is required for this model

`jackson-databind` is declared explicitly in `pom.xml` because we removed the OpenAI starter that previously pulled it transitively.

## Configuration (`src/main/resources/application.properties`)

- `server.port=5000`, `server.address=0.0.0.0` – required for the Replit webview
- `pinecone.api-key=${PINECONE_API_KEY}`
- `pinecone.index-url=${PINECONE_INDEX_URL}` – host URL of a Pinecone index with **dimension=384, metric=cosine, type=Dense**
- `gemini.api-key=${GEMINI_API_KEY}` – key from https://aistudio.google.com/apikey
- `gemini.model=gemini-2.5-flash-lite` – chosen for generous free-tier quotas. `gemini-flash-latest` works too but its free quota (20/day) is small.
- `spring.ai.embedding.transformer.onnx.model-output-name=last_hidden_state`
- `spring.servlet.multipart.max-file-size=10MB`, `spring.servlet.multipart.max-request-size=10MB` – upload size cap

All secrets come from Replit Secrets — no API keys are committed in source.

## Build & Run

System packages installed via Nix: `jdk21`, `maven`.

```bash
mvn -DskipTests package
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

The `Start application` workflow runs `mvn -q -DskipTests package && java -jar target/demo-0.0.1-SNAPSHOT.jar` on port 5000 and is the source of the preview pane.

## Notes / Gotchas

- Spring Boot 4 requires Java 21.
- The Pinecone API key and index **must belong to the same Pinecone project** — a key from one project returns 401/403 against an index in another.
- Vector dimension mismatches surface as Pinecone HTTP 400 ("Vector dimension X does not match the dimension of the index Y"). Index must be 384.
- Source files arrived with CRLF line endings; strip them with `sed -i 's/\r$//'` before doing exact-match edits.
- Original zip leaked OpenAI + Pinecone keys in `application.properties`; fallbacks have been removed and the leaked keys should be rotated by the owner.
