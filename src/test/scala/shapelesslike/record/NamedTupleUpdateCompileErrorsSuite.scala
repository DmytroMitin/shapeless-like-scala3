package shapelesslike.record

import scala.compiletime.testing.{Error, typeCheckErrors}

class NamedTupleUpdateCompileErrorsSuite extends munit.FunSuite:
  private def assertErrorContains(errors: List[Error], fragments: String*): Unit =
    assert(errors.nonEmpty, "expected compilation to fail")
    val messages = errors.map(_.message).mkString("\n")
    fragments.foreach(fragment => assert(messages.contains(fragment), messages))

  test("rejects a missing key"):
    val errors = typeCheckErrors("""
      import shapelesslike.record.syntax.*
      type Person = (name: String, age: Int)
      val person: Person = (name = "Ada", age = 37)
      val result = person.updatedByName("missing", true)
    """)
    assertErrorContains(errors, "No literal field", "missing")

  test("rejects an update to an empty schema"):
    val errors = typeCheckErrors("""
      import scala.NamedTuple.NamedTuple
      import shapelesslike.record.syntax.*
      val empty = NamedTuple.apply[EmptyTuple, EmptyTuple](EmptyTuple)
      val result = empty.updatedByName("missing", true)
    """)
    assertErrorContains(errors, "No literal field", "missing")

  test("rejects a runtime String key"):
    val errors = typeCheckErrors("""
      import shapelesslike.record.syntax.*
      type Person = (name: String, age: Int)
      val person: Person = (name = "Ada", age = 37)
      def update(key: String) = person.updatedByName(key, 38)
    """)
    assertErrorContains(
      errors,
      "No literal field",
      "key : String",
      "direct string literal"
    )

  test("rejects a widened stable String key"):
    val errors = typeCheckErrors("""
      import shapelesslike.record.syntax.*
      type Person = (name: String, age: Int)
      val person: Person = (name = "Ada", age = 37)
      val key = "age"
      val result = person.updatedByName(key, 38)
    """)
    assertErrorContains(
      errors,
      "No literal field",
      "key : String",
      "direct string literal"
    )

  test("rejects an explicitly incompatible replacement type"):
    val errors = typeCheckErrors("""
      import shapelesslike.record.syntax.*
      type Person = (name: String, age: Int)
      val person: Person = (name = "Ada", age = 37)
      val result = person.updatedByName["age", Boolean, String *: Boolean *: EmptyTuple]("age", "wrong")
    """)
    assertErrorContains(errors, "Required: Boolean")

  test("rejects an incompatible expected named-tuple output"):
    val errors = typeCheckErrors("""
      import shapelesslike.record.syntax.*
      type Person = (name: String, age: Int)
      val person: Person = (name = "Ada", age = 37)
      val result: (name: String, age: Boolean) = person.updatedByName("age", "wrong")
    """)
    assertErrorContains(errors, "No literal field", "Boolean")

  test("rejects duplicate low-level labels from NamedTuple.apply"):
    val errors = typeCheckErrors("""
      import scala.NamedTuple.NamedTuple
      import shapelesslike.record.syntax.*
      val malformed = NamedTuple.apply[("x", "x"), (Int, String)]((1, "two"))
      val result = malformed.updatedByName("x", true)
    """)
    assertErrorContains(errors, "must have equal arity and distinct literal String names")

  test("rejects duplicate low-level labels from withNames"):
    val errors = typeCheckErrors("""
      import scala.NamedTuple.*
      import shapelesslike.record.syntax.*
      val malformed = (1, "two").withNames[("x", "x")]
      val result = malformed.updatedByName("x", true)
    """)
    assertErrorContains(errors, "must have equal arity and distinct literal String names")

  test("rejects fewer low-level names than values"):
    val errors = typeCheckErrors("""
      import scala.NamedTuple.NamedTuple
      import shapelesslike.record.syntax.*
      val malformed = NamedTuple.apply["x" *: EmptyTuple, (Int, String)]((1, "two"))
      val result = malformed.updatedByName("x", true)
    """)
    assertErrorContains(errors, "must have equal arity and distinct literal String names")

  test("rejects more low-level names than values"):
    val errors = typeCheckErrors("""
      import scala.NamedTuple.NamedTuple
      import shapelesslike.record.syntax.*
      val malformed = NamedTuple.apply[("x", "y"), Int *: EmptyTuple](Tuple1(1))
      val result = malformed.updatedByName("x", true)
    """)
    assertErrorContains(errors, "must have equal arity and distinct literal String names")

  test("rejects a general String low-level name"):
    val errors = typeCheckErrors("""
      import scala.NamedTuple.NamedTuple
      import shapelesslike.record.syntax.*
      val malformed = NamedTuple.apply[String *: EmptyTuple, Int *: EmptyTuple](Tuple1(1))
      val result = malformed.updatedByName("x", true)
    """)
    assertErrorContains(errors, "Malformed NamedTuple", "String")

  test("rejects a non-String singleton low-level name"):
    val errors = typeCheckErrors("""
      import scala.NamedTuple.NamedTuple
      import shapelesslike.record.syntax.*
      val malformed = NamedTuple.apply[1 *: EmptyTuple, Int *: EmptyTuple](Tuple1(1))
      val result = malformed.updatedByName("x", true)
    """)
    assertErrorContains(errors, "Malformed NamedTuple", "1")

  test("rejects unresolved abstract schema names"):
    val errors = typeCheckErrors("""
      import shapelesslike.record.RecordSchema
      def schema[K <: String & Singleton]: RecordSchema[K *: EmptyTuple, Int *: EmptyTuple] =
        summon[RecordSchema[K *: EmptyTuple, Int *: EmptyTuple]]
    """)
    assertErrorContains(errors, "must have equal arity and distinct literal String names")

  test("rejects a widened singleton path as a schema name"):
    val errors = typeCheckErrors("""
      import shapelesslike.record.RecordSchema
      def schema(label: String & Singleton): RecordSchema[label.type *: EmptyTuple, Int *: EmptyTuple] =
        summon[RecordSchema[label.type *: EmptyTuple, Int *: EmptyTuple]]
    """)
    assertErrorContains(errors, "must have equal arity and distinct literal String names")

  test("rejects a union of literal names as one schema name"):
    val errors = typeCheckErrors("""
      import shapelesslike.record.RecordSchema
      type EitherName = "left" | "right"
      val schema = summon[RecordSchema[EitherName *: EmptyTuple, Int *: EmptyTuple]]
    """)
    assertErrorContains(errors, "must have equal arity and distinct literal String names")

  test("cannot derive duplicate names under generic substitution"):
    val errors = typeCheckErrors("""
      import shapelesslike.record.RecordSchema
      def duplicate[K <: String & Singleton]: RecordSchema[(K, K), (Int, Int)] =
        summon[RecordSchema[(K, K), (Int, Int)]]
    """)
    assertErrorContains(errors, "must have equal arity and distinct literal String names")

  test("generic update must forward RecordSchema evidence"):
    val errors = typeCheckErrors("""
      import scala.NamedTuple.NamedTuple
      import shapelesslike.record.UpdateByName
      import shapelesslike.record.syntax.*
      def update[N <: Tuple, V <: Tuple, K <: String & Singleton, B, OutV <: Tuple](
          record: NamedTuple[N, V], key: K, replacement: B
      )(using UpdateByName[N, V, K, B, OutV]): NamedTuple[N, OutV] =
        record.updatedByName[K, B, OutV](key, replacement)
    """)
    assertErrorContains(errors, "must have equal arity and distinct literal String names")

  test("generic update must forward UpdateByName evidence"):
    val errors = typeCheckErrors("""
      import scala.NamedTuple.NamedTuple
      import shapelesslike.record.RecordSchema
      import shapelesslike.record.syntax.*
      def update[N <: Tuple, V <: Tuple, K <: String & Singleton, B, OutV <: Tuple](
          record: NamedTuple[N, V], key: K, replacement: B
      )(using RecordSchema[N, V]): NamedTuple[N, OutV] =
        record.updatedByName[K, B, OutV](key, replacement)
    """)
    assertErrorContains(errors, "No literal field")

  test("expected output cannot steer update evidence to another computation"):
    val errors = typeCheckErrors("""
      import shapelesslike.record.UpdateByName
      type Names = ("left", "right")
      type Values = (Int, String)
      val steered = summon[UpdateByName[Names, Values, "right", Boolean, (Boolean, String)]]
    """)
    assertErrorContains(errors, "No literal field", "right")

  test("cannot directly implement RecordSchema evidence"):
    val errors = typeCheckErrors("""
      import shapelesslike.record.RecordSchema
      val forged = new RecordSchema[("x", "x"), (Int, Int)] {}
    """)
    assertErrorContains(errors, "sealed", "RecordSchema")

  test("cannot directly implement UpdateByName evidence"):
    val errors = typeCheckErrors("""
      import shapelesslike.record.UpdateByName
      val forged = new UpdateByName[("x", "y"), (Int, Int), "x", String, (String, Int)]:
        private[record] def apply(values: (Int, Int), replacement: String): (String, Int) =
          (replacement, values._2)
    """)
    assertErrorContains(errors, "sealed", "UpdateByName")

  test("cannot instantiate or subclass private RecordSchema implementations"):
    val errors = typeCheckErrors("""
      val empty = new shapelesslike.record.RecordSchema.Empty
      val cons = new shapelesslike.record.RecordSchema.Cons["x", EmptyTuple, Int, EmptyTuple]
    """)
    assertErrorContains(errors, "cannot be accessed", "RecordSchema")

  test("cannot instantiate or subclass private UpdateByName implementations"):
    val errors = typeCheckErrors("""
      val head = new shapelesslike.record.UpdateByName.Head["x", EmptyTuple, Int, EmptyTuple, String]
    """)
    assertErrorContains(errors, "cannot be accessed", "UpdateByName")
