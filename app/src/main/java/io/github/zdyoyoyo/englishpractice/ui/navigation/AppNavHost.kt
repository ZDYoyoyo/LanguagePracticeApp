package io.github.zdyoyoyo.englishpractice.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.zdyoyoyo.englishpractice.data.StudyMode
import io.github.zdyoyoyo.englishpractice.ui.home.HomeScreen
import io.github.zdyoyoyo.englishpractice.ui.practice.PracticeSetupScreen
import io.github.zdyoyoyo.englishpractice.ui.quiz.QuizScreen
import io.github.zdyoyoyo.englishpractice.ui.wrongbook.WrongBookScreen

@Composable
fun AppNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = HomeRoute) {
        composable<HomeRoute> {
            HomeScreen(
                onPractice = { nav.navigate(PracticeSetupRoute) },
                onWrongBook = { nav.navigate(WrongBookRoute) },
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
    }
}
