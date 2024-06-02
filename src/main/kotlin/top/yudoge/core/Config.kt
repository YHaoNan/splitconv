package top.yudoge.top.yudoge.core

data class Config(
    val threads: Int,
    val outdir: String,
    val defaultOutFile: String,
    val tmpdir: String,
    val bufSize: Int,
    // 正在排队的split任务大小。当用户的splitf过重时，有可能有splitf的执行跟不上文件行的读取，当内存中排队的splitf
    // 到达此数量，此时就让读文件的线程阻塞一会儿
    val queueSplitTaskSize: Int,
    val hashedTmpFileName: Boolean
) {


    class Builder {
        private var threads: Int? = null
        private var outdir: String? = null
        private var defaultOutFile: String? = null
        private var tmpdir: String? = null
        private var bufSize: Int? = null
        private var queueSplitTaskSize: Int? = null
        private var hashedTmpFileName: Boolean? = null

        fun threads(threads: Int): Builder {
            this.threads = threads
            return this
        }

        fun outdir(outdir: String): Builder {
            this.outdir = outdir
            return this
        }

        fun defaultOutFile(defaultOutFile: String): Builder {
            this.defaultOutFile = defaultOutFile
            return this
        }

        fun tmpdir(tmpdir: String): Builder {
            this.tmpdir = tmpdir
            return this
        }

        fun bufSize(bufSize: Int): Builder {
            this.bufSize = bufSize
            return this
        }

        fun queueSplitTaskSize(queueSplitTaskSize: Int): Builder {
            this.queueSplitTaskSize = queueSplitTaskSize
            return this
        }

        fun hashedTmpFileName(hashedTmpFileName: Boolean): Builder {
            this.hashedTmpFileName = hashedTmpFileName
            return this
        }

        fun build(): Config {
            val default = defaultConfig()
            return Config(
                if (threads == null) default.threads else threads!!,
                if (outdir == null) default.outdir else outdir!!,
                if (defaultOutFile == null) default.defaultOutFile else defaultOutFile!!,
                if (tmpdir == null) default.tmpdir else tmpdir!!,
                if (bufSize == null) default.bufSize else bufSize!!,
                if (queueSplitTaskSize == null) default.queueSplitTaskSize else queueSplitTaskSize!!,
                if (hashedTmpFileName == null) default.hashedTmpFileName else hashedTmpFileName!!,
            )
        }
    }

    companion object {
        fun defaultConfig(): Config {
            return Config(
                1,
//                Runtime.getRuntime().availableProcessors(),
                "./out",
                "out.txt",
                "./tmp",
                // buffer大小1MB
                1024 * 1024,
                1000,
                false
            )
        }

    }

}

