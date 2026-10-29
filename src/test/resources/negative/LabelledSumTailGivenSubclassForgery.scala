// Expected to fail when compiled alone against the built library.
package shapelesslike.coproduct
class Forged extends LabelledSum.LabelPosition.tail["y",Tuple1["x"],Int,Tuple1[Int],"x",0,Int]
