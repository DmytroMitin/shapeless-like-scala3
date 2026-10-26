# Indexed ordered Coproduct

## Capability and boundary

`shapelesslike.coproduct.Coproduct` is an experimental positional sum over an ordinary Scala type tuple. Unlike a Scala union, it preserves order, multiplicity, and the exact occurrence that was constructed:

```scala
import shapelesslike.coproduct.Coproduct
import shapelesslike.coproduct.Coproduct.Handlers

type Alts = Int *: String *: Int *: EmptyTuple

val first = Coproduct.at[Alts, 0].inject(7)
val last = Coproduct.at[Alts, 2].inject(7)

val firstAt0: Option[Int] = first.projectAt[0]
val firstAt2: Option[Int] = first.projectAt[2]
val lastAt0: Option[Int] = last.projectAt[0]
val lastAt2: Option[Int] = last.projectAt[2]
```

`firstAt0` and `lastAt2` are `Some(7)`; the other two projections are `None`. The carrier is invariant. There is no safe public constructor for `Coproduct[EmptyTuple]`.

This is a narrow infrastructure primitive for code that is generic in an alternative tuple. Prefer an enum or sealed ADT for a stable named domain, a union when occurrence identity is irrelevant, and `Either` for a naturally binary choice.

## Construction and projection

Construction is by zero-based type-level index:

```scala
Coproduct.at[Alts, 0].inject(7)
Coproduct.at[Alts, 1].inject("seven")
Coproduct.at[Alts, 2].inject(7)
```

`Position[Alts, I, A]` structurally proves that index `I` has the exact static type `A`. Wrong value types, negative indices, out-of-range indices, and empty-list construction fail to compile. There is deliberately no type-only injection, first-match rule, subtype fallback, or priority search.

For concrete tuples and literal indices, `projectAt[I]` returns `Option[Tuple.Elem[Alts, I]]`. Generic code should carry the explicit witness and use `projectAtAs[I, A]` because `Tuple.Elem[Alts, I]` need not reduce while both parameters are abstract:

```scala
import shapelesslike.coproduct.Coproduct.Position

def injectAt[As <: Tuple, I <: Int, A](value: A)(using
    Position[As, I, A]
): Coproduct[As] =
  Coproduct.at[As, I].inject(value)

def projectAt[As <: Tuple, I <: Int, A](value: Coproduct[As])(using
    Position[As, I, A]
): Option[A] =
  value.projectAtAs[I, A]
```

Static types are exact. For `AnyVal *: Int *: EmptyTuple`, inject the first alternative with `(1: AnyVal)`. For `1 *: String *: EmptyTuple`, ordinary method inference widens a literal, so preserve the singleton and supply it explicitly:

```scala
val one: 1 = 1
val singleton = Coproduct.at[1 *: String *: EmptyTuple, 0].inject[1](one)
```

## Exhaustive positional fold

`Handlers[Alts, R]` contains one `A => R` function per position. Handler position, not input type alone, distinguishes repeated alternatives:

```scala
val handlers: Handlers[Alts, String] =
  ((i: Int) => s"first:$i") *:
    ((s: String) => s"text:$s") *:
    ((i: Int) => s"second:$i") *:
    EmptyTuple

val result: String = last.fold[String](handlers)
```

`result` is `"second:7"`. Missing, extra, wrong-input, and wrong-result handlers fail to compile. Supply `R` explicitly; inference through the handler match type is not promised. A common supertype or explicit union may be used for different branch outputs. Exactly one handler executes.

The existing `Poly1` registry is keyed only by input type, so it cannot provide different bodies for the two `Int` positions without an additional position/tag. Positional Poly1 integration is not part of this API.

## Representation and value semantics

The implementation is a private nested `Here`/`There` GADT behind the sealed public carrier. Structural `Position` evidence is owner-controlled; the node constructors and evidence implementations are private. Construction, projection, ordinal calculation, folding, equality, hashing, and display contain no unchecked cast, `@unchecked`, reflection, or impossible-state fallback.

Within the same `Alts`, equality compares ordinal and contained value. Equal same-position values hash equally; ordinal contributes to the hash, but different positions are not promised distinct hashes. `toString` includes the numeric ordinal and value, such as `Coproduct(index=2, value=7)`, but is not a serialization format. Static schema identity across different erased `Alts` is not promised by universal equality.

Position `I` uses one head node plus `I` tail nodes and traverses that structure for ordinal, projection, and folding. Ten alternatives are compile-tested, but this is not a benchmark or a large-arity performance guarantee.

## Current limitations

The implemented slice excludes type-directed selection, transformations (`map`, `remove`, `partition`, `align`, and reorder), labels, `Mirror` conversion, serialization, macros, and cross-version or binary-compatibility guarantees. It is tested on Scala 3.9.0, targets JVM 17 bytecode, and remains experimental.
