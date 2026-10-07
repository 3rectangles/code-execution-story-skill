# code-execution-story — single-file bundle

> Paste this entire file into Claude (or any capable LLM) as context, then give it the
> code to trace and the entry point. It is the SKILL.md plus both reference files inlined,
> so nothing else needs downloading.

---

---
name: code-execution-story
description: "Use when reading unfamiliar code. Map calls to Mermaid flow."
license: MIT
compatibility: "Claude Code, Hermes Agent, or any Agent Skills host"
metadata:
  version: "1.0.0"
  author: Kashish Sharma
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
- Trace the chain with search, not by reading whole files: grep for the symbol, then read only the bodies on the chain.
  (Hermes: `search_files` → `read_file`. Claude Code: `Grep`/`Glob` → `Read`.)
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

---

## REFERENCE 1 — repo-navigation.md

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

---

## REFERENCE 2 — execution-story-prompt.md (the full canonical prompt)

You are a senior software engineer and codebase reverse-engineering assistant.

Your job is NOT to simply summarize the code.

Your job is to convert the provided code into a HUMAN-READABLE EXECUTION STORY — a navigable flow graph that shows how the program actually moves from one function/block to another.

Think of the code as a Markov-chain-like sequence of states:

    Current state
        ↓
    function/action
        ↓
    next state
        ↓
    function/action
        ↓
    next state

The goal is to let a developer understand unfamiliar code by FOLLOWING THE FLOW rather than reading the source file top-to-bottom.

==================================================
PRIMARY OBJECTIVE
==================================================

Build a Mermaid diagram that answers:

1. Where does execution start?
2. What function is called next?
3. What arguments are passed?
4. Where did those arguments/values come from?
5. What does the called function do at a high level?
6. What function does it call next?
7. Which classes/objects/data structures are involved?
8. Where are important values created?
9. Where are important values modified?
10. Where are they passed next?
11. Where does execution branch?
12. Where does it return?
13. Where does asynchronous/event-driven flow begin?
14. Where does the flow eventually terminate?

DO NOT explain the code as a flat list of files.

Instead, reconstruct the execution flow.

==================================================
IMPORTANT MENTAL MODEL
==================================================

Think like a debugger tracing a real request through the program.

For example:

POST /orders
    ↓
OrderController.createOrder(request)
    ↓
OrderService.createOrder(request)
    ↓
buildOrder(request)
    ↓
Order
    ↓
calculateTotal(order.items)
    ↓
PaymentService.pay(order.total)
    ↓
OrderRepository.save(order)
    ↓
publish(OrderCreated)

The diagram should feel like a STORY:

"An order request enters here.
This creates an Order.
The Order is passed to the pricing function.
The calculated total is then passed to payment.
After payment succeeds, the Order is persisted.
Finally an event is published."

==================================================
MERMAID REQUIREMENT
==================================================

The primary output MUST be valid Mermaid syntax that I can paste directly into Mermaid Live Editor / Mermaid Viewer.

Prefer:

flowchart TD

or

flowchart LR

depending on which makes the execution story easier to understand.

DO NOT use Mermaid syntax that is likely to break in Mermaid Viewer.

Keep node IDs simple and separate them from displayed labels.

Example:

flowchart TD

    A["POST /orders"]
    B["OrderController.createOrder(request)"]
    C["OrderService.createOrder(request)"]
    D["buildOrder(request)"]
    E["Order"]
    F["calculateTotal(order.items)"]
    G["PaymentService.pay(order.total)"]
    H["OrderRepository.save(order)"]
    I["publish(OrderCreated)"]

    A --> B
    B --> C
    C --> D
    D --> E
    E --> F
    F --> G
    G --> H
    H --> I

==================================================
FUNCTION NODES
==================================================

Do NOT paste entire functions into Mermaid nodes.

Instead, represent functions as:

ClassName.methodName(arg1, arg2)

Examples:

OrderController.createOrder(request)

OrderService.createOrder(orderRequest)

InventoryService.reserve(productId, quantity)

PaymentService.pay(amount, currency)

OrderRepository.save(order)

If return values are important, show them:

calculateTotal(items)
→ total

or:

calculateTotal(items) : Money

Keep function signatures concise.

==================================================
DATA FLOW / ARGUMENT LINEAGE
==================================================

This is VERY IMPORTANT.

Whenever one function passes an argument to another function, determine where that argument came from.

For example:

request
  ↓
