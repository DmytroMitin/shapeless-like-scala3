package shapelesslike

import shapelesslike.poly.{Absent, Case, Poly1}
import shapelesslike.tuple.syntax.*
import shapelesslike.tuple.typeclasses.Mapper

class Poly1MapperSuite extends munit.FunSuite:
  test("maps a heterogeneous tuple with an exact result type"):
    val transform = Poly1.empty
      .add(Case.of[Int](_.toString))
      .add(Case.of[String](_.length))
      .add(Case.of[Boolean](!_))

    val result = (1 *: "abc" *: true *: EmptyTuple).mapWith(transform)
    val exact: String *: Int *: Boolean *: EmptyTuple = result

    assertEquals(exact, "1" *: 3 *: false *: EmptyTuple)

  test("preserves duplicate positions and supports an expected exact output"):
    val transform = Poly1.empty
      .add(Case.of[Int](_.toString))
      .add(Case.of[String](_.length))

    val result: String *: Int *: String *: EmptyTuple =
      (1 *: "ab" *: 3 *: EmptyTuple).mapWith(transform)

    assertEquals(result, "1" *: 2 *: "3" *: EmptyTuple)

  test("maps EmptyTuple without requiring a case and maps a singleton exactly"):
    val empty = EmptyTuple.mapWith(Poly1.empty)
    val intToString = Poly1.empty.add(Case.of[Int](_.toString))
    val singleton: String *: EmptyTuple = (1 *: EmptyTuple).mapWith(intToString)

    assertEquals(empty, EmptyTuple)
    assertEquals(singleton, "1" *: EmptyTuple)

  test("maps an abstract tuple through explicit Mapper evidence"):
    def mapped[C <: Tuple, T <: Tuple, O <: Tuple](poly: Poly1[C], input: T)(using
        Mapper[C, T, O]
    ): O = input.mapWith(poly)

    val transform = Poly1.empty
      .add(Case.of[Int](_.toString))
      .add(Case.of[String](_.length))

    val inferred = mapped(transform, 1 *: "abcd" *: EmptyTuple)
    val exactInferred: String *: Int *: EmptyTuple = inferred
    val result: String *: Int *: EmptyTuple = mapped(transform, 1 *: "abcd" *: EmptyTuple)

    assertEquals(exactInferred, "1" *: 4 *: EmptyTuple)
    assertEquals(result, "1" *: 4 *: EmptyTuple)

  test("generic Poly1 construction carries substitution-stable absence evidence"):
    def build[A, B](first: A => String, second: B => Long)(using
        Absent[Case[A, String] *: EmptyTuple, B]
    ): Poly1[Case[B, Long] *: Case[A, String] *: EmptyTuple] =
      Poly1.empty.add(Case.of[A](first)).add(Case.of[B](second))

    val poly = build[Int, String](_.toString, _.length.toLong)
    val result: String *: Long *: EmptyTuple =
      (1 *: "abc" *: EmptyTuple).mapWith(poly)

    assertEquals(result, "1" *: 3L *: EmptyTuple)

  test("executes each selected body once per position and none for EmptyTuple"):
    var intCalls = 0
    var stringCalls = 0
    var booleanCalls = 0

    val transform = Poly1.empty
      .add(Case.of[Int] { value =>
        intCalls += 1
        value.toString
      })
      .add(Case.of[String] { value =>
        stringCalls += 1
        value.length
      })
      .add(Case.of[Boolean] { value =>
        booleanCalls += 1
        !value
      })

    val result = (1 *: "ab" *: 3 *: true *: EmptyTuple).mapWith(transform)
    val emptyResult = EmptyTuple.mapWith(transform)

    assertEquals(result, "1" *: 2 *: "3" *: false *: EmptyTuple)
    assertEquals(emptyResult, EmptyTuple)
    assertEquals((intCalls, stringCalls, booleanCalls), (2, 1, 1))

  test("keeps equal-shaped Poly1 values isolated"):
    val first = Poly1.empty.add(Case.of[Int](i => s"first:$i"))
    val second = Poly1.empty.add(Case.of[Int](i => s"second:$i"))

    assertEquals((1 *: EmptyTuple).mapWith(first), "first:1" *: EmptyTuple)
    assertEquals((1 *: EmptyTuple).mapWith(second), "second:1" *: EmptyTuple)

  test("supports an explicitly registered singleton-literal input"):
    val literal = Poly1.empty.add(Case.of[1](_ => "one"))
    val one: 1 = 1
    val input: 1 *: EmptyTuple = one *: EmptyTuple
    val result: String *: EmptyTuple = input.mapWith(literal)

    assertEquals(result, "one" *: EmptyTuple)

  test("selects exact Int and AnyVal keys from separate registries"):
    val intPoly = Poly1.empty.add(Case.of[Int](_ => "int"))
    val anyValPoly = Poly1.empty.add(Case.of[AnyVal](_ => "any-val"))
    val widened: AnyVal *: EmptyTuple = (1: AnyVal) *: EmptyTuple

    assertEquals((1 *: EmptyTuple).mapWith(intPoly), "int" *: EmptyTuple)
    assertEquals(widened.mapWith(anyValPoly), "any-val" *: EmptyTuple)

  test("maps representative tuple arities 10, 22, and 32"):
    val intToString = Poly1.empty.add(Case.of[Int](_.toString))

    val ten =
      (1 *: 2 *: 3 *: 4 *: 5 *: 6 *: 7 *: 8 *: 9 *: 10 *: EmptyTuple)
        .mapWith(intToString)
    val exactTen: String *: String *: String *: String *: String *: String *: String *: String *: String *: String *: EmptyTuple = ten

    val twentyTwo =
      (1 *: 2 *: 3 *: 4 *: 5 *: 6 *: 7 *: 8 *: 9 *: 10 *: 11 *: 12 *: 13 *: 14 *: 15 *: 16 *: 17 *: 18 *: 19 *: 20 *: 21 *: 22 *: EmptyTuple)
        .mapWith(intToString)
    val exactTwentyTwo: String *: String *: String *: String *: String *: String *: String *: String *: String *: String *: String *: String *: String *: String *: String *: String *: String *: String *: String *: String *: String *: String *: EmptyTuple = twentyTwo

    val thirtyTwo =
      (1 *: 2 *: 3 *: 4 *: 5 *: 6 *: 7 *: 8 *: 9 *: 10 *: 11 *: 12 *: 13 *: 14 *: 15 *: 16 *: 17 *: 18 *: 19 *: 20 *: 21 *: 22 *: 23 *: 24 *: 25 *: 26 *: 27 *: 28 *: 29 *: 30 *: 31 *: 32 *: EmptyTuple)
        .mapWith(intToString)
    val exactThirtyTwo: String *: String *: String *: String *: String *: String *: String *: String *: String *: String *: String *: String *: String *: String *: String *: String *: String *: String *: String *: String *: String *: String *: String *: String *: String *: String *: String *: String *: String *: String *: String *: String *: EmptyTuple = thirtyTwo

    assertEquals(exactTen.productArity, 10)
    assertEquals(exactTwentyTwo.productArity, 22)
    assertEquals(exactThirtyTwo.productArity, 32)
    assertEquals(exactThirtyTwo.productElement(31), "32")
