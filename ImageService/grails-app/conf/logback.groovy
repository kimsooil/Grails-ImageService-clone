import grails.util.BuildSettings
import grails.util.Environment


// See http://logback.qos.ch/manual/groovy.html for details on configuration
appender('STDOUT', ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%level %logger - %msg%n"
    }
}

appender('AUDIT', FileAppender) {
    encoder(PatternLayoutEncoder) {
        file = "${targetDir}/ImageService-audit.log"
        append = true
        encoder(PatternLayoutEncoder) {
            pattern = "%level %logger - %msg%n"
        }
    }
}

root(ERROR, ['STDOUT'])
logger("grails.app.controllers", INFO)

if(Environment.current == Environment.DEVELOPMENT) {
    def targetDir = BuildSettings.TARGET_DIR
    if(targetDir) {

        appender("FULL_STACKTRACE", FileAppender) {

            file = "${targetDir}/stacktrace.log"
            append = true
            encoder(PatternLayoutEncoder) {
                pattern = "%level %logger - %msg%n"
            }
        }
        logger("StackTrace", ERROR, ['FULL_STACKTRACE'], false )
    }
}
