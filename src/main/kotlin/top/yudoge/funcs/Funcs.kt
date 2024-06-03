package top.yudoge.top.yudoge.funcs

import top.yudoge.top.yudoge.core.Line
import java.io.InputStreamReader
import java.io.Writer

fun writeClassPathFileHook(filePath: String) = fun (writer: Writer) {
    val resourceAsStream = Thread.currentThread().contextClassLoader.getResourceAsStream(filePath)
    val reader = InputStreamReader(resourceAsStream!!)
    writer.write(reader.readText())
}


fun convByPrevLine(lines: Iterator<Line>, lineHandler: (prev: Line?, cur: Line) -> Unit) {
    var prevLine: Line? = null

    // 遍历每一行
    for (line in lines) {
        // 若有前一行，计算与前一行的时间差
        lineHandler(prevLine, line)
        prevLine = line
    }
}