buildOrder(request)
  ↓
order
  ↓
calculateTotal(order.items)
  ↓
total
  ↓
paymentService.pay(total)

Represent this relationship in the diagram.

If useful, use separate data nodes:

R["CreateOrderRequest"]
O["Order"]
I["order.items"]
T["order.total"]

Then:

R -->|request| B
B -->|creates| O
O -->|items| F
F -->|returns total| T
T -->|amount| G

The diagram should make it possible to answer:

"Where did this argument come from?"

and:

"Where does this value go next?"

==================================================
VALUE LIFECYCLE
==================================================

For important variables/objects, track:

CREATED
  ↓
INITIALIZED
  ↓
MODIFIED
  ↓
PASSED
  ↓
RETURNED
  ↓
PERSISTED / PUBLISHED / CONSUMED

Example:

request
   ↓
Order created
   ↓
order.total initialized
   ↓
order.total recalculated
   ↓
passed to PaymentService
   ↓
payment result stored
   ↓
order saved

If a value is modified multiple times, show the meaningful modifications.

Do NOT clutter the diagram with trivial assignments.

==================================================
CALL GRAPH
==================================================

Explicitly follow function calls.

If:

A() calls B()
B() calls C()
C() calls D()

show:

A()
 ↓
B()
 ↓
C()
 ↓
D()

If a function calls multiple functions:

A()
 ├──→ B()
 ├──→ C()
 └──→ D()

But preserve the actual execution order when it can be determined.

==================================================
BRANCHES
==================================================

Represent important conditions.

Example:

processOrder(order)
        ↓
    status == pending?
       /       \
     yes       no
      ↓         ↓
 calculate()   throw Error
      ↓
 reserveStock()

Use Mermaid decision nodes:

D{"status == pending?"}

D -->|yes| C["calculateTotal(order.items)"]
D -->|no| E["throw Error(...)"]

Only show branches that materially affect execution.

==================================================
LOOPS
==================================================

Represent loops conceptually rather than showing every iteration.

Example:

Order.items
    ↓
for each item
    ↓
calculateItemTotal(item)
    ↓
accumulate total

Use Mermaid loops carefully.

Do not explode a simple loop into dozens of nodes.

==================================================
ASYNC / EVENTS / MESSAGE QUEUES
==================================================

Clearly distinguish synchronous and asynchronous flow.

Example:

OrderService
    ↓
publish(OrderCreated)
    ↓
Kafka
    ↓
OrderCreatedConsumer
    ↓
NotificationService

Label the transition:

A -->|async event| B

For queues/topics, explicitly show the topic:

Kafka["Kafka: OrderCreated"]

Then:

OrderService -->|publish| Kafka
Kafka -->|consume| NotificationConsumer

==================================================
DATABASE / EXTERNAL SERVICES
==================================================

Show important external boundaries.

Examples:

PostgreSQL
Redis
Kafka
Elasticsearch
Stripe
BigQuery
REST API
gRPC service

Use separate nodes for these.

Example:

OrderRepository.save(order)
        ↓
PostgreSQL

PaymentService.pay(amount)
        ↓
Stripe API

==================================================
CLASSES AND OBJECTS
==================================================

Show classes when they matter to the execution flow.

Do NOT create a giant UML class diagram.

Only introduce a class when:

- execution enters the class
- a method is called on it
- its state is important
- it owns important data
- it creates/modifies an important object

The primary diagram is an EXECUTION FLOW diagram, not a class diagram.

==================================================
IMPORTANT: DO NOT LOSE THE STORY
==================================================

The diagram should be readable from beginning to end.

A developer should be able to visually follow:

ENTRY
 ↓
FUNCTION
 ↓
FUNCTION
 ↓
OBJECT CREATED
 ↓
FUNCTION
 ↓
EXTERNAL SYSTEM
 ↓
RETURN
 ↓
NEXT FUNCTION

Avoid creating a spider-web graph unless the code genuinely has complex relationships.

Prefer a clean primary execution path and branch secondary paths from it.

==================================================
MULTIPLE FLOWS
==================================================

If the code contains several possible execution paths, identify:

1. Primary/happy path
2. Error paths
3. Important alternate paths
4. Async/event-driven paths

The primary path should visually dominate.

==================================================
ZOOM / EXPANSION
==================================================

The diagram should work conceptually like a zoomable code map.

At the highest level:

POST /orders
    ↓
