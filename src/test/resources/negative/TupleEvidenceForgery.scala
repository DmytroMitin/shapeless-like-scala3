package shapelesslike.tuple.typeclasses

import shapelesslike.poly.{Absent, Case}

val forgedSelector =
  new Selector[Case[Int, String] *: EmptyTuple, Int, String]:
    def apply(cases: Case[Int, String] *: EmptyTuple, value: Int): String =
      "forged"

val forgedSelectorHead =
  new Selector.head[Int, String, EmptyTuple]:
    override def apply(
        cases: Case[Int, String] *: EmptyTuple,
        value: Int
    ): String = "forged"

val forgedSelectorTail =
  new Selector.tail[
    String,
    Int,
    Case[Int, String] *: EmptyTuple,
    Int,
    String
  ](using
    summon[Absent[Case[String, Int] *: EmptyTuple, Int]],
    summon[Selector[Case[Int, String] *: EmptyTuple, Int, String]]
  ):
    override def apply(
        cases: Case[String, Int] *: Case[Int, String] *: EmptyTuple,
        value: Int
    ): String = "forged"

val forgedMapper =
  new Mapper[EmptyTuple, EmptyTuple, EmptyTuple]:
    def apply(cases: EmptyTuple, input: EmptyTuple): EmptyTuple = EmptyTuple

val forgedMapperEmpty =
  new Mapper.empty[EmptyTuple]:
    override def apply(cases: EmptyTuple, input: EmptyTuple): EmptyTuple =
      EmptyTuple

val forgedMapperCons =
  new Mapper.cons[
    Case[Int, String] *: EmptyTuple,
    Int,
    EmptyTuple,
    String,
    EmptyTuple
  ](using
    summon[Selector[Case[Int, String] *: EmptyTuple, Int, String]],
    summon[Mapper[Case[Int, String] *: EmptyTuple, EmptyTuple, EmptyTuple]]
  ):
    override def apply(
        cases: Case[Int, String] *: EmptyTuple,
        input: Int *: EmptyTuple
    ): String *: EmptyTuple = "forged" *: EmptyTuple
