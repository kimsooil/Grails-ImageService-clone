package edu.usf.cims.ImageService

import java.nio.file.Files
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import org.imgscalr.Scalr
import org.imgscalr.Scalr.Method
import org.imgscalr.Scalr.Mode
import org.springframework.core.io.Resource

/**
*
*/
class ViewController {
    def tokenValidatorService
    def imageLocatorService

    def index = {}

    def viewImage = {

        // The token is the image filename
        def token = params.requestFile.replaceAll(/(\.jpg|\.JPG)$/,"")

        def result = tokenValidatorService.validateToken(params.serviceName, token)

        def imageData = imageLocatorService.locate(params.serviceName, result)

        def logMessage = "VIEW|${request.getRemoteAddr()}|${params.serviceName}|${token}|${result.result}|${result.message}|${imageData.type}"

        if (result.result == 'error'){
            log.error(logMessage)
        } else {
            log.info(logMessage)
        }

        // Read image from file
        response.contentType = "image/jpeg"
        response.outputStream << Files.readAllBytes(imageData.file.toPath())
        return
    }

    def resizeImage = {
        // Image size must be an integer
        if ( (! params.width.isNumber()) || (! params.height.isNumber()) ) {
            renderError(500 , "Width and Height must be numbers")
            return
        }

        // Image width must be in the correct range
        if( (params.width.toInteger() > grailsApplication.config.image_service.maxImageWidth.toInteger())
            || (params.width.toInteger() < grailsApplication.config.image_service.minImageWidth.toInteger())
          ){
            renderError(500, "Image width must be between ${grailsApplication.config.image_service.minImageWidth} and ${grailsApplication.config.image_service.maxImageWidth}")
            return
        }

        // Image height must be in the correct range
        if( (params.height.toInteger() > grailsApplication.config.image_service.maxImageHeight.toInteger())
            || (params.height.toInteger() < grailsApplication.config.image_service.minImageHeight.toInteger())
          ){
            renderError(500, "Image height must be between ${grailsApplication.config.image_service.minImageHeight} and ${grailsApplication.config.image_service.maxImageHeight}")
            return
        }

        def token = params.requestFile.replaceAll(/(\.jpg|\.JPG)$/,"").trim()

        def result = tokenValidatorService.validateToken(params.serviceName, token)

        def imageData = imageLocatorService.locate(params.serviceName, result)

        def logMessage = "RESIZE|${request.getRemoteAddr()}|${params.serviceName}|${token}|${result.result}|${result.message}|${imageData.type}|${params.width}|${params.height}"

        if (result.result == 'error'){
            log.error(logMessage)
        } else {
            log.info(logMessage)
        }

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
