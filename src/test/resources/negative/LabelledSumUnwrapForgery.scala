// Expected to fail when compiled alone against the built library.
package shapelesslike.coproduct
object UnwrapForgery:
  val sum = LabelledSum.of[Tuple1["x"],Tuple1[Int]].inject("x",42)
  val forged: Coproduct[Tuple1[Int]] = sum
