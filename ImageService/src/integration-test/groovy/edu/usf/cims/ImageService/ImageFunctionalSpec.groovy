package edu.usf.cims.ImageService

import edu.usf.cims.Security
import grails.test.mixin.integration.Integration
import grails.transaction.*

import spock.lang.*
import geb.spock.*
import edu.usf.cims.ImageUtil
import javax.imageio.ImageIO

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@Integration
@Rollback
class ImageFunctionalSpec extends GebSpec {

    def setup() {
    }

    def cleanup() {
        new File('/tmp/test.jpg').delete()
    }

    void "test Request bad token"() {
        when:
        go "/"
        new FileOutputStream('/tmp/test.jpg').withStream { s ->
            s << new BufferedInputStream( new ByteArrayInputStream(downloadBytes('/view/bad_test/U12345678_61eca870a86cd0d8238c8530ef5280d4c01640.jpg')) )
        }

        then:
        def difference = ImageUtil.getPercentDifference(new File("grails-app/assets/images/rocky.jpg"), new File('/tmp/test.jpg'))
        difference == 0
    }

    void "test Request good image"() {
        given:
        def unixTime = new Date().getTime() / 1000 as Integer
        def token = Security.AESencrypt("${unixTime}|U12345678", 'AfoaKlDM4AjVyjo38f0NOs4O6hXM1T32')

        when:
        go "/"
        new FileOutputStream('/tmp/test.jpg').withStream { s ->
            s << new BufferedInputStream( new ByteArrayInputStream(downloadBytes("/view/test/${token}.jpg")) )
        }

        then:
        def difference = ImageUtil.getPercentDifference(new File("grails-app/assets/images/U12345678.jpg"), new File('/tmp/test.jpg'))
        difference == 0
    }

    void "test Request missing image"() {
        given:
        def unixTime = new Date().getTime() / 1000 as Integer
        def token = Security.AESencrypt("${unixTime}|U12345679", 'AfoaKlDM4AjVyjo38f0NOs4O6hXM1T32')

        when:"A missing image filename is requested"
        go "/"
        new FileOutputStream('/tmp/test.jpg').withStream { s ->
            s << new BufferedInputStream( new ByteArrayInputStream(downloadBytes("/view/test/${token}.jpg")) )
        }

        then:
        def difference = ImageUtil.getPercentDifference(new File("grails-app/assets/images/rocky.jpg"), new File('/tmp/test.jpg'))
        difference == 0
    }

    void "test Request private image"() {
        given:
        def unixTime = new Date().getTime() / 1000 as Integer
        def token = Security.AESencrypt("${unixTime}|U34567890", 'AfoaKlDM4AjVyjo38f0NOs4O6hXM1T32')

        when:"A private image filename is requested"
        go "/"
        new FileOutputStream('/tmp/test.jpg').withStream { s ->
            s << new BufferedInputStream( new ByteArrayInputStream(downloadBytes("/view/test/${token}.jpg")) )
        }

        then:
        def difference = ImageUtil.getPercentDifference(new File("grails-app/assets/images/private/U34567890.jpg"), new File('/tmp/test.jpg'))
        difference == 0
    }

    void "test Resize must be numeric"() {
        when:"A good image filename is requested"
        go "/view/test/width/height/U12345678_61eca870a86cd0d8238c8530ef5280d4c01640.jpg"

        then:
        $("h2").text() == "Width and Height must be numbers"
    }

    void "test Bad width options"() {
        when:"A good image filename is requested"
        go "/view/test/5000/200/U12345678_61eca870a86cd0d8238c8530ef5280d4c01640.jpg"

        then:
        $("h2").text().contains("Image width must be between")
    }

    void "test Bad height options"() {
        when:"A good image filename is requested"
        go "/view/test/200/5000/U12345678_61eca870a86cd0d8238c8530ef5280d4c01640.jpg"

        then:
        $("h2").text().contains("Image height must be between")
    }

    void "test Request good resize"() {
        given:
        def unixTime = new Date().getTime() / 1000 as Integer
        def token = Security.AESencrypt("${unixTime}|U12345678", 'AfoaKlDM4AjVyjo38f0NOs4O6hXM1T32')

        when:
        go "/"
        new FileOutputStream('/tmp/test.jpg').withStream { s ->
            s << new BufferedInputStream( new ByteArrayInputStream(downloadBytes("/view/test/250/250/${token}.jpg")) )
        }

        then:
        def difference = ImageUtil.getPercentDifference(new File("grails-app/assets/images/U12345678.jpg"), new File('/tmp/test.jpg'))
        difference == null
        def bimg = ImageIO.read(new File('/tmp/test.jpg'))
        bimg.getWidth() == 250
        bimg.getHeight() == 250
    }
}
