package edu.usf.cims.idcard.ImageFetcher

import groovy.sql.Sql
import groovy.io.FileType

import groovy.util.logging.Slf4j

import groovy.util.CliBuilder
import org.apache.commons.cli.Option

import java.awt.image.BufferedImage
import javax.imageio.ImageIO

import org.imgscalr.Scalr

import org.imgscalr.Scalr.*

@Slf4j
class ImageFetchTool {

  public static String PATTERN = "%d{ABSOLUTE} %-5p [%c{1}] %m%n"

  public static void main(String[] args) {

    def start = System.currentTimeMillis()

    def opt = getCommandLineOptions(args)
    def config = getConfigSettings(opt)

    try {
      def cardDataSQL = Sql.newInstance( config.cardData.connector, config.cardData.user, config.cardData.password, config.cardData.driver )

      // Use today's date unless the --date switch was used
      def date = new Date().format('yyyyMMdd') as String
      if (opt.date) date = opt.date

      def selectStatementAllActive = "SELECT ID_PERSON, ID_IMAGE_FILE_NAME FROM idcard.id WHERE id.ID_ACTIVE_CODE='A' AND ID_PERSON LIKE 'U%' AND ID_IMAGE_FILE_NAME IS NOT NULL" as String
      def selectStatementSingleDay = "SELECT ID_PERSON, ID_IMAGE_FILE_NAME FROM idcard.id WHERE id.ID_ACTIVE_CODE='A' AND ID_PERSON LIKE 'U%' AND ID_IMAGE_FILE_NAME LIKE '${date}%'" as String

      // Just select the cards created on a specific day unless the --all switch was used
      def activeStatement = selectStatementSingleDay
      if (opt.all) activeStatement = selectStatementAllActive

      def images = 0
      def errors = 0
      def toPrivate = 0
      def fromPrivate = 0

      cardDataSQL.eachRow(activeStatement) { row ->
        def identifier = row.ID_PERSON
        def path = row.ID_IMAGE_FILE_NAME.trim().tokenize('\\')
        def fileName = path.pop()
        def directory = path.pop()

        def origFileLocation = "${config.origBaseDir}/${directory}/${fileName}"
        def newFileLocation = "${config.newBaseDir}/${identifier}.jpg"
        def image = new File(origFileLocation)

        if(image.canRead()) {
          BufferedImage imageData = ImageIO.read(image)
          log.debug "${image.path} => ${newFileLocation}"
          BufferedImage thumbnail = Scalr.resize(imageData, Scalr.Method.SPEED, Scalr.Mode.FIT_TO_HEIGHT, 200, 200)
          def cropX = thumbnail.getWidth() / 2 as int
          thumbnail = Scalr.pad(thumbnail, 100)
          thumbnail = Scalr.crop(thumbnail,cropX,100,200,200)
          ImageIO.write(thumbnail, 'JPEG', new File(newFileLocation))
          thumbnail.flush()
          images += 1
        } else {
          log.warn "Missing: ${identifier} ${row.ID_IMAGE_FILE_NAME.trim()} ${image.path}"
          errors += 1
        }
      }

      def privateDir = new File(config.privateDir)
      def publicDir = new File(config.newBaseDir)

      // Get the list of files who need to be in the private directory.
      def selectPrivacySet = "SELECT o.usfid FROM names n JOIN oasis o ON n.badge = o.badge WHERE n.privacy != 0" as String
      def privacyDataSQL = Sql.newInstance(config.privacyData.connector, config.privacyData.user, config.privacyData.password, config.privacyData.driver )

      def needPrivacy = []
      privacyDataSQL.eachRow(selectPrivacySet) { row ->
        needPrivacy.push("${row.usfid}.jpg")
      }

      // Get the files that are currently in the private directory.
      def havePrivacy = []
      privateDir.eachFileRecurse (FileType.FILES) { file ->
        havePrivacy.push(file.name)
      }

      // Get the differences.
      def moveToPrivate = needPrivacy - havePrivacy
      def moveToPublic = havePrivacy - needPrivacy

      // Move images into the private directory.
      moveToPrivate.each() { fileName ->
        def file = new File("${config.newBaseDir}/${fileName}")
        if (file.canRead()) {
          log.debug "Moving ${fileName} to private directory"
          file.renameTo(new File(privateDir, file.getName()))
          toPrivate += 1
        }
      }

      // Move images into the public directory.
      moveToPublic.each() { fileName ->
        def file = new File("${privateDir.path}/${fileName}")
        if (file.canRead()) {
          log.debug "Moving ${fileName} to public directory"
          file.renameTo(new File(publicDir, file.getName()))
          fromPrivate += 1
        }
      }

      def now = System.currentTimeMillis()

      log.info "${now-start}ms|images: ${images}|errors: ${errors}|toPrivate: ${toPrivate}|fromPrivate: ${fromPrivate}"

    }catch(Exception e) {
      exitOnError e.message
    }
  }

  private static getCommandLineOptions(String[] args){
    //Parse command-line options
    def cli = new CliBuilder(
            usage:"ImageFetchTool [options]",
            header:"\nAvailable options (use -h for help):\n",
            width:100)
    cli.with {
      h longOpt:'help', 'usage information', required: false
      a longOpt:'all', 'process all card images', required: false
      d longOpt:'date', args:1, argName:'date', 'date (format: yyyyMMdd) to process images for', required: false
      c longOpt:'config', args:1, argName:'configFileName', 'groovy config file', required: false
    }

    def options = cli.parse(args)

    // Display usage if --help is given
    if(options.help) {
      cli.usage()
      System.exit(0)
    }

    return options
  }

  private static getConfigSettings(options){
    def config = new ConfigObject()

    // Directory that contains the originals
    config.origBaseDir = '/Volumes/photos/USF'
    // Directory to store the modified images
    config.newBaseDir = '/Users/epierce/tmp/thumbs'
    // Where to store the images of people with privacy set
    config.privateDir = "${config.newBaseDir}/private"

    /** Defaut configuration values can be set in $HOME/.ImageFetchTool.groovy **/
    def defaultConfigFile = new File(System.getProperty("user.home")+'/.ImageFetchTool.groovy')

    // The default config file is not required, so if it doesn't exist don't throw an exception
    if (defaultConfigFile.exists() && defaultConfigFile.canRead()) {
      config = config.merge(new ConfigSlurper().parse(defaultConfigFile.toURL()))
    }

    //Merge the config file that was passed on the commandline
    if(options.config){
      def newConfigFile = new File(options.config)
      config = config.merge(new ConfigSlurper().parse(newConfigFile.toURL()))
    }

    return config
  }

  private static exitOnError(errorString){
    println("\nERROR: ${errorString}\n")
    System.exit(1)
  }

}