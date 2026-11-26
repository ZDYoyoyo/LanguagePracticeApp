# 英語練習 App（English Practice）

離線優先、零後端的英語學習 Android App。Kotlin + Jetpack Compose。

- 題庫是放在靜態空間的 JSON（目前 GitHub raw，之後可換 Cloudflare Pages），有版本號，有更新才下載
- 學習資料（作答、錯題、收藏、SM-2 複習排程、打卡）全部存在手機（Room），移除 App 即清除
- 可選擇用 Google 登入，把進度備份到**使用者自己的** Google Drive App 專用資料夾
- AdMob：做完一輪後偶爾的插頁廣告 + 使用者自選的獎勵廣告（換提示次數）

---

## 1. 架構

```
┌──────────── UI 層（Jetpack Compose）────────────┐
│  Screen（畫面） ←觀察 StateFlow─  ViewModel        │
└───────────────────────────┬──────────────────────┘
                            │ 呼叫
┌──────────── Data / Domain 層 ───────────────────┐
│  QuestionBankRepository  StudyRepository  CloudSync │
│  domain/（純邏輯：SM-2、打卡、PK、答案判斷…）        │
└─────┬──────────────┬───────────────┬─────────────┘
      │              │               │
  題庫 JSON       Room (SQLite)   Google Drive appDataFolder
 (GitHub raw)     手機本機          使用者自己的雲端
```

- **MVVM + 單向資料流**：畫面只顯示 `uiState`、把事件交給 ViewModel；ViewModel 呼叫 Repository；資料變了透過 `Flow` 自動更新畫面。
- **手動依賴注入**：`AppContainer` 建立所有共用物件（類似 Spring 的 ApplicationContext，但沒有框架魔法）。專案變大後可換 Hilt。
- **純邏輯和 Android 分離**：`domain/` 不依賴 Android，可用一般 JUnit 測試（不需要模擬器）。
- **時間可注入**（`AppClock`）：測試可以「快轉」到隔天，驗證複習排程與打卡。

### 檔案結構
```
question-bank/               ← 題庫（同時是 App 內建的離線種子資料）
  version.json               ← 很小的版本檔，App 先比對它
  questions.json             ← 完整題庫
docs/                        ← 隱私權政策、Data safety、兒童政策說明
app/src/main/java/io/github/zdyoyoyo/englishpractice/
  EnglishApp.kt / MainActivity.kt / AppContainer.kt
  data/
    model/Question.kt        ← 題目資料結構
    bank/                    ← 題庫：下載 → 驗證 → 快取 → 讀取
    local/                   ← Room：Entity（資料表）、DAO（查詢）、Database
    prefs/AppPreferences.kt  ← 小設定（SharedPreferences）
    StudyRepository.kt       ← 作答紀錄、錯題收錄、SM-2 更新、打卡（核心規則）
  domain/                    ← 純邏輯：Sm2Scheduler、StreakCalculator、DailyPicker、PkEngine、
                               AnswerChecker、QuestionSelector（聰明選題）、StatsCalculator、HintLogic
  notify/                    ← 每日提醒（AlarmManager 系統鬧鐘 + 開機重排）
  widget/                    ← 桌面小工具（連續打卡、今日目標、今日單字卡）
  share/                     ← 分享成績卡（Canvas 畫圖 + FileProvider）
  auth/GoogleAuthManager.kt  ← Google 登入 + Drive 授權
  sync/                      ← 備份格式、同步規則、Drive REST client
  ads/                       ← AdMob、廣告頻率規則、提示次數
  ui/                        ← 各畫面：home、practice、quiz、wrongbook、favorites、pk、stats、words、settings
app/src/test/                ← 單元測試（114 個）
```

---

## 2. 本機建置與測試

需求：Android Studio（最新穩定版）或 JDK 17+ 與 Android SDK 36。

