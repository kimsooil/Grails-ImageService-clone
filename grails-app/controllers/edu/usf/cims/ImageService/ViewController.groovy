package edu.usf.cims.ImageService

import java.awt.image.BufferedImage

import javax.imageio.ImageIO
import org.imgscalr.Scalr
import org.imgscalr.Scalr.Method
import org.imgscalr.Scalr.Mode
import org.codehaus.groovy.grails.core.io.ResourceLocator
import org.springframework.core.io.Resource

/**
*
*/
class ViewController {
    def grailsApplication
    def grailsResourceLocator
    def tokenValidatorService
    def imageLocatorService

    def index = {}

    def viewImage = {
        def token = params.requestFile.replaceAll(/(\.jpg|\.JPG)$/,"")

        def result = tokenValidatorService.validateToken(params.serviceName, token)

        def imageData = imageLocatorService.locate(params.serviceName, result)

        def logMessage = "VIEW|${request.getRemoteAddr()}|${params.serviceName}|${token}|${result.result}|${result.message}|${imageData.type}"

        if (result.result == 'error'){
            log.error(logMessage)
        } else {
            log.info(logMessage)
        }

        //Read image from file
        def BufferedImage i = ImageIO.read(imageData.file)
        response.contentType = "image/jpeg"
        ImageIO.write(i, "jpg", response.outputStream)
        return
    }

    def resizeImage = {
        //Check images resize parameters
        if ((! params.width.isNumber() ) || (! params.height.isNumber() )) {
            renderError(500 , "Width and Height must be numbers")
            return
        }
        if((params.width.toInteger() > grailsApplication.config.maxImageWidth.toInteger()) || (params.width.toInteger() < grailsApplication.config.minImageWidth.toInteger()) ){
            renderError(500, "Image width must be between ${grailsApplication.config.minImageWidth} and ${grailsApplication.config.maxImageWidth}")
            return
        }
        if((params.height.toInteger() > grailsApplication.config.maxImageHeight.toInteger()) || (params.height.toInteger() < grailsApplication.config.minImageHeight.toInteger()) ){
            renderError(500, "Image height must be between ${grailsApplication.config.minImageHeight} and ${grailsApplication.config.maxImageHeight}")
            return
        }

        def token = params.requestFile.replaceAll(/(\.jpg|\.JPG)$/,"")

        def result = tokenValidatorService.validateToken(params.serviceName, token)

        def imageData = imageLocatorService.locate(params.serviceName, result)

        log.error("RESIZE|${request.getRemoteAddr()}|${params.serviceName}|${token}|${result.result}|${result.message}|${imageData.type}|${params.width}|${params.height}")

        //Read image from file
        BufferedImage orig = ImageIO.read(imageData.file)
        BufferedImage scaled = Scalr.resize(orig, Scalr.Method.SPEED, Scalr.Mode.FIT_EXACT, params.width as int, params.height as int)
        orig.flush()
        response.contentType = "image/jpeg"
        ImageIO.write(scaled, "jpg", response.outputStream)
        return
    }

    def showError = {
        renderError(500, "Bad Request")
    }

    def renderError(statusCode, errorMessage){
        response.status = statusCode
        render "<H1>Error</H1><H2>${errorMessage}</H2>"
    }
}
