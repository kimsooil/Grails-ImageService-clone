package edu.usf.cims.ImageService

import edu.usf.cims.Security
import groovy.time.TimeCategory
import grails.core.GrailsApplication

import javax.crypto.*
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.DESKeySpec
import javax.crypto.spec.IvParameterSpec
import java.security.spec.KeySpec
import java.io.UnsupportedEncodingException
import org.apache.commons.codec.binary.Base64
import java.security.SecureRandom

class TokenValidatorService {
    GrailsApplication grailsApplication

    static transactional = false

    def validateToken(String serviceName, String token) {

        def results = [result:"", message: ""]

        def serviceData = grailsApplication.config.image_service.services[serviceName] ?: ''
        def plaintext = ''

        // Make sure the service passed is allowed to use this service
        if (serviceData == '') {
            results = [result: 'error', message: 'Invalid service']
            return results
        }

        switch (serviceData.tokenAlg) {
            // AES256 encrypted token
            case 'AES':
                try {
                    plaintext = Security.AESdecrypt(token, serviceData.key)
                } catch(Exception e) {
                    results = [result: 'error', message: "Token decryption failed"]
                    return results
                }

                // All decrypted tokens should be in this format: unixtime${serviceData.separator}U########
                if (! (plaintext ==~ /\d{10,13}.U\d{8}.*/)) {
                    results = [result: 'error', message: "Token decryption failed or incorrect token format. Result was: ${plaintext}"]
                    return results
                }

                // Split the token into the generated time and the Unumber
                def tokenData = plaintext.tokenize(serviceData.separator)

                use (TimeCategory) {
                    // Convert max/min time offset into a Date object
                    def minTime = ((grailsApplication.config.image_service.maxTimeDrift).seconds.ago).time
                    def maxTime = ((grailsApplication.config.image_service.maxTimeDrift).seconds.from.now).time
                    def myTime = tokenData[0].toLong() * 1000

                    if(!( minTime < myTime && myTime < maxTime)){
                        results = [result: 'error', message: "Token time [${myTime}] out of range [${minTime} - ${maxTime}"]
                    } else {
                        results = [result: 'success', message: tokenData[1]]
                    }
                }
                break

            // Message digest hashed value
            case 'MD5':
            case 'SHA-1':
            case 'SHA-256':
            case 'SHA-384':
            case 'SHA-512':
                // Split the Unumber from the hash.
                def tokenData = token.tokenize(serviceData.separator)

                def tokenFormat = serviceData.tokenFormat ?: '%1$s%2$s'
                def tokenText = String.format(tokenFormat, tokenData[0], serviceData.key)

                // Generate a hash for the key/unumber combination
                def myHash = Security.digest(tokenText, serviceData.tokenAlg, serviceData.encoding)

                // Compare the given hash with the one we just generated
                if (myHash == tokenData[1]){
                    results = [result: 'success', message: tokenData[0]]
                } else {
                    results = [result: 'error', message: "Token hash ${tokenData[1]} not valid for ${tokenData[0]}.  Expected ${myHash}"]
                }
                break

            // HMAC Hashed value
            case 'HmacMD5':
            case 'HmacSHA1':
            case 'HmacSHA256':
            case 'HmacSHA384':
            case 'HmacSHA512':
                // Split the Unumber from the hash.
                def tokenData = token.tokenize(serviceData.separator)

                // Generate a hash for the Unumber
                def myHash = Security.HMACdigest(tokenData[0], serviceData.key, serviceData.tokenAlg, serviceData.encoding)

                // Compare the given hash with the one we just generated
                if (myHash == tokenData[1]){
                    results = [result: 'success', message: tokenData[0]]
                } else {
                    results = [result: 'error', message: "Token hash ${tokenData[1]} not valid for ${tokenData[0]}.  Expected ${myHash}"]
                }
                break

            default:
                results = [result: 'error', message: "Token validation failed.  Invalid algorithm specified"]
                return results
        }
        return results
    }
    
    def lookupToken(String token) {
        def results = [result:"", message: ""]

        def serviceData = grailsApplication.config.image_service.services[serviceName] ?: ''
        def services = grailsApplication.config.image_service.services ?: []
        def plaintext = ''

        // Make sure the service passed is allowed to use this service
        if (serviceData == '') {
            results = [result: 'error', message: 'Invalid service']
            return results
        }
        
        def mulesoftKey = grailsApplication.config.image_service.mulesoftKey ?: ''
        
        try {
            plaintext = Security.AESdecrypt(token, mulesoftKey)
        } catch(Exception e) {
            results = [result: 'error', message: "Token decryption failed"]
            return results
        }
        
        // Split the token into the generated time and the Unumber
        def tokenData = plaintext.tokenize(serviceData.separator)

        use (TimeCategory) {
            // Convert max/min time offset into a Date object
            def minTime = ((grailsApplication.config.image_service.maxTimeDrift).seconds.ago).time
            def maxTime = ((grailsApplication.config.image_service.maxTimeDrift).seconds.from.now).time
            def myTime = tokenData[0].toLong() * 1000

            if(!( minTime < myTime && myTime < maxTime)){
                results = [result: 'error', message: "Token time [${myTime}] out of range [${minTime} - ${maxTime}"]
            } else {
                def mulesoftService = services.find { key,value -> 
                    return (tokenData[1] in value.mulesoft)
                }
                if(mulesoftService) {
                    results = [result: 'success', message: AESencrypt([ mulesoftService.key,mulesoftService.value.key ].join(mulesoftService.value.separator), mulesoftKey) ]
                } else {
                    results = [result: 'error', message: "Client ID is not associated with a configured ImageService profile"]
                }
            }
        }
    }    
  
    private static AESencrypt(input, key) {
        byte[] output = null
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
        if(output == null) {
            throw new AssertionError("Output cannot be generated and is NULL! You may need to install the Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy if you haven't already")
        }

        return new String(Base64.encodeBase64URLSafe(output))

    } // end AESencrypt  
  
}
