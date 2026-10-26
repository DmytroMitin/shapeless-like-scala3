package shapelesslike.examples

import shapelesslike.coproduct.Coproduct
import shapelesslike.coproduct.Coproduct.{Handlers, Position}

private object OrderedDecoderModel:
  trait Decoder[A]

  type Outputs[Ds <: Tuple] <: Tuple = Ds match
    case EmptyTuple         => EmptyTuple
    case Decoder[a] *: tail => a *: Outputs[tail]

  def success[Ds <: Tuple, I <: Int, A](value: A)(using
      Position[Outputs[Ds], I, A]
  ): Coproduct[Outputs[Ds]] =
    Coproduct.at[Outputs[Ds], I].inject(value)

@main def orderedCoproductExample(): Unit =
  import OrderedDecoderModel.*

  type Decoders = Decoder[Int] *: Decoder[String] *: Decoder[Int] *: EmptyTuple
  type Results = Outputs[Decoders]

  val decoded = success[Decoders, 2, Int](42)
  val handlers: Handlers[Results, String] =
    ((i: Int) => s"decimal:$i") *:
      ((s: String) => s"quoted:$s") *:
      ((i: Int) => s"hex:$i") *:
      EmptyTuple

  println(decoded.fold[String](handlers))
