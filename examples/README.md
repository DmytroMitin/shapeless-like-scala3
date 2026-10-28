# Runnable examples

`Poly1TupleMapperExample.scala` is compiled as part of the test configuration and can be run with:

```text
sbt "Test / runMain shapelesslike.examples.poly1TupleMapperExample"
```

It maps `(Int, String, Boolean)` to exactly `(String, Int, Boolean)` using three independently typed cases.

`OrderedCoproductExample.scala` demonstrates a generic decoder-result tuple whose first and third alternatives are both `Int`, then preserves that the hexadecimal branch at position 2 succeeded:

```text
sbt "Test / runMain shapelesslike.examples.orderedCoproductExample"
```

`NamedTupleUpdateExample.scala` parses one field of a standard named tuple from
`String` to `Int` while preserving its literal labels and other values:

```text
sbt "Test / runMain shapelesslike.examples.namedTupleUpdateExample"
```

`TuplePartitionExactExample.scala` partitions a typed pipeline into both Warning
occurrences and the exact remaining Request/Cache tuple:

```text
sbt "Test / runMain shapelesslike.examples.tuplePartitionExactExample"
```
