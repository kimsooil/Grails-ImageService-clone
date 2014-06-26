package edu.usf.cims

import spock.lang.*

class ImageServiceClientSpec extends spock.lang.Specification {

    def "Get an encrypted token"() {
        when:
        def imageServiceHost = 'localhost'
        def imageServicePort = 8080
        def serviceName = 'test'
        def serviceKey = 'AfoaKlDM4AjVyjo38f0NOs4O6hXM1T32'
        def usfid = 'U44989263'

        then:
        ImageServiceClient.getImageUrl(imageServiceHost, imageServicePort, serviceName, serviceKey, usfid) =~
            "https://localhost:8080/ImageService/view/test/.*.jpg"
    }

    def "resize an image"() {
        when:
        def imageServiceHost = 'localhost'
        def imageServicePort = 8080
        def serviceName = 'test'
        def serviceKey = 'AfoaKlDM4AjVyjo38f0NOs4O6hXM1T32'
        def usfid = 'U12345678'
        def width = 400
        def height = 400

        then:
        ImageServiceClient.getResizedImageUrl(imageServiceHost, imageServicePort, serviceName, serviceKey, usfid, width, height) =~
            "https://localhost:8080/ImageService/view/test/400/400/.*.jpg"
    }

}
