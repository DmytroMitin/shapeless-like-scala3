package shapelesslike.poly

import scala.annotation.implicitNotFound

/** One statically typed case in a [[Poly1]] registry. */
final class Case[A, O] private[Case] (private[shapelesslike] val run: A => O)

object Case:
  /** Starts construction of a case whose exact static input type is `A`. */
  def of[A]: Builder[A] = new Builder[A]

  final class Builder[A] private[Case] ():
    def apply[O](f: A => O): Case[A, O] = new Case(f)

/** Evidence that `Cases` is proven not to contain an input key `A`. */
@implicitNotFound("Poly1 already contains a case for the static input type ${A}")
sealed trait Absent[Cases <: Tuple, A]

object Absent:
  type Contains[Cases <: Tuple, A] <: Boolean = Cases match
    case EmptyTuple => false
    case Case[A, o] *: tail => true
    case _ *: tail => Contains[tail, A]

  final given derived[Cases <: Tuple, A](using
      Contains[Cases, A] =:= false
  ): Absent[Cases, A] with {}

/** A value-level registry of unary cases, keyed by exact static input type. */
final class Poly1[Cases <: Tuple] private[Poly1] (private[shapelesslike] val cases: Cases):
  /**
   * Adds a case when its key is proven distinct from every existing key.
   * Related or unresolved abstract key types are conservatively rejected unless
   * the caller carries substitution-stable [[Absent]] evidence.
   */
  def add[A, O](entry: Case[A, O])(using Absent[Cases, A]): Poly1[Case[A, O] *: Cases] =
    new Poly1(entry *: cases)

object Poly1:
  /** An empty case registry. */
  val empty: Poly1[EmptyTuple] = new Poly1(EmptyTuple)
