package shapelesslike.tuple.typeclasses

import scala.annotation.implicitNotFound
import scala.util.NotGiven
import shapelesslike.poly.Case

@implicitNotFound("No case maps the exact static input type ${A} in this Poly1 registry")
sealed trait Selector[Cases <: Tuple, A, O]:
  def apply(cases: Cases, value: A): O

object Selector:
  final given head[A, O, T <: Tuple]: Selector[Case[A, O] *: T, A, O] with
    def apply(cases: Case[A, O] *: T, value: A): O =
      cases.head.run(value)

  final given tail[H, HO, T <: Tuple, A, O](using
      notSame: NotGiven[A =:= H],
      tailSelector: Selector[T, A, O]
  ): Selector[Case[H, HO] *: T, A, O] with
    def apply(cases: Case[H, HO] *: T, value: A): O =
      tailSelector(cases.tail, value)

/** Executable evidence that maps `In` to the exact tuple type `Out`. */
@implicitNotFound(
  "Cannot map tuple ${In} with this Poly1 registry to ${Out}; a case is missing or the expected output is wrong"
)
sealed trait Mapper[Cases <: Tuple, In <: Tuple, Out <: Tuple]:
  def apply(cases: Cases, input: In): Out

object Mapper:
  final given empty[Cases <: Tuple]: Mapper[Cases, EmptyTuple, EmptyTuple] with
    def apply(cases: Cases, input: EmptyTuple): EmptyTuple = EmptyTuple

  final given cons[Cases <: Tuple, H, T <: Tuple, OH, OT <: Tuple](using
      selector: Selector[Cases, H, OH],
      tailMapper: Mapper[Cases, T, OT]
  ): Mapper[Cases, H *: T, OH *: OT] with
    def apply(cases: Cases, input: H *: T): OH *: OT =
      selector(cases, input.head) *: tailMapper(cases, input.tail)
