package io.github.zdyoyoyo.englishpractice.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import java.io.File

/**
 * 把 [ShareCardContent] 畫成 1080×1350 的圖片（IG / LINE 都適合的 4:5 直式），
 * 存在 App 的暫存資料夾，再用系統分享選單分享。不需要任何儲存權限。
 */
object ShareCardRenderer {
    private const val W = 1080
    private const val H = 1350

    fun render(content: ShareCardContent): Bitmap {
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        // 背景漸層
        val bg = Paint().apply {
            shader = LinearGradient(0f, 0f, W.toFloat(), H.toFloat(), Color.parseColor("#2E6BE6"), Color.parseColor("#1A3E8C"), Shader.TileMode.CLAMP)
        }
        c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), bg)
        // 中間的白色卡片
        val card = RectF(70f, 220f, W - 70f, H - 230f)
        c.drawRoundRect(card, 48f, 48f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })

        fun text(size: Float, color: Int, bold: Boolean = false) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            textAlign = Paint.Align.CENTER
        }
        val cx = W / 2f
        val maxWidth = card.width() - 80f
        /** 文字太長就縮小字級，確保不超出卡片 */
        fun fit(p: Paint, str: String): Paint {
            while (p.measureText(str) > maxWidth && p.textSize > 20f) p.textSize -= 2f
            return p
        }
        c.drawText("英語練習", cx, 140f, text(64f, Color.WHITE, bold = true))
        c.drawText(content.headline, cx, 330f, fit(text(52f, Color.parseColor("#1A3E8C"), bold = true), content.headline))
        c.drawText(content.bigValue, cx, 600f, fit(text(230f, Color.parseColor("#2E6BE6"), bold = true), content.bigValue))
        c.drawText(content.bigLabel, cx, 680f, fit(text(44f, Color.parseColor("#555555")), content.bigLabel))
        var y = 820f
        content.lines.forEach { line ->
            c.drawText(line, cx, y, fit(text(42f, Color.parseColor("#333333")), line))
            y += 80f
        }
        c.drawText("多益單字・文法・聽力・閱讀", cx, H - 140f, text(40f, Color.WHITE))
        c.drawText("一起來練英文吧！", cx, H - 80f, text(36f, Color.parseColor("#DDE6FF")))
        return bmp
    }

    /** 產生圖片並開啟分享選單 */
    fun share(context: Context, content: ShareCardContent) {
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(dir, "english-practice-share.png")
        file.outputStream().use { render(content).compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "我在用「英語練習」準備多益！")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "分享成績").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
