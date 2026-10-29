// Expected to fail when compiled alone against the built library.
package shapelesslike.coproduct
class Forged extends LabelledSum.LabelPosition.head["x",EmptyTuple,Int,EmptyTuple]
