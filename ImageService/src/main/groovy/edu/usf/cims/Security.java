package edu.usf.cims;

/**
* Class for encrypting/decrypting data using AES128
*
* CBC-mode generates a random initialization vector and prepends it the cipher text
**/

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.security.spec.KeySpec;
import java.io.UnsupportedEncodingException;
import org.apache.commons.codec.binary.Base64;
import java.security.SecureRandom;
import javax.crypto.Mac;
import java.security.MessageDigest;

public class Security {

  public static String AESencrypt(String input, String key){
    byte[] crypted = null;
    try{

      // Create a random initialization vector.
      SecureRandom random = new SecureRandom();
      byte[] randBytes = new byte[16];
      random.nextBytes(randBytes);
      IvParameterSpec iv = new IvParameterSpec(randBytes);

      SecretKeySpec skey = new SecretKeySpec(key.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, skey, iv);

        byte[] ivBytes = iv.getIV();
        byte[] inputBytes = input.getBytes();
        byte[] plaintext = new byte[ivBytes.length + inputBytes.length];

        // Prepend the IV to the ciphertext.
        System.arraycopy(ivBytes, 0, plaintext, 0, ivBytes.length);
        System.arraycopy(inputBytes, 0, plaintext, ivBytes.length, inputBytes.length);

        crypted = cipher.doFinal(plaintext);
      }catch(Exception e){
          System.out.println(e.toString());
      }
      return new String(Base64.encodeBase64URLSafeString(crypted));
  }

  public static String AESdecrypt(String input, String key){
      byte[] output = null;
      byte[] rawData = Base64.decodeBase64(input);
      byte[] iv = new byte[16];
      byte[] cipherText = new byte[rawData.length - iv.length];

      // Split the iv from the ciphertext.
      System.arraycopy(rawData, 0, iv, 0, 16);
      System.arraycopy(rawData, 16, cipherText, 0, cipherText.length);
      try{
        SecretKeySpec skey = new SecretKeySpec(key.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, skey, new IvParameterSpec(iv));

        output = cipher.doFinal(cipherText);
      }catch(Exception e){
        System.out.println(e.toString());
      }
      return new String(output);
  }

  public static String digest(String msg, String algo, String encoding) {
    String digest = null;
    try {
        MessageDigest crypt = MessageDigest.getInstance(algo);
        crypt.reset();
        crypt.update(msg.getBytes(encoding));
        byte[] bytes = crypt.digest();

        StringBuffer hash = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                hash.append('0');
            }
            hash.append(hex);
        }
        digest = hash.toString();
    } catch (Exception e){
        System.out.println(e.toString());
    }
    return digest;
  }

  public static String HMACdigest(String msg, String keyString, String algo, String encoding) {
    String digest = null;
    try {
        SecretKeySpec key = new SecretKeySpec(keyString.getBytes(), algo);
        Mac mac = Mac.getInstance(algo);
        mac.init(key);

        byte[] bytes = mac.doFinal(msg.getBytes(encoding));

        StringBuffer hash = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                hash.append('0');
            }
            hash.append(hex);
        }
        digest = hash.toString();
    } catch (Exception e){
        System.out.println(e.toString());
    }
    return digest;
  }
}
