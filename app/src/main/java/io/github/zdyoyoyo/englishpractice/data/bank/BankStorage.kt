package io.github.zdyoyoyo.englishpractice.data.bank

import android.content.Context
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** 下載來源（介面方便測試時換成假的） */
fun interface RemoteBankSource {
    /** 下載 baseUrl + path 的文字內容 */
    suspend fun fetch(path: String): String
}

/** 本機快取與 App 內建題庫的讀寫 */
interface BankStore {
    fun readCached(): String?
    fun writeCached(text: String)
    fun readBundled(): String
}

/** 用最基本的 HttpURLConnection（Java 工程師應該很熟悉）下載，不額外引入網路函式庫 */
class HttpRemoteBankSource(private val baseUrl: String) : RemoteBankSource {
    override suspend fun fetch(path: String): String {
        val conn = URL(baseUrl + path).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 15_000
        conn.useCaches = false
        try {
            if (conn.responseCode !in 200..299) throw IOException("HTTP ${conn.responseCode} for $path")
            return conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}

class FileBankStore(private val context: Context) : BankStore {
    private val file get() = File(context.filesDir, "question_bank.json")

    override fun readCached(): String? = file.takeIf { it.exists() }?.readText()

    /** 先寫暫存檔再改名，避免寫到一半當機導致快取壞掉 */
    override fun writeCached(text: String) {
        val tmp = File(context.filesDir, "question_bank.json.tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(file)) {
            file.writeText(text)
            tmp.delete()
        }
    }

    override fun readBundled(): String =
        context.assets.open("questions.json").bufferedReader().use { it.readText() }
}
