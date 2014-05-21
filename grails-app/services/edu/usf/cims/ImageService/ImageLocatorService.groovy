package edu.usf.cims.ImageService


class ImageLocatorService {
    def grailsApplication

    static transactional = false

    def locate(String serviceName, Map tokenResult){

        if (tokenResult.result == 'success') {
            //Check for the existince of a picture in the 'normal' directory
            def imageFile = new File("${grailsApplication.config.normalImageDir}/${tokenResult.message}.jpg")

            if(imageFile.exists() ){
                log.debug("Returned public image for [${tokenResult.message}]")
                return [type: "public_image", file: imageFile]
            } else {
                //No image found.  Can the service see images with privacy set?
                if (grailsApplication.config.services[serviceName].privacy) {
                    imageFile = new File("${grailsApplication.config.privateImageDir}/${tokenResult.message}.jpg")
                    if(imageFile.exists() ) {
                        log.debug("Returned private image for [${tokenResult.message}] Service [${serviceName}]")
                        return [type: "private_image", file: imageFile]
                    }
                }

                //No image found in private directory or this service can't view them.  Return default pic
                log.debug("No image found for [${tokenResult.message}]")
                return [type: "default_image", file: new File(grailsApplication.config.defaultImage)]
            }


        } else {
            //Token result was not successful - send the default image
            return [type: "error", file: new File(grailsApplication.config.defaultImage)]
        }
    }
}
