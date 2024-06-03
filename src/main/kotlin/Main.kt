package top.yudoge

import top.yudoge.top.yudoge.core.*
import top.yudoge.top.yudoge.funcs.*
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
/**
 * SplitConvert示例程序
 *
 * SplitConvert概念、使用方法，详见 top.yudoge.core.SplitConv
 */
fun main() {
    val logFile = "C:\\Users\\15941\\Downloads\\all.log"

    // 第一个SplitConv任务，输出文本格式的日志文件
    runSplitConv(
        inputFile = logFile,
        splitf = ::splitf,
        convf = ::convf
    )


    // 第二个SplitConv任务，输出HTML格式的日志Timeline
    runSplitConv(
        inputFile = logFile,
        splitf = ::splitf,
        convf = ::convfHtml,
        beforeHook = writeClassPathFileHook("html_pre"),
        endHook = writeClassPathFileHook("html_post"),
    )

}

fun splitf(sc: SplitConv, line: Line) {
    // 以线程名分类
    val threadName = extractThreadName(line.content)
    // 只有那些具有线程名并且线程名以http-nio开头的被发射
    if (threadName != null && threadName.startsWith("http-nio")) {
        sc.emit(threadName, line)
    }
}

fun convf(sc: SplitConv, type: String, lines: Iterator<Line>) {
    convfTemplate(lines) { line, timeDiff ->
        sc.out(
            line    = line.compute { "+${timeDiff} ${it}" },
            outFile = "${type}.txt"
        )
    }
}

fun convfHtml(sc: SplitConv, type: String, lines: Iterator<Line>) {
    convfTemplate(lines) { line, timeDiff ->
        sc.out(
            line    = line.compute { "'>>>${timeDiff}<<< ${it.replace("\'", "\\'")}'," },
            outFile = "${type}.html"
        )
    }
}

fun convfTemplate(lines: Iterator<Line>, out: (Line, String) -> Unit) {
    convByPrevLine(lines) { prev, cur ->
        var timeDiff = "0"
        prev?.let {
            val prevTime = extractTime(prev.content)
            val curTime  = extractTime(cur.content)
            if (prevTime != null && curTime != null) {
                timeDiff = "${Duration.between(prevTime, curTime).abs().toMillis()}"
            }

            out(cur, timeDiff)
        }
    }
}