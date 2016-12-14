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

/**
* ImageFetchTool
**/
@Slf4j
class ImageFetchTool {

  public static String PATTERN = "%d{ABSOLUTE} %-5p [%c{1}] %m%n"

  public static void main(String[] args) {
    def start = System.currentTimeMillis()
    def opt = getCommandLineOptions(args)
    def config = getConfigSettings(opt)
    
    def images = 0
    def toPrivate = 0
    def toPublic = 0
    def toInactive = 0

    try {
      Sql.withInstance(config.privacyData.connector, config.privacyData.user, config.privacyData.password, config.privacyData.driver ) { namssql ->
        def privacyCheckSQL = "SELECT COUNT(*) as `found` FROM names n JOIN oasis o ON n.badge = o.badge AND o.usfid=:usfid WHERE n.privacy != 0 LIMIT 1"
        try {
          Sql.withInstance( config.cardData.connector, config.cardData.user, config.cardData.password, config.cardData.driver ) { idsql ->
            def activeCardCheckSQL = "SELECT COUNT(*) AS FOUND FROM IDCARD.ID WHERE ID_IMAGE_FILE_NAME IS NOT NULL AND ID_ACTIVE_CODE='A' AND ID_PERSON LIKE :usfid AND ROWNUM = 1"
            def inactiveCardListSQL = "SELECT ID_PERSON, ID_IMAGE_FILE_NAME, ID_ISSUE_DATE FROM IDCARD.ID WHERE ID_IMAGE_FILE_NAME IS NOT NULL AND ID_PERSON LIKE :usfid ORDER BY ID_ISSUE_DATE DESC"
            def activeCardSQL = "SELECT ID_PERSON, ID_IMAGE_FILE_NAME FROM IDCARD.ID WHERE ID_IMAGE_FILE_NAME IS NOT NULL AND ID_ACTIVE_CODE='A' AND ID_PERSON LIKE :usfid AND ROWNUM = 1 ORDER BY ID_ISSUE_DATE DESC"            
            // Get a list of all persons who appear to have a picture
            idsql.eachRow({ o ->
              if (o.all) {
                return "SELECT ID_PERSON AS USFID FROM IDCARD.ID WHERE ID_IMAGE_FILE_NAME IS NOT NULL GROUP BY ID_PERSON" as String
              } else {
                def date = new Date().format('yyyyMMdd') as String
                if (o.date) {
                  date = opt.date
                }
                return "SELECT ID_PERSON AS USFID FROM IDCARD.ID WHERE ID_IMAGE_FILE_NAME IS NOT NULL AND ID_ISSUE_DATE > TO_DATE('${date}','YYYYMMDD') GROUP BY ID_PERSON" as String
              }
              return 
            }.call(opt)) { urow ->
                // Check to see if the user is active 
              def oldimages = []
              def transferimage = { ipath,newFileLocation ->
                def patharr = ipath.trim().tokenize('\\')
                def fileName = patharr.pop()
                while(patharr.size() > 0) {
                  def i = new File(new File([config.origBaseDir,patharr.join('/')].join('/')),fileName)
                  println "Trying to read ${i.absolutePath} => ${newFileLocation}"
                  if(i.canRead()) {
                    BufferedImage imageData = ImageIO.read(i)
                    
                    log.debug "${i.absolutePath} => ${newFileLocation}"
                    BufferedImage thumbnail = Scalr.resize(imageData, Scalr.Method.SPEED, Scalr.Mode.FIT_TO_HEIGHT, 200, 200)
                    def cropX = thumbnail.getWidth() / 2 as int
                    
                    // Add a white matte around the image so that we can crop it square and not lose any of the image
                    thumbnail = Scalr.pad(thumbnail, 100, java.awt.Color.WHITE)
                    thumbnail = Scalr.crop(thumbnail,cropX,100,200,200)
                    ImageIO.write(thumbnail, 'JPEG', new File(newFileLocation))
                    thumbnail.flush()
                    log.debug "Transferring ${i.path} to ${newFileLocation}"
                    return true;
                  } else {
                    patharr.remove(0)
                  }
                }
                return false
              }
              if(idsql.firstRow(activeCardCheckSQL.toString(),[usfid:urow.USFID]).FOUND) {
                def ac = idsql.firstRow(activeCardSQL.toString(),[usfid:urow.USFID])
                if(namssql.firstRow(privacyCheckSQL.toString(),[usfid:urow.USFID]).found) {
                  oldimages.plus([new File("${config.inactiveDir}/${urow.USFID}.jpg"),new File("${config.newBaseDir}/${urow.USFID}.jpg")])
                  oldimages.each{ i -> 
                    if(i.canRead()) {
                      boolean fileMoved = i.renameTo(new File(new File(config.privateDir), i.getName()))
                    }                  
                  }
                  oldimages.clear()
                  if(transferimage.call(ac.ID_IMAGE_FILE_NAME,"${config.privateDir}/${urow.USFID}.jpg")) {
                    images++
                    toPrivate++
                  }
                } else {
                  oldimages.plus([new File("${config.inactiveDir}/${urow.USFID}.jpg"),new File("${config.privateDir}/${urow.USFID}.jpg")])
                  oldimages.each{ i -> 
                    if(i.canRead()) {
                      boolean fileMoved = i.renameTo(new File(new File(config.newBaseDir), i.getName()))
                    }                  
                  }
                  oldimages.clear()
                  if(transferimage.call(ac.ID_IMAGE_FILE_NAME,"${config.newBaseDir}/${urow.USFID}.jpg")) {
                    images++
                    toPublic++
                  }
                }                             
              } else {
                // Move any images from private or public to inactive
                oldimages.plus([new File("${config.newBaseDir}/${urow.USFID}.jpg"),new File("${config.privateDir}/${urow.USFID}.jpg")])
                oldimages.each{ i ->
                  if(i.canRead()) {
                    boolean fileMoved = i.renameTo(new File(new File(config.inactiveDir), i.getName()))
                  }
                }
                oldimages.clear()
                boolean found = false
                idsql.eachRow(inactiveCardListSQL.toString(),[usfid:urow.USFID]) { ia ->
                  if(!found) {
                    found = transferimage.call(ia.ID_IMAGE_FILE_NAME,"${config.inactiveDir}/${urow.USFID}.jpg")
                    if(found) { 
                      images++ 
                      toInactive++
                    }
                  }
                }
              }
            }
          }
          def now = System.currentTimeMillis()
          log.info "${now-start}ms|images: ${images}|toPublic: ${toPublic}|toPrivate: ${toPrivate}|toInactive: ${toInactive}"
        } catch(Exception e) {
          exitOnError e.message
        }
      }
    } catch(Exception e) {
      exitOnError e.message
    }
    
  }
  public static void main_old(String[] args) {

    def start = System.currentTimeMillis()

    def opt = getCommandLineOptions(args)
    def config = getConfigSettings(opt)

    try {
            
      def cardDataSQL = Sql.newInstance( config.cardData.connector, config.cardData.user, config.cardData.password, config.cardData.driver )

      // Use today's date unless the --date switch was used
      def date = new Date().format('yyyyMMdd') as String
      if (opt.date) date = opt.date

      // SQL queries for the IDCD schema on diamond
      // def selectStatementAllActive = "SELECT ID_PERSON, ID_IMAGE_FILE_NAME FROM idcard.id WHERE id.ID_ACTIVE_CODE='A' AND ID_PERSON LIKE 'U%' AND ID_IMAGE_FILE_NAME IS NOT NULL" as String
      // def selectStatementSingleDay = "SELECT ID_PERSON, ID_IMAGE_FILE_NAME FROM idcard.id WHERE id.ID_ACTIVE_CODE='A' AND ID_PERSON LIKE 'U%' AND ID_IMAGE_FILE_NAME LIKE '${date}%'" as String

      def selectStatementAllActive = "SELECT ID_PERSON, ID_IMAGE_FILE_NAME FROM IDCARD.ID WHERE ID_IMAGE_FILE_NAME IS NOT NULL" as String
      def selectStatementSingleDay = "SELECT ID_PERSON, ID_IMAGE_FILE_NAME FROM IDCARD.ID WHERE ID_IMAGE_FILE_NAME LIKE '${date}%'" as String
      // id.ID_ACTIVE_CODE='A' AND ID_PERSON LIKE 'U%'
      // Just select the cards created on a specific day unless the --all switch was used
      def activeStatement = selectStatementSingleDay
      if (opt.all) activeStatement = selectStatementAllActive

      def images = 0
      def errors = 0
      def toPrivate = 0
      def fromPrivate = 0

      cardDataSQL.eachRow(activeStatement) { row ->
        def identifier = row.ID_PERSON
//        def path = row.ID_IMAGE_FILE_NAME.trim().minus('P:\\USF').tokenize('\\')
//        def fileName = path.pop()
//        def directory = path.pop()
//
//        def origFileLocation = "${config.origBaseDir}/${directory}/${fileName}"
//        def newFileLocation = row.ID_ACTIVE_CODE.equalsIgnoreCase( 'A' ) ? "${config.newBaseDir}/${identifier}.jpg" : "${config.inactiveDir}/${identifier}.jpg"
//        def image = new File(origFileLocation)
//
//        // Create a 200x200 copy of the image
//        if(image.canRead()) {
//          BufferedImage imageData = ImageIO.read(image)
//          log.debug "${image.path} => ${newFileLocation}"
//          BufferedImage thumbnail = Scalr.resize(imageData, Scalr.Method.SPEED, Scalr.Mode.FIT_TO_HEIGHT, 200, 200)
//          def cropX = thumbnail.getWidth() / 2 as int
//
//          // Add a white matte around the image so that we can crop it square and not lose any of the image
//          thumbnail = Scalr.pad(thumbnail, 100, java.awt.Color.WHITE)
//          thumbnail = Scalr.crop(thumbnail,cropX,100,200,200)
//          ImageIO.write(thumbnail, 'JPEG', new File(newFileLocation))
//          thumbnail.flush()
//          images += 1
//        } else {
//          log.warn "Missing: ${identifier} ${row.ID_IMAGE_FILE_NAME.trim()} ${image.path}"
//          errors += 1
//        }
      }

//      def privateDir = new File(config.privateDir)
//      def publicDir = new File(config.newBaseDir)
//      def inactiveDir = new File(config.inactiveDir)
//
//      // Get the list of files that should be in the private directory using the NAMS database.
//      def selectPrivacySet = "SELECT o.usfid FROM names n JOIN oasis o ON n.badge = o.badge WHERE n.privacy != 0" as String
//      def privacyDataSQL = Sql.newInstance(config.privacyData.connector, config.privacyData.user, config.privacyData.password, config.privacyData.driver )
//
//      def needPrivacy = []
//      privacyDataSQL.eachRow(selectPrivacySet) { row ->
//        needPrivacy.push("${row.usfid}.jpg")
//      }
//
//      // Get the files that are currently in the private directory.
//      def havePrivacy = []
//      privateDir.eachFileRecurse (FileType.FILES) { file ->
//        havePrivacy.push(file.name)
//      }
//
//      // Get the differences.
//      def moveToPrivate = needPrivacy - havePrivacy
//      def moveToPublic = havePrivacy - needPrivacy
//
//      // Move images into the private directory.
//      moveToPrivate.each() { fileName ->
//        def file = new File("${config.newBaseDir}/${fileName}")
//        if (file.canRead()) {
//          log.debug "Moving ${fileName} to private directory"
//          file.renameTo(new File(privateDir, file.getName()))
//          toPrivate += 1
//        }
//      }
//
//      // Move images into the public directory.
//      moveToPublic.each() { fileName ->
//        def file = new File("${privateDir.path}/${fileName}")
//        if (file.canRead()) {
//          log.debug "Moving ${fileName} to public directory"
//          file.renameTo(new File(publicDir, file.getName()))
//          fromPrivate += 1
//        }
//      }
//
//      def now = System.currentTimeMillis()
//
//      log.info "${now-start}ms|images: ${images}|errors: ${errors}|toPrivate: ${toPrivate}|fromPrivate: ${fromPrivate}"
//
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
      _ longOpt:'all', 'process all card images', required: false
      _ longOpt:'date', args:1, argName:'date', "date (format: yyyyMMdd) to process images for.  Default: ${new Date().format('yyyyMMdd')}", required: false
      _ longOpt:'config', args:1, argName:'configFileName', 'groovy config file **REQUIRED**', required: true
    }

    def options = cli.parse(args)

    if(! options) System.exit(1)

    if(options.help) {
      cli.usage()
      System.exit(0)
    }

    return options
  }

  private static getConfigSettings(options){
    def config = new ConfigObject()

    // Directory that contains the originals
    config.origBaseDir = ''
    // Directory to store the modified images
    config.newBaseDir = ''
    // Where to store the images of people with privacy set
    config.privateDir = "${config.newBaseDir}/private"

    // Merge the config file that was passed on the commandline
    def newConfigFile = new File(options.config)
    config.merge(new ConfigSlurper().parse(newConfigFile.toURL()))
  }

  private static exitOnError(errorString){
    println("\nERROR: ${errorString}\n")
    System.exit(1)
  }

}
