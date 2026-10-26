package shapelesslike.tuple

import shapelesslike.poly.Poly1
import shapelesslike.tuple.typeclasses.Mapper

object syntax:
  extension [In <: Tuple](input: In)
    /** Maps each position using the case keyed by its exact static input type. */
    def mapWith[Cases <: Tuple, Out <: Tuple](poly: Poly1[Cases])(using
        mapper: Mapper[Cases, In, Out]
    ): Out =
      mapper(poly.cases, input)