```bash
cp local.properties.example local.properties   # 填入 sdk.dir（Android Studio 會自動填）
./gradlew testDebugUnitTest    # 跑全部單元測試
./gradlew assembleDebug        # 產生 app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug         # 裝到已連線的手機/模擬器
```
在 Android Studio：File → Open 選專案資料夾 → 等 Gradle Sync 完成 → 按 ▶ Run。

> 沒有設定任何金鑰也能建置與執行：Google 登入按鈕會顯示「尚未設定」，廣告使用 Google 測試廣告。

重點測試：
| 測試檔 | 驗證內容 |
|---|---|
| `Sm2SchedulerTest` / `ReviewSchedulingTest` | SM-2 間隔、EF 計算、提前練習不改排程、答錯重排、今天該複習幾題 |
| `FavoritesTest` | 收藏切換、「其實是猜的」改排明天複習、**資料庫 Migration SQL 與 schema 一致** |
| `WrongBookRecordingTest` | 答錯收錄、次數累加、錯題本答對才移出、再錯重新收錄 |
| `QuestionBankParserTest` | 題庫格式驗證，**含 repo 裡真正的 questions.json** |
| `QuestionBankRepositoryTest` | 版本比對、下載、快取、離線、壞檔不覆蓋 |
| `SmartSelectionAndStatsTest` | 聰明選題（沒做過→常錯→已會）、同篇閱讀／聽力不拆開、題型篩選、學習統計 |
| `MockAndGoalTest` | 模擬考組卷（聽力在前、比例、不拆短文）、預估分數與誤差區間、連勝保護補打卡 |
| `SyncTest` | 兩支模擬手機的備份 / 還原、較新為準、首次同步詢問 |
| `StreakCalculatorTest`、`DailyAndPkTest`、`AdsAndHintsTest` | 打卡、每日一題、PK 計分、廣告頻率 |

---

## 2.5 GitHub Actions 自動建置（手機也能拿到新版 APK）

`.github/workflows/android.yml`：每次 push 都會跑測試並建置測試版 APK。
- **main 更新時**：APK 會放到固定的 Release `debug-latest`，用手機打開
  `https://github.com/ZDYoyoyo/LanguagePracticeApp/releases/tag/debug-latest` 下載 `EnglishPractice-debug.apk` 即可安裝。
- 其他分支：到 Actions → 該次執行 → Artifacts 下載（zip）。

需要的 Secrets（GitHub → repo → Settings → Secrets and variables → Actions → New repository secret）：

| Secret | 內容 |
|---|---|
| `DEBUG_KEYSTORE_BASE64` | 測試版簽章金鑰的 base64 文字。固定金鑰才能「覆蓋安裝」並讓 Google 登入辨識 App |
| `GOOGLE_WEB_CLIENT_ID` | Google 登入的 Web Client ID（見 3.1） |
| `RELEASE_KEYSTORE_BASE64`、`RELEASE_STORE_PASSWORD`、`RELEASE_KEY_ALIAS`、`RELEASE_KEY_PASSWORD` | 上架用上傳金鑰。設定後 main 更新會產生 `app-release.aab`（Artifacts）和 `EnglishPractice-release-test.apk`（Releases） |
| `ADMOB_APP_ID`、`ADMOB_INTERSTITIAL_ID`、`ADMOB_REWARDED_ID` | 正式廣告 ID（正式上架前設定） |

versionCode 由 CI 依執行次數自動遞增（`VERSION_CODE`），不用手動改。

在自己電腦建置想用同一把測試金鑰：把 keystore 放在 repo 外，`local.properties` 設 `DEBUG_STORE_FILE=/path/to/debug.keystore`。

## 3. 機密設定（都放 `local.properties`，已被 .gitignore 排除）

| 鍵 | 用途 |
|---|---|
| `GOOGLE_WEB_CLIENT_ID` | Google 登入用的 Web OAuth Client ID |
| `DEBUG_STORE_FILE` | （選填）測試版共用簽章金鑰的路徑 |
| `ADMOB_APP_ID` / `ADMOB_INTERSTITIAL_ID` / `ADMOB_REWARDED_ID` | 正式廣告 ID（**只有 release 建置會用**；debug 永遠是測試 ID） |
| `RELEASE_STORE_FILE` / `..._PASSWORD` / `RELEASE_KEY_ALIAS` | 上架簽章 |

