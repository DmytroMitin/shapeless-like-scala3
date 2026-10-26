package shapelesslike.coproduct

import scala.compiletime.testing.{Error, typeCheckErrors}

class CoproductCompileErrorsSuite extends munit.FunSuite:
  private def assertErrorContains(errors: List[Error], fragments: String*): Unit =
    assert(errors.nonEmpty, "expected compilation to fail")
    val messages = errors.map(_.message).mkString("\n")
    fragments.foreach(fragment => assert(messages.contains(fragment), messages))

  test("rejects a wrong value at a valid index"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.Coproduct
      type Alts = Int *: String *: Int *: EmptyTuple
      val value = Coproduct.at[Alts, 1].inject(42)
    """)
    assertErrorContains(errors, "Index", "Int", "Alts")

  test("rejects a negative injection index"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.Coproduct
      type Alts = Int *: String *: EmptyTuple
      val value = Coproduct.at[Alts, -1].inject(42)
    """)
    assertErrorContains(errors, "Index", "-1")

  test("rejects an injection index equal to tuple length"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.Coproduct
      type Alts = Int *: String *: EmptyTuple
      val value = Coproduct.at[Alts, 2].inject(42)
    """)
    assertErrorContains(errors, "Index", "2")

  test("rejects empty-list injection"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.Coproduct
      val value = Coproduct.at[EmptyTuple, 0].inject(42)
    """)
    assertErrorContains(errors, "Index", "EmptyTuple")

  test("rejects a negative projection index"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.Coproduct
      type Alts = Int *: String *: EmptyTuple
      val value = Coproduct.at[Alts, 0].inject(42)
      val projected = value.projectAt[-1]
    """)
    assertErrorContains(errors, "Index", "-1")

  test("rejects an out-of-range projection index"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.Coproduct
      type Alts = Int *: String *: EmptyTuple
      val value = Coproduct.at[Alts, 0].inject(42)
      val projected = value.projectAt[2]
    """)
    assertErrorContains(errors, "Index", "2")

  test("rejects abstract construction without carried Position evidence"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.Coproduct
      def inject[Alts <: Tuple, I <: Int, A](value: A): Coproduct[Alts] =
        Coproduct.at[Alts, I].inject(value)
    """)
    assertErrorContains(errors, "Index", "Alts")

  test("rejects a missing fold handler"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.Coproduct
      type Alts = Int *: String *: Int *: EmptyTuple
      val value = Coproduct.at[Alts, 0].inject(1)
      value.fold[String](
        ((i: Int) => i.toString) *:
          ((s: String) => s) *:
          EmptyTuple
      )
    """)
    assertErrorContains(errors, "Required", "Int => String")

  test("rejects an extra fold handler"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.Coproduct
      type Alts = Int *: String *: EmptyTuple
      val value = Coproduct.at[Alts, 0].inject(1)
      value.fold[String](
        ((i: Int) => i.toString) *:
          ((s: String) => s) *:
          ((b: Boolean) => b.toString) *:
          EmptyTuple
      )
    """)
    assertErrorContains(errors, "Required", "Boolean => String")

  test("rejects a wrong first handler input"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.Coproduct
      type Alts = Int *: String *: EmptyTuple
      val value = Coproduct.at[Alts, 0].inject(1)
      value.fold[String](
        ((s: String) => s) *:
          ((s: String) => s) *:
          EmptyTuple
      )
    """)
    assertErrorContains(errors, "Required", "Int => String")

  test("rejects a wrong handler input at the later duplicate position"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.Coproduct
      type Alts = Int *: String *: Int *: EmptyTuple
      val value = Coproduct.at[Alts, 2].inject(1)
      value.fold[String](
        ((i: Int) => i.toString) *:
          ((s: String) => s) *:
          ((s: String) => s) *:
          EmptyTuple
      )
    """)
    assertErrorContains(errors, "Required", "Int => String")

  test("rejects a handler output incompatible with explicit fold result"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.Coproduct
      type Alts = Int *: String *: EmptyTuple
      val value = Coproduct.at[Alts, 0].inject(1)
      value.fold[String](
        ((i: Int) => i) *:
          ((s: String) => s) *:
          EmptyTuple
      )
    """)
    assertErrorContains(errors, "(i : Int)", "Required: String")

  test("rejects singleton widening without an explicit singleton type"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.Coproduct
      type Alts = 1 *: String *: EmptyTuple
      val one: 1 = 1
      val value = Coproduct.at[Alts, 0].inject(one)
    """)
    assertErrorContains(errors, "Index", "Int", "Alts")

  test("does not expose type-only injection"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.Coproduct
      type Alts = Int *: String *: Int *: EmptyTuple
      val value: Coproduct[Alts] = Coproduct.inject[Int](1)
    """)
    assertErrorContains(errors, "inject")

  test("does not expose type-only selection"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.Coproduct
      type Alts = Int *: String *: Int *: EmptyTuple
      val value = Coproduct.at[Alts, 0].inject(1)
      val selected = value.select[Int]
    """)
    assertErrorContains(errors, "select")

  test("cannot directly implement the sealed empty carrier"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.Coproduct
      val empty = new Coproduct[EmptyTuple]:
        def ordinal: Int = 0
        def fold[R](handlers: Coproduct.Handlers[EmptyTuple, R]): R = ???
    """)
    assertErrorContains(errors, "sealed", "Coproduct")

  test("cannot directly implement Position evidence"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.Coproduct
      val forged = new Coproduct.Position[Int *: EmptyTuple, 0, Int]:
        private[coproduct] def inject(value: Int): Coproduct[Int *: EmptyTuple] = ???
        private[coproduct] def project(value: Coproduct[Int *: EmptyTuple]): Option[Int] = None
    """)
    assertErrorContains(errors, "sealed", "Position")
