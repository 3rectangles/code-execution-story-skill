# code-execution-story

A Hermes Agent skill that turns unfamiliar code into a **Mermaid execution-story
diagram** — a navigable flow of how a program actually moves state from entry
point to exit — instead of a flat file-by-file summary.

Based on a prompt by **Kashish Sharma**, packaged as a Hermes skill.

## What it does

Give it a codebase and one entry point (an HTTP handler, `main()`, a Kafka
consumer, a CLI command). It traces the real call chain and produces:

1. An execution story (plain-language flow)
2. **One copy-pasteable Mermaid diagram** (the deliverable)
3. Important data flows / value lifecycles
4. A function map in execution order
5. Key dependencies (DB, queue, external API edges)
6. Things to investigate — what the source did *not* reveal

It never invents flow: every hop is tagged `DEFINITELY CALLED / POSSIBLY CALLED /
INFERRED FROM FRAMEWORK / UNKNOWN`, and missing bodies show
`[implementation not provided]`.

## Layout

```
SKILL.md                              # slim trigger + workflow + output contract
references/execution-story-prompt.md  # the full canonical prompt (all rules, examples)
references/repo-navigation.md         # how to slice a real repo into context cheaply
```

## Install (Hermes)

```sh
cp -R . ~/.hermes/skills/software-development/code-execution-story
```

Then load it in a session with `skill_view(name='code-execution-story')`.

## Usage

> "Use the code-execution-story skill on `OrderController.createOrder` in this repo."

## License

MIT — see [LICENSE](LICENSE).
