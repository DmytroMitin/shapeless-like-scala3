# Exact static tuple partition

Experimental on Scala 3.9.0, targeting JVM 17. `partitionExact[Q](input)` selects
all elements whose **static type is exactly Q**, returning two ordinary tuples.
Both bucket types are inferred; relative order within each is preserved, and
every original position appears exactly once across the pair.

```scala
import shapelesslike.tuple.syntax.*
val input: (Int, String, Int) = (11, "x", 22)
val inferred = partitionExact[Int](input)
val exact: ((Int, Int), Tuple1[String]) = inferred
assert(exact == ((11, 22), Tuple1("x")))
```

This is a query builder, rather than a receiver extension: supplying Q alone
leaves both output type parameters available for inference at the builder's
`apply`. It coexists with standard tuple methods and `mapWith` under the same
syntax import. Empty input yields two empty tuples; a provably disjoint input
yields empty Yes. Duplicate matching types retain their distinct values.

## Conservative comparison domain

Exact identity differs from subtyping and runtime membership. `Int` is a subtype
of `AnyVal` and `Any`, String of CharSequence, and singleton `1` of Int; none of
those pairs has equal static types. Automatic comparison is intentionally partial:
these related pairs, and unknown abstract pairs, can prevent derivation. For example,
`partitionExact[AnyVal](Tuple1(1))` fails to compile. The error says that an exact
comparison is unresolved or expected buckets are wrong; it does not assert absence.

To retain singleton identity, declare the tuple type explicitly:

```scala
val one: Tuple1[1] = Tuple1(1)
val result: (Tuple1[1], EmptyTuple) = partitionExact[1](one)
// partitionExact[Int](one) does not derive.
```

There is no failed-search fallback, subtype mode, ClassTag, runtime value test,
selector, remover or alignment operation. For fixed schemas, destructuring or
indexing can be simpler. For runtime ADT collections, use pattern matching and
`partitionMap`; widening a tuple's elements to an ADT parent loses static case
information that this operation cannot recover.

## Generic forwarding and evidence ownership

```scala
import shapelesslike.tuple.typeclasses.PartitionExact

def forward[T <: Tuple, Q, Y <: Tuple, N <: Tuple](input: T)(using
    PartitionExact[T, Q, Y, N]
): (Y, N) = partitionExact[Q](input)
```

The invariant, sealed relation associates reconstruction with exact output types.
Its implementations are private and final. Both recursive branches require a
positive Boolean equality proof from an invariant-key match predicate; a stuck
comparison enables neither branch. The comparator is package-restricted plumbing,
not a supported general equality API. Safe Scala clients cannot implement the
relation or invoke its private implementation constructors to choose false buckets.
There are no casts or impossible-state throws in this library kernel; standard
Scala tuple primitives have their own internal implementation.

A carried structural witness does **not** automatically imply a generic equality
between its output and `Tuple.Filter`. No Aux/type-member or parallel match-type
surface is exposed. Scala 3.9 has type-level Filter, Map and FlatMap but no
Tuple.FilterNot alias or corresponding general value-level partition.
See [pinned standard source](https://github.com/scala/scala3/blob/777528f19a58e794c9954a42f433373472ec57f8/library/src/scala/Tuple.scala#L213-L243).
Historical Shapeless 2 [Partition](https://github.com/milessabin/shapeless/blob/6a958474c81b7eef3c602d2ac826a5e881a9e392/core/shared/src/main/scala/shapeless/ops/hlists.scala#L1009-L1061)
provides an exact-type HList precedent; this operation uses standard Scala tuples
and a conservative positive-proof domain.

## Limits and verification

The checked suite covers arities 0, 1, 10 and 22, generic forwarding, inference,
identity/order preservation, expected-output rejection and related/abstract refusals.
A separate inferred arity-32 compile exceeded a 20-second probe budget and was
stopped; it is not qualified by the suite. These are bounded compilation
observations, not a maximum arity or performance
promise. Recursive evidence/search depth grows with tuple size. Standard tuple tail
and prepend can copy data, so no O(n) time or stack-safety claim is made. Other
Scala versions and binary compatibility are unqualified. The independent negative
clients under `src/test/resources/negative/PartitionExact*` check ownership beyond
the in-process compile-negative suite. No release is implied.
