package com.sleep.fan.utility;

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.sleep.fan.R
import com.sleep.fan.signin.SocialData


class GoogleLoginUtil(
    private val context: Context,
    private val firebaseAuth: FirebaseAuth,
    private val activityLauncher: ActivityResultLauncher<Intent>,
) {
    private val sharedPreferences: SharedPreferences
    private val googleSignInClient: GoogleSignInClient

    init {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.google_cloud_sign_in_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    fun googleLogin() {
        val signInIntent = googleSignInClient.signInIntent
        activityLauncher.launch(signInIntent)
    }

    fun handleGoogleSignIn(
        completedTask: Task<GoogleSignInAccount?>,
        callback: GoogleSignInCallback,
    ) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            if (account != null) {
                val tokenId = account.idToken
                val name = account.displayName
                saveToSharedPreferences(tokenId, name)
                callback.onSuccess(
                    SocialData(
                        "Google", account.id!!, account.displayName!!,
                        account.givenName!!, account.familyName!!, account.email!!
                    )
                )
            }
        } catch (e: ApiException) {
            Log.e(TAG, "Google sign-in failed", e)
            callback.onFailure(e)
        }
    }

    private fun saveToSharedPreferences(tokenId: String?, name: String?) {
        val editor = sharedPreferences.edit()
        editor.putString(PREF_TOKEN_ID, tokenId)
        editor.putString(PREF_NAME, name)
        editor.apply()
    }

    val tokenId: String?
        get() = sharedPreferences.getString(PREF_TOKEN_ID, null)
    val name: String?
        get() = sharedPreferences.getString(PREF_NAME, null)

    fun signOut() {
        firebaseAuth.signOut()
        googleSignInClient.signOut()
        clearSharedPreferences()
    }

    private fun clearSharedPreferences() {
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
    }

    interface GoogleSignInCallback {
        fun onSuccess(socialData: SocialData?)
        fun onFailure(exception: Exception?)
    }

    companion object {
        private const val TAG = "GoogleLoginUtil"
        private const val PREFS_NAME = "MyPrefsFile"
        private const val PREF_TOKEN_ID = "token_id"
        private const val PREF_NAME = "name"
    }
}


