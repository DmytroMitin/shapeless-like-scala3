package shapelesslike.record

object RecordSchema:
  val forged: _root_.shapelesslike.record.RecordSchema[("x", "x"), (Int, Int)] =
    new _root_.shapelesslike.record.RecordSchema[("x", "x"), (Int, Int)] {}

object UpdateByName:
  val forged =
    new _root_.shapelesslike.record.UpdateByName[
      "x" *: EmptyTuple,
      Int *: EmptyTuple,
      "x",
      String,
      String *: EmptyTuple
    ]:
      private[record] def apply(
          values: Int *: EmptyTuple,
          replacement: String
      ): String *: EmptyTuple = replacement *: EmptyTuple
