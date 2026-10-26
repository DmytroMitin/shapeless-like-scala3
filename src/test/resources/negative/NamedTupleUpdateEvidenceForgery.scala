package shapelesslike.record

val forgedSchema = new RecordSchema[("x", "x"), (Int, Int)] {}

val forgedUpdate =
  new UpdateByName[("x", "y"), (Int, Int), "x", String, (String, Int)]:
    private[record] def apply(
        values: (Int, Int),
        replacement: String
    ): (String, Int) = (replacement, values._2)
