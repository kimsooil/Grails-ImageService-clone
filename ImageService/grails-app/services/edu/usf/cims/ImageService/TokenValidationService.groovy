package edu.usf.cims.ImageService

import edu.usf.cims.Security
import groovy.time.*

class TokenValidatorService {
    def grailsApplication

    static transactional = false

    def validateToken(String serviceName, String token){

        def results = [results:"", message: ""]

        def serviceData = grailsApplication.config.image_service.services[serviceName] ?: ''
        def plaintext = ''

        // Make sure the service passed is allowed to use this service
        if (serviceData == '') {
            results = [results: 'error', message: 'Invalid service']
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
                    results = [result: 'error', message: "Token decryption failed or incorrect token format. Result was: ${plaintext.encodeAsBase64()}"]
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
}
