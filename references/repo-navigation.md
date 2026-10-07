# Repo navigation — slice a real repo into context cheaply

Reconstruct an execution flow WITHOUT loading the whole codebase. The diagram is
only as good as the slice you actually read.

## 1. Find the entry point
One flow needs one entry. Grep the framework's registration points:

- **Spring / Java**: `@RestController`, `@Controller`, `@GetMapping`, `@PostMapping`, `@RequestMapping`, `@KafkaListener`, `@Scheduled`
- **Python**: `@app.route`, `@router`, FastAPI/Flask decorators; CLI: `if __name__ == "__main__"`, argparse/click/typer
- **Go**: `func main`, `http.HandleFunc`, `gin.`/`echo.` routers
- **Node**: `app.get`/`router.post`, `express()`, `server.listen`
- **JVM batch / consumers**: `public static void main`, `@JmsListener`, `MessageListener`, `ConsumerRecord`

Pick the endpoint tied to the request you actually care about.

## 2. Trace with search, not by reading whole files
1. Grep for the symbol to locate it. (Hermes `search_files`; Claude Code `Grep` / `Glob`.)
2. Read ONLY the function body, using an offset/limit. (Hermes `read_file`; Claude Code `Read`.)
3. Follow each call into the next body. Stop at external boundaries (DB driver, HTTP client, SDK, queue).

## 3. Hop ledger
Keep a running table while tracing — it becomes the diagram AND output sections 4/5:

| # | caller (file:line) | callee (file:line) | args passed | returns | confidence |
|---|---|---|---|---|---|

The `confidence` column reuses the accuracy tags (DEFINITELY / POSSIBLY / INFERRED / UNKNOWN).

## 4. Context budget
- Read function bodies, not files. Use `offset`/`limit`.
- Depth-first along the chain; do not fan out into unrelated files.
- Do not diagram library internals — model them as external nodes (PostgreSQL, Kafka, Stripe API).
- If the repo is huge, map only the slice reachable from the chosen entry point.

## 5. Framework notes — what the source may hide
- **DI / Spring wiring**: the concrete class injected at a call site is a runtime fact — tag INFERRED FROM FRAMEWORK.
- **Kafka / queues**: producer and consumer are separate flows; bridge them with an explicit topic node.
- **Dynamic dispatch / interfaces / reflection**: the concrete implementation is a runtime fact — POSSIBLY CALLED.
- **AOP / proxies / annotations**: behaviour like transactions, retries, caching is not in the method body — note it, don't draw it as calls.
- **ORM**: `repository.save(x)` generates SQL; show the repository -> DB edge, never invent the SQL.
