package shapelesslike.examples

import shapelesslike.poly.{Case, Poly1}
import shapelesslike.tuple.syntax.*

@main def poly1TupleMapperExample(): Unit =
  val transform = Poly1.empty
    .add(Case.of[Int](_.toString))
    .add(Case.of[String](_.length))
    .add(Case.of[Boolean](!_))

  val mapped: (String, Int, Boolean) =
    (1, "abc", true).mapWith(transform)

  println(mapped)
