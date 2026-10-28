package shapelesslike.tuple

import shapelesslike.tuple.syntax.*
import shapelesslike.tuple.typeclasses.PartitionExact

class PartitionExactSuite extends munit.FunSuite:
  test("collects every matching occurrence without expected output steering"):
    val input: (Int, String, Int) = (11, "x", 22)
    val inferred = partitionExact[Int](input)
    def exact[Y <: Tuple, N <: Tuple](value: (Y, N))(using
        Y =:= (Int, Int), N =:= Tuple1[String]
    ): Unit = ()
    exact(inferred)
    val expected: ((Int, Int), Tuple1[String]) = partitionExact[Int](input)
    assertEquals(inferred, ((11, 22), Tuple1("x")))
    assertEquals(expected, inferred)

  test("empty and singleton boundaries including abstract query"):
    def empty[Q]: (EmptyTuple, EmptyTuple) = partitionExact[Q](EmptyTuple)
    val yes: (Tuple1[Int], EmptyTuple) = partitionExact[Int](Tuple1(7))
    val no: (EmptyTuple, Tuple1[String]) = partitionExact[Int](Tuple1("x"))
    assertEquals(empty[Int], (EmptyTuple, EmptyTuple))
    assertEquals(yes, (Tuple1(7), EmptyTuple))
    assertEquals(no, (EmptyTuple, Tuple1("x")))

  test("all or no matches and both bucket orders"):
    val all: ((Int, Int), EmptyTuple) = partitionExact[Int]((1, 2))
    val none: (EmptyTuple, (String, Boolean, String)) = partitionExact[Int](("a", true, "b"))
    val middle: (Tuple1[String], (Int, Boolean)) = partitionExact[String]((1, "m", false))
    val last: (Tuple1[Boolean], (Int, String)) = partitionExact[Boolean]((1, "z", true))
    assertEquals(all, ((1, 2), EmptyTuple))
    assertEquals(none, (EmptyTuple, ("a", true, "b")))
    assertEquals(middle, (Tuple1("m"), (1, false)))
    assertEquals(last, (Tuple1(true), (1, "z")))

  test("preserves payload identity and each position across a pipeline"):
    final case class Request(id: Int)
    final case class Warning(text: String)
    final case class Cache(hit: Boolean)
    val r = Request(1)
    val a = Warning("slow")
    val c = Cache(true)
    val b = Warning("retry")
    val input: (Request, Warning, Cache, Warning) = (r, a, c, b)
    val result: ((Warning, Warning), (Request, Cache)) = partitionExact[Warning](input)
    assert(result._1._1 eq a)
    assert(result._1._2 eq b)
    assert(result._2._1 eq r)
    assert(result._2._2 eq c)
    assertEquals(result, ((Warning("slow"), Warning("retry")), (Request(1), Cache(true))))

  test("generic forwarding and reflexive abstract identity"):
    def forward[T <: Tuple, Q, Y <: Tuple, N <: Tuple](t: T)(using
        PartitionExact[T, Q, Y, N]
    ): (Y, N) = partitionExact[Q](t)
    def same[A](a: A): (Tuple1[A], EmptyTuple) = partitionExact[A](Tuple1(a))
    assertEquals(forward[(Int, String, Int), Int, (Int, Int), Tuple1[String]]((1, "a", 2)),
      ((1, 2), Tuple1("a")))
    assertEquals(same("abstract"), (Tuple1("abstract"), EmptyTuple))

  test("preserved literal identity and subtype facts are separate"):
    summon[1 <:< Int]
    summon[Int <:< AnyVal]
    summon[String <:< CharSequence]
    val input: (1, String) = (1, "x")
    val result: (Tuple1[1], Tuple1[String]) = partitionExact[1](input)
    assertEquals(result, (Tuple1[1](1), Tuple1("x")))

  test("coexists with standard tuple methods and mapWith imports"):
    import shapelesslike.poly.{Case, Poly1}
    val mapped = (1, 2).mapWith(Poly1.empty.add(Case.of[Int](_.toString)))
    assertEquals(partitionExact[String](mapped), (("1", "2"), EmptyTuple))
    assertEquals((1, "x").splitAt(1), (Tuple1(1), Tuple1("x")))

  test("preserves exact tuple arity 10"):
    type Input = Int *: Int *: Int *: Int *: Int *: Int *: Int *: Int *: Int *: Int *: EmptyTuple
    val input: Input = 1 *: 2 *: 3 *: 4 *: 5 *: 6 *: 7 *: 8 *: 9 *: 10 *: EmptyTuple
    val result: (Input, EmptyTuple) = partitionExact[Int](input)
    assertEquals(result._1.toList, (1 to 10).toList)
    assertEquals(result._2, EmptyTuple)

  test("preserves exact tuple arity 22"):
    type Input = Int *: Int *: Int *: Int *: Int *: Int *: Int *: Int *: Int *: Int *: Int *: Int *: Int *: Int *: Int *: Int *: Int *: Int *: Int *: Int *: Int *: Int *: EmptyTuple
    val input: Input = 1 *: 2 *: 3 *: 4 *: 5 *: 6 *: 7 *: 8 *: 9 *: 10 *: 11 *: 12 *: 13 *: 14 *: 15 *: 16 *: 17 *: 18 *: 19 *: 20 *: 21 *: 22 *: EmptyTuple
    val result: (Input, EmptyTuple) = partitionExact[Int](input)
    assertEquals(result._1.toList, (1 to 22).toList)
    assertEquals(result._2, EmptyTuple)
