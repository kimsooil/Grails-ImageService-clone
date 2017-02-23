package edu.usf.cims.idcard.ImageFetcher

import groovy.sql.Sql
import groovy.io.FileType
import groovy.util.logging.Slf4j
import groovy.util.CliBuilder
import groovy.json.JsonOutput  
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
    def summary = [
      toPrivate: 0,
      toPublic: 0,
      toInactive: 0,
      images: 0
    ]
    try {
      Sql.withInstance(config.privacyData.connector, config.privacyData.user, config.privacyData.password, config.privacyData.driver ) { namssql ->
        try {
          Sql.withInstance( config.cardData.connector, config.cardData.user, config.cardData.password, config.cardData.driver ) { idsql ->
            // Get a list of all persons who appear to have a picture
            try {
              if(opt.all) {
                def getusfidsSQL = "SELECT ID_PERSON AS USFID FROM IDCARD.ID WHERE ID_IMAGE_FILE_NAME IS NOT NULL GROUP BY ID_PERSON"
                idsql.eachRow(getusfidsSQL.toString()) { r ->
                  processCard(r,idsql,namssql,config).each({ k,v ->
                    summary[k] += v
                  })
                }
              } else if(opt.usfid) {
                def usfid = opt.usfid.value as String
                System.out.println(usfid)
                def getusfidsSQL = "SELECT ID.ID_PERSON AS USFID FROM IDCARD.ID WHERE ID.ID_IMAGE_FILE_NAME IS NOT NULL AND ID.ID_PERSON LIKE ? GROUP BY ID.ID_PERSON"
                idsql.eachRow(getusfidsSQL.toString(),[ usfid ]) { r ->
                  processCard(r,idsql,namssql,config).each({ k,v ->
                    summary[k] += v
                  })
                }
              } else {
                def date = new Date().format('yyyyMMdd') as String
                if (opt.date) {
                  date = opt.date.value as String
                }
                def getusfidsSQL = "SELECT ID_PERSON AS USFID FROM IDCARD.ID WHERE ID_IMAGE_FILE_NAME IS NOT NULL AND ID_ISSUE_DATE > TO_DATE( ? ,'YYYYMMDD') GROUP BY ID_PERSON"
                idsql.eachRow(getusfidsSQL.toString(), [ date ]) { r-> 
                  processCard(r,idsql,namssql,config).each({ k,v ->
                    summary[k] += v
                  })
                }
              }  
            } catch(Exception e) {
              e.printStackTrace(System.out);
              exitOnError e.message
            }
          }
          def now = System.currentTimeMillis()
          log.info "${now-start}ms|images: ${summary.images}|toPublic: ${summary.toPublic}|toPrivate: ${summary.toPrivate}|toInactive: ${summary.toInactive}"
        } catch(Exception e) {
          exitOnError e.message
          e.printStackTrace(System.out);
        }
      }
    } catch(Exception e) {
      exitOnError e.message
      e.printStackTrace(System.out);
    }    
  }
  private static transferimage(ipath,newFileLocation,config) {
    def patharr = ipath.trim().tokenize('\\')
    def fileName = patharr.pop()
    while(patharr.size() > 0) {
      def i = new File(new File([config.origBaseDir,patharr.join('/')].join('/')),fileName)
      log.debug "Trying to read ${i.absolutePath} => ${newFileLocation}"
      System.out.println("Trying to read ${i.absolutePath} => ${newFileLocation}")
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
        System.out.println("Transferring ${i.path} to ${newFileLocation}")
        return true;
      } else {
        System.out.println("Cannot locate ${i.path}")
        patharr.remove(0)
      }
    }
    return false
  }
  
  private static processCard(urow,idsql,namssql,config) {
    def activeCardCheckSQL = "SELECT COUNT(*) AS FOUND FROM IDCARD.ID WHERE ID_IMAGE_FILE_NAME IS NOT NULL AND ID_ACTIVE_CODE='A' AND ID_PERSON LIKE :usfid AND ROWNUM = 1"
    def inactiveCardListSQL = "SELECT ID_PERSON, ID_IMAGE_FILE_NAME, ID_ISSUE_DATE FROM IDCARD.ID WHERE ID_IMAGE_FILE_NAME IS NOT NULL AND ID_PERSON LIKE :usfid ORDER BY ID_ISSUE_DATE DESC"
    def activeCardSQL = "SELECT ID_PERSON, ID_IMAGE_FILE_NAME FROM IDCARD.ID WHERE ID_IMAGE_FILE_NAME IS NOT NULL AND ID_ACTIVE_CODE='A' AND ID_PERSON LIKE :usfid AND ROWNUM = 1 ORDER BY ID_ISSUE_DATE DESC"            
    def privacyCheckSQL = "SELECT COUNT(*) as `found` FROM names n JOIN oasis o ON n.badge = o.badge AND o.usfid=:usfid WHERE n.privacy != 0 LIMIT 1"
    def summary = [
      toPrivate: 0,
      toPublic: 0,
      toInactive: 0,
      images: 0
    ]
    // System.out.println(JsonOutput.toJson(urow))
    def oldimages = []
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
        if(transferimage(ac.ID_IMAGE_FILE_NAME,"${config.privateDir}/${urow.USFID}.jpg",config)) {
          summary.images++
          summary.toPrivate++
        }
      } else {
        oldimages.plus([new File("${config.inactiveDir}/${urow.USFID}.jpg"),new File("${config.privateDir}/${urow.USFID}.jpg")])
        oldimages.each{ i -> 
          if(i.canRead()) {
            boolean fileMoved = i.renameTo(new File(new File(config.newBaseDir), i.getName()))
          }                  
        }
        oldimages.clear()
        if(transferimage(ac.ID_IMAGE_FILE_NAME,"${config.newBaseDir}/${urow.USFID}.jpg",config)) {
          summary.images++
          summary.toPublic++
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
          found = transferimage(ia.ID_IMAGE_FILE_NAME,"${config.inactiveDir}/${urow.USFID}.jpg",config)
          if(found) { 
            summary.images++ 
            summary.toInactive++
          }
        }
      }
    }
    return summary
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
      _ longOpt:'usfid', args: 1, 'process single card images', required: false
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
