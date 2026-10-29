package shapelesslike.coproduct

import scala.compiletime.testing.{Error, typeCheckErrors}

class LabelledSumCompileErrorsSuite extends munit.FunSuite:
  private def assertErrorContains(errors: List[Error], fragments: String*): Unit =
    assert(errors.nonEmpty, "expected compilation to fail")
    val messages = errors.map(_.message).mkString("\n")
    fragments.foreach(fragment => assert(messages.contains(fragment), messages))

  test("rejects a missing literal label"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.{Coproduct, LabelledSum}
      type N = ("decimal", "text", "hex")
      type V = (Int, String, Int)
      val sum = LabelledSum.of[N,V].inject("missing", 42)
    """)
    assertErrorContains(errors, "No literal label", "missing")

  test("rejects a wrong payload at a present label"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.{Coproduct, LabelledSum}
      type N = ("decimal", "text", "hex")
      type V = (Int, String, Int)
      val sum = LabelledSum.of[N,V].inject("text", 42)
    """)
    assertErrorContains(errors, "No literal label", "Int")

  test("rejects injection into an empty schema"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.{Coproduct, LabelledSum}
      val sum = LabelledSum.of[EmptyTuple,EmptyTuple].inject("missing",42)
    """)
    assertErrorContains(errors, "No literal label", "EmptyTuple")

  test("wrong expected projection payload cannot steer to another alternative"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.{Coproduct, LabelledSum}
      type N = ("decimal", "text", "hex")
      type V = (Int, String, Int)
      val sum = LabelledSum.of[N,V].inject("hex",42)
      val wrong: Option[String] = sum.project("hex")
    """)
    assertErrorContains(errors, "No literal label", "String")

  test("wrong explicit projection index cannot select an equal payload occurrence"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.{Coproduct, LabelledSum}
      type N = ("decimal", "text", "hex")
      type V = (Int, String, Int)
      val sum = LabelledSum.of[N,V].inject("hex",42)
      val wrong = sum.project["hex",0,Int]("hex")
    """)
    assertErrorContains(errors, "No literal label", "hex")

  test("wrong explicit injection index cannot select an equal payload occurrence"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.{Coproduct, LabelledSum}
      type N = ("decimal", "text", "hex")
      type V = (Int, String, Int)
      val wrong = LabelledSum.of[N,V].inject["hex",0,Int]("hex",42)
    """)
    assertErrorContains(errors, "No literal label", "hex")

  test("expected index cannot steer the lookup relation"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.{Coproduct, LabelledSum}
      import shapelesslike.coproduct.LabelledSum.LabelPosition
      type N = ("decimal", "text", "hex")
      type V = (Int, String, Int)
      val wrong = summon[LabelPosition[N,V,"hex",0,Int]]
    """)
    assertErrorContains(errors, "No literal label", "hex")

  test("expected payload cannot steer the lookup relation"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.{Coproduct, LabelledSum}
      import shapelesslike.coproduct.LabelledSum.LabelPosition
      type N = ("decimal", "text", "hex")
      type V = (Int, String, Int)
      val wrong = summon[LabelPosition[N,V,"text",1,Int]]
    """)
    assertErrorContains(errors, "No literal label", "Int")

  test("builder injection rejects duplicate same payload"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.{Coproduct, LabelledSum}
      type N = ("x","x")
      type V = (Int,Int)
      val sum = LabelledSum.of[N,V].inject("x",42)
    """)
    assertErrorContains(errors, "LabelledSum schema", "globally distinct literal String")

  test("raw attachment rejects duplicate same payload"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.{Coproduct, LabelledSum}
      type N = ("x","x")
      type V = (Int,Int)
      val raw = Coproduct.at[V,0].inject(42)
      val sum = LabelledSum.fromCoproduct[N,V](raw)
    """)
    assertErrorContains(errors, "LabelledSum attachment schema", "globally distinct literal String")

  test("builder injection rejects duplicate different payload"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.{Coproduct, LabelledSum}
      type N = ("x","x")
      type V = (Int,String)
      val sum = LabelledSum.of[N,V].inject("x",42)
    """)
    assertErrorContains(errors, "LabelledSum schema", "globally distinct literal String")

  test("raw attachment rejects duplicate different payload"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.{Coproduct, LabelledSum}
      type N = ("x","x")
      type V = (Int,String)
      val raw = Coproduct.at[V,0].inject(42)
      val sum = LabelledSum.fromCoproduct[N,V](raw)
    """)
    assertErrorContains(errors, "LabelledSum attachment schema", "globally distinct literal String")

  test("builder injection rejects fewer names than values"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.{Coproduct, LabelledSum}
      type N = Tuple1["x"]
      type V = (Int,String)
      val sum = LabelledSum.of[N,V].inject("x",42)
    """)
    assertErrorContains(errors, "LabelledSum schema", "globally distinct literal String")

  test("raw attachment rejects fewer names than values"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.{Coproduct, LabelledSum}
      type N = Tuple1["x"]
      type V = (Int,String)
      val raw = Coproduct.at[V,0].inject(42)
      val sum = LabelledSum.fromCoproduct[N,V](raw)
    """)
    assertErrorContains(errors, "LabelledSum attachment schema", "globally distinct literal String")

  test("builder injection rejects more names than values"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.{Coproduct, LabelledSum}
      type N = ("x","y")
      type V = Tuple1[Int]
      val sum = LabelledSum.of[N,V].inject("x",42)
    """)
    assertErrorContains(errors, "LabelledSum schema", "globally distinct literal String")

  test("raw attachment rejects more names than values"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.{Coproduct, LabelledSum}
      type N = ("x","y")
      type V = Tuple1[Int]
      val raw = Coproduct.at[V,0].inject(42)
      val sum = LabelledSum.fromCoproduct[N,V](raw)
    """)
    assertErrorContains(errors, "LabelledSum attachment schema", "globally distinct literal String")

  test("builder injection rejects non-string singleton name"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.{Coproduct, LabelledSum}
      type N = Tuple1[1]
      type V = Tuple1[Int]
      val sum = LabelledSum.of[N,V].inject("x",42)
    """)
    assertErrorContains(errors, "LabelledSum schema", "globally distinct literal String")

  test("raw attachment rejects non-string singleton name"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.{Coproduct, LabelledSum}
      type N = Tuple1[1]
      type V = Tuple1[Int]
      val raw = Coproduct.at[V,0].inject(42)
      val sum = LabelledSum.fromCoproduct[N,V](raw)
    """)
    assertErrorContains(errors, "LabelledSum attachment schema", "globally distinct literal String")

  test("builder injection rejects general String name"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.{Coproduct, LabelledSum}
      type N = Tuple1[String]
      type V = Tuple1[Int]
      val sum = LabelledSum.of[N,V].inject("x",42)
    """)
    assertErrorContains(errors, "LabelledSum schema", "globally distinct literal String")

  test("raw attachment rejects general String name"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.{Coproduct, LabelledSum}
      type N = Tuple1[String]
      type V = Tuple1[Int]
      val raw = Coproduct.at[V,0].inject(42)
      val sum = LabelledSum.fromCoproduct[N,V](raw)
    """)
    assertErrorContains(errors, "LabelledSum attachment schema", "globally distinct literal String")

  test("builder injection rejects literal union name"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.{Coproduct, LabelledSum}
      type N = Tuple1["x" | "y"]
      type V = Tuple1[Int]
      val sum = LabelledSum.of[N,V].inject("x",42)
    """)
    assertErrorContains(errors, "LabelledSum schema", "globally distinct literal String")

  test("raw attachment rejects literal union name"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.{Coproduct, LabelledSum}
      type N = Tuple1["x" | "y"]
      type V = Tuple1[Int]
      val raw = Coproduct.at[V,0].inject(42)
      val sum = LabelledSum.fromCoproduct[N,V](raw)
    """)
    assertErrorContains(errors, "LabelledSum attachment schema", "globally distinct literal String")

  test("builder injection rejects duplicate labels introduced by concat"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.{Coproduct, LabelledSum}
      type N = Tuple.Concat[Tuple1["x"],Tuple1["x"]]
      type V = (Int,Int)
      val sum = LabelledSum.of[N,V].inject("x",42)
    """)
    assertErrorContains(errors, "LabelledSum schema", "globally distinct literal String")

  test("raw attachment rejects duplicate labels introduced by concat"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.{Coproduct, LabelledSum}
      type N = Tuple.Concat[Tuple1["x"],Tuple1["x"]]
      type V = (Int,Int)
      val raw = Coproduct.at[V,0].inject(42)
      val sum = LabelledSum.fromCoproduct[N,V](raw)
    """)
    assertErrorContains(errors, "LabelledSum attachment schema", "globally distinct literal String")

  test("builder rejects abstract singleton schema labels"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.{Coproduct, LabelledSum}
      def bad[K <: String & Singleton] = LabelledSum.of[Tuple1[K],Tuple1[Int]]
    """)
    assertErrorContains(errors, "LabelledSum schema")

  test("attachment rejects abstract singleton schema labels"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.{Coproduct, LabelledSum}
      def bad[K <: String & Singleton](raw: Coproduct[Tuple1[Int]]) = LabelledSum.fromCoproduct[Tuple1[K],Tuple1[Int]](raw)
    """)
    assertErrorContains(errors, "LabelledSum attachment schema")

  test("builder rejects an unknown singleton path as a name"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.{Coproduct, LabelledSum}
      def bad(label: String & Singleton) = LabelledSum.of[Tuple1[label.type],Tuple1[Int]]
    """)
    assertErrorContains(errors, "LabelledSum schema")

  test("attachment rejects an unknown singleton path as a name"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.{Coproduct, LabelledSum}
      def bad(label: String & Singleton, raw: Coproduct[Tuple1[Int]]) = LabelledSum.fromCoproduct[Tuple1[label.type],Tuple1[Int]](raw)
    """)
    assertErrorContains(errors, "LabelledSum attachment schema")

  test("runtime String injection keys are not literals"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.{Coproduct, LabelledSum}
      type N = ("decimal", "text", "hex")
      type V = (Int, String, Int)
      def bad(key: String) = LabelledSum.of[N,V].inject(key,42)
    """)
    assertErrorContains(errors, "Cannot prove")

  test("widened stable String projection keys are not literals"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.{Coproduct, LabelledSum}
      type N = ("decimal", "text", "hex")
      type V = (Int, String, Int)
      val sum = LabelledSum.of[N,V].inject("hex",42)
      val key = "hex"
      val wrong = sum.project(key)
    """)
    assertErrorContains(errors, "Cannot prove")

  test("abstract key forwarding needs a literal proof"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.{Coproduct, LabelledSum}
      import shapelesslike.coproduct.LabelledSum.LabelPosition
      import shapelesslike.coproduct.Coproduct.Position
      import shapelesslike.record.RecordSchema
      def bad[K <: String & Singleton](key: K)(using LabelPosition[Tuple1[K],Tuple1[Int],K,0,Int], Position[Tuple1[Int],0,Int], RecordSchema[Tuple1[K],Tuple1[Int]]) = LabelledSum.of[Tuple1[K],Tuple1[Int]].inject[K,0,Int](key,42)
    """)
    assertErrorContains(errors, "Cannot prove")

  test("abstract builder cannot omit whole-schema evidence"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.{Coproduct, LabelledSum}
      def bad[N <: Tuple,V <: Tuple] = LabelledSum.of[N,V]
    """)
    assertErrorContains(errors, "LabelledSum schema")

  test("abstract attachment cannot omit whole-schema evidence"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.{Coproduct, LabelledSum}
      def bad[N <: Tuple,V <: Tuple](raw: Coproduct[V]) = LabelledSum.fromCoproduct[N,V](raw)
    """)
    assertErrorContains(errors, "LabelledSum attachment schema")

  test("generic injection cannot omit lookup evidence"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.{Coproduct, LabelledSum}
      import shapelesslike.coproduct.Coproduct.Position
      import shapelesslike.record.RecordSchema
      import scala.compiletime.ops.any.IsConst
      def bad[N <: Tuple,V <: Tuple,K <: String & Singleton,I <: Int,A](key: K, value: A)(using RecordSchema[N,V], IsConst[K] =:= true, Position[V,I,A]) = LabelledSum.of[N,V].inject[K,I,A](key,value)
    """)
    assertErrorContains(errors, "No literal label")

  test("generic projection cannot omit Position evidence"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.{Coproduct, LabelledSum}
      import shapelesslike.coproduct.LabelledSum.LabelPosition
      import scala.compiletime.ops.any.IsConst
      def bad[N <: Tuple,V <: Tuple,K <: String & Singleton,I <: Int,A](sum: LabelledSum[N,V], key: K)(using IsConst[K] =:= true, LabelPosition[N,V,K,I,A]) = sum.project[K,I,A](key)
    """)
    assertErrorContains(errors, "Index")

  test("unresolved head cannot be skipped before becoming equal under substitution"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.{Coproduct, LabelledSum}
      import shapelesslike.coproduct.LabelledSum.LabelPosition
      import scala.compiletime.ops.any.IsConst
      def skip[H <: String & Singleton,K <: String & Singleton](using IsConst[H] =:= true, IsConst[K] =:= true): LabelPosition[(H,K),(Int,Int),K,1,Int] = summon[LabelPosition[(H,K),(Int,Int),K,1,Int]]
    """)
    assertErrorContains(errors, "No literal label")

  test("explicit tail construction still needs positive inequality"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.{Coproduct, LabelledSum}
      import shapelesslike.coproduct.LabelledSum.LabelPosition
      def skip[H <: String & Singleton,K <: String & Singleton] = LabelPosition.tail[H,Tuple1[K],Int,Tuple1[Int],K,0,Int]
    """)
    assertErrorContains(errors, "Cannot prove")

  test("abstract lookup and Position do not prove a Tuple Elem equality"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.{Coproduct, LabelledSum}
      import shapelesslike.coproduct.LabelledSum.LabelPosition
      import shapelesslike.coproduct.Coproduct.Position
      def bridge[N <: Tuple,V <: Tuple,K <: String & Singleton,I <: Int,A](using LabelPosition[N,V,K,I,A], Position[V,I,A]): Tuple.Elem[V,I] =:= A = summon[Tuple.Elem[V,I] =:= A]
    """)
    assertErrorContains(errors, "Cannot prove")

  test("raw carrier cannot be assigned directly to opaque view"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.{Coproduct, LabelledSum}
      type N = ("decimal", "text", "hex")
      type V = (Int, String, Int)
      val raw = Coproduct.at[V,2].inject(42)
      val forged: LabelledSum[N,V] = raw
    """)
    assertErrorContains(errors, "Required:", "LabelledSum")

  test("opaque view cannot be assigned directly to raw carrier"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.{Coproduct, LabelledSum}
      type N = ("decimal", "text", "hex")
      type V = (Int, String, Int)
      val sum = LabelledSum.of[N,V].inject("hex",42)
      val raw: Coproduct[V] = sum
    """)
    assertErrorContains(errors, "Required:", "Coproduct")

  test("different label schemas are not the same static type"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.{Coproduct, LabelledSum}
      type N = ("decimal", "text", "hex")
      type V = (Int, String, Int)
      val sum = LabelledSum.of[N,V].inject("hex",42)
      val wrong: LabelledSum[("count","word","code"),V] = sum
    """)
    assertErrorContains(errors, "Required:", "count")

  test("private validated builder constructor is inaccessible"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.{Coproduct, LabelledSum}
      val forged = new LabelledSum.Builder[("x","x"),(Int,Int)]()
    """)
    assertErrorContains(errors, "cannot be accessed")

  test("sealed lookup cannot be implemented"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.{Coproduct, LabelledSum}
      import shapelesslike.coproduct.LabelledSum.LabelPosition
      val forged = new LabelPosition[Tuple1["x"],Tuple1[Int],"x",0,Int] {}
    """)
    assertErrorContains(errors, "sealed", "LabelPosition")

  test("private lookup Head cannot be constructed"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.{Coproduct, LabelledSum}
      import shapelesslike.coproduct.LabelledSum.LabelPosition
      val forged = new LabelPosition.Head["x",EmptyTuple,Int,EmptyTuple]
    """)
    assertErrorContains(errors, "cannot be accessed")

  test("singleton payload inference cannot widen to an unrelated exact payload"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.{Coproduct, LabelledSum}
      val sum = LabelledSum.of[Tuple1["one"],Tuple1[1]].inject("one",1)
    """)
    assertErrorContains(errors, "No literal label", "Int")

  test("delegated fold rejects missing handlers"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.{Coproduct, LabelledSum}
      type N = ("decimal", "text", "hex")
      type V = (Int, String, Int)
      val sum = LabelledSum.of[N,V].inject("hex",42)
      val bad = sum.fold[String](((i: Int) => i.toString, (s: String) => s))
    """)
    assertErrorContains(errors, "Required:", "Int => String")

  test("delegated fold rejects extra handlers"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.{Coproduct, LabelledSum}
      val sum = LabelledSum.of[Tuple1["x"],Tuple1[Int]].inject("x",42)
      val bad = sum.fold[String](((i: Int) => i.toString, (s: String) => s))
    """)
    assertErrorContains(errors, "Required:", "EmptyTuple")

  test("delegated fold rejects wrong handler payload"):
    val errors = typeCheckErrors("""
      import shapelesslike.coproduct.{Coproduct, LabelledSum}
      type N = ("decimal", "text", "hex")
      type V = (Int, String, Int)
      val sum = LabelledSum.of[N,V].inject("hex",42)
      val bad = sum.fold[String](((i: Int) => i.toString, (s: String) => s, (s: String) => s))
    """)
    assertErrorContains(errors, "Required:", "Int => String")
