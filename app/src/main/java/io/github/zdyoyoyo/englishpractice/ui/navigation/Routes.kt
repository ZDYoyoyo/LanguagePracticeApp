package io.github.zdyoyoyo.englishpractice.ui.navigation

import kotlinx.serialization.Serializable

/* Navigation 的「型別安全路由」：每個畫面是一個 @Serializable 物件，參數就是欄位 */

@Serializable object HomeRoute
@Serializable object PracticeSetupRoute
@Serializable object WrongBookRoute
@Serializable object PkRoute
@Serializable object SettingsRoute

@Serializable
data class QuizRoute(
    val mode: String,
    val tag: String? = null,
    val difficulty: Int? = null,
)
