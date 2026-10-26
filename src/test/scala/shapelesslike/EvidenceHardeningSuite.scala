package shapelesslike

import scala.compiletime.testing.typeCheckErrors
import shapelesslike.poly.{Case, Poly1}
import shapelesslike.tuple.syntax.*
import shapelesslike.tuple.typeclasses.{Mapper, Selector}

class EvidenceHardeningSuite extends munit.FunSuite:
  private def selectRaw[Cases <: Tuple, A, O](cases: Cases, value: A)(using
      selector: Selector[Cases, A, O]
  ): O = selector(cases, value)

  private def mapRaw[Cases <: Tuple, In <: Tuple, Out <: Tuple](
      cases: Cases,
      input: In
  )(using mapper: Mapper[Cases, In, Out]): Out = mapper(cases, input)

  test("raw duplicate cases use the first matching case in concrete evidence"):
    type Cases = Case[Int, Long] *: Case[Int, String] *: EmptyTuple

    val cases: Cases =
      Case.of[Int](i => i.toLong + 10L) *:
        Case.of[Int](i => s"old:$i") *:
        EmptyTuple

    val selected: Long = summon[Selector[Cases, Int, Long]].apply(cases, 1)
    val mapped: Long *: EmptyTuple =
      summon[Mapper[Cases, Int *: EmptyTuple, Long *: EmptyTuple]].apply(
        cases,
        1 *: EmptyTuple
      )
    val inferredSelected = selectRaw(cases, 1)
    val inferredMapped = mapRaw(cases, 1 *: EmptyTuple)
    val inferredSelectedExact: Long = inferredSelected
    val inferredMappedExact: Long *: EmptyTuple = inferredMapped

    assertEquals(selected, 11L)
    assertEquals(mapped, 11L *: EmptyTuple)
    assertEquals(inferredSelectedExact, 11L)
    assertEquals(inferredMappedExact, 11L *: EmptyTuple)

  test("Selector derivation cannot assume abstract keys differ"):
    val errors = typeCheckErrors("""
      import shapelesslike.poly.Case
      import shapelesslike.tuple.typeclasses.Selector

      def select[A, B, OA, OB](
          cases: Case[B, OB] *: Case[A, OA] *: EmptyTuple,
          value: A
      ): OA =
        summon[
          Selector[Case[B, OB] *: Case[A, OA] *: EmptyTuple, A, OA]
        ].apply(cases, value)
    """)

    assert(errors.nonEmpty, "abstract-key Selector derivation must be rejected")
    assert(errors.exists(_.message.contains("Selector")), errors.mkString("\n"))

  test("Mapper derivation cannot assume abstract keys differ"):
    val errors = typeCheckErrors("""
      import shapelesslike.poly.Case
      import shapelesslike.tuple.typeclasses.Mapper

      def map[A, B, OA, OB](
          cases: Case[B, OB] *: Case[A, OA] *: EmptyTuple,
          input: A *: EmptyTuple
      ): OA *: EmptyTuple =
        summon[
          Mapper[
            Case[B, OB] *: Case[A, OA] *: EmptyTuple,
            A *: EmptyTuple,
            OA *: EmptyTuple
          ]
        ].apply(cases, input)
    """)

    assert(errors.nonEmpty, "abstract-key Mapper derivation must be rejected")
    assert(errors.exists(_.message.contains("Mapper")), errors.mkString("\n"))

  test("a later duplicate cannot be selected by expected Selector output type"):
    val errors = typeCheckErrors("""
      import shapelesslike.poly.Case
      import shapelesslike.tuple.typeclasses.Selector

      type Cases = Case[Int, Long] *: Case[Int, String] *: EmptyTuple
      val selector = summon[Selector[Cases, Int, String]]
    """)

    assert(errors.nonEmpty, "expected output must not steer Selector past the first key")
    assert(
      errors.exists(_.message.contains("No case maps the exact static input type")),
      errors.mkString("\n")
    )

  test("a later duplicate cannot be selected by expected Mapper output type"):
    val errors = typeCheckErrors("""
      import shapelesslike.poly.Case
      import shapelesslike.tuple.typeclasses.Mapper

      type Cases = Case[Int, Long] *: Case[Int, String] *: EmptyTuple
      val mapper = summon[
        Mapper[Cases, Int *: EmptyTuple, String *: EmptyTuple]
      ]
    """)

    assert(errors.nonEmpty, "expected output must not steer Mapper past the first key")
    assert(
      errors.exists(_.message.contains("Cannot map tuple")),
      errors.mkString("\n")
    )

  test("Selector cannot skip a related raw prefix without stable absence evidence"):
    val errors = typeCheckErrors("""
      import shapelesslike.poly.Case
      import shapelesslike.tuple.typeclasses.Selector

      type Cases = Case[AnyVal, String] *: Case[Int, Long] *: EmptyTuple
      val selector = summon[Selector[Cases, Int, Long]]
    """)

    assert(errors.nonEmpty, "related raw keys must be conservatively rejected")
    assert(
      errors.exists(_.message.contains("No case maps the exact static input type")),
      errors.mkString("\n")
    )

  test("Mapper cannot skip a related raw prefix without stable absence evidence"):
    val errors = typeCheckErrors("""
      import shapelesslike.poly.Case
      import shapelesslike.tuple.typeclasses.Mapper

      type Cases = Case[AnyVal, String] *: Case[Int, Long] *: EmptyTuple
      val mapper = summon[
        Mapper[Cases, Int *: EmptyTuple, Long *: EmptyTuple]
      ]
    """)

    assert(errors.nonEmpty, "related raw keys must be conservatively rejected")
    assert(
      errors.exists(_.message.contains("Cannot map tuple")),
      errors.mkString("\n")
    )

  test("disjoint raw keys retain tail selection and mapping"):
    type Cases = Case[String, Int] *: Case[Int, String] *: EmptyTuple

    val cases: Cases =
      Case.of[String](_.length) *:
        Case.of[Int](i => s"int:$i") *:
        EmptyTuple

    val selected = summon[Selector[Cases, Int, String]].apply(cases, 2)
    val mapped =
      summon[Mapper[Cases, Int *: EmptyTuple, String *: EmptyTuple]]
        .apply(cases, 2 *: EmptyTuple)

    assertEquals(selected, "int:2")
    assertEquals(mapped, "int:2" *: EmptyTuple)

  test("normal Poly1 construction and mapWith remain coherent"):
    val poly = Poly1.empty
      .add(Case.of[Int](i => s"int:$i"))
      .add(Case.of[String](_.length))

    val result: String *: Int *: EmptyTuple =
      (3 *: "abcd" *: EmptyTuple).mapWith(poly)

    assertEquals(result, "int:3" *: 4 *: EmptyTuple)
