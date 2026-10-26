package shapelesslike.examples

import shapelesslike.record.syntax.*

@main def namedTupleUpdateExample(): Unit =
  type Raw = (name: String, age: String, active: Boolean)
  type Parsed = (name: String, age: Int, active: Boolean)

  val raw: Raw = (name = "Ada", age = "37", active = true)
  val parsed: Parsed = raw.updatedByName("age", raw.age.toInt)

  println(s"parsed=${parsed.toTuple}, ageIsInt=${parsed.age}")
