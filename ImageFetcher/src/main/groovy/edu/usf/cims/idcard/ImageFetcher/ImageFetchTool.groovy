package edu.usf.cims.idcard.ImageFetcher

import groovy.sql.Sql
import groovy.io.FileType
import groovy.util.logging.Slf4j
import groovy.util.CliBuilder
import groovy.util.FileNameFinder
import groovy.json.JsonOutput  
import org.apache.commons.cli.Option
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import org.imgscalr.Scalr
import org.imgscalr.Scalr.*

/**
 * ImageFetchTool
 * 
 * This tool reads from a file share and does a structured copy as used by ImageService.
 * Much of this logic had to be rewritten in 2017 to handle inactive cards and
 * do 'best case' matching against an ID card directory structure that is out of sync with 
 * what's recorded in it's database.
 * 
 * @author Eric Pierce
 * @author James Jones
 * @company University of South Florida
 */
@Slf4j
class ImageFetchTool {

  public static String PATTERN = "%d{ABSOLUTE} %-5p [%c{1}] %m%n"
  /**
   * Handles the list of cards that will be processed by this tool 
   */
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
            def imageFetchHandler = new ImageFetchHandler(idsql,namssql,config,opt)
            imageFetchHandler.processImages()
          }
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
  /**
   * Handles incoming command line options and returns the parsed object
   * 
   * @param  args  An array of argements originating from the options passed on the command line
   * @return The parsed command object from the CliBuilder
   * @see Map
   */
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
  /**
   * Merge the config file that was passed on the commandline with the options 
   * from the command line
   * 
   * @param  options
   * @return A config slurper object containing the merged settings
   * @see ConfigObject
   */
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
  /**
   * An exception catch handler for exit
   * 
   * @param   errorString   A string to be sent to the console on exit
   **/
  private static exitOnError(errorString){
    println("\nERROR: ${errorString}\n")
    System.exit(1)
  }

}
