package shapelesslike.record

import scala.NamedTuple.NamedTuple
import shapelesslike.record.syntax.*

class NamedTupleUpdateSuite extends munit.FunSuite:
  private def updateGeneric[
      N <: Tuple,
      V <: Tuple,
      K <: String & Singleton,
      B,
      OutV <: Tuple
  ](
      record: NamedTuple[N, V],
      key: K,
      replacement: B
  )(using
      RecordSchema[N, V],
      UpdateByName[N, V, K, B, OutV]
  ): NamedTuple[N, OutV] =
    record.updatedByName[K, B, OutV](key, replacement)

  private def forwardSchema[N <: Tuple, V <: Tuple](using
      schema: RecordSchema[N, V]
  ): RecordSchema[N, V] = schema

  test("same-type and type-changing updates retain exact named-tuple schemas"):
    type Person = (name: String, age: Int)
    val person: Person = (name = "Ada", age = 37)

    val birthday: Person = person.updatedByName("age", 38)
    val rendered: (name: String, age: String) =
      person.updatedByName("age", "38")
    val inferred = person.updatedByName("age", "thirty-eight")
    val inferredExact: (name: String, age: String) = inferred

    assertEquals(birthday.toTuple, ("Ada", 38))
    assertEquals(rendered.toTuple, ("Ada", "38"))
    assertEquals(inferredExact.toTuple, ("Ada", "thirty-eight"))

  test("first middle and last fields update independently"):
    type Row = (id: Int, label: String, active: Boolean)
    val row: Row = (id = 1, label = "one", active = false)

    val first: (id: String, label: String, active: Boolean) =
      row.updatedByName("id", "001")
    val middle: (id: Int, label: Int, active: Boolean) =
      row.updatedByName("label", 3)
    val last: (id: Int, label: String, active: String) =
      row.updatedByName("active", "yes")

    assertEquals(first.toTuple, ("001", "one", false))
    assertEquals(middle.toTuple, (1, 3, false))
    assertEquals(last.toTuple, (1, "one", "yes"))

  test("empty schema exists and singleton schema changes exactly"):
    summon[RecordSchema[EmptyTuple, EmptyTuple]]

    type Single = (value: Int)
    val single: Single = Tuple1(1)
    val changed: (value: String) = single.updatedByName("value", "one")

    assertEquals(changed.toTuple, Tuple1("one"))

  test("heterogeneous and repeated value types select by name"):
    type Row = (left: Int, text: String, right: Int, flag: Boolean)
    val row: Row = (left = 1, text = "two", right = 3, flag = true)

    val changed: (left: Int, text: String, right: String, flag: Boolean) =
      row.updatedByName("right", "three")

    assertEquals(changed.toTuple, (1, "two", "three", true))

  test("literal singleton key and generic evidence forwarding retain exact output"):
    type Raw = (name: String, age: String, active: Boolean)
    type Values = String *: String *: Boolean *: EmptyTuple
    type ParsedValues = String *: Int *: Boolean *: EmptyTuple
    val raw: Raw = (name = "Ada", age = "37", active = true)
    val age: "age" = "age"

    val direct: (name: String, age: Int, active: Boolean) =
      raw.updatedByName("age", 37)
    val stable: (name: String, age: Int, active: Boolean) =
      raw.updatedByName(age, 37)
    val generic: NamedTuple[("name", "age", "active"), ParsedValues] =
      updateGeneric[("name", "age", "active"), Values, "age", Int, ParsedValues](
        raw,
        age,
        37
      )

    assertEquals(direct, stable)
    assertEquals(generic, direct)

  test("standard Names DropNames selection and tuple representation remain exact"):
    type Raw = (name: String, age: String, active: Boolean)
    type Parsed = (name: String, age: Int, active: Boolean)
    val raw: Raw = (name = "Ada", age = "37", active = true)
    val parsed: Parsed = raw.updatedByName("age", 37)

    summon[NamedTuple.Names[Parsed] =:= ("name", "age", "active")]
    summon[NamedTuple.DropNames[Parsed] =:= (String, Int, Boolean)]
    val selected: Int = parsed.age
    val tuple: (String, Int, Boolean) = parsed.toTuple

    assertEquals(selected, 37)
    assertEquals(tuple, ("Ada", 37, true))

  test("ten-field schemas update first middle and last"):
    type Ten = (
        f0: Int,
        f1: String,
        f2: Boolean,
        f3: Long,
        f4: Double,
        f5: Char,
        f6: Byte,
        f7: Short,
        f8: Float,
        f9: BigInt
    )
    val ten: Ten = (
      f0 = 0,
      f1 = "one",
      f2 = true,
      f3 = 3L,
      f4 = 4.0,
      f5 = '5',
      f6 = 6.toByte,
      f7 = 7.toShort,
      f8 = 8.0f,
      f9 = BigInt(9)
    )

    val first: (
        f0: String,
        f1: String,
        f2: Boolean,
        f3: Long,
        f4: Double,
        f5: Char,
        f6: Byte,
        f7: Short,
        f8: Float,
        f9: BigInt
    ) = ten.updatedByName("f0", "zero")
    val middle: (
        f0: Int,
        f1: String,
        f2: Boolean,
        f3: Long,
        f4: Double,
        f5: String,
        f6: Byte,
        f7: Short,
        f8: Float,
        f9: BigInt
    ) = ten.updatedByName("f5", "five")
    val last: (
        f0: Int,
        f1: String,
        f2: Boolean,
        f3: Long,
        f4: Double,
        f5: Char,
        f6: Byte,
        f7: Short,
        f8: Float,
        f9: String
    ) = ten.updatedByName("f9", "nine")

    assertEquals(first.f0, "zero")
    assertEquals(middle.f5, "five")
    assertEquals(last.f9, "nine")

  test("schema evidence forwards abstractly for a concrete safe schema"):
    type Names = ("left", "right")
    type Values = (Int, Int)

    val schema: RecordSchema[Names, Values] = forwardSchema[Names, Values]
    assert(schema ne null)
