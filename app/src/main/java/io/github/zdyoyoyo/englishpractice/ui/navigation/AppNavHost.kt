package io.github.zdyoyoyo.englishpractice.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.zdyoyoyo.englishpractice.MainActivity
import io.github.zdyoyoyo.englishpractice.data.StudyMode
import io.github.zdyoyoyo.englishpractice.ui.home.HomeScreen
import io.github.zdyoyoyo.englishpractice.ui.pk.PkScreen
import io.github.zdyoyoyo.englishpractice.ui.practice.PracticeSetupScreen
import io.github.zdyoyoyo.englishpractice.ui.quiz.QuizScreen
import io.github.zdyoyoyo.englishpractice.ui.settings.SettingsScreen
import io.github.zdyoyoyo.englishpractice.ui.wrongbook.WrongBookScreen

@Composable
fun AppNavHost(pendingOpen: String? = null, onPendingOpenHandled: () -> Unit = {}) {
    val nav = rememberNavController()

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
            QuizScreen(onBack = { nav.popBackStack() })
        }
        composable<WrongBookRoute> {
            WrongBookScreen(
                onBack = { nav.popBackStack() },
                onRedo = { nav.navigate(QuizRoute(StudyMode.WRONG_BOOK.key)) },
            )
        }
        composable<PkRoute> { PkScreen(onBack = { nav.popBackStack() }) }
        composable<SettingsRoute> { SettingsScreen(onBack = { nav.popBackStack() }) }
    }
}
