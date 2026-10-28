package shapelesslike.examples

import shapelesslike.tuple.syntax.*

@main def tuplePartitionExactExample(): Unit =
  final case class Request(id: Int)
  final case class Warning(message: String)
  final case class Cache(hit: Boolean)

  val input: (Request, Warning, Cache, Warning) =
    (Request(1), Warning("slow"), Cache(true), Warning("retry"))
  val result: ((Warning, Warning), (Request, Cache)) = partitionExact[Warning](input)
  assert(result._1 == (Warning("slow"), Warning("retry")))
  assert(result._2 == (Request(1), Cache(true)))
  println(s"warnings=${result._1}, remaining=${result._2}")
