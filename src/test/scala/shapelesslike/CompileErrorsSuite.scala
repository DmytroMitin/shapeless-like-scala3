package shapelesslike

import scala.compiletime.testing.{Error, typeCheckErrors}

class CompileErrorsSuite extends munit.FunSuite:
  private def assertErrorContains(errors: List[Error], fragments: String*): Unit =
    assert(errors.nonEmpty, "expected compilation to fail")
    val messages = errors.map(_.message).mkString("\n")
    fragments.foreach(fragment => assert(messages.contains(fragment), messages))

  test("rejects a missing case in the first position"):
    val errors = typeCheckErrors("""
      import shapelesslike.poly.{Case, Poly1}
      import shapelesslike.tuple.syntax.*
      val poly = Poly1.empty.add(Case.of[String](_.length))
      val result = (1 *: "a" *: EmptyTuple).mapWith(poly)
    """)
    assertErrorContains(errors, "Cannot map tuple", "case is missing")

  test("rejects a missing case in a non-first position"):
    val errors = typeCheckErrors("""
      import shapelesslike.poly.{Case, Poly1}
      import shapelesslike.tuple.syntax.*
      val poly = Poly1.empty.add(Case.of[Int](_.toString))
      val result = (1 *: true *: EmptyTuple).mapWith(poly)
    """)
    assertErrorContains(errors, "Cannot map tuple", "case is missing")

  test("rejects duplicate same-output cases from imported values"):
    val errors = typeCheckErrors("""
      import shapelesslike.poly.{Case, Poly1}
      import shapelesslike.tuple.syntax.*
      object Cases:
        val first = Case.of[Int](i => s"first:$i")
        val second = Case.of[Int](i => s"second:$i")
      import Cases.*
      val poly = Poly1.empty.add(first).add(second)
      val result: String *: EmptyTuple = (1 *: EmptyTuple).mapWith(poly)
    """)
    assertErrorContains(errors, "Poly1 already contains a case", "Int")

  test("rejects duplicate different-output cases without an expected result"):
    val errors = typeCheckErrors("""
      import shapelesslike.poly.{Case, Poly1}
      import shapelesslike.tuple.syntax.*
      val poly = Poly1.empty
        .add(Case.of[Int](_.toString))
        .add(Case.of[Int](_.toLong))
      val result = (1 *: EmptyTuple).mapWith(poly)
    """)
    assertErrorContains(errors, "Poly1 already contains a case", "Int")

  test("expected String output cannot select one of two different-output cases"):
    val errors = typeCheckErrors("""
      import shapelesslike.poly.{Case, Poly1}
      import shapelesslike.tuple.syntax.*
      val poly = Poly1.empty
        .add(Case.of[Int](_.toLong))
        .add(Case.of[Int](_.toString))
      val result: String *: EmptyTuple = (1 *: EmptyTuple).mapWith(poly)
    """)
    assertErrorContains(errors, "Poly1 already contains a case", "Int")

  test("expected Long output cannot select one of two different-output cases"):
    val errors = typeCheckErrors("""
      import shapelesslike.poly.{Case, Poly1}
      import shapelesslike.tuple.syntax.*
      val poly = Poly1.empty
        .add(Case.of[Int](_.toString))
        .add(Case.of[Int](_.toLong))
      val result: Long *: EmptyTuple = (1 *: EmptyTuple).mapWith(poly)
    """)
    assertErrorContains(errors, "Poly1 already contains a case", "Int")

  test("rejects a wrong expected output despite an unrelated matching output type"):
    val errors = typeCheckErrors("""
      import shapelesslike.poly.{Case, Poly1}
      import shapelesslike.tuple.syntax.*
      val poly = Poly1.empty
        .add(Case.of[Int](_.toString))
        .add(Case.of[Long](identity))
      val result: Long *: EmptyTuple = (1 *: EmptyTuple).mapWith(poly)
    """)
    assertErrorContains(errors, "Cannot map tuple", "expected output is wrong")

  test("rejects mapping with a different Poly1 value that lacks the case"):
    val errors = typeCheckErrors("""
      import shapelesslike.poly.{Case, Poly1}
      import shapelesslike.tuple.syntax.*
      val first = Poly1.empty.add(Case.of[Int](_.toString))
      val second = Poly1.empty.add(Case.of[String](_.length))
      val result = (1 *: EmptyTuple).mapWith(second)
    """)
    assertErrorContains(errors, "Cannot map tuple", "case is missing")

  test("does not use an Int case for a statically widened AnyVal"):
    val errors = typeCheckErrors("""
      import shapelesslike.poly.{Case, Poly1}
      import shapelesslike.tuple.syntax.*
      val poly = Poly1.empty.add(Case.of[Int](_.toString))
      val input: AnyVal *: EmptyTuple = (1: AnyVal) *: EmptyTuple
      val result = input.mapWith(poly)
    """)
    assertErrorContains(errors, "Cannot map tuple", "case is missing")

  test("does not use an Int case for an explicitly singleton-typed input"):
    val errors = typeCheckErrors("""
      import shapelesslike.poly.{Case, Poly1}
      import shapelesslike.tuple.syntax.*
      val poly = Poly1.empty.add(Case.of[Int](_.toString))
      val one: 1 = 1
      val input: 1 *: EmptyTuple = one *: EmptyTuple
      val result = input.mapWith(poly)
    """)
    assertErrorContains(errors, "Cannot map tuple", "case is missing")

  test("conservatively rejects related Int and AnyVal keys in one registry"):
    val errors = typeCheckErrors("""
      import shapelesslike.poly.{Case, Poly1}
      val poly = Poly1.empty
        .add(Case.of[Int](_.toString))
        .add(Case.of[AnyVal](_.toString))
    """)
    assertErrorContains(errors, "Poly1 already contains a case", "AnyVal")

  test("rejects a wrong external Out in a generic method"):
    val errors = typeCheckErrors("""
      import shapelesslike.poly.{Case, Poly1}
      import shapelesslike.tuple.syntax.*
      import shapelesslike.tuple.typeclasses.Mapper
      def mapped[C <: Tuple, T <: Tuple, O <: Tuple](poly: Poly1[C], input: T)(using Mapper[C, T, O]): O =
        input.mapWith(poly)
      val poly = Poly1.empty.add(Case.of[Int](_.toString))
      val result: Long *: EmptyTuple = mapped(poly, 1 *: EmptyTuple)
    """)
    assertErrorContains(errors, "Mapper")

  test("does not infer duplicate-key absence for unrelated abstract type parameters"):
    val errors = typeCheckErrors("""
      import shapelesslike.poly.{Case, Poly1}
      def build[A, B](first: A => String, second: B => Long) =
        Poly1.empty.add(Case.of[A](first)).add(Case.of[B](second))
    """)
    assertErrorContains(errors, "Absent")

  test("an explicit generic absence requirement is rejected when keys become equal"):
    val errors = typeCheckErrors("""
      import shapelesslike.poly.{Absent, Case, Poly1}
      def build[A, B](first: A => String, second: B => Long)(using
          Absent[Case[A, String] *: EmptyTuple, B]
      ) = Poly1.empty.add(Case.of[A](first)).add(Case.of[B](second))
      val duplicate = build[Int, Int](_.toString, _.toLong)
    """)
    assertErrorContains(errors, "Absent")

  test("Selector implementation classes cannot be subclassed to replace a stored body"):
    val errors = typeCheckErrors("""
      import shapelesslike.poly.Case
      import shapelesslike.tuple.typeclasses.Selector
      val hijack: Selector[Case[Int, String] *: EmptyTuple, Int, String] =
        new Selector.head[Int, String, EmptyTuple]:
          override def apply(cases: Case[Int, String] *: EmptyTuple, value: Int): String = "hijacked"
    """)
    assertErrorContains(errors, "final")
  test("Mapper implementation classes cannot be subclassed"):
    val errors = typeCheckErrors("""
      import shapelesslike.tuple.typeclasses.Mapper
      val hijack: Mapper[EmptyTuple, EmptyTuple, EmptyTuple] =
        new Mapper.empty[EmptyTuple]:
          override def apply(cases: EmptyTuple, input: EmptyTuple): EmptyTuple = EmptyTuple
    """)
    assertErrorContains(errors, "final")
