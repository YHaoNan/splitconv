package top.yudoge.top.yudoge.core

import java.io.Writer
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun md5(input: String): String {
    val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
    return bytes.joinToString("") {
        "%02x".format(it)
    }
}

fun sanitizeFilename(input: String): String {
    // 定义文件名中允许的字符集合
    val allowedChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789._-"

    // 过滤字符串中不允许的字符，并限制长度为前 50 个字符
    return input.filter { it in allowedChars }.take(50)
}

val pattern = "\\[(.*?)\\]".toRegex()
fun extractThreadName(input: String): String? {
    val matches = pattern.findAll(input).toList()

    return if (matches.size > 1) {
        matches[1].groupValues[1]
    } else {
        null
    }
}
val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:SSS")

fun extractTime(input: String): LocalDateTime? {
    if (input.length < 22) return null
    val timeStr = input.substring(0, 23)
    try {
        val time = LocalDateTime.parse(timeStr, formatter)
        return time
    } catch (e: Exception) { }
    return null
}

fun LocalDateTime.isBefore(
    timeStr: String,
    formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
): Boolean {
    var date = formatter.parse(timeStr, LocalDateTime::from)
    return this.isBefore(date)
}

fun LocalDateTime.isAfter(
    timeStr: String,
    formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
): Boolean {
    var date = formatter.parse(timeStr, LocalDateTime::from)
    return this.isAfter(date)
}

fun runSplitConv(
    inputFile: String,
    splitf: (SplitConv, Line) -> Unit,
    convf: (SplitConv, type: String, lines: Iterator<Line>) -> Unit,
    config: Config = Config.defaultConfig(),
    beforeHook: ((Writer) -> Unit)? = null,
    endHook: ((Writer) -> Unit)? = null
) {
    val sc = SplitConvImpl(inputFile, config)
    sc.setup(splitf, convf)
    beforeHook?.let { sc.beforeHook(it) }
    endHook?.let { sc.endHook(it) }

    sc.start()
}

