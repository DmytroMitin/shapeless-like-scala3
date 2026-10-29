// Expected to fail when compiled alone against the built library.
package client
import shapelesslike.coproduct.LabelledSum
object Attack:
  val forged = new LabelledSum.Type[Tuple1["x"],Tuple1[Int]]
