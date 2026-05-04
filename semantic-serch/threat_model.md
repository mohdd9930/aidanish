# Threat Model

## Project Overview

This project is a Java 21 / Spring Boot 4 semantic search and RAG application. It serves a small static HTML/JavaScript frontend and exposes JSON endpoints under `/api` for adding text documents, searching Pinecone vector metadata, and asking Gemini to answer questions using retrieved snippets. Embeddings are generated locally with Spring AI's ONNX transformer model; Pinecone and Google Gemini are external managed services reached via `RestTemplate` using secrets from environment variables.

The application is intended to run as a public web service on Replit. In production, `NODE_ENV`-style development assumptions do not apply to this Java service, but platform TLS is assumed for client/server traffic. The Replit mockup sandbox and local preview-only experimentation are out of production scope.

## Assets

- **Indexed document text** -- User-submitted text is stored as Pinecone vector metadata and can be returned by `/api/search` and `/api/ask` sources. It may contain personal, private, or business-sensitive content.
- **Vector database integrity and availability** -- Pinecone stores the shared corpus used for retrieval. Unauthorized writes or spam can poison answers, corrupt search quality, and consume storage/query quota.
- **Gemini and Pinecone API keys** -- Secrets are loaded from environment variables and authorize paid or quota-limited external service calls.
- **External service quotas and application compute** -- Local embedding generation, Pinecone calls, and Gemini calls consume CPU, memory, latency budget, and third-party quota/cost.
- **Application logs** -- Logs may be persisted by the hosting environment and must not contain sensitive document text, questions, secrets, or provider error details.

## Trust Boundaries

- **Browser/client to Spring API** -- All input to `/api/add`, `/api/search`, `/api/ask`, and `/api/health` is untrusted. The API must validate size and content, authenticate or intentionally gate sensitive operations, and avoid trusting frontend-only controls.
- **Spring API to Pinecone** -- The backend sends embeddings and document metadata to Pinecone using `PINECONE_API_KEY` and `PINECONE_INDEX_URL`. The configured index URL is trusted configuration, not user input.
- **Spring API to Gemini** -- The backend sends retrieved snippets and user questions to Gemini using `GEMINI_API_KEY`. Prompts may include untrusted indexed content and must be treated as data crossing to an external model service.
- **Public to shared corpus boundary** -- If endpoints are public, unauthenticated users can attempt to read, add, or influence documents in the shared Pinecone namespace.
- **Production vs development boundary** -- `.local/`, caches, Replit skill files, workflow logs, Maven wrapper bootstrap behavior, and preview-only experimentation are dev/runtime support areas and should not be treated as production application attack surfaces unless reachable from the deployed service.

## Scan Anchors

- Production entry point: `src/main/java/com/example/demo/DemoApplication.java` starts the Spring Boot service on `server.address=0.0.0.0`, `server.port=5000`.
- Public API surface: `src/main/java/com/example/demo/controller/SearchController.java` defines `POST /api/add`, `POST /api/search`, `POST /api/ask`, and `POST /api/health`.
- Highest-risk backend flows: `SearchController` -> `SemanticSearchService` -> local `EmbeddingModel` -> `PineconeRepository`; `/api/ask` additionally calls `GeminiClient`.
- External service clients: `src/main/java/com/example/demo/repository/PineconeRepository.java`, `src/main/java/com/example/demo/service/GeminiClient.java`, and timeouts in `src/main/java/com/example/demo/config/RestTemplateConfig.java`.
- Frontend rendering surface: `src/main/resources/static/index.html`; any API-provided text rendered with `innerHTML` must be escaped or inserted as text.
- Dev-only areas to ignore by default: `.local/`, `.cache/`, `target/`, workflow logs, Maven wrapper download implementation, and local-only run artifacts.

## Threat Categories

### Spoofing

The application currently has no user identity layer. Any production endpoint that modifies or reveals a non-public shared corpus must either deliberately be public or require authentication. If authentication is introduced later, every data-accessing endpoint must validate it server-side; client-side UI controls are not sufficient.

### Tampering

`/api/add` writes user-controlled text into the shared Pinecone namespace. The service must prevent unauthorized or abusive writes from poisoning the corpus, and must treat retrieved text as untrusted when constructing prompts or rendering sources. Prompt instructions should make clear that retrieved snippets are data, not trusted system instructions.

### Information Disclosure

Indexed documents are returned by search and RAG source responses and may also be sent to Gemini. The service must ensure that users cannot retrieve documents they are not allowed to see, avoid returning unnecessary metadata, avoid logging sensitive text/questions, and keep API keys out of URLs, responses, and logs.

### Denial of Service

Embedding generation, Pinecone queries/upserts, and Gemini calls are resource- and quota-consuming operations. Public endpoints must bound request size, request rate, and concurrent expensive work. External calls should have timeouts and clear error handling; this application already configures `RestTemplate` connect/read timeouts.

### Elevation of Privilege

The current code does not execute shell commands, build SQL queries, deserialize arbitrary types, or accept server-side file paths. The primary privilege risk is broken access control around the shared Pinecone corpus and quota-bearing external services. If admin or per-user datasets are added later, authorization must be enforced on the server for every operation.
