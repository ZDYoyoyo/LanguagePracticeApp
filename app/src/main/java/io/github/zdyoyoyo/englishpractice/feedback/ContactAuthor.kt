package io.github.zdyoyoyo.englishpractice.feedback

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import io.github.zdyoyoyo.englishpractice.BuildConfig

/**
 * 聯絡作者：開啟使用者自己的 Email App 並預填內容。
 * 不經過任何伺服器；使用者看過內容、自己按「傳送」才會寄出。
 */
object ContactAuthor {
    val email: String get() = BuildConfig.SUPPORT_EMAIL
    val isConfigured: Boolean get() = email.isNotBlank()

    fun sendFeedback(context: Context): Boolean =
        open(context, "[英語練習] 建議 / 問題回報", "請在這裡寫下你的建議或遇到的問題：\n\n\n" + deviceInfo())

    fun reportQuestion(context: Context, questionId: String, prompt: String): Boolean =
        open(
            context,
            "[英語練習] 題目回報 $questionId",
            "題目：$prompt\n題號：$questionId\n\n哪裡有問題？（例如答案錯誤、解析不清楚、錯字）\n\n\n" + deviceInfo(),
        )

    /** 只附上排查問題需要的基本資訊，讓使用者在寄出前看得到 */
    private fun deviceInfo() =
        "---\nApp 版本：${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n" +
            "Android：${Build.VERSION.RELEASE}\n機型：${Build.MANUFACTURER} ${Build.MODEL}\n"

    private fun open(context: Context, subject: String, body: String): Boolean {
        if (!isConfigured) return false
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }
}
