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

This slice is for anonymous generic alternative tuples whose occurrence identity matters. Declared enums/sealed ADTs with `Mirror` remain the normal domain-model choice. Type-only selection, transforms, Mirror conversion and serialization are deferred. The [ordered Coproduct design note](ORDERED_COPRODUCT.md) defines the exact public contract.

### Implemented labelled sum view

`LabelledSum[Names, Values]` is an opaque view over the original `Coproduct[Values]`:

- unchanged `RecordSchema` validates equal arity and globally unique literal String labels before builder creation or raw attachment;
- sealed invariant `LabelPosition[N,V,K,I,A]` maps a label to its exact position/payload with positive head/tail evidence;
- existing `Position` performs injection and `projectAtAs` projection; ordinal and exhaustive positional fold delegate to the raw carrier;
- explicit unwrap/validated attach preserve the same raw reference with no extra active-carrier wrapper;
- equality/hash/display inherit ordinal/payload behavior and may coincide across different static label schemas.

Names provide stable addresses under type-level prefix composition. They are not
runtime provenance. An enum/ADT remains preferable for a fixed domain. The
[labelled sum contract](LABELLED_SUM.md) records literal/generic constraints and
the explicit relabelling/equality boundary. Broader labelled transforms and
runtime metadata remain deferred.

### Implemented NamedTuple update slice

`shapelesslike.record` adds one operation over the standard Scala 3 named-tuple
representation:

- `RecordSchema[N,V]` positively proves lockstep arity, concrete literal string
  names, and global name uniqueness;
- `UpdateByName[N,V,K,B,OutV]` walks names and values together and computes the
  exact changed value tuple, using positive type-level inequality for tail
  search;
- `record.updatedByName(key, replacement)` preserves the name tuple, order, and
  untouched values while returning standard `NamedTuple[N,OutV]`;
- evidence implementations are owner-controlled, and the tuple reconstruction
  kernel is cast-free with no runtime name lookup.

This is replacement of a present field, not upsert. Dynamic keys, malformed
low-level schemas, other record transforms, macros, `Mirror` conversion, and a
parallel record representation are excluded. The [NamedTuple update note](NAMEDTUPLE_UPDATE.md)
defines the exact domain and limitations.

### Exact static tuple partition

`PartitionExact[In,Q,Yes,No]` is invariant structural executable evidence for
all-occurrence, stable partition into standard tuples. Both hit and miss require
positive proofs from a package-restricted partial invariant-key comparator.
Private final implementations close witness construction. `partitionExact[Q](input)`
in tuple syntax infers both outputs through a query builder. Related/abstract
comparisons can be stuck and cause refusal; no failed-search or runtime fallback
exists. There is no generic Filter equality claim or additional match-type API.
See the [contract](TUPLE_PARTITION_EXACT.md).

## Quality bars

Compile-positive and compile-negative tests, runtime laws where appropriate, type equality assertions, basic compilation-cost observations for larger tuples, and explicit version matrix. Prefer understandable failure messages and maintainable designs over a comprehensive compatibility facade.
