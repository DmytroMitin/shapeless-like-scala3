package shapelesslike.design

import scala.compiletime.testing.typeCheckErrors

class CoherenceModelsSuite extends munit.FunSuite:
  private def assertAmbiguous(errors: List[scala.compiletime.testing.Error]): Unit =
    assert(errors.nonEmpty)
    assert(errors.exists(_.message.toLowerCase.contains("ambiguous")), errors.map(_.message).mkString("\n"))

  test("explicit-output model lets an expected output select a conflicting result type"):
    val inferredErrors = typeCheckErrors("""
      import shapelesslike.design.ExplicitModel.*
      case object P
      object TextCase:
        given Case[P.type, Int, String] = Case(_.toString)
      object LongCase:
        given Case[P.type, Int, Long] = Case(_.toLong)
      import TextCase.given
      import LongCase.given
      val result = (1 *: EmptyTuple).mapWith(P)
    """)
    assertAmbiguous(inferredErrors)

    val textErrors = typeCheckErrors("""
      import shapelesslike.design.ExplicitModel.*
      case object P
      object TextCase:
        given Case[P.type, Int, String] = Case(_.toString)
      object LongCase:
        given Case[P.type, Int, Long] = Case(_.toLong)
      import TextCase.given
      import LongCase.given
      val result: String *: EmptyTuple = (1 *: EmptyTuple).mapWith(P)
    """)
    assertEquals(textErrors, Nil)

    val longErrors = typeCheckErrors("""
      import shapelesslike.design.ExplicitModel.*
      case object P
      object TextCase:
        given Case[P.type, Int, String] = Case(_.toString)
      object LongCase:
        given Case[P.type, Int, Long] = Case(_.toLong)
      import TextCase.given
      import LongCase.given
      val result: Long *: EmptyTuple = (1 *: EmptyTuple).mapWith(P)
    """)
    assertEquals(longErrors, Nil)

  test("explicit-output imported same-output witnesses are silently selected by import priority"):
    val errors = typeCheckErrors("""
      import shapelesslike.design.ExplicitModel.*
      case object P
      object First:
        given Case[P.type, Int, String] = Case(i => s"first:$i")
      object Second:
        given Case[P.type, Int, String] = Case(i => s"second:$i")
      import First.given
      import Second.given
      val result: String *: EmptyTuple = (1 *: EmptyTuple).mapWith(P)
    """)
    assertEquals(errors, Nil)

    import ExplicitModel.*
    object P
    object FirstRuntime:
      given Case[P.type, Int, String] = Case(i => s"first:$i")
    object SecondRuntime:
      given Case[P.type, Int, String] = Case(i => s"second:$i")
    import FirstRuntime.given
    import SecondRuntime.given
    val selected = (1 *: EmptyTuple).mapWith(P)
    assert(selected == ("second:1" *: EmptyTuple))

  test("explicit-output same-scope named witnesses are ambiguous"):
    val errors = typeCheckErrors("""
      import shapelesslike.design.ExplicitModel.*
      case object P
      given first: Case[P.type, Int, String] = Case(i => s"first:$i")
      given second: Case[P.type, Int, String] = Case(i => s"second:$i")
      val result: String *: EmptyTuple = (1 *: EmptyTuple).mapWith(P)
    """)
    assertAmbiguous(errors)

    val inferredErrors = typeCheckErrors("""
      import shapelesslike.design.ExplicitModel.*
      case object P
      given first: Case[P.type, Int, String] = Case(i => s"first:$i")
      given second: Case[P.type, Int, String] = Case(i => s"second:$i")
      val result = (1 *: EmptyTuple).mapWith(P)
    """)
    assertAmbiguous(inferredErrors)

  test("explicit-output generic methods retain expected-output selection"):
    val errors = typeCheckErrors("""
      import shapelesslike.design.ExplicitModel.*
      case object P
      object TextCase:
        given Case[P.type, Int, String] = Case(_.toString)
      object LongCase:
        given Case[P.type, Int, Long] = Case(_.toLong)
      import TextCase.given
      import LongCase.given
      def mapAs[T <: Tuple, O <: Tuple](input: T)(using mapper: Mapper[P.type, T, O]): O = mapper(input)
      val text: String *: EmptyTuple = mapAs(1 *: EmptyTuple)
      val number: Long *: EmptyTuple = mapAs(1 *: EmptyTuple)
    """)
    assertEquals(errors, Nil)

  test("associated-output model rejects different-output conflicts in inferred and expected contexts"):
    val inferredErrors = typeCheckErrors("""
      import shapelesslike.design.AssociatedModel.*
      case object P
      object TextCase:
        given Case.Aux[P.type, Int, String] = Case(_.toString)
      object LongCase:
        given Case.Aux[P.type, Int, Long] = Case(_.toLong)
      import TextCase.given
      import LongCase.given
      val result = (1 *: EmptyTuple).mapWith(P)
    """)
    assertAmbiguous(inferredErrors)

    val textErrors = typeCheckErrors("""
      import shapelesslike.design.AssociatedModel.*
      case object P
      object TextCase:
        given Case.Aux[P.type, Int, String] = Case(_.toString)
      object LongCase:
        given Case.Aux[P.type, Int, Long] = Case(_.toLong)
      import TextCase.given
      import LongCase.given
      val result: String *: EmptyTuple = (1 *: EmptyTuple).mapWith(P)
    """)
    assertAmbiguous(textErrors)

    val longErrors = typeCheckErrors("""
      import shapelesslike.design.AssociatedModel.*
      case object P
      object TextCase:
        given Case.Aux[P.type, Int, String] = Case(_.toString)
      object LongCase:
        given Case.Aux[P.type, Int, Long] = Case(_.toLong)
      import TextCase.given
      import LongCase.given
      val result: Long *: EmptyTuple = (1 *: EmptyTuple).mapWith(P)
    """)
    assertAmbiguous(longErrors)

  test("associated-output imported same-output witnesses do not yield an exact expected result"):
    val errors = typeCheckErrors("""
      import shapelesslike.design.AssociatedModel.*
      case object P
      object First:
        given Case.Aux[P.type, Int, String] = Case(i => s"first:$i")
      object Second:
        given Case.Aux[P.type, Int, String] = Case(i => s"second:$i")
      import First.given
      import Second.given
      val result: String *: EmptyTuple = (1 *: EmptyTuple).mapWith(P)
    """)
    assert(errors.nonEmpty)
    assert(errors.exists(_.message.contains("Required: String *: EmptyTuple")), errors.map(_.message).mkString("\n"))

    import AssociatedModel.*
    object P
    object FirstRuntime:
      given Case.Aux[P.type, Int, String] = Case(i => s"first:$i")
    object SecondRuntime:
      given Case.Aux[P.type, Int, String] = Case(i => s"second:$i")
    import FirstRuntime.given
    import SecondRuntime.given
    val selected = (1 *: EmptyTuple).mapWith(P)
    assert(selected == ("second:1" *: EmptyTuple))

  test("associated-output same-scope named witnesses are ambiguous without an expected result"):
    val errors = typeCheckErrors("""
      import shapelesslike.design.AssociatedModel.*
      case object P
      given first: Case.Aux[P.type, Int, String] = Case(i => s"first:$i")
      given second: Case.Aux[P.type, Int, String] = Case(i => s"second:$i")
      val result = (1 *: EmptyTuple).mapWith(P)
    """)
    assertAmbiguous(errors)

  test("associated-output model leaves an abstract tuple result path-dependent"):
    val errors = typeCheckErrors("""
      import shapelesslike.design.AssociatedModel.*
      case object P
      given Case.Aux[P.type, Int, String] = Case(_.toString)
      given Case.Aux[P.type, String, Int] = Case(_.length)
      def mapped[T <: Tuple](input: T)(using mapper: Mapper[P.type, T]): mapper.Out = mapper(input)
      val result: String *: Int *: EmptyTuple = mapped(1 *: "ab" *: EmptyTuple)
      summon[result.type <:< (String *: Int *: EmptyTuple)]
    """)
    assert(errors.nonEmpty)
    assert(errors.exists(_.message.contains("Required: (String, Int)")), errors.map(_.message).mkString("\n"))

  test("associated-output generic method cannot be steered by an external Out"):
    val errors = typeCheckErrors("""
      import shapelesslike.design.AssociatedModel.*
      case object P
      object TextCase:
        given Case.Aux[P.type, Int, String] = Case(_.toString)
      object LongCase:
        given Case.Aux[P.type, Int, Long] = Case(_.toLong)
      import TextCase.given
      import LongCase.given
      def mapAs[T <: Tuple, O <: Tuple](input: T)(using mapper: Mapper[P.type, T], ev: mapper.Out =:= O): O =
        ev(mapper(input))
      val result: String *: EmptyTuple = mapAs(1 *: EmptyTuple)
    """)
    assertAmbiguous(errors)

  test("associated-output model isolates Poly identities"):
    val errors = typeCheckErrors("""
      import shapelesslike.design.AssociatedModel.*
      case object P
      case object Q
      given Case.Aux[P.type, Int, String] = Case(_.toString)
      val result = (1 *: EmptyTuple).mapWith(Q)
    """)
    assert(errors.nonEmpty)
    assert(errors.exists(_.message.contains("Mapper")), errors.map(_.message).mkString("\n"))

  test("registry model infers exact concrete and abstract tuple results"):
    val errors = typeCheckErrors("""
      import shapelesslike.design.RegistryModel.*
      val poly = Poly1.empty
        .add(Case.of[Int](_.toString))
        .add(Case.of[String](_.length))
        .add(Case.of[Boolean](!_))
      val concrete = (1 *: "abc" *: true *: EmptyTuple).mapWith(poly)
      summon[concrete.type <:< (String *: Int *: Boolean *: EmptyTuple)]
      def mapped[C <: Tuple, T <: Tuple, O <: Tuple](p: Poly1[C], input: T)(using Mapper[C, T, O]): O =
        input.mapWith(p)
      val generic: String *: Int *: EmptyTuple = mapped(poly, 1 *: "ab" *: EmptyTuple)
    """)
    assertEquals(errors, Nil)

  test("registry model rejects duplicate inputs before inferred or expected mapping"):
    val sameOutputErrors = typeCheckErrors("""
      import shapelesslike.design.RegistryModel.*
      val poly = Poly1.empty
        .add(Case.of[Int](i => s"first:$i"))
        .add(Case.of[Int](i => s"second:$i"))
      val result = (1 *: EmptyTuple).mapWith(poly)
    """)
    assert(sameOutputErrors.nonEmpty)
    assert(sameOutputErrors.exists(_.message.contains("Absent")), sameOutputErrors.map(_.message).mkString("\n"))

    val differentOutputErrors = typeCheckErrors("""
      import shapelesslike.design.RegistryModel.*
      val poly = Poly1.empty
        .add(Case.of[Int](_.toString))
        .add(Case.of[Int](_.toLong))
      val result: Long *: EmptyTuple = (1 *: EmptyTuple).mapWith(poly)
    """)
    assert(differentOutputErrors.nonEmpty)
    assert(differentOutputErrors.exists(_.message.contains("Absent")), differentOutputErrors.map(_.message).mkString("\n"))

  test("registry model keeps bodies isolated between equal-shaped Poly values"):
    import RegistryModel.*
    val first = Poly1.empty.add(Case.of[Int](i => s"first:$i"))
    val second = Poly1.empty.add(Case.of[Int](i => s"second:$i"))

    assertEquals((1 *: EmptyTuple).mapWith(first), "first:1" *: EmptyTuple)
    assertEquals((1 *: EmptyTuple).mapWith(second), "second:1" *: EmptyTuple)
