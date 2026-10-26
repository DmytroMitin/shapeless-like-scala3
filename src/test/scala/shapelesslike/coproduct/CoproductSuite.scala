package shapelesslike.coproduct

import shapelesslike.coproduct.Coproduct.{Handlers, Position}

class CoproductSuite extends munit.FunSuite:
  private final case class ConstantHash(value: Int):
    override def hashCode(): Int = 0

  private def injectGeneric[Alts <: Tuple, I <: Int, A](value: A)(using
      Position[Alts, I, A]
  ): Coproduct[Alts] =
    Coproduct.at[Alts, I].inject(value)

  private def projectGeneric[Alts <: Tuple, I <: Int, A](value: Coproduct[Alts])(using
      Position[Alts, I, A]
  ): Option[A] =
    value.projectAtAs[I, A]

  private def foldGeneric[Alts <: Tuple, R](
      value: Coproduct[Alts],
      handlers: Handlers[Alts, R]
  ): R =
    value.fold[R](handlers)

  private trait Decoder[A]

  private type DecoderOutputs[Ds <: Tuple] <: Tuple = Ds match
    case EmptyTuple         => EmptyTuple
    case Decoder[a] *: tail => a *: DecoderOutputs[tail]

  private def recordDecoderSuccess[Ds <: Tuple, I <: Int, A](value: A)(using
      Position[DecoderOutputs[Ds], I, A]
  ): Coproduct[DecoderOutputs[Ds]] =
    Coproduct.at[DecoderOutputs[Ds], I].inject(value)

  private trait Task[A]

  private type TaskResults[Ts <: Tuple] <: Tuple = Ts match
    case EmptyTuple      => EmptyTuple
    case Task[a] *: tail => a *: TaskResults[tail]

  private def recordTaskWinner[Ts <: Tuple, I <: Int, A](value: A)(using
      Position[TaskResults[Ts], I, A]
  ): Coproduct[TaskResults[Ts]] =
    Coproduct.at[TaskResults[Ts], I].inject(value)

  test("empty and singleton schemas retain exact public types"):
    type Empty = Coproduct[EmptyTuple]
    summon[Empty <:< Coproduct[EmptyTuple]]

    type Single = String *: EmptyTuple
    val value: Coproduct[Single] = Coproduct.at[Single, 0].inject("only")
    val projected: Option[String] = value.projectAt[0]
    val handlers: Handlers[Single, Int] = ((s: String) => s.length) *: EmptyTuple

    assertEquals(value.ordinal, 0)
    assertEquals(projected, Some("only"))
    assertEquals(value.fold[Int](handlers), 4)

  test("repeated alternatives preserve their injected positions"):
    type Alts = Int *: String *: Int *: EmptyTuple
    val first: Coproduct[Alts] = Coproduct.at[Alts, 0].inject(7)
    val middle: Coproduct[Alts] = Coproduct.at[Alts, 1].inject("seven")
    val last: Coproduct[Alts] = Coproduct.at[Alts, 2].inject(7)

    val firstAt0: Option[Int] = first.projectAt[0]
    val firstAt2: Option[Int] = first.projectAt[2]
    val lastAt0: Option[Int] = last.projectAt[0]
    val lastAt2: Option[Int] = last.projectAt[2]

    assertEquals(List(first.ordinal, middle.ordinal, last.ordinal), List(0, 1, 2))
    assertEquals(firstAt0, Some(7))
    assertEquals(firstAt2, None)
    assertEquals(lastAt0, None)
    assertEquals(lastAt2, Some(7))

  test("fold dispatches exactly one position-specific handler"):
    type Alts = Int *: String *: Int *: EmptyTuple
    val first = Coproduct.at[Alts, 0].inject(7)
    val middle = Coproduct.at[Alts, 1].inject("seven")
    val last = Coproduct.at[Alts, 2].inject(7)
    var calls = 0

    val handlers: Handlers[Alts, String] =
      ((i: Int) => { calls += 1; s"first:$i" }) *:
        ((s: String) => { calls += 1; s"text:$s" }) *:
        ((i: Int) => { calls += 1; s"second:$i" }) *:
        EmptyTuple

    assertEquals(first.fold[String](handlers), "first:7")
    assertEquals(calls, 1)
    assertEquals(middle.fold[String](handlers), "text:seven")
    assertEquals(calls, 2)
    assertEquals(last.fold[String](handlers), "second:7")
    assertEquals(calls, 3)

  test("related and singleton alternatives use exact static types"):
    type Related = AnyVal *: Int *: EmptyTuple
    val wide = Coproduct.at[Related, 0].inject(1: AnyVal)
    val exact = Coproduct.at[Related, 1].inject(1)

    assertEquals(wide.projectAt[0], Some(1: AnyVal))
    assertEquals(wide.projectAt[1], None)
    assertEquals(exact.projectAt[0], None)
    assertEquals(exact.projectAt[1], Some(1))

    type SingletonAlt = 1 *: String *: EmptyTuple
    val one: 1 = 1
    val singleton = Coproduct.at[SingletonAlt, 0].inject[1](one)
    val projected: Option[1] = singleton.projectAt[0]
    val expected: Option[1] = Some(one)
    assertEquals(projected, expected)

  test("generic evidence supports injection projection and fold"):
    type Alts = Int *: String *: Int *: EmptyTuple
    val value = injectGeneric[Alts, 2, Int](42)
    val handlers: Handlers[Alts, String] =
      ((i: Int) => s"first:$i") *:
        ((s: String) => s"text:$s") *:
        ((i: Int) => s"second:$i") *:
        EmptyTuple

    val projected: Option[Int] = projectGeneric[Alts, 2, Int](value)
    assertEquals(projected, Some(42))
    assertEquals(foldGeneric[Alts, String](value, handlers), "second:42")

  test("derived decoder and task result tuples preserve callback positions"):
    type Decoders = Decoder[Int] *: Decoder[String] *: Decoder[Int] *: EmptyTuple
    type Outputs = DecoderOutputs[Decoders]
    val decoded = recordDecoderSuccess[Decoders, 2, Int](42)
    val decoderHandlers: Handlers[Outputs, String] =
      ((i: Int) => s"decimal:$i") *:
        ((s: String) => s"quoted:$s") *:
        ((i: Int) => s"hex:$i") *:
        EmptyTuple

    assertEquals(projectGeneric[Outputs, 0, Int](decoded), None)
    assertEquals(projectGeneric[Outputs, 2, Int](decoded), Some(42))
    assertEquals(foldGeneric[Outputs, String](decoded, decoderHandlers), "hex:42")

    type Tasks = Task[Int] *: Task[String] *: Task[Int] *: EmptyTuple
    type Results = TaskResults[Tasks]
    val winner = recordTaskWinner[Tasks, 2, Int](7)
    val taskHandlers: Handlers[Results, String] =
      ((i: Int) => s"cache:$i") *:
        ((s: String) => s"remote:$s") *:
        ((i: Int) => s"database:$i") *:
        EmptyTuple

    assertEquals(projectGeneric[Results, 0, Int](winner), None)
    assertEquals(projectGeneric[Results, 2, Int](winner), Some(7))
    assertEquals(foldGeneric[Results, String](winner, taskHandlers), "database:7")

  test("fold accepts explicit common-supertype and union results"):
    type Alts = Int *: String *: EmptyTuple
    val number = Coproduct.at[Alts, 0].inject(3)
    val text = Coproduct.at[Alts, 1].inject("three")

    val anyHandlers: Handlers[Alts, Any] =
      ((i: Int) => i: Any) *: ((s: String) => s: Any) *: EmptyTuple
    val unionHandlers: Handlers[Alts, Int | String] =
      ((i: Int) => i: Int | String) *:
        ((s: String) => s: Int | String) *:
        EmptyTuple

    val anyResult: Any = number.fold[Any](anyHandlers)
    val unionResult: Int | String = text.fold[Int | String](unionHandlers)
    assertEquals(anyResult, 3)
    assertEquals(unionResult, "three")

  test("ten-alternative schemas support positions zero five and nine"):
    type Alts =
      Int *: String *: Boolean *: Long *: Double *:
        Char *: Byte *: Short *: Float *: BigInt *: EmptyTuple

    val zero = Coproduct.at[Alts, 0].inject(10)
    val five = Coproduct.at[Alts, 5].inject('x')
    val nine = Coproduct.at[Alts, 9].inject(BigInt(99))
    val handlers: Handlers[Alts, String] =
      ((v: Int) => s"0:$v") *:
        ((v: String) => s"1:$v") *:
        ((v: Boolean) => s"2:$v") *:
        ((v: Long) => s"3:$v") *:
        ((v: Double) => s"4:$v") *:
        ((v: Char) => s"5:$v") *:
        ((v: Byte) => s"6:$v") *:
        ((v: Short) => s"7:$v") *:
        ((v: Float) => s"8:$v") *:
        ((v: BigInt) => s"9:$v") *:
        EmptyTuple

    assertEquals(zero.projectAt[0], Some(10))
    assertEquals(five.projectAt[5], Some('x'))
    assertEquals(nine.projectAt[9], Some(BigInt(99)))
    assertEquals(zero.fold[String](handlers), "0:10")
    assertEquals(five.fold[String](handlers), "5:x")
    assertEquals(nine.fold[String](handlers), "9:99")

  test("equality hash and display include positional identity"):
    type Alts = Int *: String *: Int *: EmptyTuple
    val first = Coproduct.at[Alts, 0].inject(7)
    val firstAgain = Coproduct.at[Alts, 0].inject(7)
    val differentValue = Coproduct.at[Alts, 0].inject(8)
    val last = Coproduct.at[Alts, 2].inject(7)

    assertEquals(first, firstAgain)
    assertEquals(first.hashCode, firstAgain.hashCode)
    assertNotEquals(first, differentValue)
    assertNotEquals(first, last)
    assert(first.toString.contains("0"), first.toString)
    assert(first.toString.contains("7"), first.toString)
    assert(last.toString.contains("2"), last.toString)
    assert(last.toString.contains("7"), last.toString)
    assert(!first.toString.contains("Here"), first.toString)
    assert(!last.toString.contains("There"), last.toString)

    type Hashed = ConstantHash *: String *: ConstantHash *: EmptyTuple
    val hashedFirst = Coproduct.at[Hashed, 0].inject(ConstantHash(1))
    val hashedLast = Coproduct.at[Hashed, 2].inject(ConstantHash(1))
    assertNotEquals(hashedFirst.hashCode, hashedLast.hashCode)
