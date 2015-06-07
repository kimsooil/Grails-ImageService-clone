package edu.usf.cims.ImageService

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import spock.lang.*
import grails.test.mixin.TestFor

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
@TestFor(ImageLocatorService)
class ImageLocatorServiceSpec extends Specification {

    static doWithConfig(c) {
        c.image_service.defaultImage = "grails-app/assets/images/rocky.jpg"
        c.image_service.normalImageDir = "grails-app/assets/images"
        c.image_service.privateImageDir = "grails-app/assets/images/private"

        c.image_service.services.public_test = [
                tokenAlg: 'AES',
                privacy: false,
                separator: '|',
                encoding: 'ASCII',
                key: 'AfoaKlDM4AjVyjo38f0NOs4O6hXM1T32'
        ]
        c.image_service.services.private_test = [
                tokenAlg: 'SHA-1',
                privacy: true,
                separator: '_',
                encoding: 'ASCII',
                key: '8fKqPyfAah56cRXM0Qafkom10zn7Upw2'
        ]
    }

    def cleanup() {
    }

    def "Return Rocky the Bull on error"() {
        given:
        def imageLocatorService=grailsApplication.mainContext.getBean('imageLocatorService')

        when:
        def result = imageLocatorService.locate('public_test', [result: "error"])
        then:
        result.type == "error"
        result.file instanceof java.io.File
        result.file.path.contains 'grails-app/assets/images/rocky.jpg'
    }

    def "Return Rocky the Bull on unknown Unumber"() {
        given:
        def imageLocatorService=grailsApplication.mainContext.getBean('imageLocatorService')

        when:
        def result = imageLocatorService.locate('public_test', [result: "success", message: "U999999999"])
        then:
        result.type == "default_image"
        result.file instanceof java.io.File
        result.file.path.contains 'grails-app/assets/images/rocky.jpg'
    }

    def "Return Rocky the Bull on private pics"() {
        given:
        def imageLocatorService=grailsApplication.mainContext.getBean('imageLocatorService')

        when:
        def result = imageLocatorService.locate('public_test', [result: "success", message: "U34567890"])
        then:
        result.type == "default_image"
        result.file instanceof java.io.File
        result.file.path.contains 'grails-app/assets/images/rocky.jpg'
    }

    def "Return a public image to a public service"() {
        given:
        def imageLocatorService=grailsApplication.mainContext.getBean('imageLocatorService')

        when:
        def result = imageLocatorService.locate('public_test', [result: "success", message: "U12345678"])
        then:
        result.type == "public_image"
        result.file instanceof java.io.File
        result.file.path.contains 'grails-app/assets/images/U12345678.jpg'
    }

    def "Return a public image to a private service"() {
        given:
        def imageLocatorService=grailsApplication.mainContext.getBean('imageLocatorService')

        when:
        def result = imageLocatorService.locate('private_test', [result: "success", message: "U12345678"])
        then:
        result.type == "public_image"
        result.file instanceof java.io.File
        result.file.path.contains 'grails-app/assets/images/U12345678.jpg'
    }

    def "Return a private image"() {
        given:
        def imageLocatorService=grailsApplication.mainContext.getBean('imageLocatorService')

        when:
        def result = imageLocatorService.locate('private_test', [result: "success", message: "U34567890"])
        then:
        result.type == "private_image"
        result.file instanceof java.io.File
        result.file.path.contains 'grails-app/assets/images/private/U34567890.jpg'
    }
}
