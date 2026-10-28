package shapelesslike.tuple.typeclasses
class Forged extends PartitionExact[Tuple1[Int],Int,EmptyTuple,Tuple1[Int]]:
  def apply(input:Tuple1[Int]) = (EmptyTuple,input)
