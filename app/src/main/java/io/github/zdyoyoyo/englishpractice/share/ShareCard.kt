package io.github.zdyoyoyo.englishpractice.share

import io.github.zdyoyoyo.englishpractice.domain.MockScore
import io.github.zdyoyoyo.englishpractice.domain.StatsCalculator

/** 分享圖片上要放的文字（純資料，方便測試） */
data class ShareCardContent(
    val headline: String,
    val bigValue: String,
    val bigLabel: String,
    val lines: List<String>,
)

object ShareCards {
    fun forMock(score: MockScore, streakDays: Int): ShareCardContent = ShareCardContent(
        headline = "多益模擬考成績",
        bigValue = "${score.estimatedTotal}",
        bigLabel = "預估多益分數",
        lines = listOfNotNull(
            "🎧 聽力 ${score.listeningCorrect}/${score.listeningTotal}　📖 閱讀 ${score.readingCorrect}/${score.readingTotal}",
            "可能落在 ${score.low}～${score.high} 分",
            if (streakDays > 0) "🔥 已連續打卡 $streakDays 天" else null,
        ),
    )

    fun forProgress(s: StatsCalculator.Summary, bestMock: Int?, achievementCount: Int): ShareCardContent = ShareCardContent(
        headline = "我的英語學習成果",
        bigValue = "${s.streak.longest}",
        bigLabel = "最長連續打卡（天）",
        lines = listOfNotNull(
            "✍️ 累計作答 ${s.totalAttempts} 次・正確率 ${(s.accuracy * 100).toInt()}%",
            "📚 做過 ${s.answeredQuestions} 題・已熟練 ${s.matureQuestions} 題",
            bestMock?.let { "📝 模擬考最佳預估 $it 分" },
            if (achievementCount > 0) "🏅 解鎖 $achievementCount 個成就" else null,
        ),
    )
}
