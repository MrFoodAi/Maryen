package com.maryen.app.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Vault AES-256-GCM ancorato al Keystore hardware del dispositivo.
 * Cifra dati sensibili locali (es. API key, log consensi).
 * Nessun dato sensibile tocca mai il disco in chiaro.
 */
class Vault(ctx: Context) {

    private val keystore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private val dir = File(ctx.filesDir, "vault").apply { mkdirs() }

    init { ensureMasterKey() }

    private fun ensureMasterKey() {
        if (keystore.containsAlias(ALIAS)) return
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(false)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        gen.generateKey()
    }

    private fun masterKey(): SecretKey =
        (keystore.getEntry(ALIAS, null) as KeyStore.SecretKeyEntry).secretKey

    fun put(name: String, plaintext: ByteArray) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, masterKey())
        val iv = cipher.iv
        val ct = cipher.doFinal(plaintext)
        File(dir, "$name.bin").writeBytes(iv + ct)
    }

    fun get(name: String): ByteArray? {
        val f = File(dir, "$name.bin")
        if (!f.exists()) return null
        val raw = f.readBytes()
        val iv = raw.copyOfRange(0, 12)
        val ct = raw.copyOfRange(12, raw.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, masterKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(ct)
    }

    fun putString(name: String, value: String) = put(name, value.toByteArray())
    fun getString(name: String): String? = get(name)?.toString(Charsets.UTF_8)

    fun wipe(name: String) { File(dir, "$name.bin").delete() }
    fun wipeAll() { dir.listFiles()?.forEach { it.delete() } }

    companion object { private const val ALIAS = "maryen_master_v1" }
}
