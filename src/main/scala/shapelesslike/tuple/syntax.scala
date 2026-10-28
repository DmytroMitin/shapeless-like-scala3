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

  /** Starts an exact-static partition query; both result tuple types are inferred. */
  def partitionExact[Q]: PartitionQuery[Q] = new PartitionQuery[Q]

  final class PartitionQuery[Q] private[syntax] ():
    def apply[In <: Tuple, Yes <: Tuple, No <: Tuple](input: In)(using
        partition: shapelesslike.tuple.typeclasses.PartitionExact[In, Q, Yes, No]
    ): (Yes, No) = partition(input)
