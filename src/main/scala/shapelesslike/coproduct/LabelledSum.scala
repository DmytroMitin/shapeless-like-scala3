package shapelesslike.coproduct

import scala.annotation.implicitNotFound
import scala.compiletime.ops.any.{==, IsConst}
import scala.compiletime.ops.int.S
import shapelesslike.coproduct.Coproduct.{Handlers, Position}
import shapelesslike.record.RecordSchema

/** One active alternative addressed by a unique literal name in `Names`. */
type LabelledSum[Names <: Tuple, Values <: Tuple] = LabelledSum.Type[Names, Values]

object LabelledSum:
  /** Labels are static addresses; the runtime value is the original Coproduct. */
  opaque type Type[Names <: Tuple, Values <: Tuple] = Coproduct[Values]

  /** Validates the entire aligned schema before permitting injection. */
  def of[N <: Tuple, V <: Tuple](using
      @implicitNotFound("LabelledSum schema ${N}/${V} requires equal arity and globally distinct literal String labels (RecordSchema)")
      schema: RecordSchema[N, V]
  ): Builder[N, V] = new Builder[N, V]

  final class Builder[N <: Tuple, V <: Tuple] private[LabelledSum] ():
    /** The literal label fixes both the index and the exact static payload type. */
    def inject[K <: String & Singleton, I <: Int, A](key: K, value: A)(using
        IsConst[K] =:= true,
        LabelPosition[N, V, K, I, A],
        Position[V, I, A]
    ): LabelledSum[N, V] = Coproduct.at[V, I].inject[A](value)

  /** Explicitly interprets a raw carrier under a validated static label schema. */
  def fromCoproduct[N <: Tuple, V <: Tuple](raw: Coproduct[V])(using
      @implicitNotFound("LabelledSum attachment schema ${N}/${V} requires equal arity and globally distinct literal String labels (RecordSchema)")
      schema: RecordSchema[N, V]
  ): LabelledSum[N, V] = raw

  extension [N <: Tuple, V <: Tuple](sum: LabelledSum[N, V])
    /** Returns the exact original carrier object. */
    def toCoproduct: Coproduct[V] = sum

    /** The active zero-based position; labels are not stored at runtime. */
    def ordinal: Int = sum.toCoproduct.ordinal

    /** Projects the unique labelled position, even when payload types repeat. */
    def project[K <: String & Singleton, I <: Int, A](key: K)(using
        IsConst[K] =:= true,
        LabelPosition[N, V, K, I, A],
        Position[V, I, A]
    ): Option[A] = sum.toCoproduct.projectAtAs[I, A]

    /** Runs exactly one of the raw carrier's exhaustive positional handlers. */
    def fold[R](handlers: Handlers[V, R]): R = sum.toCoproduct.fold[R](handlers)

  /**
   * Invariant structural label/index/payload evidence, not a schema validator.
   * Generic callers carry this and Position; neither implies an abstract
   * Tuple.Elem equality. Only the owner can implement the relation.
   */
  @implicitNotFound("No literal label ${K} at index ${I} with exact payload ${A} in LabelledSum schema ${N}/${V}. Use a direct string literal or an explicitly singleton-typed key")
  sealed trait LabelPosition[N <: Tuple, V <: Tuple, K <: String & Singleton, I <: Int, A]

  object LabelPosition:
    private final class Head[K <: String & Singleton, NT <: Tuple, A, VT <: Tuple]
        extends LabelPosition[K *: NT, A *: VT, K, 0, A]

    private final class Tail[H <: String & Singleton, NT <: Tuple, B, VT <: Tuple,
        K <: String & Singleton, I <: Int, A]
        extends LabelPosition[H *: NT, B *: VT, K, S[I], A]

    given head[K <: String & Singleton, NT <: Tuple, A, VT <: Tuple]
        : LabelPosition[K *: NT, A *: VT, K, 0, A] = new Head[K, NT, A, VT]

    given tail[H <: String & Singleton, NT <: Tuple, B, VT <: Tuple,
        K <: String & Singleton, I <: Int, A](using
        different: (H == K) =:= false,
        next: LabelPosition[NT, VT, K, I, A]
    ): LabelPosition[H *: NT, B *: VT, K, S[I], A] = new Tail[H, NT, B, VT, K, I, A]
