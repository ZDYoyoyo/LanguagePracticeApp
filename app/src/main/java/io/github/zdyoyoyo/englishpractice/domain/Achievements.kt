package io.github.zdyoyoyo.englishpractice.domain

/** 計算成就需要的數字（全部從本機資料算出來） */
data class AchievementContext(
    val totalAttempts: Int = 0,
    val totalCorrect: Int = 0,
    val longestStreak: Int = 0,
    val goalDays: Int = 0,
    val perfectRounds: Int = 0,
    val mockCount: Int = 0,
    val bestMockScore: Int = 0,
    val listeningCorrect: Int = 0,
    val readingCorrect: Int = 0,
    val vocabPracticed: Int = 0,
    val masteredWrong: Int = 0,
    val coveragePercent: Int = 0,
)

data class Achievement(
    val id: String,
    val emoji: String,
    val title: String,
    val description: String,
    val target: Int,
    val progress: (AchievementContext) -> Int,
) {
    fun current(ctx: AchievementContext) = progress(ctx).coerceAtMost(target)
    fun isUnlocked(ctx: AchievementContext) = progress(ctx) >= target
}

object Achievements {
    val ALL: List<Achievement> = listOf(
        Achievement("first_step", "🌱", "第一步", "答對第一題", 1) { it.totalCorrect },
        Achievement("answers_100", "💯", "百題斬", "累計作答 100 次", 100) { it.totalAttempts },
        Achievement("answers_500", "🔥", "五百題達人", "累計作答 500 次", 500) { it.totalAttempts },
        Achievement("answers_1000", "👑", "千題王", "累計作答 1000 次", 1000) { it.totalAttempts },
        Achievement("streak_3", "📅", "三天打卡", "連續打卡 3 天", 3) { it.longestStreak },
        Achievement("streak_7", "🗓️", "一週不間斷", "連續打卡 7 天", 7) { it.longestStreak },
        Achievement("streak_30", "🏆", "月月堅持", "連續打卡 30 天", 30) { it.longestStreak },
        Achievement("goal_days_10", "🎯", "目標達人", "累計 10 天達成每日目標", 10) { it.goalDays },
        Achievement("perfect_round", "✨", "完美一輪", "一輪 5 題以上全部答對", 1) { it.perfectRounds },
        Achievement("perfect_5", "🌟", "完美主義", "5 輪全部答對", 5) { it.perfectRounds },
        Achievement("mock_first", "📝", "初試身手", "完成第一次模擬考", 1) { it.mockCount },
        Achievement("mock_5", "📈", "持續挑戰", "完成 5 次模擬考", 5) { it.mockCount },
        Achievement("mock_600", "🥉", "多益 600", "模擬考預估分數達 600", 600) { it.bestMockScore },
        Achievement("mock_800", "🥈", "多益 800", "模擬考預估分數達 800", 800) { it.bestMockScore },
        Achievement("mock_900", "🥇", "多益 900", "模擬考預估分數達 900", 900) { it.bestMockScore },
        Achievement("listening_50", "🎧", "順風耳", "聽力累計答對 50 題", 50) { it.listeningCorrect },
        Achievement("reading_50", "📖", "閱讀高手", "閱讀累計答對 50 題", 50) { it.readingCorrect },
        Achievement("vocab_100", "🔤", "單字收集家", "練過 100 個不同的單字", 100) { it.vocabPracticed },
        Achievement("mastered_10", "✅", "訂正達人", "從錯題本訂正 10 題", 10) { it.masteredWrong },
        Achievement("explorer_50", "🧭", "題庫探險家", "做過題庫一半的題目", 50) { it.coveragePercent },
    )

    fun byId(id: String) = ALL.firstOrNull { it.id == id }

    /** 回傳「現在達成、但還沒記錄為已解鎖」的成就 */
    fun newlyUnlocked(ctx: AchievementContext, alreadyUnlocked: Set<String>): List<Achievement> =
        ALL.filter { it.id !in alreadyUnlocked && it.isUnlocked(ctx) }
}
