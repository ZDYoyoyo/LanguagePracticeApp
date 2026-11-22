package io.github.zdyoyoyo.englishpractice.auth

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.tasks.await

/**
 * Google 登入分兩步（Google 官方建議的新做法）：
 * 1. 「驗證」：Credential Manager + Sign in with Google → 知道是哪個帳號（取得 email 顯示用）
 * 2. 「授權」：AuthorizationClient 只要求 drive.appdata 範圍 → 拿到 access token 存取 App 專用資料夾
 *
 * 需要的設定（見 README）：
 * - Google Cloud 專案啟用 Drive API、設定 OAuth 同意畫面
 * - 建立「Android」OAuth Client（套件名稱 + SHA-1，不需要放進 App）
 * - 建立「Web 應用程式」OAuth Client，把它的 Client ID 填到 local.properties 的 GOOGLE_WEB_CLIENT_ID
 */
class GoogleAuthManager(private val appContext: Context, private val webClientId: String) {

    val isConfigured: Boolean get() = webClientId.isNotBlank()

    data class Account(val email: String, val displayName: String?)

    /** 驗證：跳出 Google 帳號選擇畫面 */
    suspend fun signIn(activity: Activity): Account {
        check(isConfigured) { "尚未設定 GOOGLE_WEB_CLIENT_ID" }
        val option = GetSignInWithGoogleOption.Builder(webClientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val result = CredentialManager.create(activity).getCredential(activity, request)
        val credential = result.credential
        require(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            "非預期的登入憑證類型"
        }
        val google = GoogleIdTokenCredential.createFrom(credential.data)
        return Account(email = google.id, displayName = google.displayName)
    }

    sealed interface DriveAuth {
        data class Granted(val accessToken: String) : DriveAuth
        /** 需要使用者在畫面上同意：用 ActivityResultLauncher 啟動這個 PendingIntent */
        data class NeedsConsent(val pendingIntent: PendingIntent) : DriveAuth
    }

    /** 授權：要求 drive.appdata。已經同意過會直接拿到 token（不跳畫面），適合「開 App 自動同步」 */
    suspend fun authorizeDrive(): DriveAuth {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(DRIVE_APPDATA_SCOPE)))
            .build()
        val result = Identity.getAuthorizationClient(appContext).authorize(request).await()
        return if (result.hasResolution()) {
            DriveAuth.NeedsConsent(result.pendingIntent!!)
        } else {
            DriveAuth.Granted(result.accessToken ?: error("沒有取得 access token"))
        }
    }

    /** 使用者在同意畫面按下允許後，從回傳的 Intent 取出 token */
    fun tokenFromConsentResult(data: Intent?): String? = runCatching {
        Identity.getAuthorizationClient(appContext).getAuthorizationResultFromIntent(data).accessToken
    }.getOrNull()

    suspend fun signOut() {
        CredentialManager.create(appContext).clearCredentialState(ClearCredentialStateRequest())
    }

    companion object {
        const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
    }
}
