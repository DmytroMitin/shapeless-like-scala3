package shapelesslike.examples

import shapelesslike.coproduct.{Coproduct, LabelledSum}
import shapelesslike.coproduct.Coproduct.Handlers

@main def labelledSumExample(): Unit =
  type Names = ("decimal", "text", "hex")
  type Values = (Int, String, Int)
  val hex = LabelledSum.of[Names, Values].inject("hex", 42)
  val selected: Option[Int] = hex.project("hex")
  val missing: Option[Int] = hex.project("decimal")
  val handlers: Handlers[Values, String] =
    ((i: Int) => s"decimal:$i") *:
      ((s: String) => s"text:$s") *:
      ((i: Int) => s"hex:$i") *: EmptyTuple
  assert(selected.contains(42))
  assert(missing.isEmpty)
  assert(hex.ordinal == 2)
  assert(hex.fold[String](handlers) == "hex:42")

  type PrefixedNames = Tuple.Concat[Tuple1["binary"], Names]
  type PrefixedValues = Tuple.Concat[Tuple1[Boolean], Values]
  val prefixed = LabelledSum.of[PrefixedNames, PrefixedValues].inject("hex", 42)
  val stillHex: Option[Int] = prefixed.project("hex")
  assert(stillHex.contains(42))
  assert(prefixed.ordinal == 3)

  val raw: Coproduct[Values] = hex.toCoproduct
  val attached = LabelledSum.fromCoproduct[Names, Values](raw)
  assert(attached.toCoproduct eq raw)
  println(s"hex=$selected decimal=$missing fold=${hex.fold[String](handlers)}")
  println(s"prefix changes ordinal ${hex.ordinal} -> ${prefixed.ordinal}; hex=$stillHex")
