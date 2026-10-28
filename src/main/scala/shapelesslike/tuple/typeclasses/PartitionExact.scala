package shapelesslike.tuple.typeclasses

import scala.annotation.implicitNotFound

/** Stable partition of all exact static occurrences of Q, with two exact outputs.
 * Related or unresolved types may prevent a decision; no runtime tests are used.
 */
@implicitNotFound(
  "Cannot partition ${In} by exact static type ${Q} into ${Yes}/${No}; an exact comparison is unresolved or the expected buckets are wrong"
)
sealed trait PartitionExact[In <: Tuple, Q, Yes <: Tuple, No <: Tuple]:
  private[tuple] def apply(input: In): (Yes, No)

object PartitionExact:
  private[shapelesslike] final class Key[A] private[PartitionExact] ()
  private[shapelesslike] type Eq[A, B] <: Boolean = Key[A] match
    case Key[B] => true
    case _ => false

  private final class Empty[Q] extends PartitionExact[EmptyTuple, Q, EmptyTuple, EmptyTuple]:
    def apply(input: EmptyTuple): (EmptyTuple, EmptyTuple) = (EmptyTuple, EmptyTuple)

  private final class Hit[H, T <: Tuple, Q, Y <: Tuple, N <: Tuple](
      next: PartitionExact[T, Q, Y, N]
  ) extends PartitionExact[H *: T, Q, H *: Y, N]:
    def apply(input: H *: T): (H *: Y, N) =
      val (yes, no) = next(input.tail)
      (input.head *: yes, no)

  private final class Miss[H, T <: Tuple, Q, Y <: Tuple, N <: Tuple](
      next: PartitionExact[T, Q, Y, N]
  ) extends PartitionExact[H *: T, Q, Y, H *: N]:
    def apply(input: H *: T): (Y, H *: N) =
      val (yes, no) = next(input.tail)
      (yes, input.head *: no)

  given empty[Q]: PartitionExact[EmptyTuple, Q, EmptyTuple, EmptyTuple] = new Empty[Q]

  given hit[H, T <: Tuple, Q, Y <: Tuple, N <: Tuple](using
      same: Eq[H, Q] =:= true,
      next: PartitionExact[T, Q, Y, N]
  ): PartitionExact[H *: T, Q, H *: Y, N] = new Hit(next)

  given miss[H, T <: Tuple, Q, Y <: Tuple, N <: Tuple](using
      different: Eq[H, Q] =:= false,
      next: PartitionExact[T, Q, Y, N]
  ): PartitionExact[H *: T, Q, Y, H *: N] = new Miss(next)
