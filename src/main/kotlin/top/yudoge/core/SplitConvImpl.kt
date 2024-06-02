package top.yudoge.top.yudoge.core

import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.LineNumberReader
import java.io.Writer
import java.util.concurrent.BlockingQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock

/**
 * 目前假设split造出的分类不会太多，没有对分类多的情况做过多的内存优化
 *
 * 实测多线程目前也没比单线程强...
 * 所以 目前split阶段支持多线程，但是没写多线程写临时文件之后的reorder，如果使用多线程将会造成最终输出乱序的后果...
 * conv阶段直接不支持多线程
 */
class SplitConvImpl(
    private val inputFile: String,
    private val config: Config = Config.defaultConfig()
) : SplitConv {

    private lateinit var splitf: (SplitConv, Line) -> Unit
    private lateinit var convf: (SplitConv, type: String, lines: Iterator<Line>) -> Unit
    private var beforeHook: ((Writer) -> Unit)? = null
    private var endHook: ((Writer) -> Unit)? = null
    private val splitThreads: List<Thread>
    private val splitTaskQueue: BlockingQueue<Line>
    private val tmpFileWriters: MutableMap<String, Writer> = mutableMapOf()
    private val tmpFileWriteLock: MutableMap<String, ReentrantLock> = mutableMapOf()
    private val typeCounter: MutableMap<String, Int> = mutableMapOf()
    private val outFileWriter: MutableMap<String, Writer> = mutableMapOf()

    private val bigLock = ReentrantLock()

    private val splitDone: CountDownLatch = CountDownLatch(1)


    init {
        // 创建具有配置中指定数量个线程的线程池
        this.splitTaskQueue = LinkedBlockingQueue()
        this.splitThreads   = ArrayList<Thread>()
        for (i in 0..this.config.threads-1) {
            splitThreads.add(Thread(SplitTaskRunner(this)))
        }
    }

    private val log = LoggerFactory.getLogger(SplitConvImpl::class.java)

    override fun setup(splitf: (SplitConv, Line) -> Unit, convf: (SplitConv, type: String, lines: Iterator<Line>) -> Unit) {
        this.splitf = splitf
        this.convf = convf
    }

    override fun emit(type: String, line: Line) {
        this.bigLock.lock()
        if (!tmpFileWriters.containsKey(type)) {
            tmpFileWriters.put(
                type, BufferedWriter(FileWriter(File(this.config.tmpdir, this.tmpName(type))), this.config.bufSize)
            )
        }
        if (!tmpFileWriteLock.containsKey(type)) {
            tmpFileWriteLock.put(type, ReentrantLock())
        }
        if (!typeCounter.containsKey(type)) {
            typeCounter.put(type, 0)
        } else {
            typeCounter.put(type, typeCounter.get(type)!! + 1)
        }
        val noUnderType = typeCounter[type]!!
        this.bigLock.unlock()

        log.debug("[EMIT] type => ${type}, line => ${line.no}")
        tmpFileWriteLock[type]!!.lock()
        this.tmpFileWriters[type]!!.write("${line.no} ${noUnderType} ${line.content}\n")
        tmpFileWriteLock[type]!!.unlock()
    }

    override fun out(line: Line, outFile: String?) {
        val outFileName = outFile ?: this.config.defaultOutFile
        if (!outFileWriter.containsKey(outFileName)) {
            outFileWriter.put(
                outFileName, BufferedWriter(FileWriter(File(this.config.outdir, outFileName)), this.config.bufSize)
            )
            beforeHook?.let { it(outFileWriter[outFileName]!!) }
        }


        outFileWriter.get(outFileName)!!.write("${line.content}\n")
    }

    override fun start() {
        // split阶段
        this.splitPhase()
        // 重排序临时分类文件
        this.reorderTmp()
        // conv阶段
        this.convPhase()
    }

    override fun beforeHook(hook: (Writer) -> Unit) {
        this.beforeHook = hook
    }

    override fun endHook(hook: (Writer) -> Unit) {
        this.endHook = hook
    }

    private fun tmpName(type: String): String {
        if (this.config.hashedTmpFileName) {
            return md5(type)
        }
        return sanitizeFilename(type)
    }

    private fun callBeforeHook(writer: Writer) {
        log.debug("[BeforeHook] start...")
        if (this.beforeHook != null) this.beforeHook!!(writer)
        log.debug("[BeforeHook] end...")
    }
    private fun callEndHook(writer: Writer) {
        log.debug("[EndHook] start...")
        if (this.endHook != null) this.endHook!!(writer)
        log.debug("[EndHook] end...")
    }

    private fun splitPhase() {
        log.debug("[SPLIT] start...")
        // 1. 开始split阶段，即读取文件中每一行，调度到对应线程中
        // 开启所有split线程
        for (i in 0..this.config.threads-1) {
            this.splitThreads[i].start()
        }
        val reader = LineNumberReader(BufferedReader(FileReader(inputFile)))
        var lineNo = 0
        reader.forEachLine { line ->
            this.splitTaskQueue.put(Line(lineNo, -1, line))
            lineNo++
        }

        while (this.splitTaskQueue.isNotEmpty()) {
            log.debug("[SPLIT] all line has been read. waiting for all split task done... remaining ${this.splitTaskQueue.size}")
            Thread.sleep(1000)
        }

        // splitDone的目的是让所有splitThread线程发现全部splitTask已经处理完成，以退出
        // 当全部行读完，并且splitTaskQueue中已经空掉，此时splitThread中的线程不会再接到
        // 任何新的split任务，此时只要等它们执行完当前任务（若有）后等待看到countDown即可
        splitDone.countDown()
        for (t in this.splitThreads) {
            t.join() // 等待全部split线程退出
        }
        for (wr in this.tmpFileWriters) {
            wr.value.flush()
            wr.value.close()
        }

        log.debug("[SPLIT] end... {$lineNo} lines handled")
    }

    private fun reorderTmp() {
        val workers = mutableListOf<Thread>()
        for (type in this.typeCounter.keys) {
            workers.add(Thread {
                DiskFileLineSorter(File(this.config.tmpdir, this.tmpName(type)), this.typeCounter[type]!!)
            })
        }

        for (worker in workers) {
            worker.join()
        }
    }

    private fun convPhase() {
        val regex = Regex("""^(\d+) (\d+) (.*?)$""")
        for (type in this.typeCounter.keys) {
            val reader = LineNumberReader(
                BufferedReader(FileReader(File(this.config.tmpdir, this.tmpName(type))), this.config.bufSize)
            )

            val lineNo = AtomicInteger(0)

            val iterator = reader.lines().map { line ->
                val match = regex.find(line)
                if (match == null) {
                    log.warn("[CONV] faild to read ${type} lineNo. ${lineNo.get()} -- contents: ${line}")
                    throw RuntimeException()
                }
                val totalLineNum = match!!.groupValues[1].toInt()
                val typeLineNum = match.groupValues[2].toInt()
                val content = match.groupValues[3]

                lineNo.incrementAndGet()

                Line(totalLineNum, typeLineNum, content)
            }

            this.convf(this, type, iterator.iterator())
            for (wr in outFileWriter) {
                wr.value.flush()
            }
        }

        for (wr in outFileWriter) {
            callEndHook(wr.value)
            wr.value.flush()
            wr.value.close()
        }
    }


    class SplitTaskRunner(val sc: SplitConvImpl) : Runnable {
        private val log = LoggerFactory.getLogger(SplitTaskRunner::class.java)
        override fun run() {
            while (sc.splitDone.count == 1L) {
                val line = sc.splitTaskQueue.poll(1, TimeUnit.SECONDS)
                if (line != null) {
                    log.debug("[SplitRunner] take one line ${line.no}")
                    try {
                        sc.splitf(sc, line)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            log.debug("[SplitRunner] exit...")
        }
    }
}
