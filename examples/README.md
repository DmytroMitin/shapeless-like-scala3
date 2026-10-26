# Runnable examples

`Poly1TupleMapperExample.scala` is compiled as part of the test configuration and can be run with:

```text
sbt "Test / runMain shapelesslike.examples.poly1TupleMapperExample"
```

It maps `(Int, String, Boolean)` to exactly `(String, Int, Boolean)` using three independently typed cases.
