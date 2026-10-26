package shapelesslike.record

import scala.NamedTuple.NamedTuple
import scala.annotation.implicitNotFound
import scala.compiletime.ops.any.{==, IsConst}

/**
 * Positive evidence that names and values form a supported record schema.
 *
 * The tuples must have equal arity and the names must be distinct compile-time
 * string literals. Implementations are owned by this library so malformed
 * low-level named tuples cannot supply forged schema evidence in safe Scala.
 */
@implicitNotFound(
  "NamedTuple schema ${N}/${V} must have equal arity and distinct literal String names"
)
sealed trait RecordSchema[N <: Tuple, V <: Tuple]

object RecordSchema:
  private final class Empty extends RecordSchema[EmptyTuple, EmptyTuple]

  private final class Cons[
      K <: String & Singleton,
      NT <: Tuple,
      A,
      VT <: Tuple
  ] extends RecordSchema[K *: NT, A *: VT]

  given empty: RecordSchema[EmptyTuple, EmptyTuple] = new Empty

  given cons[
      K <: String & Singleton,
      NT <: Tuple,
      A,
      VT <: Tuple
  ](using
      IsConst[K] =:= true,
      Tuple.Contains[NT, K] =:= false,
      RecordSchema[NT, VT]
  ): RecordSchema[K *: NT, A *: VT] =
    new Cons[K, NT, A, VT]

/**
 * Structural evidence for replacing the value at the unique field `K`.
 *
 * `OutV` is the exact value tuple after replacement. The implementation walks
 * names and values in lockstep and never inspects a field name at runtime.
 */
@implicitNotFound(
  "No literal field ${K} can be updated in NamedTuple schema ${N}/${V} with output ${OutV}. The field must exist and the key must be a direct string literal or an explicitly singleton-typed value"
)
sealed trait UpdateByName[
    N <: Tuple,
    V <: Tuple,
    K <: String & Singleton,
    B,
    OutV <: Tuple
]:
  private[record] def apply(values: V, replacement: B): OutV

object UpdateByName:
  private final class Head[
      K <: String & Singleton,
      NT <: Tuple,
      A,
      VT <: Tuple,
      B
  ] extends UpdateByName[K *: NT, A *: VT, K, B, B *: VT]:
    def apply(values: A *: VT, replacement: B): B *: VT =
      replacement *: values.tail

  private final class Tail[
      H <: String & Singleton,
      NT <: Tuple,
      A,
      VT <: Tuple,
      K <: String & Singleton,
      B,
      OutT <: Tuple
  ](next: UpdateByName[NT, VT, K, B, OutT])
      extends UpdateByName[H *: NT, A *: VT, K, B, A *: OutT]:
    def apply(values: A *: VT, replacement: B): A *: OutT =
      values.head *: next(values.tail, replacement)

  given head[
      K <: String & Singleton,
      NT <: Tuple,
      A,
      VT <: Tuple,
      B
  ]: UpdateByName[K *: NT, A *: VT, K, B, B *: VT] =
    new Head[K, NT, A, VT, B]

  given tail[
      H <: String & Singleton,
      NT <: Tuple,
      A,
      VT <: Tuple,
      K <: String & Singleton,
      B,
      OutT <: Tuple
  ](using
      different: (H == K) =:= false,
      next: UpdateByName[NT, VT, K, B, OutT]
  ): UpdateByName[H *: NT, A *: VT, K, B, A *: OutT] =
    new Tail[H, NT, A, VT, K, B, OutT](next)

object syntax:
  extension [N <: Tuple, V <: Tuple](record: NamedTuple[N, V])
    /**
     * Replaces the existing literal-named field and computes its new value type.
     */
    def updatedByName[
        K <: String & Singleton,
        B,
        OutV <: Tuple
    ](key: K, replacement: B)(using
        schema: RecordSchema[N, V],
        update: UpdateByName[N, V, K, B, OutV]
    ): NamedTuple[N, OutV] =
      NamedTuple.apply[N, OutV](update(record.toTuple, replacement))
