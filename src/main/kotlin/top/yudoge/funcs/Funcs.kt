package top.yudoge.top.yudoge.funcs

import top.yudoge.top.yudoge.core.Line
import top.yudoge.top.yudoge.core.SplitConv
import top.yudoge.top.yudoge.core.extractThreadName
import top.yudoge.top.yudoge.core.extractTime
import java.time.Duration

/**
 * 根据线程名分类并且去除非http请求处理器的日志
 */
fun S_ByThreadNameAndFilterNonHttpNIO(sc: SplitConv, line: Line) {
    // 以线程名分类
    val threadName = extractThreadName(line.content)
    // 只有那些具有线程名并且线程名以http-nio开头的被发射
    if (threadName != null && threadName.startsWith("http-nio")) {
        sc.emit(threadName, line)
    }
}

/**
 * 转换器，在行前添加与上一行的间隔时间
 */
fun C_AddTimeDiffAndOutToMultiFiles(sc: SplitConv, type: String, lines: Iterator<Line>) {
    var prevLine: Line? = null

    // 遍历每一行
    for (line in lines) {
        var prefix: String = ""

        // 若有前一行，计算与前一行的时间差
        if (prevLine != null) {
            val curlineTime = extractTime(line.content)
            val prevLineTime = extractTime(prevLine.content)
            if (curlineTime != null && prevLineTime != null) {
                val dur = Duration.between(prevLineTime, curlineTime)
                prefix = "+${dur.abs().toMillis()}ms "
            }
        }

        // 输出行
        sc.out(line.compute { prefix + it }, type)

        prevLine = line
    }
}

