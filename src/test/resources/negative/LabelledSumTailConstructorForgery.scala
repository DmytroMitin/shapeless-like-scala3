// Expected to fail when compiled alone against the built library.
package shapelesslike.coproduct
object Attack:
  val forged = new LabelledSum.LabelPosition.Tail["y",Tuple1["x"],Int,Tuple1[Int],"x",0,Int]
