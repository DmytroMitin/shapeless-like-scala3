package shapelesslike.coproduct

object Coproduct:
  type Alts = Int *: String *: EmptyTuple
  val forgedAt =
    new _root_.shapelesslike.coproduct.Coproduct.At[Alts, 0]()
