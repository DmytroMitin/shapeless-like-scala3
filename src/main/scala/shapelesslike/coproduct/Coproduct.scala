package shapelesslike.coproduct

import scala.annotation.implicitNotFound
import scala.compiletime.ops.int.S

/**
 * A value from exactly one position of the ordered alternative tuple `Alts`.
 *
 * Position is part of the value's identity, so repeated alternative types stay
 * distinguishable. The carrier is invariant and has no safe empty constructor.
 */
sealed trait Coproduct[Alts <: Tuple]:
  import Coproduct.{Handlers, Position}

  /** The zero-based position used to construct this value. */
  def ordinal: Int

  /** Projects a concrete literal index to its exact tuple element type. */
  final def projectAt[I <: Int](using
      position: Position[Alts, I, Tuple.Elem[Alts, I]]
  ): Option[Tuple.Elem[Alts, I]] =
    position.project(this)

  /** Projects through explicitly carried evidence when `Alts` or `I` is abstract. */
  final def projectAtAs[I <: Int, A](using
      position: Position[Alts, I, A]
  ): Option[A] =
    position.project(this)

  /** Eliminates this value with one ordered handler per alternative. */
  def fold[R](handlers: Handlers[Alts, R]): R

  final override def equals(other: Any): Boolean = other match
    case that: Coproduct[?] =>
      ordinal == that.ordinal && Coproduct.valueOf(this) == Coproduct.valueOf(that)
    case _ => false

  final override def hashCode(): Int =
    31 * ordinal + Coproduct.valueOf(this).##

  final override def toString: String =
    s"Coproduct(index=$ordinal, value=${Coproduct.valueOf(this)})"

object Coproduct:
  /** One `A => R` handler for each `A` in `Alts`, in positional order. */
  type Handlers[Alts <: Tuple, R] <: Tuple = Alts match
    case EmptyTuple => EmptyTuple
    case h *: t     => (h => R) *: Handlers[t, R]

  private def valueOf(value: Coproduct[?]): Any = value match
    case Here(head)  => head
    case There(tail) => valueOf(tail)

  /**
   * Structural evidence that index `I` of `Alts` has the exact static type `A`.
   *
   * The explicit `A` output remains usable when `Tuple.Elem[Alts, I]` cannot
   * reduce under abstract `Alts` and `I`.
   */
  @implicitNotFound("Index ${I} does not identify a ${A} alternative of ${Alts}")
  sealed trait Position[Alts <: Tuple, I <: Int, A]:
    private[coproduct] def inject(value: A): Coproduct[Alts]
    private[coproduct] def project(value: Coproduct[Alts]): Option[A]

  object Position:
    private final class Head[H, T <: Tuple]
        extends Position[H *: T, 0, H]:
      def inject(value: H): Coproduct[H *: T] = Here(value)

      def project(value: Coproduct[H *: T]): Option[H] = value match
        case Here(head) => Some(head)
        case There(_)   => None

    private final class Tail[H, T <: Tuple, I <: Int, A](
        next: Position[T, I, A]
    ) extends Position[H *: T, S[I], A]:
      def inject(value: A): Coproduct[H *: T] =
        There(next.inject(value))

      def project(value: Coproduct[H *: T]): Option[A] = value match
        case Here(_)     => None
        case There(tail) => next.project(tail)

    given head[H, T <: Tuple]: Position[H *: T, 0, H] =
      new Head[H, T]

    given tail[H, T <: Tuple, I <: Int, A](using
        next: Position[T, I, A]
    ): Position[H *: T, S[I], A] =
      new Tail[H, T, I, A](next)

  /** Begins construction at the literal zero-based index `I`. */
  def at[Alts <: Tuple, I <: Int]: At[Alts, I] =
    new At[Alts, I]

  final class At[Alts <: Tuple, I <: Int] private[Coproduct] ():
    /** Constructs only when `I` points to the exact static value type `A`. */
    def inject[A](value: A)(using
        position: Position[Alts, I, A]
    ): Coproduct[Alts] =
      position.inject(value)

  private final case class Here[H, T <: Tuple](value: H)
      extends Coproduct[H *: T]:
    def ordinal: Int = 0

    def fold[R](handlers: Handlers[H *: T, R]): R =
      handlers.head(value)

  private final case class There[H, T <: Tuple](tail: Coproduct[T])
      extends Coproduct[H *: T]:
    def ordinal: Int = 1 + tail.ordinal

    def fold[R](handlers: Handlers[H *: T, R]): R =
      tail.fold(handlers.tail)
