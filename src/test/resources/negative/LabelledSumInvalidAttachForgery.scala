// Expected to fail when compiled alone against the built library.
package client
import shapelesslike.coproduct.{Coproduct,LabelledSum}
object Attack:
  val raw = Coproduct.at[(Int,Int),0].inject(42)
  val forged = LabelledSum.fromCoproduct[("x","x"),(Int,Int)](raw)
