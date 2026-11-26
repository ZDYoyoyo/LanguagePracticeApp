package io.github.zdyoyoyo.englishpractice.ui.navigation

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import io.github.zdyoyoyo.englishpractice.ui.onboarding.OnboardingScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.zdyoyoyo.englishpractice.EnglishApp
import io.github.zdyoyoyo.englishpractice.announce.OpenTarget
import io.github.zdyoyoyo.englishpractice.data.StudyMode
import io.github.zdyoyoyo.englishpractice.ui.favorites.FavoritesScreen
import io.github.zdyoyoyo.englishpractice.ui.achievements.AchievementsScreen
import io.github.zdyoyoyo.englishpractice.ui.flashcards.FlashcardsScreen
import io.github.zdyoyoyo.englishpractice.ui.dictation.DictationScreen
import io.github.zdyoyoyo.englishpractice.ui.home.HomeScreen
import io.github.zdyoyoyo.englishpractice.ui.mock.MockSetupScreen
import io.github.zdyoyoyo.englishpractice.ui.pk.PkScreen
import io.github.zdyoyoyo.englishpractice.ui.practice.PracticeSetupScreen
import io.github.zdyoyoyo.englishpractice.ui.quiz.QuizScreen
import io.github.zdyoyoyo.englishpractice.ui.settings.SettingsScreen
import io.github.zdyoyoyo.englishpractice.ui.stats.StatsScreen
import io.github.zdyoyoyo.englishpractice.ui.words.WordListScreen
import io.github.zdyoyoyo.englishpractice.ui.wrongbook.WrongBookScreen

@Composable
fun AppNavHost(pendingOpen: String? = null, onPendingOpenHandled: () -> Unit = {}) {
    val nav = rememberNavController()
    val activity = LocalActivity.current
    val ads = (LocalContext.current.applicationContext as EnglishApp).container.ads
    // 完成一輪後「可能」顯示插頁廣告（頻率由 AdPolicy 控制）
    val onSessionFinished: (Int) -> Unit = { count -> activity?.let { ads.onSessionCompleted(it, count) } }

    /** 通知或公告按鈕要打開的畫面（OpenTarget）；不認得就留在首頁 */
    val openTarget: (String) -> Unit = { target ->
        val route: Any? = when (target) {
            OpenTarget.DAILY -> QuizRoute(StudyMode.DAILY.key)
            OpenTarget.REVIEW -> QuizRoute(StudyMode.REVIEW.key)
            OpenTarget.PRACTICE -> PracticeSetupRoute
            OpenTarget.MOCK -> MockSetupRoute
            OpenTarget.FLASHCARDS -> FlashcardsRoute
            OpenTarget.DICTATION -> DictationRoute
            OpenTarget.PK -> PkRoute
            OpenTarget.ACHIEVEMENTS -> AchievementsRoute
            OpenTarget.STATS -> StatsRoute
            OpenTarget.SETTINGS -> SettingsRoute
            else -> null
        }
        route?.let { nav.navigate(it) { launchSingleTop = true } }
    }

    // 從通知點進來 → 直接開對應畫面（例如每日一題）
    LaunchedEffect(pendingOpen) {
        if (pendingOpen != null) {
            openTarget(pendingOpen)
            onPendingOpenHandled()
        }
    }

    val container = (LocalContext.current.applicationContext as EnglishApp).container
    // 第一次打開先看新手引導
    val start: Any = remember { if (container.prefs.onboardingDone) HomeRoute else OnboardingRoute }
    NavHost(navController = nav, startDestination = start) {
        composable<OnboardingRoute> {
            OnboardingScreen(onDone = { startWithMock ->
                nav.navigate(HomeRoute) { popUpTo(OnboardingRoute) { inclusive = true } }
                if (startWithMock) nav.navigate(QuizRoute(StudyMode.MOCK.key, count = 20))
            })
        }
        composable<HomeRoute> {
            HomeScreen(
                onPractice = { nav.navigate(PracticeSetupRoute) },
                onReview = { nav.navigate(QuizRoute(StudyMode.REVIEW.key)) },
                onDaily = { nav.navigate(QuizRoute(StudyMode.DAILY.key)) },
                onWrongBook = { nav.navigate(WrongBookRoute) },
                onFavorites = { nav.navigate(FavoritesRoute) },
                onStats = { nav.navigate(StatsRoute) },
                onMock = { nav.navigate(MockSetupRoute) },
                onAchievements = { nav.navigate(AchievementsRoute) },
                onFlashcards = { nav.navigate(FlashcardsRoute) },
                onDictation = { nav.navigate(DictationRoute) },
                onWatchAdForFreeze = { onReward -> activity?.let { ads.showRewarded(it, onReward) } ?: false },
                onWords = { nav.navigate(WordListRoute) },
                onPk = { nav.navigate(PkRoute) },
                onSettings = { nav.navigate(SettingsRoute) },
                onOpenTarget = openTarget,
            )
        }
        composable<PracticeSetupRoute> {
            PracticeSetupScreen(
                onBack = { nav.popBackStack() },
                onStart = { o ->
                    nav.navigate(
                        QuizRoute(
                            StudyMode.PRACTICE.key,
                            o.filter.encodeTags(), o.filter.encodeDifficulties(), o.filter.encodeCategories(),
                            o.filter.encodeSkills(), o.count, o.preferNew,
                        )
                    )
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
        composable<MockSetupRoute> {
            MockSetupScreen(
                onBack = { nav.popBackStack() },
                onStart = { n -> nav.navigate(QuizRoute(StudyMode.MOCK.key, count = n)) },
            )
        }
        composable<DictationRoute> { DictationScreen(onBack = { nav.popBackStack() }) }
        composable<FlashcardsRoute> { FlashcardsScreen(onBack = { nav.popBackStack() }) }
        composable<AchievementsRoute> { AchievementsScreen(onBack = { nav.popBackStack() }) }
        composable<StatsRoute> {
            StatsScreen(
                onBack = { nav.popBackStack() },
                onPracticeSkill = { skill -> nav.navigate(QuizRoute(StudyMode.PRACTICE.key, skills = skill, count = 10)) },
            )
        }
        composable<WordListRoute> { WordListScreen(onBack = { nav.popBackStack() }) }
        composable<SettingsRoute> { SettingsScreen(onBack = { nav.popBackStack() }) }
    }
}
