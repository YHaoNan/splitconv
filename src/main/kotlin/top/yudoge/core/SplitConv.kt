package top.yudoge.top.yudoge.core

import java.io.Writer

/**
 * SplitConv是一个用于执行文本拆分任务的框架，一个常见的使用场景是日志文件的处理，比如你可能有如下需求：
 *  1. 按照日志文件中每行日志携带的线程名称拆分日志文件成多个文件
 *  2. 解析日志文件中不同类型的日志（如SQL、RPC），按不同的规则进行高亮处理（term高亮）
 *  3. 解析日志文件，输出人类友好的HTML分析页面
 *  4. 解析日志文件，去除每一行的前缀
 *  5. 解析日志文件，在每一行前面加上一些内容（如和上一行的时间差）
 *  6. 解析日志文件，只保留特定时间范围内的日志
 *
 * 日志文件往往很大，动辄几百MB，甚至GB，在处理这些任务时往往希望：
 *  1. 利用多核CPU的能力
 *  2. 使用有限的内存
 *
 * 我们不想对于每一个需求都编写一个特定用途的程序，因为上面那两个问题每次都要考虑一遍，而且可能并不容易
 * 对于上述每一个需求，它们的差异很小，但是多核处理和内存处理的代码可能要比实现那些需求的逻辑还多
 *
 * SplitConv框架将需求拆分成两个阶段：
 *  1. split阶段：将日志文件中的每一行分类
 *  2. conv阶段：对于每一个分类，执行分类对应的逻辑
 *
 * 我们只给用户暴露这两个函数让它们实现，比如上述第一个需求，用户可以编写这样的split conv函数：
 * ```kotlin
 * fun split(line) {
 *   // 从行中提取线程名
 *   var threadName = extractThreadName(line)
 *   // 向框架发射该行的分类结果
 *   emit(threadName, line)
 * }
 * ```
 *
 * ```kotlin
 * fun conv(type, lines) {
 *    // type是类型, lines是该类型下的所有行，它是一个迭代器
 *    for line in lines {
 *      // 向框架发出out请求，会将参数1原封不动的追加到outFileName文件下
 *      out(line, outFileName="${type}-out")
 *    }
 * }
 * ```
 *
 * 如果你知道大名鼎鼎的MapReduce框架，没错，这就是一个特定用途的MapReduce
 */
interface SplitConv {
    /**
     * setup方法用于向框架提供splitf和convf
     */
    fun setup(
        splitf: (SplitConv, Line) -> Unit,
        convf: (SplitConv, type: String, lines: Iterator<Line>) -> Unit
    )

    /**
     * 向框架发射一个分类结果
     */
    fun emit(type: String, line: Line)

    /**
     * 告诉框架可以将该行输出到outFile中
     */
    fun out(line: Line, outFile: String? = null)

    /**
     * 开始splitconv
     */
    fun start()

    /**
     * 每一个conv开始之前的hook
     */
    fun beforeHook(hook: (Writer)->Unit)

    /**
     * 每一个conv结束之后的hook
     */
    fun endHook(hook: (Writer)->Unit)
}