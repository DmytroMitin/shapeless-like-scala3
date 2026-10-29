# Labelled sum view

`shapelesslike.coproduct.LabelledSum[Names, Values]` is an experimental opaque
view over the existing ordered `Coproduct[Values]`. It holds **one active
alternative**. Unique literal string labels address its positions; repeated
payload types remain separate alternatives. A named tuple instead holds all its
fields at once.

Prefer a standard enum or sealed ADT, with `Mirror` for derivation, for a stable
declared domain. This view serves generic code that composes type-level schema
fragments and needs a named address even when prefixing changes an ordinal.
Scala unions discard ordering and duplicate alternatives; Typelevel Shapeless 3
already provides derivation over declared sums. See the pinned
[Scala Mirror API](https://github.com/scala/scala3/blob/777528f19a58e794c9954a42f433373472ec57f8/library/src/scala/deriving/Mirror.scala)
and [Shapeless 3 K0 representation](https://github.com/typelevel/shapeless-3/blob/1d89fac9c7a3af2ee4878171ce745ef122ed3291/modules/deriving/src/main/scala/shapeless3/deriving/kinds.scala#L233-L247).

## Construction, projection and fold

```scala
import shapelesslike.coproduct.LabelledSum
import shapelesslike.coproduct.Coproduct.Handlers

type Names = ("decimal", "text", "hex")
type Values = (Int, String, Int)
val hex = LabelledSum.of[Names, Values].inject("hex", 42)
val selected: Option[Int] = hex.project("hex")     // Some(42)
val missing: Option[Int] = hex.project("decimal")  // None
assert(hex.ordinal == 2)

val handlers: Handlers[Values, String] =
  ((i: Int) => s"decimal:$i") *:
    ((s: String) => s"text:$s") *:
    ((i: Int) => s"hex:$i") *: EmptyTuple
assert(hex.fold[String](handlers) == "hex:42")
```

The literal label fixes the index and exact static payload type. Ordinary calls
infer both; there is no manual ordinal. `project` returns `Some` only when that
position is active. Missing labels and wrong payloads are compile-time errors.
`fold[R]` delegates to the raw carrier: one handler per position in schema order,
exactly one invocation, and separate bodies for equal payload types. Supply `R`
explicitly. Missing, wrong or extra positional handlers are rejected.

## Schema and key requirements

`of` validates the entire schema **before** returning a builder. Both `of` and
`fromCoproduct` require the unchanged `record.RecordSchema[Names, Values]` proof:
equal arity, globally unique concrete literal String names. It proves facts
about aligned type lists; reuse does not make the sum a record. Entry diagnostics
add labelled-sum context to this proof's record-oriented name.

Empty schemas are valid but offer no injection or safe carrier constructor.
Singleton schemas support their only branch. Duplicate names (even with different
payload types), mismatched arity in either direction, non-string names, general
`String`, unions of literals and unresolved abstract singleton names are rejected.
Labels are case-sensitive with no normalization.

Direct string literals and explicitly singleton-typed stable keys work:

```scala
val key: "hex" = "hex"
val stable = LabelledSum.of[Names, Values].inject(key, 42)
val result: Option[Int] = stable.project(key)
```

A runtime `String` or widened `val key = "hex"` is rejected. Related payload
types such as `AnyVal`, `Int` and `1` can coexist under distinct labels because
lookup uses names. Payload inference remains exact: use `(1: AnyVal)` for that
alternative. For a singleton payload, preserve all necessary type arguments, e.g.
`LabelledSum.of[Tuple1["one"], Tuple1[1]].inject["one", 0, 1]("one", 1: 1)`.

## Composition and abstraction

```scala
type PrefixedNames = Tuple.Concat[Tuple1["binary"], Names]
type PrefixedValues = Tuple.Concat[Tuple1[Boolean], Values]
val prefixed = LabelledSum.of[PrefixedNames, PrefixedValues].inject("hex", 42)
assert(prefixed.ordinal == 3)
val stillHex: Option[Int] = prefixed.project("hex")
```

The call-site label stays `"hex"` while the ordinal changes from 2 to 3.
Concatenating `("load", "save")/(String, String)` and
`("retries", "timeout")/(Int, Int)` likewise preserves distinct named positions.
Overlapping names introduced by concatenation fail schema validation. This is
compile-time schema composition, not runtime extensibility.

Generic helpers carry explicit schema, literal, lookup and positional evidence:

```scala
import scala.compiletime.ops.any.IsConst
import shapelesslike.record.RecordSchema
import shapelesslike.coproduct.Coproduct.Position
import shapelesslike.coproduct.LabelledSum.LabelPosition

def inject[N <: Tuple, V <: Tuple, K <: String & Singleton, I <: Int, A](
    key: K, value: A
)(using RecordSchema[N, V], IsConst[K] =:= true,
    LabelPosition[N, V, K, I, A], Position[V, I, A]): LabelledSum[N, V] =
  LabelledSum.of[N, V].inject[K, I, A](key, value)

def project[N <: Tuple, V <: Tuple, K <: String & Singleton, I <: Int, A](
    sum: LabelledSum[N, V], key: K
)(using IsConst[K] =:= true,
    LabelPosition[N, V, K, I, A], Position[V, I, A]): Option[A] =
  sum.project[K, I, A](key)
```

Forward `K` explicitly: inferring `key.type` instead can lose the carried
`IsConst[K]` proof. Projection of an existing validated view does not need to
revalidate its whole schema. `LabelPosition` alone is not a schema validator;
it proves only a structural label/index/payload relation. Its head fixes index
0 and payload; its tail advances the index only with the positive
`(H == K) =:= false` witness. No failed-search inequality or runtime string
comparison is used. The evidence is invariant, sealed and privately implemented;
expected output types cannot redirect lookup to another position.

Neither `LabelPosition` nor `Position` proves the abstract equality
`Tuple.Elem[V, I] =:= A`. Delegation uses `projectAtAs[I, A]`, so no such equality
or cast is required. Generic schema composition must carry proofs for its composed
names and payload tuples; derivation is deliberately conservative under abstraction.

## Raw attachment and value semantics

`hex.toCoproduct` has exactly type `Coproduct[Values]`.
`LabelledSum.fromCoproduct[Names, Values](raw)` requires whole-schema validation
and retains **the same raw reference object**, ordinal and payload. The opaque
type is owned inside `LabelledSum`; safe Scala clients, including code in the same package,
cannot assign raw and labelled types directly or call the private builder
constructor.

Attaching the same raw carrier under another valid name tuple explicitly
reinterprets its positions. Labels are static addresses chosen by the caller,
not trusted runtime provenance or a preserved original label. There is no
implicit rename or conversion between different labelled types.

Universal equality, hashing and display inherit the raw carrier's behavior:
same ordinal/equal payload can compare equal **across different label schemas**.
Those schemas remain different static types. Different positions compare unequal,
but distinct hashes are not guaranteed. Display remains numeric, e.g.
`Coproduct(index=2, value=42)`; it is not label-aware or a serialization format.

## Limits and verification

The view adds no active-carrier wrapper, runtime label array/map, erased payload
storage, unsafe cast, reflection or alternative sum GADT. Builders and evidence
can still allocate, and positional traversal/recursive schema derivation have
costs. Ten labels are covered as a bounded compiler sanity check, not a benchmark
or arbitrary-arity claim.

The implementation and compiled [example](../examples/LabelledSumExample.scala)
are source-tested on Scala 3.9.0 with JDK 17 and JDK 25, targeting JVM 17 bytecode.
All APIs remain experimental; there is no binary release or cross-version promise.
Runtime lookup, enumeration, generic NamedTuple conversion, label-aware Poly1,
transforms, Mirror adapters, codecs and stronger runtime label identity are outside
this surface.
