# Build layout

The repository currently builds one sbt artifact. Capability boundaries are packages, not physical subprojects:

- `shapelesslike.poly`
- `shapelesslike.tuple.typeclasses`
- `shapelesslike.tuple.syntax`

Future ordered coproduct, NamedTuple, derivation, or contextual-search work is not pre-created as empty modules. A separate build boundary should be added only when a real dependency or publishing boundary requires it.
