# Poly1 and exact tuple Mapper

## Capability and gap

Scala 3.9.0's standard `Tuple.map` accepts a parametric function of shape `[t] => t => F[t]`. That is ideal when one type constructor describes every result, but it does not model an open set of independently typed cases such as `Int => String`, `String => Int`, and `Boolean => Boolean`. Typelevel Shapeless 3 focuses on generic derivation and does not provide the Shapeless 2 `Poly1` plus heterogeneous tuple mapping surface.

This project therefore implements a narrow operation over ordinary Scala tuples:

- `Case[A, O]` contains one type-safe `A => O` body;
- `Poly1[Cases]` stores a typed tuple of cases;
- `Mapper[Cases, In, Out]` recursively maps `EmptyTuple` and `H *: T`;
- `input.mapWith(poly)` infers `Out` exactly and returns an ordinary tuple.

The implementation is in `shapelesslike.poly`, `shapelesslike.tuple.typeclasses`, and `shapelesslike.tuple.syntax`. The mapper kernel contains no `asInstanceOf`.

## Coherence decision

The selected model is a per-`Poly1` typed registry, not an open set of global case givens. `Poly1.add` requires a match-type proof that the input key is absent, so each constructible registry has at most one case for an exact static input type. Failed implicit search (`NotGiven[A =:= B]`) is deliberately not used as inequality: it would let a generic builder accept abstract `A` and `B`, then create duplicates when both were instantiated as `Int`. Mapper derivation follows the proven-unique registry structure; an externally expected output tuple cannot choose another implementation.

The public type-level result is therefore uniquely determined by the registry type and the static input tuple type for every constructible `Poly1`. `Case` and `Poly1` constructors are restricted to their class/companion owners, so downstream code cannot forge a registry by declaring the library package name. Two equal-shaped `Poly1` values may contain different bodies, but mapping uses the cases stored in the value that was supplied, so bodies cannot leak between identities. Selector and mapper implementation classes are final, preventing indirect subclasses from replacing stored bodies.

Scala 3.9.0 compiler experiments in `CoherenceModelsSuite` also retain the rejected alternatives as executable evidence:

- with `Case[P, A, O]`, conflicting different-output givens are ambiguous without an expected result, but an expected tuple can select either result; same-output named givens in one scope are ambiguous, while sequential wildcard/given imports with colliding generated names silently select the later body;
- with a naïve `Case[P, A] { type Out }` and associated `Mapper.Out`, different-output conflicts are ambiguous in inferred and expected contexts, but same-output sequential imports can still select the later body; moreover the recursive result remains path-dependent and does not satisfy the required exact assignment through an abstract tuple;
- the typed registry rejects duplicate input keys before mapping and preserves exact inference for concrete and abstract tuple inputs.

## Static dispatch and limitations

Case lookup is invariant and exact. `Int`, `AnyVal`, and singleton type `1` are different keys. Ordinary tuple construction usually widens literal elements—for example, `1 *: EmptyTuple` has an `Int` head unless an expected tuple type preserves `1`. There is no subtype fallback, runtime class dispatch, source-order tie-break, or implicit-priority hierarchy.

Registry uniqueness is conservative. The absence match type intentionally remains unresolved for abstract key pairs and for related keys such as `Int`/`AnyVal` and `Int`/`1`; those keys cannot coexist in one registry. A generic builder may accept an explicit `Absent[...]` context parameter, which is resolved at a concrete call only when the key set is provably distinct. This restriction prevents equality that appears only after type substitution from changing the mapper result.

This slice does not cover Poly2, composition, fallback cases, folds, filters, `NamedTuple` name preservation, products through `Mirror`, ordered coproducts, or a macro DSL. The build is pinned to Scala 3.9.0 and targets JVM 17; other Scala versions are not claimed.

## Evidence

- `Poly1MapperSuite` checks exact types, values, duplicates, empty/singleton tuples, abstract tuple methods, evidence-delayed generic registry construction, static input keys, body execution counts, identity isolation, and arities 10, 22, and 32.
- `CompileErrorsSuite` checks missing cases, duplicate keys with same and different outputs, both expected-output choices, wrong expected output, wrong Poly value, widened inputs, singleton inputs, related-key rejection, abstract-key construction, final evidence implementations, and a wrong external generic result.
- `CoherenceModelsSuite` records inferred/expected and same-scope/imported evidence conflicts, selected runtime bodies, generic mapping shapes, identity separation, and the model comparison that selected the registry.
- `src/test/resources/negative/ConstructorForgery.scala` is a separately compiled same-package negative source proving downstream code cannot bypass the `Case` or `Poly1` builders.
- `examples/Poly1TupleMapperExample.scala` is compiled with the test configuration and is runnable with the command in the root README.

Primary references: [Scala 3.9 `Tuple.scala`](https://github.com/scala/scala3/blob/3.9.0/library/src/scala/Tuple.scala), [Typelevel Shapeless 3](https://github.com/typelevel/shapeless-3), and [Scala 2 Shapeless `Poly`](https://github.com/milessabin/shapeless/blob/main/core/shared/src/main/scala/shapeless/poly.scala).
