package com.scriptam.app.bridge

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.function
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Hardware-backed encrypted secret vault using AES-256-GCM via Android KeyStore.
 *
 * Allows scripts to securely store and retrieve API keys, tokens, and passwords:
 * - `Native.secrets.get(key)` / `Native.getSecret(key)`
 * - `Native.secrets.set(key, value)` / `Native.setSecret(key, value)`
 * - `Native.secrets.remove(key)`
 */
class SecretsModule(context: Context) {

    private val prefs = context.getSharedPreferences("scriptam_encrypted_vault", Context.MODE_PRIVATE)

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "ScriptamMasterKey"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val IV_SEPARATOR = "]"
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(KEY_ALIAS)) {
            val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
            return entry.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val secretKey = getOrCreateKey()
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val iv = cipher.iv
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        val ivB64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        val cipherB64 = Base64.encodeToString(cipherText, Base64.NO_WRAP)
        return "$ivB64$IV_SEPARATOR$cipherB64"
    }

    fun decrypt(encryptedPayload: String): String? {
        return try {
            val parts = encryptedPayload.split(IV_SEPARATOR)
            if (parts.size != 2) return null
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val cipherText = Base64.decode(parts[1], Base64.NO_WRAP)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), spec)

            val plainBytes = cipher.doFinal(cipherText)
            String(plainBytes, Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    fun getSecret(key: String): String? {
        val encrypted = prefs.getString(key, null) ?: return null
        return decrypt(encrypted)
    }

    fun setSecret(key: String, value: String) {
        val encrypted = encrypt(value)
        prefs.edit().putString(key, encrypted).apply()
    }

    fun removeSecret(key: String) {
        prefs.edit().remove(key).apply()
    }

    fun install(quickJs: QuickJs) {
        quickJs.function<Any?>("__native_secret_get") { args ->
            val key = args.firstOrNull()?.toString() ?: return@function null
            getSecret(key)
        }

        quickJs.function<Unit>("__native_secret_set") { args ->
            val key = args.getOrNull(0)?.toString() ?: return@function
            val value = args.getOrNull(1)?.toString() ?: ""
            setSecret(key, value)
        }

        quickJs.function<Unit>("__native_secret_remove") { args ->
            val key = args.firstOrNull()?.toString() ?: return@function
            removeSecret(key)
        }
    }
}
