import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// 讀取 local.properties（不進版控）裡的金鑰；找不到就用空字串
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun localProp(key: String, default: String = ""): String =
    (localProps.getProperty(key) ?: System.getenv(key))?.trim()?.takeIf { it.isNotEmpty() } ?: default

// Google 官方 AdMob 測試 ID（公開的，可以 commit）。開發時一定要用測試 ID，用正式 ID 點自己的廣告會被停權
val admobTestAppId = "ca-app-pub-3940256099942544~3347511713"
val admobTestInterstitialId = "ca-app-pub-3940256099942544/1033173712"
val admobTestRewardedId = "ca-app-pub-3940256099942544/5224354917"
fun gradleProp(key: String, default: String) = (project.findProperty(key) as String?) ?: default

android {
    namespace = "io.github.zdyoyoyo.englishpractice"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.zdyoyoyo.englishpractice"
        minSdk = 26
        targetSdk = 36
        // Play 每次上傳都要更大的 versionCode：CI 會用 VERSION_CODE（執行次數）自動遞增，本機建置用預設值
        versionCode = localProp("VERSION_CODE", "2").toInt()
        versionName = "0.3.0"

        val bankUrl = (project.findProperty("questionBankBaseUrl") as String?) ?: ""
        buildConfigField("String", "QUESTION_BANK_BASE_URL", "\"$bankUrl\"")
        // Google 登入用的 Web Client ID：從 local.properties 讀取，不寫死在程式碼、不進版控
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${localProp("GOOGLE_WEB_CLIENT_ID")}\"")

        // 廣告的兒童相關設定（見 docs/children-and-ads.md）
        buildConfigField("boolean", "ADS_CHILD_DIRECTED", gradleProp("adsChildDirected", "false"))
        buildConfigField("String", "ADS_MAX_CONTENT_RATING", "\"${gradleProp("adsMaxContentRating", "G")}\"")
        buildConfigField("String", "SUPPORT_EMAIL", "\"${gradleProp("supportEmail", "")}\"")
        buildConfigField("String", "PRIVACY_POLICY_URL", "\"${gradleProp("privacyPolicyUrl", "")}\"")
    }

    // 簽章設定：keystore 檔一律不進版控，路徑與密碼從 local.properties 或環境變數（GitHub Actions Secrets）讀取
    signingConfigs {
        // 測試版共用金鑰：讓每次建置（不管在哪台電腦或 GitHub Actions）簽出來的 APK 都能覆蓋安裝，
        // Google 登入也靠它的 SHA-1 辨識 App。沒有設定時退回 Android 預設的本機 debug 金鑰
        create("sharedDebug") {
            val path = localProp("DEBUG_STORE_FILE")
            if (path.isNotEmpty()) {
                storeFile = file(path)
                storePassword = localProp("DEBUG_STORE_PASSWORD", "android")
                keyAlias = localProp("DEBUG_KEY_ALIAS", "androiddebugkey")
                keyPassword = localProp("DEBUG_KEY_PASSWORD", "android")
            }
        }
        create("release") {
            val storePath = localProp("RELEASE_STORE_FILE")
            if (storePath.isNotEmpty()) {
                storeFile = file(storePath)
                storePassword = localProp("RELEASE_STORE_PASSWORD")
                keyAlias = localProp("RELEASE_KEY_ALIAS")
                keyPassword = localProp("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            if (localProp("DEBUG_STORE_FILE").isNotEmpty()) signingConfig = signingConfigs.getByName("sharedDebug")
            // debug 一律使用測試廣告，就算 local.properties 有填正式 ID 也不會用到
            manifestPlaceholders["admobAppId"] = admobTestAppId
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"$admobTestInterstitialId\"")
            buildConfigField("String", "ADMOB_REWARDED_ID", "\"$admobTestRewardedId\"")
        }
        release {
            manifestPlaceholders["admobAppId"] = localProp("ADMOB_APP_ID", admobTestAppId)
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"${localProp("ADMOB_INTERSTITIAL_ID", admobTestInterstitialId)}\"")
            buildConfigField("String", "ADMOB_REWARDED_ID", "\"${localProp("ADMOB_REWARDED_ID", admobTestRewardedId)}\"")
            if (localProp("RELEASE_STORE_FILE").isNotEmpty()) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    // 題庫只維護一份：根目錄 question-bank/ 同時當作 App 內建的離線種子資料
    sourceSets["main"].assets.srcDirs("src/main/assets", rootProject.file("question-bank"))

    testOptions { unitTests.isReturnDefaultValues = true }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    // Google 登入（Credential Manager）+ Drive 授權（AuthorizationClient）
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)
    implementation(libs.play.services.auth)
    implementation(libs.kotlinx.coroutines.play.services)
    // 廣告 + 使用者同意管理（歐盟等地區法規要求）
    implementation(libs.play.services.ads)
    implementation(libs.ump)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

// 單元測試會讀 question-bank/ 驗證題庫，宣告成輸入：題庫一改就會重新跑測試（不會用到舊的快取結果）
tasks.withType<Test>().configureEach {
    inputs.dir(rootProject.file("question-bank")).withPropertyName("questionBank")
}
