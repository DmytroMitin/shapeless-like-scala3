package shapelesslike.coproduct

val forgedEmpty = new Coproduct[EmptyTuple]:
  def ordinal: Int = 0
  def fold[R](handlers: Coproduct.Handlers[EmptyTuple, R]): R = ???

val forgedPosition = new Coproduct.Position[Int *: EmptyTuple, 0, Int]:
  private[coproduct] def inject(value: Int): Coproduct[Int *: EmptyTuple] = ???
  private[coproduct] def project(
      value: Coproduct[Int *: EmptyTuple]
  ): Option[Int] = None
