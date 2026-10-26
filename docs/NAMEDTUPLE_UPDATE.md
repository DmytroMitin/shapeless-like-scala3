# NamedTuple `updatedByName`

## Capability and boundary

`shapelesslike.record.syntax.updatedByName` is an experimental operation over
the standard Scala 3 `NamedTuple`. It replaces one existing field selected by a
compile-time literal name and computes the exact changed value type:

```scala
import shapelesslike.record.syntax.*

type Raw = (name: String, age: String, active: Boolean)
val raw: Raw = (name = "Ada", age = "37", active = true)

val parsed: (name: String, age: Int, active: Boolean) =
  raw.updatedByName("age", raw.age.toInt)
```

The result is another standard named tuple. Names, order, cardinality, and all
untouched values and types are preserved. The replacement may keep or change
the selected field's type. A missing field is a compile-time error: this method
does not append or upsert.

Use direct named-tuple reconstruction for one closed, known schema. This helper
is intended for code that is generic in the schema and needs one transparent
by-name replacement. It is not a record wrapper, object mapper, optics system,
or dynamic map.

## Literal keys and exact inference

A direct literal and an explicitly singleton-typed stable value are supported:

```scala
val age: "age" = "age"
val direct = raw.updatedByName("age", 37)
val stable = raw.updatedByName(age, 37)
```

An arbitrary runtime `String`, including a widened `val age = "age"`, is
rejected. Field lookup is entirely compile-time; there is no runtime string
comparison or missing-key branch.

The output value tuple is inferred exactly, with or without an expected result.
An incompatible expected result cannot steer evidence search to a different
field or replacement type.

## Supported schema proof

Every call requires library-derived `RecordSchema[N, V]` evidence. This proof
defines the supported low-level domain:

- names and values have equal arity;
- every name is a concrete compile-time string literal;
- names are globally distinct.

This check matters because the general low-level `NamedTuple.apply[N, V]` and
`withNames[N]` entry points can express some schemas that ordinary named-tuple
syntax rejects. Duplicate names, mismatched arity, general `String`, non-string
names, unresolved abstract names, singleton paths, and literal-name unions do
not receive schema evidence. The empty schema is valid, but it has no field to
update.

`RecordSchema` uses positive `IsConst` and `Tuple.Contains` equality witnesses.
`UpdateByName` consumes names and values in lockstep; a tail step requires the
positive proof `(H == K) =:= false`. Neither relation treats failed implicit
lookup as proof of inequality. Their implementations are sealed and private so
ordinary safe client code can carry evidence but cannot forge it.

## Generic use

Abstract code carries both public evidence relations explicitly:

```scala
import scala.NamedTuple.NamedTuple
import shapelesslike.record.{RecordSchema, UpdateByName}
import shapelesslike.record.syntax.*

def update[
    N <: Tuple,
    V <: Tuple,
    K <: String & Singleton,
    B,
    OutV <: Tuple
](record: NamedTuple[N, V], key: K, replacement: B)(using
    RecordSchema[N, V],
    UpdateByName[N, V, K, B, OutV]
): NamedTuple[N, OutV] =
  record.updatedByName[K, B, OutV](key, replacement)
```

Explicit type arguments in the forwarding call preserve the abstract `K`
instead of accidentally asking for evidence about `key.type`.

## Representation, safety, and cost

The updater reconstructs an ordinary tuple prefix with `head`, `tail`, and `*:`
and wraps the exact output through standard `NamedTuple.apply`. The kernel has
no cast, unchecked match, reflection, erased `Any` payload, `null`, impossible
throw, macro, compiled field-name array, or custom record value.

Runtime reconstruction is linear in the selected field position and allocates
that tuple prefix. Ten fields are compile-tested as a sanity check, not a
benchmark or arbitrary-arity performance claim.

## Current limitations

The operation does not support runtime names, missing-field append, remove,
rename, projection/reorder, merge, deep paths, callback modification, case-class
conversion, or `Mirror` integration. It is tested on Scala 3.9.0, targets JVM 17
bytecode, and carries no compatibility promise for other Scala versions.
