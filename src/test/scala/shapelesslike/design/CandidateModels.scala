package shapelesslike.design

private[design] object ExplicitModel:
  trait Case[P, A, O]:
    def apply(value: A): O

  object Case:
    def apply[P, A, O](f: A => O): Case[P, A, O] =
      new Case[P, A, O]:
        def apply(value: A): O = f(value)

  trait Mapper[P, In <: Tuple, Out <: Tuple]:
    def apply(input: In): Out

  object Mapper:
    given empty[P]: Mapper[P, EmptyTuple, EmptyTuple] with
      def apply(input: EmptyTuple): EmptyTuple = EmptyTuple

    given cons[P, H, T <: Tuple, OH, OT <: Tuple](using
        headCase: Case[P, H, OH],
        tailMapper: Mapper[P, T, OT]
    ): Mapper[P, H *: T, OH *: OT] with
      def apply(input: H *: T): OH *: OT =
        headCase(input.head) *: tailMapper(input.tail)

  extension [In <: Tuple](input: In)
    def mapWith[P, Out <: Tuple](poly: P)(using mapper: Mapper[P, In, Out]): Out =
      mapper(input)

private[design] object AssociatedModel:
  trait Case[P, A]:
    type Out
    def apply(value: A): Out

  object Case:
    type Aux[P, A, O] = Case[P, A] { type Out = O }

    def apply[P, A, O](f: A => O): Aux[P, A, O] =
      new Case[P, A]:
        type Out = O
        def apply(value: A): O = f(value)

  trait Mapper[P, In <: Tuple]:
    type Out <: Tuple
    def apply(input: In): Out

  object Mapper:
    type Aux[P, In <: Tuple, O <: Tuple] = Mapper[P, In] { type Out = O }

    given empty[P]: Mapper[P, EmptyTuple] with
      type Out = EmptyTuple
      def apply(input: EmptyTuple): EmptyTuple = EmptyTuple

    given cons[P, H, T <: Tuple](using
        headCase: Case[P, H],
        tailMapper: Mapper[P, T]
    ): Mapper[P, H *: T] with
      type Out = headCase.Out *: tailMapper.Out
      def apply(input: H *: T): Out =
        headCase(input.head) *: tailMapper(input.tail)

  extension [In <: Tuple](input: In)
    def mapWith[P](poly: P)(using mapper: Mapper[P, In]): mapper.Out =
      mapper(input)

private[design] object RegistryModel:
  import scala.util.NotGiven

  final class Case[A, O] private (val run: A => O)

  object Case:
    final class Builder[A] private[Case] ():
      def apply[O](f: A => O): Case[A, O] = new Case(f)

    def of[A]: Builder[A] = new Builder[A]

  sealed trait Absent[Cases <: Tuple, A]

  object Absent:
    type Contains[Cases <: Tuple, A] <: Boolean = Cases match
      case EmptyTuple => false
      case Case[A, o] *: tail => true
      case _ *: tail => Contains[tail, A]

    final given derived[Cases <: Tuple, A](using
        Contains[Cases, A] =:= false
    ): Absent[Cases, A] with {}

  final class Poly1[Cases <: Tuple] private (val cases: Cases):
    def add[A, O](entry: Case[A, O])(using Absent[Cases, A]): Poly1[Case[A, O] *: Cases] =
      new Poly1(entry *: cases)

  object Poly1:
    val empty: Poly1[EmptyTuple] = new Poly1(EmptyTuple)

  sealed trait Selector[Cases <: Tuple, A, O]:
    def apply(cases: Cases, value: A): O

  object Selector:
    final given head[A, O, T <: Tuple]: Selector[Case[A, O] *: T, A, O] with
      def apply(cases: Case[A, O] *: T, value: A): O = cases.head.run(value)

    final given tail[H, HO, T <: Tuple, A, O](using
        notSame: NotGiven[A =:= H],
        tailSelector: Selector[T, A, O]
    ): Selector[Case[H, HO] *: T, A, O] with
      def apply(cases: Case[H, HO] *: T, value: A): O =
        tailSelector(cases.tail, value)

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

  extension [In <: Tuple](input: In)
    def mapWith[Cases <: Tuple, Out <: Tuple](poly: Poly1[Cases])(using
        mapper: Mapper[Cases, In, Out]
    ): Out = mapper(poly.cases, input)
