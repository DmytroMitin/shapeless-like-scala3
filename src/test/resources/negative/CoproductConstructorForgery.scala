package shapelesslike.coproduct

type Alts = Int *: String *: EmptyTuple

val forgedAt = new Coproduct.At[Alts, 0]()
val forgedHere = Coproduct.Here[Int, String *: EmptyTuple](1)
val forgedThere = Coproduct.There[Int, String *: EmptyTuple](
  Coproduct.at[String *: EmptyTuple, 0].inject("wrong outer value")
)
