# Architecture

## Principle: one semantics, multiple useful representations

Candidate package families (subject to Prompt 001 review):

```text
shapelesslike.poly
shapelesslike.tuple.matchtypes
shapelesslike.tuple.typeclasses
shapelesslike.coproduct.matchtypes
shapelesslike.coproduct.typeclasses
shapelesslike.record.matchtypes
shapelesslike.record.typeclasses
shapelesslike.union.matchtypes
shapelesslike.union.typeclasses
shapelesslike.generic
shapelesslike.search
```

These are *package-level conceptual boundaries*, **not** a commitment to ten sbt subprojects. Implement physical modules only when dependencies or publishing boundaries require them.

### Match types

Express type-level computation and reductions. Challenges include stuck match types, opaque/abstract inputs, proof of equality, inference and compiler version differences.

### Type classes

Represent evidence and value-level algorithms, potentially with an `Out` member. Challenges include implicit search, divergence/ambiguity, erased representations, result inference and instance priority.

### Interoperability

Do not assume the compiler can prove `Typeclass[A]#Out =:= MatchTypeResult[A]` generically. Explore constructive witnesses, Aux patterns, explicit evidence, or one implementation shared between surfaces. Test exact inferred result types and failure cases. Avoid unchecked casts unless an invariant is explicitly proven and reviewed.

### Representations

- Standard tuples for products/HList-like operations.
- Standard named tuples for records when feasible (verify APIs in the chosen Scala baseline).
- Scala union types do **not** preserve positional alternatives or duplicates; an ordered coproduct needs a different representation.
- Scala sum derivation through `Mirror` is distinct from an arbitrary ordered coproduct; conversions and ordering need a contract.
- Labelled variants require an explicit label semantics.

### Implemented first vertical slice

The initial artifact implements a Shapeless-like ad-hoc `Poly1` registry and heterogeneous tuple `Mapper`:

- `shapelesslike.poly` holds typed cases and a duplicate-key-rejecting per-Poly registry;
- `shapelesslike.tuple.typeclasses` derives exact recursive mapping evidence;
- `shapelesslike.tuple.syntax` provides `tuple.mapWith(poly)` without shadowing standard `Tuple.map`;
- ordinary tuples remain the only input/output representation;
- the mapper kernel uses no unchecked cast.

The [design note](POLY1_TUPLE_MAPPER.md) records the coherence experiments, selected registry semantics, and first-slice limitations. Later package families remain hypotheses and require separate gates.

### Implemented ordered Coproduct slice

`shapelesslike.coproduct` implements an invariant ordered sum over a standard type tuple:

- a sealed `Coproduct[Alts]` carrier uses private nested `Here`/`There` nodes;
- structural `Position[Alts, I, A]` evidence constructs and projects an exact zero-based position without type inequality or subtype search;
- concrete `projectAt[I]` returns `Option[Tuple.Elem[Alts, I]]`, while `projectAtAs[I, A]` supports abstract callers carrying explicit evidence;
- `Handlers[Alts, R]` gives exhaustive positional fold, including distinct bodies for repeated element types;
- the kernel is cast-free and keeps constructors/evidence implementations owner-controlled.

This slice is for anonymous generic alternative tuples whose occurrence identity matters. Declared enums/sealed ADTs with `Mirror` remain the normal domain-model choice. Type-only selection, transforms, labels, Mirror conversion and serialization are deferred. The [ordered Coproduct design note](ORDERED_COPRODUCT.md) defines the exact public contract.

## Quality bars

Compile-positive and compile-negative tests, runtime laws where appropriate, type equality assertions, basic compilation-cost observations for larger tuples, and explicit version matrix. Prefer understandable failure messages and maintainable designs over a comprehensive compatibility facade.
