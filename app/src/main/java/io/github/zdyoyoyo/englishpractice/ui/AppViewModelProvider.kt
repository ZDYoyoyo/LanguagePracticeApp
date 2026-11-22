package io.github.zdyoyoyo.englishpractice.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import io.github.zdyoyoyo.englishpractice.AppContainer
import io.github.zdyoyoyo.englishpractice.EnglishApp

/** 在 ViewModel factory 裡取得 AppContainer 的小工具 */
fun CreationExtras.appContainer(): AppContainer = (this[APPLICATION_KEY] as EnglishApp).container
