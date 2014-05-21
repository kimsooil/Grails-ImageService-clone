package edu.usf.cims.ImageService

import edu.usf.cims.Security
import groovy.time.*

class TokenValidatorService {
    def grailsApplication

    static transactional = false

    def validateToken(String serviceName, String token){

        def results = [results:"", message: ""]

        def serviceData = grailsApplication.config.services[serviceName]
        def plaintext = ''

        try {
            plaintext = Security.AESdecrypt(token, serviceData.key)
        } catch(Exception e) {
            results = [result: 'error', message: "Token decryption failed"]
            return results
        }

        //All decrypted tokens should be in this format: unixtime|U########
        if (! (plaintext ==~ /\d{1,}\|U\d{8}/)) {
            results = [result: 'error', message: "Token decryption failed or incorrect token format. Result was: ${plaintext.encodeAsBase64()}"]
            return results
        }

        //split the token into the generated time and the Unumber
        def tokenData = plaintext.tokenize('|')

        use (TimeCategory) {
            // Convert max/min time offset into a Date object
            def minTime = ((grailsApplication.config.maxTimeDrift).seconds.ago).time
            def maxTime = ((grailsApplication.config.maxTimeDrift).seconds.from.now).time
            def myTime = tokenData[0].toLong() * 1000

            if(!( minTime < myTime && myTime < maxTime)){
                results = [result: 'error', message: "Token time [${myTime}] out of range [${minTime} - ${maxTime}"]
            } else {
                results = [result: 'success', message: tokenData[1]]
            }
        }
        return results
    }
}
