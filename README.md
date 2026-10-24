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

**Research/architecture bootstrap.** There is no published API, build, binary artifact, or implementation yet. The first planned vertical slice is ad-hoc `Poly1` plus tuple mapping, subject to a gap-analysis decision.

See [scope](docs/PROJECT_SCOPE.md), [architectural hypotheses](docs/ARCHITECTURE.md), and [feature candidates](docs/FEATURE_CANDIDATES.md). The planned source and example directories are tracked through [modules/README.md](modules/README.md) and [examples/README.md](examples/README.md).

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

Investigate each operation's Scala 2 semantics and its Scala 3 alternatives before implementation. No compatibility, binary artifacts, or supported Scala version range are promised at this stage. Contributions and design discussions are welcome once a narrow API is specified.
