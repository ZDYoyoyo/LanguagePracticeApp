package io.github.zdyoyoyo.englishpractice.ui.navigation

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.zdyoyoyo.englishpractice.EnglishApp
import io.github.zdyoyoyo.englishpractice.MainActivity
import io.github.zdyoyoyo.englishpractice.data.StudyMode
import io.github.zdyoyoyo.englishpractice.ui.favorites.FavoritesScreen
import io.github.zdyoyoyo.englishpractice.ui.home.HomeScreen
import io.github.zdyoyoyo.englishpractice.ui.pk.PkScreen
import io.github.zdyoyoyo.englishpractice.ui.practice.PracticeSetupScreen
import io.github.zdyoyoyo.englishpractice.ui.quiz.QuizScreen
import io.github.zdyoyoyo.englishpractice.ui.settings.SettingsScreen
import io.github.zdyoyoyo.englishpractice.ui.wrongbook.WrongBookScreen

@Composable
fun AppNavHost(pendingOpen: String? = null, onPendingOpenHandled: () -> Unit = {}) {
    val nav = rememberNavController()
    val activity = LocalActivity.current
    val ads = (LocalContext.current.applicationContext as EnglishApp).container.ads
    // 完成一輪後「可能」顯示插頁廣告（頻率由 AdPolicy 控制）
    val onSessionFinished: (Int) -> Unit = { count -> activity?.let { ads.onSessionCompleted(it, count) } }

    // 從通知點進來 → 直接開每日一題
    LaunchedEffect(pendingOpen) {
        if (pendingOpen == MainActivity.OPEN_DAILY) {
            nav.navigate(QuizRoute(StudyMode.DAILY.key)) { launchSingleTop = true }
            onPendingOpenHandled()
        }
    }

    NavHost(navController = nav, startDestination = HomeRoute) {
        composable<HomeRoute> {
            HomeScreen(
                onPractice = { nav.navigate(PracticeSetupRoute) },
                onReview = { nav.navigate(QuizRoute(StudyMode.REVIEW.key)) },
                onDaily = { nav.navigate(QuizRoute(StudyMode.DAILY.key)) },
                onWrongBook = { nav.navigate(WrongBookRoute) },
                onFavorites = { nav.navigate(FavoritesRoute) },
                onPk = { nav.navigate(PkRoute) },
                onSettings = { nav.navigate(SettingsRoute) },
            )
        }
        composable<PracticeSetupRoute> {
            PracticeSetupScreen(
                onBack = { nav.popBackStack() },
                onStart = { tag, difficulty ->
                    nav.navigate(QuizRoute(StudyMode.PRACTICE.key, tag, difficulty))
                },
            )
        }
        composable<QuizRoute> {
            QuizScreen(
                onBack = { nav.popBackStack() },
                onSessionFinished = onSessionFinished,
                onWatchRewardedAd = { onReward -> activity?.let { ads.showRewarded(it, onReward) } ?: false },
            )
        }
        composable<WrongBookRoute> {
            WrongBookScreen(
                onBack = { nav.popBackStack() },
                onRedo = { nav.navigate(QuizRoute(StudyMode.WRONG_BOOK.key)) },
            )
        }
        composable<FavoritesRoute> {
            FavoritesScreen(
                onBack = { nav.popBackStack() },
                onPractice = { nav.navigate(QuizRoute(StudyMode.FAVORITES.key)) },
            )
        }
        composable<PkRoute> { PkScreen(onBack = { nav.popBackStack() }, onMatchFinished = onSessionFinished) }
        composable<SettingsRoute> { SettingsScreen(onBack = { nav.popBackStack() }) }
    }
}
