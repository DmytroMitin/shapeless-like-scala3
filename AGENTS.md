# Repository instructions

This is the **public product** repository. Keep source code, runnable examples, tests, public design documentation, and user-facing instructions here.

- The private control repository (shapeless-like-scala3-control) holds controller roadmap, prompts, handoffs, reviews, and private decisions. Do not copy private materials, credentials, private paths, or internal workflow logs here.
- The cloud ChatGPT controller owns task selection and acceptance; local Codex can investigate/implement within an explicitly authorized prompt and produces a handoff.
- Do not start unrelated implementation work or pre-create modules merely to match a proposed package diagram.
- Research-first: verify a gap against Scala 3 and Typelevel Shapeless 3 before adding an API. Prefer standard `Tuple`, `NamedTuple`, `Mirror`, `NotGiven`, etc. when they cover the need.
- Distinguish design sketches from compilable code. Document version dependencies, compile-time errors, ambiguity and limitations.
- Preserve user work. No force-push, release, dependency change, cross-project mutation, or public posting without explicit authorization.
- Publishing requires a separate explicit instruction; a task completion report is not permission to push.
- Run the narrowest relevant verification available, report actual commands/results and any checks that could not run.
