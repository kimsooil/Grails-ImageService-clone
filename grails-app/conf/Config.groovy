//Load external config
grails.config.locations = [ "file:/usr/local/etc/grails/${appName}.groovy" ]

grails.project.groupId = appName // change this to alter the default package name and Maven publishing destination
grails.mime.file.extensions = true // enables the parsing of file extensions from URLs into the request format
grails.mime.use.accept.header = false
grails.mime.types = [
    all:           '*/*',
    jpg:           'image/jpeg',
    png:           'image/png',
    atom:          'application/atom+xml',
    css:           'text/css',
    csv:           'text/csv',
    form:          'application/x-www-form-urlencoded',
    html:          ['text/html','application/xhtml+xml'],
    js:            'text/javascript',
    json:          ['application/json', 'text/json'],
    multipartForm: 'multipart/form-data',
    rss:           'application/rss+xml',
    text:          'text/plain',
    xml:           ['text/xml', 'application/xml']
]

grails.urlmapping.cache.maxsize = 1000

// The default codec used to encode data with ${}
grails.views.default.codec = "none" // none, html, base64
grails.views.gsp.encoding = "UTF-8"
grails.converters.encoding = "UTF-8"
// enable Sitemesh preprocessing of GSP pages
grails.views.gsp.sitemesh.preprocess = true
// scaffolding templates configuration
grails.scaffolding.templates.domainSuffix = 'Instance'

// Set to false to use the new Grails 1.2 JSONBuilder in the render method
grails.json.legacy.builder = false
// enabled native2ascii conversion of i18n properties files
grails.enable.native2ascii = true
// packages to include in Spring bean scanning
grails.spring.bean.packages = []
// whether to disable processing of multi part requests
grails.web.disable.multipart=false

// request parameters to mask when logging exceptions
grails.exceptionresolver.params.exclude = ['password']

// configure auto-caching of queries by default (if false you can cache individual queries with 'cache: true')
grails.hibernate.cache.queries = false

// Set the URL to / instead of /ImageService
grails.app.context = '/'

environments {
    development {
        grails.logging.jul.usebridge = true
    }
    production {
        grails.logging.jul.usebridge = false
        // TODO: grails.serverURL = "http://www.changeme.com"
    }
}

// log4j configuration
log4j = {
    // Example of changing the log pattern for the default console appender:
    //
    //appenders {
    //    console name:'stdout', layout:pattern(conversionPattern: '%c{2} %m%n')
    //}

    error  'org.codehaus.groovy.grails.web.servlet',        // controllers
           'org.codehaus.groovy.grails.web.pages',          // GSP
           'org.codehaus.groovy.grails.web.sitemesh',       // layouts
           'org.codehaus.groovy.grails.web.mapping.filter', // URL mapping
           'org.codehaus.groovy.grails.web.mapping',        // URL mapping
           'org.codehaus.groovy.grails.commons',            // core / classloading
           'org.codehaus.groovy.grails.plugins',            // plugins
           'org.codehaus.groovy.grails.orm.hibernate',      // hibernate integration
           'org.springframework',
           'org.hibernate',
           'net.sf.ehcache.hibernate'
}


//Default config - override in /usr/local/etc/grails/ImageService.groovy
maxImageWidth = 400
maxImageHeight = 600
minImageWidth = 10
minImageHeight = 20

maxTimeDrift = 30 //in seconds

defaultImage = '/tmp/rocky.jpg'
normalImageDir = '/tmp/images'
privateImageDir = '/tmp/images-priv'


// Keys to decrypt tokens with
services = [
    test : [tokenAlg: 'AES', privacy: false, separator: '|', encoding: 'ASCII', key:'AfoaKlDM4AjVyjo38f0NOs4O6hXM1T32'],
    test2: [tokenAlg: 'HmacMD5', privacy: true, separator: '_', encoding: 'ASCII', key:'8fKqPyfAah56cRXM0Qafkom10zn7Upw2'],
    test3: [tokenAlg: 'HmacSHA1', privacy: true, separator: '-', encoding: 'UTF-8', key:'YR9LgYIGO3psi7vW62DTHFeh9Vc5lZpO'],
    test4: [tokenAlg: 'HmacSHA256', privacy: false, separator: '|', encoding: 'UTF-8', key:'6JGeveYG70dhuYXWF7JGWzzG84P4BRgn']
]
