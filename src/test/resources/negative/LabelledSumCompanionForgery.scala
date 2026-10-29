// Expected to fail when compiled alone against the built library.
package shapelesslike.coproduct
object LabelledSum:
  val raw = Coproduct.at[Tuple1[Int],0].inject(42)
  val forged: _root_.shapelesslike.coproduct.LabelledSum[Tuple1["x"],Tuple1[Int]] = raw
