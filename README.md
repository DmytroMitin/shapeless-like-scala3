# Shapeless-Like Scala 3

An experimental Scala 3 library for useful Shapeless 2-style generic and type-level programming capabilities **not adequately covered** by the Scala 3 standard library or Typelevel Shapeless 3.

Started at the [Typelevel Open Source Hack London meetup](https://www.meetup.com/london-scala/events/315196780/) ([Luma](https://luma.com/vzp35rp7)) on 24 October 2026.

## Motivation

Scala 3 has tuples, match types, union types, polymorphic function values, `Mirror` and (on sufficiently recent versions) `NamedTuple`. Typelevel Shapeless 3 has substantial derivation functionality. This project aims to **complement**, not duplicate, those APIs.

Candidate gaps include:
- Ad-hoc polymorphic functions (Shapeless-style `Poly`) and heterogeneous tuple operations.
- Ordered, positional coproducts, including repeated alternative types, versus Scala 3's unordered/idempotent union types.
- Advanced operations over standard `Tuple` and `NamedTuple` and, where justified, labelled unions.
- Explicit interoperability between match-type results and typeclass evidence / value-level implementations.
- Ergonomic `Generic` / `LabelledGeneric` adapters where they demonstrably improve on `Mirror` and Shapeless 3.
- Experiments in contextual search (`Exists`, priority, enumeration, second-best selection, conversion materialization); feasibility is **not assumed**.

## Status

The first implemented vertical slice is an ad-hoc unary `Poly1` registry plus exact mapping of ordinary Scala tuples. It is source-tested on Scala 3.9.0 and targets JVM 17 bytecode. No binary release or compatibility promise has been made yet.

See [scope](docs/PROJECT_SCOPE.md), [architecture](docs/ARCHITECTURE.md), and the [Poly1/tuple Mapper design note](docs/POLY1_TUPLE_MAPPER.md).

## Poly1 tuple mapping

`Poly1` stores one case per exact static input type. `mapWith` applies the corresponding case once at each tuple position and infers the exact heterogeneous output tuple.

```scala
import shapelesslike.poly.{Case, Poly1}
import shapelesslike.tuple.syntax.*

val transform = Poly1.empty
  .add(Case.of[Int](_.toString))
  .add(Case.of[String](_.length))
  .add(Case.of[Boolean](!_))

val mapped: (String, Int, Boolean) =
  (1, "abc", true).mapWith(transform)

assert(mapped == ("1", 3, false))
```

Selection uses the tuple element's static type. An `Int` case does not handle an element typed as `AnyVal`, and an explicitly singleton-typed `1` needs its own `Case.of[1]`. Adding a second case for the same input type is rejected when the `Poly1` is built, even if the two cases have different output types.

The uniqueness proof is intentionally conservative: related keys such as `Int` and `AnyVal` (or `Int` and singleton `1`) cannot coexist in one registry because Scala cannot prove that they remain distinct under substitution. Use separate registries for those exact dispatch domains. Generic registry builders must carry `Absent[...]` evidence explicitly; it is resolved only at a concrete, provably distinct instantiation.

## References

- [Scala 2 Shapeless](https://github.com/milessabin/shapeless)
- [Typelevel Shapeless 3](https://github.com/typelevel/shapeless-3) (derivation, Typeable, testing and related modules)
- [Scala 3 standard library / compiler](https://github.com/scala/scala3)
- [Scala 2 Shapeless Scala 3 port issue](https://github.com/milessabin/shapeless/issues/1043) and [PR](https://github.com/milessabin/shapeless/pull/1200)
- [Scala 3 tuple partitioning and subtype matching example](https://stackoverflow.com/questions/69483366/scala-3-collection-partitioning-with-subtypes)
- [Shapeless 3 and annotations example](https://stackoverflow.com/questions/74355212/shapeless3-and-annotations)
- Related Scala compiler issues: [#17212](https://github.com/scala/scala3/issues/17212), [#24114](https://github.com/scala/scala3/issues/24114)
- Contextual-search investigations: [Exists](https://stackoverflow.com/questions/57482298/is-there-a-type-class-that-checks-for-existence-of-at-least-one-implicit-of-a-ty), [LocalImplicit](https://stackoverflow.com/questions/55462253/create-an-ambiguous-low-priority-implicit), [AllImplicits](https://stackoverflow.com/questions/26723337/using-the-prolog-in-scala-to-find-available-type-class-instances), [SecondBest (1)](https://stackoverflow.com/questions/56777174/finding-the-second-matching-implicit), [SecondBest (2)](https://stackoverflow.com/questions/75806511/how-to-summon-a-given-member).
- Future optional macros: [AUXify-scala3](https://github.com/DmytroMitin/AUXify-scala3)

## Development

Run the complete suite and compile the checked example with:

```text
sbt test
sbt "Test / runMain shapelesslike.examples.poly1TupleMapperExample"
```

The build pins Scala 3.9.0, targets JDK 17, and uses MUnit 1.2.4 for tests. Continue to investigate each new operation's Scala 2 semantics and Scala 3 alternatives before implementation; no binary artifact or broader supported-version range is promised yet.
