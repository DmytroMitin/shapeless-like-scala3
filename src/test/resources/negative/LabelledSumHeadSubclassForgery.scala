// Expected to fail when compiled alone against the built library.
package shapelesslike.coproduct
class Forged extends LabelledSum.LabelPosition.Head["x",EmptyTuple,Int,EmptyTuple]
