// "Replace with 'newFun(p)'" "true"

@Deprecated("", ReplaceWith("newFun(p)"))
fun oldFun(vararg p: Double){
    newFun(p)
}

fun newFun(p: DoubleArray){}

fun foo() {
    <caret>oldFun(1.0, 2.0)
}
