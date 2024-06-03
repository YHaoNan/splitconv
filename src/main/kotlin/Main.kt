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


    // 第二个SplitConv任务，输出HTML格式的日志Timeline页面
    runSplitConv(
        inputFile = logFile,
        splitf = ::splitf,
        convf = ::convfHtml,
        beforeHook = writeClassPathFileHook("html_pre"),
        endHook = writeClassPathFileHook("html_post"),
    )

}

fun splitf(sc: SplitConv, line: Line) {
    // 提取日志行中的线程名称，并以线程名分类
    val threadName = extractThreadName(line.content)
    // 只有那些具有线程名并且线程名以http-nio开头的被发射到conv阶段
    // 带来的效果就是没有被发射那些都被过滤掉了
    if (threadName != null && threadName.startsWith("http-nio")) {
        sc.emit(threadName, line)
    }
}

fun convf(sc: SplitConv, type: String, lines: Iterator<Line>) {
    // 由于我们此次输出纯文本文件和输出html文件的convf有太多通用部分，所以这里使用了一个公用模板
    // 公用模板会处理每一行日志，与它的前一行计算时间差，而我们提供的逻辑就是拿到该行日志和这个时间差
    // 编写纯文本和html不同的输出逻辑
    convfTemplate(lines) { line, timeDiff ->
        // 对于纯文本，就将时间差简单输出到日志行前面即可，即：+12ms 原始日志行
        // 并且对于纯文本，我们将输出文件输出到分类名.txt下，即线程名.txt
        sc.out(
            line    = line.compute { "+${timeDiff} ${it}" },
            outFile = "${type}.txt"
        )
    }
}

fun convfHtml(sc: SplitConv, type: String, lines: Iterator<Line>) {
    // convfHtml将每一行日志追加到最终的timeline页面中
    convfTemplate(lines) { line, timeDiff ->
        // 输出timeline前端页面能够解析的格式，实际上是：'>>>时间<<< 原始日志行（所有的单引号会被替换成\'）',
        // 最终的输出文件是分类名.html，即线程名.html
        sc.out(
            line    = line.compute { "'>>>${timeDiff}<<< ${it.replace("\'", "\\'")}'," },
            outFile = "${type}.html"
        )
    }
}

fun convfTemplate(lines: Iterator<Line>, out: (Line, String) -> Unit) {
    // 模板
    convByPrevLine(lines) { prev, cur ->
        var timeDiff = "0"
        // 如果有前一行，计算与前一行的时间差
        prev?.let {
            val prevTime = extractTime(prev.content)
            val curTime  = extractTime(cur.content)
            if (prevTime != null && curTime != null) {
                timeDiff = "${Duration.between(prevTime, curTime).abs().toMillis()}"
            }

            // 输出给上级
            out(cur, timeDiff)
        }
    }
}