也可以用同名的**環境變數**提供（CI 用）。`.gitignore` 已排除 `local.properties`、`*.jks`、`*.keystore`、`google-services.json`、`client_secret*.json`。

### 3.1 設定 Google 登入 + Drive
1. 到 [Google Cloud Console](https://console.cloud.google.com/) 建立專案。
2. **API 和服務 → 程式庫**：啟用 **Google Drive API**。
3. **OAuth 同意畫面**：使用者類型選「外部」，填 App 名稱、支援信箱、隱私權政策網址；範圍加入 `.../auth/drive.appdata`。
   - 測試階段只有「測試使用者」名單內的帳號能登入；上架前按「發布應用程式」。
   - `drive.appdata` 只能存取 App 專用資料夾，屬於權限很小的範圍（是否需要驗證以 Console 顯示為準）。
4. **憑證 → 建立憑證 → OAuth 用戶端 ID → Android**（要建多個）：
   - 套件名稱 `io.github.zdyoyoyo.englishpractice.debug` + **debug** 簽章 SHA-1（`./gradlew signingReport` 可查）
   - 套件名稱 `io.github.zdyoyoyo.englishpractice` + **release / 上傳金鑰** SHA-1
   - 上架後再加一個：Play Console → 應用程式完整性 → **應用程式簽署金鑰** SHA-1
   - Android client **不需要**放進 App，Google 用套件名稱 + SHA-1 辨識。
5. **憑證 → 建立 OAuth 用戶端 ID → Web 應用程式**，複製 Client ID 填入 `local.properties`：
   ```
   GOOGLE_WEB_CLIENT_ID=1234567890-xxxx.apps.googleusercontent.com
   ```
6. 重新建置。設定 → 用 Google 登入並備份。

### 3.2 設定 AdMob
1. [AdMob](https://admob.google.com/) 新增應用程式（Android），取得 **App ID**（`ca-app-pub-xxx~yyy`）。
2. 建立兩個廣告單元：**插頁式**、**獎勵**，取得單元 ID（`ca-app-pub-xxx/zzz`）。
3. 填入 `local.properties`（只有 `assembleRelease` / `bundleRelease` 會使用）。
4. 調整頻率：`ads/AdPolicy.kt`（目前：前 2 輪不放、每 2 輪最多 1 次、間隔 ≥ 3 分鐘、作答中不出現）。
5. 兒童相關設定見 [`docs/children-and-ads.md`](docs/children-and-ads.md)。

### 3.3 Release 簽章與上架包
```bash
keytool -genkeypair -v -keystore ~/keys/upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
# local.properties 填 RELEASE_STORE_FILE=/home/you/keys/upload.jks 與密碼
./gradlew bundleRelease   # → app/build/outputs/bundle/release/app-release.aab 上傳到 Play Console
```
**keystore 檔與密碼請另外備份，遺失就無法更新 App**（若有啟用 Play App Signing 可申請重設上傳金鑰）。

---

## 4. 題庫格式與新增題目

`question-bank/questions.json`：
```json
{
  "schemaVersion": 1,
  "version": 2,
  "updatedAt": "2026-12-01",
  "questions": [
    {
      "id": "voc-0007",                 // 唯一、不要重複使用或改名（錯題與複習紀錄靠它對應）
      "type": "vocabulary",             // vocabulary | grammar | cloze | multiple_choice
      "prompt": "What does \"brief\" mean?",
      "word": "brief",                  // 選填：單字題填寫，「每日單字」會用到
      "options": ["簡短的", "勇敢的", "明亮的", "破碎的"],  // 空陣列 [] = 讓使用者自己輸入答案
      "answer": "簡短的",               // 有選項時必須是選項之一
      "acceptedAnswers": [],            // 選填：輸入題可接受的其他寫法（不分大小寫）
      "passage": "（選填）閱讀短文或聽力對話稿，多題可共用同一段",
      "listening": false,               // 選填：true = passage 是聽力對話（M: / W: 開頭會用男聲／女聲朗讀），作答後才顯示原文
      "glossary": {"toner": "碳粉"},     // 有 passage 時必填：重點生字（英 → 中）
      "passageTranslation": "中文翻譯",  // 有 passage 時必填：作答後可展開的中文翻譯
      "explanation": "brief（adj.）簡短的。",
      "optionNotes": {                  // 選填：錯誤選項的說明，答錯時顯示「你選的那個是什麼意思」
        "勇敢的": "勇敢的是 brave"
      },
      "difficulty": 1,                  // 1 初級、2 中級、3 進階
      "tags": ["work"]                  // 主題標籤，選題畫面會自動列出
    }
  ]
}
```

**新增題目的步驟**
1. 在 `questions.json` 加題目。
2. **兩個檔案的 `version` 都 +1**（`questions.json` 與 `version.json` 要一致）。
3. 跑 `./gradlew testDebugUnitTest` —— `QuestionBankParserTest` 會檢查格式、重複 id、正解是否在選項中、兩檔版本是否一致。
4. commit 並 push 到 `main`。使用者下次開 App 會自動下載（只有版本變大才下載）。

> 沒寫 `optionNotes` 時，若題庫裡剛好有以該選項為 `word` 的單字題，App 會自動借用那題的解析。
> 單字題的 `word`（或英文正解）會出現「🔊 聽 / 🐢 慢速 / 🎤 跟著唸」發音練習。

> 新的主題標籤要中文名稱的話，加到 `ui/practice/PracticeSetupScreen.kt` 的 `TagLabels`。
> 若題庫用到舊版 App 不支援的欄位（例如 v4 的 passage / listening 需要 versionCode 2），
> 把 `version.json` 的 `minAppVersionCode` 設成對應的 versionCode，舊版 App 會提示更新而不會下載。
> 新版 App 遇到不認得的題型只會略過那一題，不會讓整個題庫失效。

### 題庫放在哪裡
- 目前：`gradle.properties` 的 `questionBankBaseUrl` 指向
  `https://raw.githubusercontent.com/ZDYoyoyo/LanguagePracticeApp/main/question-bank/`
  - ⚠️ repo 必須是 **public**，而且檔案要在 **main** 分支，否則下載會失敗（App 會自動改用內建題庫，不影響使用）。
  - GitHub raw 有快取（約 5 分鐘）與流量限制，適合初期。
- **換成 Cloudflare Pages**（免費、CDN 較快）：
  1. Cloudflare Dashboard → Workers & Pages → 建立 → Pages → 連結這個 GitHub repo。
  2. Build command 留空，Build output directory 填 `question-bank`。
  3. 部署後得到 `https://<專案名>.pages.dev/`，把 `questionBankBaseUrl` 改成它（結尾要有 `/`），重新發版即可。
  4. 之後 push 到 main 會自動重新部署題庫。

### 發公告（「廣播」）
不用後端：編輯 `question-bank/announcements.json` 並 push 到 main 就會生效（GitHub raw 約 5 分鐘快取）。
- 用戶打開 App → 首頁最上方顯示公告卡片（一次一則，依檔案順序），按「知道了」後不再出現。
- 背景大約每 1～3 小時（有網路時，WorkManager）檢查一次；`"notify": true` 且用戶還沒在 App 裡看過的公告會發一則通知（頻道「公告與活動」，設定頁可關閉）。
- 不是即時推播：通常 1～3 小時內收到通知（手機待機時系統可能再延後）；要即時推播得另外接 Firebase Cloud Messaging。

```json
{
  "id": "2026-12-xmas",              // 唯一 ID；想讓大家再看一次就換新 ID
  "title": "聖誕挑戰賽",
  "body": "12/20–12/31 每天完成目標就送連勝保護！",
  "actionLabel": "去做題",            // 選填
  "action": "practice",              // 選填：daily / review / practice / mock / flashcards / dictation / pk / achievements / stats / settings，或 https:// 網址
  "start": "2026-12-20", "end": "2026-12-31",   // 選填，含當天
  "minVersionCode": 0, "maxVersionCode": 40,     // 選填：只給某些版本看（例如提醒舊版更新）
  "notify": true                     // 是否發背景通知
}
```
單元測試 `AnnouncementsTest` 會檢查這個檔案（ID 不重複、日期格式、動作合法），push 前跑一次測試就不怕寫錯。

---

## 5. 權限說明

| 權限 | 來源 | 用途 |
|---|---|---|
| `INTERNET` | App | 下載題庫、Google Drive 同步、載入廣告 |
| `POST_NOTIFICATIONS` | App | 每日提醒（Android 13+，使用者在設定開啟提醒時才會詢問） |
| `RECEIVE_BOOT_COMPLETED` | App | 手機重開機後重新排每日提醒（系統鬧鐘在重開機後會清空） |
| `ACCESS_NETWORK_STATE` | AdMob | 判斷網路狀態 |
| `com.google.android.gms.permission.AD_ID` | AdMob | 讀取廣告 ID（兒童導向 App 必須移除，見 docs） |
| `ACCESS_ADSERVICES_AD_ID` / `_ATTRIBUTION` / `_TOPICS` | AdMob | Android Privacy Sandbox 廣告 API |
| `WAKE_LOCK`、`FOREGROUND_SERVICE` | AdMob（內含 WorkManager） | 廣告 SDK 的背景工作 |

已**主動移除** Credential Manager 帶入的 `USE_BIOMETRIC` / `USE_FINGERPRINT`（用不到）。
不需要「精確鬧鐘」權限：提醒用 `AlarmManager.setAndAllowWhileIdle`，可能晚幾分鐘；小米等手機需把省電策略設為「無限制」。設定頁有「傳送測試通知」可以檢查通知是否正常。
發音練習**不需要麥克風權限**：朗讀用系統內建 TextToSpeech，「跟著唸」用系統的語音辨識畫面，App 只拿到辨識出的文字。
`<queries>` 宣告了朗讀、語音辨識、寄信三種系統服務（Android 11+ 套件可見性要求，不是權限）。

### 聯絡作者
在 `gradle.properties` 設定 `supportEmail=你的信箱`。設定頁會出現「意見回饋」與寄信按鈕，答題後也會有「這題有錯？回報給作者」。
寄信是開啟使用者自己的 Email App 並預填內容（含 App 版本、Android 版本、機型），使用者確認後自己按傳送。
⚠️ 這個信箱會公開在 repo 與 App 中，建議用專門收回饋的信箱。

---

## 6. 隱私與上架文件
- **[上架步驟清單（從零到正式上架）](docs/release-checklist.md)**
- [商店資訊文案與素材](docs/play-store-listing.md)
- [隱私權政策草稿（繁中＋英文）](docs/privacy-policy.md)
- [Play「資料安全性」表單填寫指引](docs/play-data-safety.md)
- [兒童政策與 AdMob 設定注意事項](docs/children-and-ads.md)

## 7. 上架前檢查清單
- [ ] `applicationId` 確認（上架後**不能改**）：`app/build.gradle.kts`
- [ ] App 名稱、圖示（`res/drawable/ic_launcher_foreground.xml`）
- [ ] Google Cloud：OAuth 同意畫面發布、Android client 加入 Play 簽署 SHA-1
- [ ] AdMob 正式 ID 填入 `local.properties`、設定內容分級、app-ads.txt
- [ ] 隱私權政策填好聯絡資訊並公開、`privacyPolicyUrl` 指向它
- [ ] Data safety、目標年齡、廣告 ID 宣告
- [ ] 改資料表時：`AppDatabase` 版本 +1 並寫 Migration（範例：`MIGRATION_1_2` 收藏、`MIGRATION_2_3` 模擬考成績），`app/schemas/` 保存各版 schema
- [ ] `versionCode` 每次上傳 +1
