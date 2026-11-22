package io.github.zdyoyoyo.englishpractice.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** 雲端備份存放處的抽象（測試時換成假的） */
interface RemoteBackupStore {
    suspend fun read(): String?
    suspend fun write(content: String)
    suspend fun delete()
}

class DriveAuthException(message: String) : IOException(message)

/**
 * 直接呼叫 Google Drive REST API v3，只操作「App 專用資料夾」(appDataFolder)：
 * - 這個資料夾使用者在 Drive 介面看不到，其他 App 也讀不到
 * - 檔案算在「使用者自己的」Drive 容量，完全不經過開發者的伺服器
 * - 需要的授權範圍只有 drive.appdata（不能讀使用者其他檔案）
 */
class DriveAppDataClient(private val accessToken: String) : RemoteBackupStore {

    @Serializable private data class FileList(val files: List<DriveFile> = emptyList())
    @Serializable private data class DriveFile(val id: String, val name: String = "", val modifiedTime: String = "")

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun read(): String? = withContext(Dispatchers.IO) {
        val id = findFileId() ?: return@withContext null
        request("GET", "$API/files/$id?alt=media")
    }

    override suspend fun write(content: String) = withContext(Dispatchers.IO) {
        val id = findFileId()
        if (id == null) {
            val boundary = "backup_boundary_${System.nanoTime()}"
            val metadata = """{"name":"$FILE_NAME","parents":["appDataFolder"],"mimeType":"application/json"}"""
            val body = "--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n$metadata\r\n" +
                "--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n$content\r\n--$boundary--"
            request("POST", "$UPLOAD/files?uploadType=multipart", body, "multipart/related; boundary=$boundary")
        } else {
            // HttpURLConnection 不支援 PATCH，Google API 接受用這個 header 覆寫方法
            request("POST", "$UPLOAD/files/$id?uploadType=media", content, "application/json; charset=UTF-8",
                extraHeaders = mapOf("X-HTTP-Method-Override" to "PATCH"))
        }
        Unit
    }

    override suspend fun delete() = withContext(Dispatchers.IO) {
        findFileId()?.let { request("DELETE", "$API/files/$it") }
        Unit
    }

    private fun findFileId(): String? {
        val q = URLEncoder.encode("name = '$FILE_NAME'", "UTF-8")
        val text = request("GET", "$API/files?spaces=appDataFolder&q=$q&fields=files(id,name,modifiedTime)")
        return json.decodeFromString(FileList.serializer(), text).files.firstOrNull()?.id
    }

    private fun request(
        method: String,
        url: String,
        body: String? = null,
        contentType: String? = null,
        extraHeaders: Map<String, String> = emptyMap(),
    ): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            conn.setRequestProperty("Authorization", "Bearer $accessToken")
            extraHeaders.forEach { (k, v) -> conn.setRequestProperty(k, v) }
            if (body != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", contentType)
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = conn.responseCode
            if (code == 401 || code == 403) throw DriveAuthException("Drive 授權失效（HTTP $code）")
            if (code !in 200..299) {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw IOException("Drive API 錯誤 HTTP $code: ${err.take(200)}")
            }
            return conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    companion object {
        const val FILE_NAME = "english_practice_backup.json"
        private const val API = "https://www.googleapis.com/drive/v3"
        private const val UPLOAD = "https://www.googleapis.com/upload/drive/v3"
    }
}
