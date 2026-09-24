package com.rwpiri.uptime.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.settingsDataStore by preferencesDataStore(name = "uptime_settings")

class SettingsStore(private val context: Context) {
    private val serverUrlKey = stringPreferencesKey("server_url")

    suspend fun setServerUrl(value: String) = context.settingsDataStore.edit { it[serverUrlKey] = value.trim().trimEnd('/') }
    suspend fun getServerUrl(): String = context.settingsDataStore.data.first()[serverUrlKey].orEmpty()
}

class EncryptedTokenStore(private val context: Context) {
    private val tokenKey = stringPreferencesKey("token_ciphertext")
    private val ivKey = stringPreferencesKey("token_iv")
    private val alias = "uptime_api_token"

    suspend fun save(token: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(token.toByteArray(StandardCharsets.UTF_8))
        context.settingsDataStore.edit {
            it[tokenKey] = Base64.encodeToString(encrypted, Base64.NO_WRAP)
            it[ivKey] = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        }
    }

    suspend fun get(): String? {
        val prefs = context.settingsDataStore.data.first()
        val ciphertext = prefs[tokenKey] ?: return null
        val iv = prefs[ivKey] ?: return null
        return runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                secretKey(),
                GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)),
            )
            String(cipher.doFinal(Base64.decode(ciphertext, Base64.NO_WRAP)), StandardCharsets.UTF_8)
        }.getOrNull()
    }

    suspend fun clear() = context.settingsDataStore.edit {
        it.remove(tokenKey)
        it.remove(ivKey)
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(
                KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setUserAuthenticationRequired(false)
                    .build(),
            )
        }.generateKey()
    }
}

@Serializable
data class WorkerState(
    val lastRun: String? = null,
    val lastStates: Map<String, Boolean> = emptyMap(),
    val certificateAlerts: Map<String, String> = emptyMap(),
)

class WorkerStateStore(private val context: Context) {
    private val stateKey = stringPreferencesKey("worker_state")
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun get(): WorkerState = runCatching {
        val raw = context.settingsDataStore.data.first()[stateKey] ?: return WorkerState()
        json.decodeFromString<WorkerState>(raw)
    }.getOrDefault(WorkerState())

    suspend fun save(state: WorkerState) = context.settingsDataStore.edit { it[stateKey] = json.encodeToString(state) }
}
