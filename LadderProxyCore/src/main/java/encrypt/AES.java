package encrypt;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;

public class AES {

    public static byte[] encrypt(byte[] rawKey, byte[] message) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(rawKey, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            return cipher.doFinal(message);
        } catch (Exception e) {
            return null;
        }
    }

    public static byte[] decrypt(byte[] rawKey, byte[] encrypted) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(rawKey, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
            return cipher.doFinal(encrypted);
        } catch (Exception e) {
            return null;
        }
    }

    public static byte[] getRawKey(String password) {
        try {
            KeyGenerator key = KeyGenerator.getInstance("AES");
            SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
            secureRandom.setSeed(password.getBytes());
            // AES加密数据块分组长度必须为128比特，密钥长度可以是128比特、192比特、256比特中的任意一个
            key.init(128, secureRandom);
            SecretKey secretKey = key.generateKey();
            return secretKey.getEncoded();
        } catch (Exception ignored) {
            return null;
        }

    }
}