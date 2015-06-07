package imageservice

import grails.util.Environment
import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration
import org.springframework.core.env.Environment
import org.springframework.context.EnvironmentAware
import org.springframework.core.env.MapPropertySource

class Application extends GrailsAutoConfiguration implements EnvironmentAware {
    static void main(String[] args) {
        GrailsApp.run(Application)
    }

    @Override
    void setEnvironment(Environment environment) {
        //Set up Configuration directory
        def imageHome = System.getenv('IMAGE_SERVICE_HOME') ?: System.getProperty('IMAGE_SERVICE_HOME') ?: "/opt/image_service"

        println ""
        println "Loading configuration from ${imageHome}"
        def appConfigured = new File(imageHome, 'ImageService.groovy').exists()
        println "Loading configuration file ${new File(imageHome, 'ImageService.groovy')}"
        println "Config file found : " + appConfigured

        if (appConfigured) {
            def config = new ConfigSlurper().parse(new File(imageHome, 'ImageService.groovy').toURL())
            environment.propertySources.addFirst(new MapPropertySource("ImageService", config))
        }
    }
}
