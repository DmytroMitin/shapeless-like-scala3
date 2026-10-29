// Expected to fail when compiled alone against the built library.
package shapelesslike.coproduct
object Attack:
  val forged = new `LabelledSum$package$LabelledSum$Builder`[("x", "x"), (Int, Int)]()
