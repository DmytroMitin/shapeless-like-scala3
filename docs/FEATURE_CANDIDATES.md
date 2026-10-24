# Candidate feature inventory (unverified)

This is a **hypothesis list**, not evidence that features are missing. Prompt 001 must turn it into a sourced feature-gap matrix.

| Family | Representative candidates | Key question |
| --- | --- | --- |
| Poly | Poly1/Poly2, typed Case, Case.Aux, composition | What is missing beyond Scala 3 polymorphic functions and typeclasses? |
| Tuple | Mapper, Collect, Partition, Selector, Fold, ZipWith | Which are not already provided or simply expressed using standard Tuple and match types? |
| Coproduct | Ordered Inl/Inr or indexed alternative, Inject, Select, Fold, Align | How to preserve position and repeated types with safe conversions? |
| Record | SelectAll, Remove, Update, Rename, Merge, MapValues | Which operations are missing from stable NamedTuple and other libraries? |
| Labelled sum | Keyed alternatives, rename, select, map | What exactly was Scala 2 Shapeless labelled Union, and what semantics are useful now? |
| Generic | Product/sum to/from, labelled representation | Where does this improve on Mirror or Shapeless 3 derivation? |
| Implicit search | Exists, LowPriority, LocalImplicit, AllImplicits, SecondBest, implicit-conversion materialisation | Which mechanisms are possible under Scala 3 search rules, and under what restrictions? |

For every feature, record original Shapeless 2 source/API, current Scala 3 baseline/version, Shapeless 3 coverage, minimal real-world use case, proof-of-gap, implementation strategy, limitations and a disposition.

Primary sources:
- https://github.com/milessabin/shapeless
- https://github.com/typelevel/shapeless-3
- https://github.com/scala/scala3
- original Stack Overflow and compiler links in the root README.
