package top.yudoge.top.yudoge.core

data class Line(
    val no: Int,
    val noType: Int,
    val content: String
) {
    fun compute(func: (String)->String): Line {
        return Line(no, noType, func(content))
    }
}