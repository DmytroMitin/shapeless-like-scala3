package shapelesslike.coproduct

val forgedHead = new Coproduct.Position.Head[Int, EmptyTuple]

val forgedTail = new Coproduct.Position.Tail[
  String,
  Int *: EmptyTuple,
  0,
  Int
](summon[Coproduct.Position[Int *: EmptyTuple, 0, Int]])
