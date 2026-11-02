package com.desego.data

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CustomCredential
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object NativeGoogleSignIn {
    suspend fun token(context: Context): String {
        val challenge = withContext(Dispatchers.IO) { BackendClient.request("api/mobile/google/challenge", "POST") }
        val option = GetSignInWithGoogleOption.Builder(challenge.getString("clientId"))
            .setNonce(challenge.getString("nonce")).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val result = try {
            CredentialManager.create(context).getCredential(context, request)
        } catch (error: GetCredentialCancellationException) {
            throw IllegalStateException("Se canceló el acceso con Google.", error)
        } catch (error: NoCredentialException) {
            throw IllegalStateException("No se pudo seleccionar una cuenta de Google. Comprueba Google Play Services y la configuración de la app en Google Cloud.", error)
        }
        val credential = result.credential
        check(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            "Google no devolvió una credencial válida."
        }
        return GoogleIdTokenCredential.createFrom(credential.data).idToken
    }

    suspend fun signOut(context: Context) {
        CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
    }
}
