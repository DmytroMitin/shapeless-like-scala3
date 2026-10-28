package shapelesslike.tuple

import scala.compiletime.testing.typeCheckErrors

class PartitionExactCompileErrorsSuite extends munit.FunSuite:
  test("unknown head"):
    val errors = typeCheckErrors("""
      import shapelesslike.tuple.syntax.*
      import shapelesslike.tuple.typeclasses.PartitionExact
      def bad[A](a:A) = partitionExact[Int]((a,1))
    """)
    assert(errors.nonEmpty)
    val messages = errors.map(_.message).mkString("\n")
    assert(messages.contains("Cannot partition"), messages)

  test("unknown query"):
    val errors = typeCheckErrors("""
      import shapelesslike.tuple.syntax.*
      import shapelesslike.tuple.typeclasses.PartitionExact
      def bad[Q](t:(Int,String)) = partitionExact[Q](t)
    """)
    assert(errors.nonEmpty)
    val messages = errors.map(_.message).mkString("\n")
    assert(messages.contains("Cannot partition"), messages)

  test("Int AnyVal"):
    val errors = typeCheckErrors("""
      import shapelesslike.tuple.syntax.*
      import shapelesslike.tuple.typeclasses.PartitionExact
      val bad = partitionExact[AnyVal](Tuple1(1))
    """)
    assert(errors.nonEmpty)
    val messages = errors.map(_.message).mkString("\n")
    assert(messages.contains("Cannot partition"), messages)

  test("Int Any"):
    val errors = typeCheckErrors("""
      import shapelesslike.tuple.syntax.*
      import shapelesslike.tuple.typeclasses.PartitionExact
      val bad = partitionExact[Any](Tuple1(1))
    """)
    assert(errors.nonEmpty)
    val messages = errors.map(_.message).mkString("\n")
    assert(messages.contains("Cannot partition"), messages)

  test("String CharSequence"):
    val errors = typeCheckErrors("""
      import shapelesslike.tuple.syntax.*
      import shapelesslike.tuple.typeclasses.PartitionExact
      val bad = partitionExact[CharSequence](Tuple1("s"))
    """)
    assert(errors.nonEmpty)
    val messages = errors.map(_.message).mkString("\n")
    assert(messages.contains("Cannot partition"), messages)

  test("literal versus Int"):
    val errors = typeCheckErrors("""
      import shapelesslike.tuple.syntax.*
      import shapelesslike.tuple.typeclasses.PartitionExact
      val in: Tuple1[1] = Tuple1(1); val bad = partitionExact[Int](in)
    """)
    assert(errors.nonEmpty)
    val messages = errors.map(_.message).mkString("\n")
    assert(messages.contains("Cannot partition"), messages)

  test("wrong arity"):
    val errors = typeCheckErrors("""
      import shapelesslike.tuple.syntax.*
      import shapelesslike.tuple.typeclasses.PartitionExact
      val bad: (Tuple1[Int], (String,Int)) = partitionExact[Int]((1,"x",2))
    """)
    assert(errors.nonEmpty)
    val messages = errors.map(_.message).mkString("\n")
    assert(messages.contains("Cannot partition"), messages)

  test("wrong bucket"):
    val errors = typeCheckErrors("""
      import shapelesslike.tuple.syntax.*
      import shapelesslike.tuple.typeclasses.PartitionExact
      val bad = summon[PartitionExact[Tuple1[Int],Int,EmptyTuple,Tuple1[Int]]]
    """)
    assert(errors.nonEmpty)
    val messages = errors.map(_.message).mkString("\n")
    assert(messages.contains("Cannot partition"), messages)

  test("cannot treat unrelated as hit"):
    val errors = typeCheckErrors("""
      import shapelesslike.tuple.syntax.*
      import shapelesslike.tuple.typeclasses.PartitionExact
      val bad = summon[PartitionExact[Tuple1[String],Int,Tuple1[String],EmptyTuple]]
    """)
    assert(errors.nonEmpty)
    val messages = errors.map(_.message).mkString("\n")
    assert(messages.contains("Cannot partition"), messages)

  test("cannot skip earlier equal"):
    val errors = typeCheckErrors("""
      import shapelesslike.tuple.syntax.*
      import shapelesslike.tuple.typeclasses.PartitionExact
      val bad = summon[PartitionExact[(Int,Int),Int,Tuple1[Int],Tuple1[Int]]]
    """)
    assert(errors.nonEmpty)
    val messages = errors.map(_.message).mkString("\n")
    assert(messages.contains("Cannot partition"), messages)

  test("cannot steer abstract prefix"):
    val errors = typeCheckErrors("""
      import shapelesslike.tuple.syntax.*
      import shapelesslike.tuple.typeclasses.PartitionExact
      def bad[A](a:A): (Tuple1[Int],Tuple1[A]) = partitionExact[Int]((a,1))
    """)
    assert(errors.nonEmpty)
    val messages = errors.map(_.message).mkString("\n")
    assert(messages.contains("Cannot partition"), messages)

  test("cannot widen dispatch via expectation"):
    val errors = typeCheckErrors("""
      import shapelesslike.tuple.syntax.*
      import shapelesslike.tuple.typeclasses.PartitionExact
      val bad: (EmptyTuple,(Any,String,Any)) = partitionExact[Int]((1,"x",2))
    """)
    assert(errors.nonEmpty)
    val messages = errors.map(_.message).mkString("\n")
    assert(messages.contains("Cannot partition"), messages)

  test("cannot implement sealed evidence"):
    val errors = typeCheckErrors("""
      import shapelesslike.tuple.syntax.*
      import shapelesslike.tuple.typeclasses.PartitionExact
      val bad = new PartitionExact[EmptyTuple,Int,EmptyTuple,EmptyTuple] {}
    """)
    assert(errors.nonEmpty)
    val messages = errors.map(_.message).mkString("\n")
    assert(messages.contains("sealed"), messages)

  test("cannot access implementation"):
    val errors = typeCheckErrors("""
      import shapelesslike.tuple.syntax.*
      import shapelesslike.tuple.typeclasses.PartitionExact
      val bad = new PartitionExact.Empty[Int]
    """)
    assert(errors.nonEmpty)
    val messages = errors.map(_.message).mkString("\n")
    assert(messages.contains("cannot be accessed"), messages)

  test("cannot subclass generated given"):
    val errors = typeCheckErrors("""
      import shapelesslike.tuple.syntax.*
      import shapelesslike.tuple.typeclasses.PartitionExact
      class Bad extends PartitionExact.hit[Int,EmptyTuple,Int,EmptyTuple,EmptyTuple]
    """)
    assert(errors.nonEmpty)
    val messages = errors.map(_.message).mkString("\n")
    assert(messages.contains("hit"), messages)

  test("abstract evidence has no Filter bridge"):
    val errors = typeCheckErrors("""
      import shapelesslike.tuple.syntax.*
      import shapelesslike.tuple.typeclasses.PartitionExact
      def bad[T <: Tuple,Q,Y <: Tuple,N <: Tuple](using PartitionExact[T,Q,Y,N]) =
  summon[Y =:= Tuple.Filter[T,[a] =>> PartitionExact.Eq[a,Q]]]
    """)
    assert(errors.nonEmpty)
    val messages = errors.map(_.message).mkString("\n")
    assert(messages.contains("Cannot prove"), messages)
