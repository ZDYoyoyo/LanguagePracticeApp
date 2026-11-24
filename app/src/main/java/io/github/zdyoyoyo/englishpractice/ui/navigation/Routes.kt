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
    val tag: String? = null,
    val difficulty: Int? = null,
    /** QuestionCategory.key，null = 全部題型 */
    val category: String? = null,
    val count: Int = 10,
    /** 優先出沒做過 / 常錯的題目 */
    val preferNew: Boolean = true,
)
