# 上架步驟清單（手機也能完成）

> 打勾順序就是建議的執行順序。用手機瀏覽器操作 Play Console / Google Cloud 時，建議切成「電腦版網站」。

## A. 準備（一次性）
- [ ] **GitHub Secrets 新增正式版上傳金鑰**（4 個：`RELEASE_KEYSTORE_BASE64`、`RELEASE_STORE_PASSWORD`、`RELEASE_KEY_ALIAS`、`RELEASE_KEY_PASSWORD`）
      → 之後每次 main 更新，Actions 都會產生上架用的 `.aab`
- [ ] **備份上傳金鑰**（`RELEASE_KEYSTORE_BASE64.txt` + `RELEASE_SECRETS.txt`）到安全的私人位置
- [ ] **Google Cloud 新增正式版 Android 用戶端**：Google Auth Platform → 用戶端 → 建立用戶端 → Android
      - 套件名稱 `io.github.zdyoyoyo.englishpractice`（沒有 .debug）
      - SHA-1：上傳金鑰的 SHA-1（寫在 `RELEASE_SECRETS.txt`）
- [ ] **測試 release 版**：Releases 頁面的 `EnglishPractice-release-test.apk`（和測試版可以同時安裝），確認能作答、Google 登入、提醒都正常
- [ ] **隱私權政策**：把 `docs/privacy-policy.md` 的「開發者」改成你在 Play 上的開發者名稱，第 7 段選「13 歲以上」版本

## B. Play Console 帳號
- [ ] 到 https://play.google.com/console 註冊**個人**開發者帳號（一次性 25 美元，要身分驗證，可能要等幾天）
- [ ] 建立應用程式：名稱、預設語言「中文（台灣）」、類型「應用程式」、「免費」

## C. 應用程式內容（Play Console → 政策 → 應用程式內容）
- [ ] 隱私權政策：貼上網址（見 `docs/play-store-listing.md`）
- [ ] 應用程式存取權：「所有功能都不需要特殊存取權」（登入是選用的）
- [ ] 廣告：**含有廣告**
- [ ] 內容分級：填問卷（教育類、無暴力／色情／賭博／使用者互動內容），通常會是「普遍級／3+」
- [ ] 目標對象：建議只勾 **13 歲以上**（見 `docs/children-and-ads.md`）
- [ ] 資料安全性：照 `docs/play-data-safety.md` 填
- [ ] 廣告 ID：「是」，用途：廣告、分析、防詐騙
- [ ] 政府／金融／健康等類別：都選「否」

## D. 商店資訊
- [ ] 照 `docs/play-store-listing.md` 貼名稱、簡短說明、完整說明
- [ ] 上傳圖示、主圖片、手機截圖（至少 2 張）

## E. 封閉測試（個人帳號必須：≥12 人、連續 14 天）
- [ ] 測試 → 封閉測試 → 建立測試群組（用 Email 名單或 Google 群組）
      - 找不到測試者：用測試者互助社群（例如 p2p-closedtester），測試者改加入他們的 Google 群組 `test-soyokaze@googlegroups.com`（**必須用 Google 群組，不是 Email 名單**），
        版本審核通過後在他們的 App 登錄：App 名稱、套件名稱、versionCode、每日測試人數（預設 14）。
        你也要每天幫別人測（App 裡按「▶ 自動」，約 1 分鐘），否則點數用完你的 App 會被暫停
      - 加完測試者後記得按「傳送審核／重新審查」
- [ ] 建立版本：上傳 Actions 產生的 `app-release.aab`（Actions → 最新一次 main 的執行 → Artifacts）
      - 第一次上傳時選擇「使用 Google 產生的應用程式簽署金鑰」（Play App Signing）
- [ ] **上傳後**：Play Console → 測試與發布 → 應用程式完整性 → 複製「**應用程式簽署金鑰**」的 SHA-1
      → 到 Google Cloud 再新增一個 Android 用戶端（同套件名稱 + 這個 SHA-1）。**不做這步，從 Play 安裝的版本 Google 登入會失敗**
- [ ] 把測試連結分享給測試者，讓他們用連結加入並安裝
- [ ] 14 天內持續修 bug、加題目、更新版本（每次上傳新的 aab 即可，versionCode 會自動遞增）

## F. 申請正式版
- [ ] 14 天後在 Play Console 申請「正式版存取權」，回答測試過程的問題（測試者回饋、做了哪些修正）
- [ ] AdMob：申請帳號、新增應用程式（連結 Play 商店）、建立「插頁式」「獎勵」廣告單元
      → GitHub Secrets 新增 `ADMOB_APP_ID`、`ADMOB_INTERSTITIAL_ID`、`ADMOB_REWARDED_ID`
      → AdMob「隱私權與訊息」建立 GDPR 同意訊息
- [ ] Google Auth Platform → 目標對象 → **發布應用程式**（不然只有測試使用者能登入）
- [ ] 上傳新版 aab 到正式版，送審
- [ ] 上架後：在你的網站放 `app-ads.txt`（AdMob 會提供內容）
