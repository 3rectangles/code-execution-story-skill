---
name: code-execution-story
description: "Use when reading unfamiliar code. Map calls to Mermaid flow."
version: 1.0.0
author: Kashish Sharma
license: MIT
platforms: [linux, macos, windows]
metadata:
  hermes:
    tags: [code-reading, reverse-engineering, mermaid, call-graph, execution-flow, debugging, onboarding]
    category: software-development
    related_skills: [github, plan]
---

# Code Execution Story

Turn unfamiliar code into a navigable execution story — a Mermaid flow of how the
program actually moves state from entry point to exit — instead of a flat
file-by-file summary.

## When to use
- Reading a new/legacy codebase and you need to follow one request end to end.
- Answering "what calls this?", "where did this argument come from?", "where does this value go next?".
- Onboarding onto a service (esp. Java/Spring, but language-agnostic).
- Explaining a call chain to someone else.

NOT for: writing new code, refactors, or UML class diagrams — this is an
execution-flow tool, not a class-dump tool.

## The one rule
Do NOT summarize files. Reconstruct the flow as states -> action -> next state,
and show where every important value was created, modified, passed, returned,
and consumed. Optimize for the RELATIONSHIPS between code, not for showing all
the code.

## Workflow

### Step 0 — Locate the real entry point (BEFORE diagramming)
The prompt only works once the right code slice is in context. Never dump the repo.
- Find the entry: HTTP handler/route, `main()`, a public API method, a queue consumer, a cron, a CLI command.
  Grep the framework's registration points (`@GetMapping`/`@PostMapping`, `@KafkaListener`, `func main`, `app.get`, `if __name__ == "__main__"`, ...).
- Trace the chain with search, not by reading whole files: `search_files` for the symbol, then `read_file` only the bodies on the chain.
- Slice: pull ONLY functions reachable from the entry point for THIS flow, and record each `file:line` as you go — Step 4 needs them.
- Full retrieval recipe: `references/repo-navigation.md`.

### Step 1 — Scope to ONE flow
One entry point per diagram. A whole service becomes a spider web. If asked for
"the codebase", pick the single most important request path, say so, and offer
sibling flows as separate diagrams.

### Step 2 — Trace, grounded in source
Follow real calls only. For each hop capture: caller -> callee, the arguments
passed, where each argument was created, and the return value.
- Tag every hop with its confidence: DEFINITELY CALLED / POSSIBLY CALLED / INFERRED FROM FRAMEWORK / UNKNOWN.
- If a body is not in context: paste `[implementation not provided]` — never invent internals.
- Missing file/line -> mark UNKNOWN. Do NOT fabricate line numbers.

### Step 3 — Produce the story + diagram
Emit the output contract below. The Mermaid diagram IS the deliverable; the prose
supports it. Prefer `flowchart TD` (use `LR` if the chain is long and linear).

### Step 4 — Verify before shipping
- Re-read each root node's `file:line` and confirm the call is really there.
- Confirm branch conditions exist in source (not presumed).
- Confirm every argument edge traces to a real origin.
- Downgrade any hop you cannot confirm to its honest tag.

## Output contract
1. **Execution story** — 2–4 sentences: entry -> ... -> exit, plain language.
2. **Mermaid flow** — ONE copy-pasteable diagram. This is the point.
3. **Important data flows** — value lifecycles (created -> passed -> returned -> consumed).
4. **Function map** — each function in execution order + one-line role.
5. **Key dependencies** — class/service/external edges (DB, queue, API).
6. **Things to investigate** — what the source did NOT reveal; say "Unknown from provided code".

Keep 4–6 lean and skip any that add nothing. Don't pad to satisfy the format.

## Mermaid safety (viewer-proof)
- Node ids simple, separate from labels: `A["OrderController.createOrder(request)"]`.
- ALWAYS quote labels. Parentheses, dots, colons, `/`, `{}`, `<br/>` are safe
  inside a quoted label; unquoted they break the viewer.
- No raw `<` `>` except `<br/>`; avoid stray `#` and `;`.
- Don't nest quotes. Don't reuse reserved ids (`end`, `graph`, `subgraph`) as node ids.
- Decisions: `D{"status == pending?"}` with `-->|yes|` / `-->|no|` edges.
- Async/external are separate nodes: `Kafka["Kafka: OrderCreated"]`, `PostgreSQL`,
  `Stripe API`; label the transition `-->|publish|`, `-->|consume|`.
- ONE diagram, a clean primary path, secondary branches off it.

## Pitfalls
- Summarizing files instead of tracing flow — the failure mode this skill exists to kill.
- Diagramming the whole repo at once -> spider web. Scope to one entry point.
- Inventing internals for methods not in context. Use `[implementation not provided]`.
- Treating framework magic as if visible in source — tag it INFERRED FROM FRAMEWORK.
- Fabricating `file:line`. Mark UNKNOWN instead.
- Over-expanding trivial helpers; expand only nodes important to understanding.

## References
- `references/execution-story-prompt.md` — the full canonical prompt (every rule, all examples).
- `references/repo-navigation.md` — how to slice a real repo into context cheaply.
