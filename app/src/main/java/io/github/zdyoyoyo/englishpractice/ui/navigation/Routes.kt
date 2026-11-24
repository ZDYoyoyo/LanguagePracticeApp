package io.github.zdyoyoyo.englishpractice.ui.navigation

import kotlinx.serialization.Serializable

/* Navigation 的「型別安全路由」：每個畫面是一個 @Serializable 物件，參數就是欄位 */

@Serializable object HomeRoute
@Serializable object PracticeSetupRoute
@Serializable object WrongBookRoute
@Serializable object FavoritesRoute
@Serializable object PkRoute
@Serializable object SettingsRoute
@Serializable object StatsRoute
@Serializable object WordListRoute

@Serializable
data class QuizRoute(
    val mode: String,
    /** 以下三個篩選都可複選，用逗號分隔；null = 全部 */
    val tags: String? = null,
    val difficulties: String? = null,
    /** QuestionCategory.key 清單 */
    val categories: String? = null,
    val count: Int = 10,
    /** 優先出沒做過 / 常錯的題目 */
    val preferNew: Boolean = true,
)
