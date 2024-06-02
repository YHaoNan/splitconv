package top.yudoge.top.yudoge.core

import org.slf4j.LoggerFactory
import java.io.File

class DiskFileLineSorter(private val file: File, private val lines: Int) {
    private val log = LoggerFactory.getLogger(this::class.java)
    /**
     * 外部归并排序
     *
     * 首先将文件拆分成blockSize大小的等分块
     */
    fun sort() {
        // do nothing 现在不支持多线程
        log.debug("[Sorting] ${file.name} --> ${lines} records")
    }
}