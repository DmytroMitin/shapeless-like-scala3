# Project scope (provisional)

## Mission

Provide practical, type-safe, Scala 3-native building blocks inspired by Scala 2 Shapeless **only where a real capability/ergonomics gap remains**.

## Selection criteria

For each candidate: (1) articulate the precise original semantics and a runnable use case; (2) compare with the current supported Scala 3 standard library, compiler and Shapeless 3; (3) decide whether simple idiomatic composition already suffices; (4) prototype exact inference and error behaviour; (5) evaluate cost, maintainability and interoperability.

Disposition vocabulary: **IMPLEMENT**, **ADAPT / WRAP**, **SKIP**, **INVESTIGATE**. Unverified items remain INVESTIGATE.

## Candidate families

1. Poly / ad-hoc typed cases and heterogeneous mapping.
2. Tuple: type-directed selectors, partitions, transformations and typeclass <-> match-type correspondences.
3. Ordered coproduct: positional alternatives, injections, eliminations, duplicate types and safe interoperability with sums.
4. Record: operations primarily on the standard `NamedTuple`, not an automatic independent record representation.
5. Labelled union / labelled sum: clarify naming, representation and relationships with ordered coproduct and Scala unions.
6. Derivation adapters: `Generic` / `LabelledGeneric` if their value-level ergonomics and representation conversions add real value.
7. Contextual search: priority, existence and other utilities, with compiler feasibility investigated rather than presumed.
8. Future optional integration with AUXify-scala3; no dependency until stable and justified.

## Non-goals (for now)

- Mechanical port of the full Scala 2 `shapeless.ops` API.
- Replacing Typelevel Shapeless 3's derivation framework or the standard `Tuple`/`NamedTuple`.
- Promising arbitrary enumeration or ranking of givens.
- Supporting every Scala 3 release from day one.
- Publishing binaries/releases before a reviewed and verified implementation.

## Public vs private

This file describes public scope. Research bookkeeping, priority decisions, controller prompts and handoffs live in the **private control repository**.
