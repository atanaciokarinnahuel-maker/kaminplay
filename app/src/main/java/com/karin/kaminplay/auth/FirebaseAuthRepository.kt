package com.karin.kaminplay.auth

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.karin.kaminplay.core.SessionState
import com.karin.kaminplay.core.UserProfile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * Google-only sign-in: Credential Manager returns a Google ID token and Firebase Auth turns
 * it into the app session. Firebase keeps the session across launches.
 */
class FirebaseAuthRepository(appContext: Context, allowPreview: Boolean) : AuthRepository {

    // google-services.json generates this string resource; it is absent in builds without Firebase.
    @SuppressLint("DiscouragedApi")
    private val webClientId: String? = appContext.resources
        .getIdentifier("default_web_client_id", "string", appContext.packageName)
        .takeIf { it != 0 }
        ?.let { appContext.getString(it) }

    private val auth: FirebaseAuth? =
        if (FirebaseApp.getApps(appContext).isNotEmpty()) FirebaseAuth.getInstance() else null

    override val isConfigured: Boolean = auth != null && webClientId != null
    override val canPreview: Boolean = allowPreview && !isConfigured

    private val _session = MutableStateFlow<SessionState>(if (auth == null) SessionState.SignedOut else SessionState.Loading)
    override val session: StateFlow<SessionState> = _session.asStateFlow()

    init {
        // Fires once immediately with the stored session, then on every sign-in and sign-out.
        auth?.addAuthStateListener { firebaseAuth ->
            _session.value = firebaseAuth.currentUser?.let { SessionState.SignedIn(it.toProfile()) } ?: SessionState.SignedOut
        }
    }

    override suspend fun signInWithGoogle(activityContext: Context): SignInResult {
        val firebaseAuth = auth
        val clientId = webClientId
        if (firebaseAuth == null || clientId == null) {
            return SignInResult.Failure("Firebase no está conectado en esta compilación.")
        }
        return try {
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(GetSignInWithGoogleOption.Builder(clientId).build())
                .build()
            val credential = CredentialManager.create(activityContext).getCredential(activityContext, request).credential
            if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                return SignInResult.Failure("Google devolvió un tipo de credencial inesperado.")
            }
            val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
            firebaseAuth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
            SignInResult.Success
        } catch (e: CancellationException) {
            throw e
        } catch (e: GetCredentialCancellationException) {
            SignInResult.Cancelled
        } catch (e: NoCredentialException) {
            SignInResult.Failure("No hay ninguna cuenta de Google disponible en este teléfono.")
        } catch (e: GetCredentialException) {
            Log.w(TAG, "Credential Manager failed", e)
            SignInResult.Failure("Google no pudo completar el inicio de sesión. Revisa que la huella SHA-1 de esta compilación esté registrada en Firebase.")
        } catch (e: GoogleIdTokenParsingException) {
            Log.w(TAG, "Invalid Google ID token", e)
            SignInResult.Failure("Google devolvió una credencial que no se pudo leer.")
        } catch (e: Exception) {
            Log.w(TAG, "Firebase sign-in failed", e)
            SignInResult.Failure("No se pudo iniciar sesión. Comprueba tu conexión e inténtalo de nuevo.")
        }
    }

    override suspend fun signOut(context: Context) {
        if (auth == null) {
            _session.value = SessionState.SignedOut
            return
        }
        auth.signOut()
        try {
            // Forget the chosen account so the next sign-in shows the account sheet again.
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Could not clear the credential state", e)
        }
    }

    override fun enterPreview() {
        if (canPreview) {
            _session.value = SessionState.SignedIn(UserProfile("preview", "Vista previa", null, null))
        }
    }

    private fun FirebaseUser.toProfile() = UserProfile(
        uid = uid,
        displayName = displayName ?: email?.substringBefore('@') ?: "",
        email = email,
        photoUrl = photoUrl?.toString(),
    )

    private companion object {
        const val TAG = "KaminAuth"
    }
}
