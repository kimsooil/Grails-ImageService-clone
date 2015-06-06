package edu.usf.cims.ImageService

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import spock.lang.*
import edu.usf.cims.Security
import grails.test.mixin.TestFor

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
@TestFor(TokenValidatorService)
class TokenValidatorServiceSpec extends Specification {

    static doWithConfig(c) {
        c.image_service.services.aes_test = [
                tokenAlg: 'AES',
                privacy: false,
                separator: '|',
                encoding: 'ASCII',
                key: 'AfoaKlDM4AjVyjo38f0NOs4O6hXM1T32'
        ]
        c.image_service.services.sha1_test = [
                tokenAlg: 'SHA-1',
                privacy: false,
                separator: '_',
                encoding: 'ASCII',
                key: '8fKqPyfAah56cRXM0Qafkom10zn7Upw2'
        ]
        c.image_service.services.shaHmac_test = [
                tokenAlg: 'HmacSHA1',
                privacy: false,
                separator: '_',
                encoding: 'ASCII',
                key: 'bfTkmK8CxKAQMABJ5Wg1xmqycvvxkdLa'
        ]
    }

    def setup() {
    }

    def cleanup() {
    }

    def "Unknown service name"() {
        given:
        def tokenValidatorService=grailsApplication.mainContext.getBean('tokenValidatorService')
        def unixTime = new Date().getTime() / 1000 as Integer
        def token = Security.AESencrypt("${unixTime}|U12345678", config.image_service.services.aes_test.key)

        when:
        def result = tokenValidatorService.validateToken('bad', token)

        then:
        result == [result:"error", message:"Invalid service"]
    }

    def "Good AES-encrypted token"() {
        given:
        def tokenValidatorService=grailsApplication.mainContext.getBean('tokenValidatorService')
        def unixTime = new Date().getTime() / 1000 as Integer
        def token = Security.AESencrypt("${unixTime}|U12345678", config.image_service.services.aes_test.key)

        when:
        def result = tokenValidatorService.validateToken('aes_test', token)

        then:
        result == [result:"success", message:"U12345678"]
    }

    def "Bad AES token - expired"() {
        given:
        def tokenValidatorService=grailsApplication.mainContext.getBean('tokenValidatorService')
        def token = Security.AESencrypt("00000000000|U12345678", config.image_service.services.aes_test.key)

        when:
        def result = tokenValidatorService.validateToken('aes_test', token)

        then:
        result.result == "error"
        result.message.contains('out of range')
    }

    def "Bad AES token - future dated"() {
        given:
        def tokenValidatorService=grailsApplication.mainContext.getBean('tokenValidatorService')
        def token = Security.AESencrypt("999999999999|U12345678", config.image_service.services.aes_test.key)

        when:
        def result = tokenValidatorService.validateToken('aes_test', token)

        then:
        result.result == "error"
        result.message.contains('out of range')
    }

    def "Bad AES key value"() {
        given:
        def tokenValidatorService=grailsApplication.mainContext.getBean('tokenValidatorService')
        def unixTime = new Date().getTime() / 1000 as Integer
        def token = Security.AESencrypt("${unixTime}|U12345678", '12345678901234567890123456789012')

        when:
        def result = tokenValidatorService.validateToken('aes_test', token)

        then:
        result == [result:"error", message:"Token decryption failed"]
    }


    def "Bad AES token format"() {
        given:
        def tokenValidatorService=grailsApplication.mainContext.getBean('tokenValidatorService')
        def unixTime = new Date().getTime() / 1000 as Integer
        def token = Security.AESencrypt("${unixTime}_blah", config.image_service.services.aes_test.key)

        when:
        def result = tokenValidatorService.validateToken('aes_test', token)

        then:
        result.result == "error"
        result.message.contains('Token decryption failed or incorrect token format')
    }

    def "Good SHA-1 hashed token"() {
        given:
        def tokenValidatorService=grailsApplication.mainContext.getBean('tokenValidatorService')
        def tokenText = String.format('%1$s%2$s', 'U12345678', config.image_service.services.sha1_test.key)
        def token = "U12345678_${Security.digest(tokenText, 'SHA-1', 'ASCII')}"

        when:
        def result = tokenValidatorService.validateToken('sha1_test', token)

        then:
        result == [result:"success", message:"U12345678"]
    }

    def "Bad SHA-1 hash (wrong key)"() {
        given:
        def tokenValidatorService=grailsApplication.mainContext.getBean('tokenValidatorService')
        def tokenText = String.format('%1$s%2$s', 'U12345678', '12345678901234567890123456789012')
        def token = "U12345678_${Security.digest(tokenText, 'SHA-1', 'ASCII')}"

        when:
        def result = tokenValidatorService.validateToken('sha1_test', token)

        then:
        result.result == "error"
        result.message.contains('not valid for U12345678')
    }

    def "Good HmacSHA1 hashed token"() {
        given:
        def tokenValidatorService=grailsApplication.mainContext.getBean('tokenValidatorService')
        def token = "U12345678_${Security.HMACdigest('U12345678', config.image_service.services.shaHmac_test.key, 'HmacSHA1', 'ASCII')}"

        when:
        def result = tokenValidatorService.validateToken('shaHmac_test', token)

        then:
        result == [result:"success", message:"U12345678"]
    }

    def "Bad HmacSHA1 hash (wrong key)"() {
        given:
        def tokenValidatorService=grailsApplication.mainContext.getBean('tokenValidatorService')
        def token = "U12345678_${Security.HMACdigest('U12345678', '12345678901234567890123456789012', 'HmacSHA1', 'ASCII')}"

        when:
        def result = tokenValidatorService.validateToken('sha1_test', token)

        then:
        result.result == "error"
        result.message.contains('not valid for U12345678')
    }
}