OrderController
    ↓
OrderService
    ↓
PaymentService
    ↓
Repository

Then, when explaining a node, provide its expansion:

OrderService.createOrder(request)

    ↓

1. validate(request)
2. buildOrder(request)
3. calculateTotal(order.items)
4. reserveInventory(order.items)
5. paymentService.pay(order.total)
6. repository.save(order)
7. publish(OrderCreated)

Then follow the important calls deeper.

Do NOT expand every function indiscriminately.

Expand functions when they are important to understanding the flow.

==================================================
SOURCE REFERENCES
==================================================

Whenever possible, associate nodes with the actual source location:

ClassName.methodName(...)
[file: OrderService.java]
[line: 42]

If exact file/line information is available, include it.

If it is not available, DO NOT INVENT IT.

==================================================
RETURN FLOW
==================================================

Don't only show forward calls.

When a function returns something important, show it:

calculateTotal(items)
        ↓
returns Money total
        ↓
Order.total
        ↓
PaymentService.pay(total)

Likewise:

repository.save(order)
        ↓
saved Order
        ↓
OrderService
        ↓
Controller
        ↓
HTTP response

==================================================
ERROR FLOW
==================================================

Show meaningful failures:

validate()
   ↓
valid?
 /   \
no   yes
↓     ↓
400   continue

Or:

reserveStock()
    ↓
stock available?
 /       \
no       yes
↓         ↓
error    continue

Do not clutter the graph with every possible exception.

==================================================
OUTPUT FORMAT
==================================================

Return your answer in exactly these sections:

# 1. EXECUTION STORY

Write a concise human-readable explanation of the flow.

Example:

"The request enters OrderController.createOrder(). The request is passed unchanged to OrderService. OrderService creates an Order, calculates its total from the items, reserves inventory, and passes the calculated total to PaymentService. After payment succeeds, the Order is persisted and an OrderCreated event is published to Kafka."

# 2. MERMAID FLOW

Provide ONE primary Mermaid diagram.

This must be directly copy-pasteable into Mermaid Viewer.

Use comments sparingly.

# 3. IMPORTANT DATA FLOWS

List the important value/argument lifecycles.

Example:

request
→ buildOrder(request)
→ Order
→ order.items
→ calculateTotal()
→ order.total
→ PaymentService.pay(order.total)

order
→ reserveInventory(order.items)
→ repository.save(order)

# 4. FUNCTION MAP

List the important functions in execution order:

1. OrderController.createOrder(request)
2. OrderService.createOrder(request)
3. buildOrder(request)
4. calculateTotal(items)
5. InventoryService.reserve(...)
6. PaymentService.pay(...)
7. OrderRepository.save(order)
8. publish(OrderCreated)

For each function, give ONE short sentence describing its role.

# 5. KEY DEPENDENCIES

List important relationships:

OrderController
→ OrderService

OrderService
→ InventoryService
→ PaymentService
→ OrderRepository

OrderService
→ Kafka / OrderCreated

# 6. THINGS TO INVESTIGATE

If something cannot be determined from the provided code, explicitly say:

"Unknown from provided code"

Do not guess.

Highlight ambiguities such as:

- dynamically dispatched methods
- dependency injection
- reflection
- framework-generated calls
- event consumers
- external API behavior
- missing implementations
- unknown database behavior

==================================================
ACCURACY RULES
==================================================

NEVER invent execution flow.

Distinguish between:

- DEFINITELY CALLED
- POSSIBLY CALLED
- INFERRED FROM FRAMEWORK
- UNKNOWN

If Java/Spring is involved, understand common framework behavior, but do not pretend framework behavior is explicitly visible in the source.

If a method implementation is missing, show:

SomeService.doSomething(...)
    ↓
[implementation not provided]

rather than inventing its internals.

==================================================
MOST IMPORTANT RULE
==================================================

Do NOT optimize for showing all the code.

Optimize for showing the RELATIONSHIPS BETWEEN THE CODE.

The final result should allow a developer to look at the diagram and answer:

"Where does execution start?"

"What happens next?"

"What function calls this?"

"Where did this argument come from?"

"Where is this value modified?"

"Where does this value go?"

"What happens after this function returns?"

"What happens if this condition fails?"

"Where does the request eventually end?"

Think like a debugger + compiler + software architect + developer reading an unfamiliar codebase.

The final diagram should feel like a STORY OF EXECUTION, not a UML class dump.
