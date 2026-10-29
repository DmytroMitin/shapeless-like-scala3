// Expected to fail when compiled alone against the built library.
package shapelesslike.coproduct
class Forged extends LabelledSum.LabelPosition[Tuple1["x"],Tuple1[Int],"x",0,Int]
