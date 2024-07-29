package tv.anoki.framework.ui

import android.util.Base64
import tv.anoki.framework.BuildConfig
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

fun decryptLicenceUrl(
    encryptedText: String?,
    secretKey: String = BuildConfig.DRM_SECREAT_KEY,
): String {
    if(encryptedText.isNullOrEmpty()) {
        return ""
    }

    val cipher = Cipher.getInstance("AES/ECB/NoPadding")
    val secretKeySpec = SecretKeySpec(secretKey.toByteArray(), "AES")
    cipher.init(Cipher.DECRYPT_MODE, secretKeySpec)
    val encryptedBytes = Base64.decode(encryptedText, Base64.DEFAULT)
    val decryptedBytes = cipher.doFinal(encryptedBytes)
    return String(decryptedBytes)
}