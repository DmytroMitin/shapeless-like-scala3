package shapelesslike.record

val forgedEmpty = new RecordSchema.Empty

val forgedCons =
  new RecordSchema.Cons["x", EmptyTuple, Int, EmptyTuple]

val forgedHead =
  new UpdateByName.Head["x", EmptyTuple, Int, EmptyTuple, String]

val forgedTail =
  new UpdateByName.Tail[
    "left",
    "right" *: EmptyTuple,
    Int,
    Int *: EmptyTuple,
    "right",
    String,
    String *: EmptyTuple
  ](summon[
    UpdateByName[
      "right" *: EmptyTuple,
      Int *: EmptyTuple,
      "right",
      String,
      String *: EmptyTuple
    ]
  ])
