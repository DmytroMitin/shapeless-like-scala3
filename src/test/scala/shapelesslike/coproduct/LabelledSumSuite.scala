package shapelesslike.coproduct

import scala.compiletime.ops.any.IsConst
import shapelesslike.coproduct.Coproduct.{Handlers, Position}
import shapelesslike.coproduct.LabelledSum.LabelPosition
import shapelesslike.record.RecordSchema

class LabelledSumSuite extends munit.FunSuite:
  private type Names = ("decimal", "text", "hex")
  private type Values = (Int, String, Int)

  private def injectGeneric[N <: Tuple, V <: Tuple, K <: String & Singleton, I <: Int, A](
      key: K, value: A
  )(using RecordSchema[N, V], IsConst[K] =:= true, LabelPosition[N, V, K, I, A],
      Position[V, I, A]): LabelledSum[N, V] =
    LabelledSum.of[N, V].inject[K, I, A](key, value)

  private def projectGeneric[N <: Tuple, V <: Tuple, K <: String & Singleton, I <: Int, A](
      sum: LabelledSum[N, V], key: K
  )(using IsConst[K] =:= true, LabelPosition[N, V, K, I, A], Position[V, I, A]): Option[A] =
    sum.project[K, I, A](key)

  private def attachGeneric[N <: Tuple, V <: Tuple](raw: Coproduct[V])(using
      RecordSchema[N, V]): LabelledSum[N, V] = LabelledSum.fromCoproduct[N, V](raw)

  private def foldGeneric[N <: Tuple, V <: Tuple, R](sum: LabelledSum[N, V],
      handlers: Handlers[V, R]): R = sum.fold[R](handlers)

  test("empty schema is valid and singleton constructs projects and folds exactly"):
    summon[RecordSchema[EmptyTuple, EmptyTuple]]
    val emptyBuilder: LabelledSum.Builder[EmptyTuple, EmptyTuple] = LabelledSum.of
    val single: LabelledSum[Tuple1["only"], Tuple1[String]] =
      LabelledSum.of[Tuple1["only"], Tuple1[String]].inject("only", "one")
    val selected = single.project("only")
    summon[selected.type <:< Option[String]]
    val handlers: Handlers[Tuple1[String], Int] = Tuple1((s: String) => s.length)
    assertEquals(single.ordinal, 0)
    assertEquals(selected, Some("one"))
    assertEquals(single.fold[Int](handlers), 3)

  test("all labels retain separate repeated-payload positions and exact projections"):
    val builder = LabelledSum.of[Names, Values]
    val decimal = builder.inject("decimal", 42)
    val text = builder.inject("text", "forty-two")
    val hex = builder.inject("hex", 42)
    val selected = hex.project("hex")
    val missing = hex.project("decimal")
    val textSelected = text.project("text")
    summon[selected.type <:< Option[Int]]
    summon[missing.type <:< Option[Int]]
    summon[textSelected.type <:< Option[String]]
    assertEquals(List(decimal.ordinal, text.ordinal, hex.ordinal), List(0, 1, 2))
    assertEquals(decimal.project("decimal"), Some(42))
    assertEquals(decimal.project("hex"), None)
    assertEquals(selected, Some(42))
    assertEquals(missing, None)
    assertEquals(textSelected, Some("forty-two"))
    var calls = List.empty[String]
    val handlers: Handlers[Values, String] =
      ((i: Int) => { calls :+= "decimal"; s"decimal:$i" }) *:
        ((s: String) => { calls :+= "text"; s"text:$s" }) *:
        ((i: Int) => { calls :+= "hex"; s"hex:$i" }) *: EmptyTuple
    assertEquals(decimal.fold[String](handlers), "decimal:42")
    assertEquals(text.fold[String](handlers), "text:forty-two")
    assertEquals(hex.fold[String](handlers), "hex:42")
    assertEquals(calls, List("decimal", "text", "hex"))

  test("the identical hex key survives a schema prefix and shifted ordinal"):
    type PrefixedNames = Tuple.Concat[Tuple1["binary"], Names]
    type PrefixedValues = Tuple.Concat[Tuple1[Boolean], Values]
    val before = LabelledSum.of[Names, Values].inject("hex", 42)
    val after = LabelledSum.of[PrefixedNames, PrefixedValues].inject("hex", 42)
    val selected: Option[Int] = after.project("hex")
    assertEquals(before.ordinal, 2)
    assertEquals(after.ordinal, 3)
    assertEquals(selected, Some(42))
    assertEquals(after.project("decimal"), None)

  test("composed command fragments retain distinct repeated String and Int alternatives"):
    type N = Tuple.Concat[("load", "save"), ("retries", "timeout")]
    type V = Tuple.Concat[(String, String), (Int, Int)]
    val builder = LabelledSum.of[N, V]
    val load = builder.inject("load", "in.txt")
    val save = builder.inject("save", "out.txt")
    val retries = builder.inject("retries", 3)
    val timeout = builder.inject("timeout", 30)
    val projected: Option[Int] = timeout.project("timeout")
    assertEquals(List(load.ordinal, save.ordinal, retries.ordinal, timeout.ordinal), List(0, 1, 2, 3))
    assertEquals(projected, Some(30))
    assertEquals(timeout.project("load"), None)
    assertEquals(timeout.project("retries"), None)
    assertEquals(save.project("save"), Some("out.txt"))
    assertEquals(save.project("load"), None)

  test("singleton stable keys and explicit generic evidence preserve K"):
    val key: "hex" = "hex"
    val direct = LabelledSum.of[Names, Values].inject(key, 42)
    val generic: LabelledSum[Names, Values] = injectGeneric[Names, Values, "hex", 2, Int](key, 42)
    val selected: Option[Int] = projectGeneric[Names, Values, "hex", 2, Int](generic, key)
    val handlers: Handlers[Values, String] =
      ((i: Int) => s"decimal:$i") *: ((s: String) => s) *: ((i: Int) => s"hex:$i") *: EmptyTuple
    assertEquals(direct.project(key), Some(42))
    assertEquals(selected, Some(42))
    assertEquals(foldGeneric[Names, Values, String](generic, handlers), "hex:42")
    assert(attachGeneric[Names, Values](generic.toCoproduct).toCoproduct eq generic.toCoproduct)

  private def composed[PN <: Tuple, PV <: Tuple](using
      RecordSchema[Tuple.Concat[PN, Names], Tuple.Concat[PV, Values]],
      LabelPosition[Tuple.Concat[PN, Names], Tuple.Concat[PV, Values], "hex", 3, Int],
      Position[Tuple.Concat[PV, Values], 3, Int]
  ): LabelledSum[Tuple.Concat[PN, Names], Tuple.Concat[PV, Values]] =
    injectGeneric[Tuple.Concat[PN, Names], Tuple.Concat[PV, Values], "hex", 3, Int]("hex", 42)

  test("abstract composed schemas forward their explicit structural proofs"):
    val value = composed[Tuple1["binary"], Tuple1[Boolean]]
    val selected: Option[Int] = value.project("hex")
    assertEquals(value.ordinal, 3)
    assertEquals(selected, Some(42))

  test("validated attach and explicit relabelling preserve exact raw object and payload"):
    val payload = new StringBuilder("raw")
    type V = (Int, StringBuilder)
    val raw: Coproduct[V] = Coproduct.at[V, 1].inject(payload)
    val original = LabelledSum.fromCoproduct[("count", "text"), V](raw)
    val relabelled = LabelledSum.fromCoproduct[("number", "buffer"), V](original.toCoproduct)
    val unwrapped: Coproduct[V] = relabelled.toCoproduct
    val selected: Option[StringBuilder] = relabelled.project("buffer")
    assert(unwrapped eq raw)
    assert(original.toCoproduct eq raw)
    assert(selected.exists(_ eq payload))
    assertEquals(relabelled.ordinal, 1)
    assertEquals(original.project("text"), Some(payload))

  test("equality hash and display inherit raw ordinal and payload across label schemas"):
    val decimal = LabelledSum.of[Names, Values].inject("decimal", 42)
    val same = LabelledSum.of[Names, Values].inject("decimal", 42)
    val different = LabelledSum.of[Names, Values].inject("decimal", 43)
    val hex = LabelledSum.of[Names, Values].inject("hex", 42)
    val other = LabelledSum.of[("count", "word", "code"), Values].inject("count", 42)
    assert(decimal.equals(same))
    assert(!decimal.equals(different))
    assert(!decimal.equals(hex))
    assert(decimal.equals(other))
    assertEquals(decimal.hashCode, same.hashCode)
    assertEquals(decimal.hashCode, other.hashCode)
    assertEquals(decimal.toString, "Coproduct(index=0, value=42)")
    assertEquals(hex.toString, "Coproduct(index=2, value=42)")

  test("related payload types and explicitly ascribed singleton payloads coexist"):
    type N = ("wide", "int", "one")
    type V = (AnyVal, Int, 1)
    val builder = LabelledSum.of[N, V]
    val wide = builder.inject("wide", 1: AnyVal)
    val int = builder.inject("int", 1)
    val one = builder.inject["one", 2, 1]("one", 1: 1)
    val selected: Option[1] = one.project("one")
    assertEquals(wide.project("wide"), Some(1: AnyVal))
    assertEquals(wide.project("int"), None)
    assertEquals(int.project("int"), Some(1))
    val expected: Option[1] = Some[1](1)
    assertEquals(selected, expected)
    assertEquals(one.project("int"), None)

  test("ten labels support last injection first and last projection and fold"):
    type N = ("k0", "k1", "k2", "k3", "k4", "k5", "k6", "k7", "k8", "k9")
    type V = (Int, Int, Int, Int, Int, Int, Int, Int, Int, Int)
    val value = LabelledSum.of[N, V].inject("k9", 99)
    val first: Option[Int] = value.project("k0")
    val last: Option[Int] = value.project("k9")
    val handlers: Handlers[V, String] =
      ((i: Int) => s"0:$i", (i: Int) => s"1:$i", (i: Int) => s"2:$i",
       (i: Int) => s"3:$i", (i: Int) => s"4:$i", (i: Int) => s"5:$i",
       (i: Int) => s"6:$i", (i: Int) => s"7:$i", (i: Int) => s"8:$i", (i: Int) => s"9:$i")
    assertEquals(value.ordinal, 9)
    assertEquals(first, None)
    assertEquals(last, Some(99))
    assertEquals(value.fold[String](handlers), "9:99")
