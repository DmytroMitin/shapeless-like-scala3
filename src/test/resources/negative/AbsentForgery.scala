package shapelesslike.poly

val forgedAbsent = new Absent[EmptyTuple, Int] {}

val forgedDerivedAbsent = new Absent.derived[EmptyTuple, Int] {}
