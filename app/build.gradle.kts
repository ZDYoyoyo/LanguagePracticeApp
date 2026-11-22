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
        versionCode = 1
        versionName = "0.1.0"

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

    // Release 簽章：只從 local.properties / 環境變數讀取，keystore 檔不進版控
    signingConfigs {
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
    implementation(libs.androidx.work.runtime.ktx)
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
