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
