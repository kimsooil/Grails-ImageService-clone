package edu.usf.cims;

/**
* Class for creating an AES128 ecrypted token for interacting with Image Service
*
**/

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.security.spec.KeySpec;
import java.io.UnsupportedEncodingException;
import org.apache.commons.codec.binary.Base64;
import java.security.SecureRandom;

import org.apache.http.client.utils.URIBuilder;
import java.net.URI;


public class ImageServiceClient {

    public static URI getImageUrl(String imageServiceHost, int imageServicePort, String serviceName,
                                    String serviceKey, String usfid){

        String unixtime = String.valueOf(System.currentTimeMillis() / 1000L);
        String plaintext = unixtime + '|' + usfid;

        String encryptedToken = ImageServiceClient.AESencrypt(plaintext, serviceKey);

        try {
            URIBuilder builder = new URIBuilder()
                .setScheme("https")
                .setHost(imageServiceHost)
                .setPort(imageServicePort)
                .setPath("/ImageService/view/" + serviceName + '/' + encryptedToken + ".jpg");

            return builder.build();
        } catch (Exception e) {
            System.out.println(e.toString());
        }

        return null;
    }

    public static URI getResizedImageUrl(String imageServiceHost, int imageServicePort, String serviceName,
                                            String serviceKey, String usfid, int width, int height){

        String unixtime = String.valueOf(System.currentTimeMillis() / 1000L);
        String plaintext = unixtime + '|' + usfid;

        String encryptedToken = ImageServiceClient.AESencrypt(plaintext, serviceKey);

        try {
          URIBuilder builder = new URIBuilder()
              .setScheme("https")
              .setHost(imageServiceHost)
              .setPort(imageServicePort)
              .setPath("/ImageService/view/" + serviceName + '/' + width + '/' + height + '/' + encryptedToken + ".jpg");

          return builder.build();
        } catch (Exception e) {
          System.out.println(e.toString());
        }

        return null;
    }

  private static String AESencrypt(String input, String key){
    byte[] output = null;
    try{

      //Create a random initialization vector
      SecureRandom random = new SecureRandom();
      byte[] randBytes = new byte[16];
      random.nextBytes(randBytes);
      IvParameterSpec iv = new IvParameterSpec(randBytes);

      SecretKeySpec skey = new SecretKeySpec(key.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, skey, iv);

        byte[] ivBytes = iv.getIV();
        byte[] inputBytes = input.getBytes();
        byte[] crypted = cipher.doFinal(inputBytes);

        output = new byte[ivBytes.length + crypted.length];

        System.arraycopy(ivBytes, 0, output, 0, ivBytes.length);
        System.arraycopy(crypted, 0, output, ivBytes.length, crypted.length);

      }catch(Exception e){
          System.out.println(e.toString());
      }
      return new String(Base64.encodeBase64(output, true, true));
  }
}