package top.yudoge

import top.yudoge.top.yudoge.core.*
import top.yudoge.top.yudoge.funcs.C_AddTimeDiffAndOutToMultiFiles
import top.yudoge.top.yudoge.funcs.S_ByThreadNameAndFilterNonHttpNIO
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
    val startTime = System.currentTimeMillis()
    val sc = SplitConvImpl("C:\\Users\\15941\\Downloads\\all.log")
    sc.setup(::S_ByThreadNameAndFilterNonHttpNIO, ::C_AddTimeDiffAndOutToMultiFiles)
    sc.start()
    print(System.currentTimeMillis() - startTime)
}