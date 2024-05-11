package com.example.keystorenotes

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


class CryptoManager {

    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    private val TRANSFORMATION = "AES/CBC/PKCS7Padding"

    fun encryptWithKeyAndPin(data: ByteArray, outputStream: OutputStream, pin: String) {
        val dataEncryptedWithPin = encryptString(data, pin)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getKey())

        val ivBytes = cipher.iv
        outputStream.write(ivBytes.size)
        outputStream.write(ivBytes)

        val encryptedBytes = cipher.doFinal(dataEncryptedWithPin)
        outputStream.write(encryptedBytes.size)
        outputStream.write(encryptedBytes)
    }



    fun decryptWithPin(inputStream: InputStream, pin: String): ByteArray {
        val ivSize = inputStream.read()
        val ivBytes = ByteArray(ivSize)
        inputStream.read(ivBytes)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val ivParameterSpec = IvParameterSpec(ivBytes)
        cipher.init(Cipher.DECRYPT_MODE, getKey(), ivParameterSpec)

        val encryptedBytesSize = inputStream.read()
        val encryptedBytes = ByteArray(encryptedBytesSize)
        inputStream.read(encryptedBytes)

        val decryptedBytes = cipher.doFinal(encryptedBytes)
        return decryptString(decryptedBytes, pin)
    }



    private fun getKey(): SecretKey {
        val existingKey = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        return existingKey?.secretKey ?: createKey()
    }

    private fun createKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            .setRandomizedEncryptionRequired(false)
            .build()
        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }

    fun generateKey(pin: String): SecretKeySpec {
        val sha = MessageDigest.getInstance("SHA-256")
        val key = sha.digest(pin.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(key.copyOf(16), "AES")
    }

    fun encryptString(dataToEncrypt: ByteArray, pin: String): ByteArray {
        val keySpec = generateKey(pin)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, IvParameterSpec(ByteArray(16)))
        return cipher.doFinal(dataToEncrypt)
    }

    fun decryptString(dataToDecrypt: ByteArray, pin: String): ByteArray {
        val keySpec = generateKey(pin)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, keySpec, IvParameterSpec(ByteArray(16)))
        return cipher.doFinal(dataToDecrypt)
    }

    companion object {
        private const val KEY_ALIAS = "MySecureKey"
    }
}
