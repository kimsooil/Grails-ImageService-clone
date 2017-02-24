/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.usf.cims.idcard.ImageFetcher

import groovy.io.FileType
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import org.imgscalr.Scalr
import org.imgscalr.Scalr.*

/**
 *
 * @author james
 */
class ImageFetchHandler {
  def idsql
  def namssql
  def config
  def opt
  def summary = [
    toPrivate: 0,
    toPublic: 0,
    toInactive: 0,
    images: 0
  ]
  def fileList = []
  
	def ImageFetchHandler(idsql,namssql,config,opt) {
    this.idsql = idsql
    this.namssql = namssql
    this.config = config
    this.opt = opt
    System.out.println("Build File List")
    this.buildFileList()
    System.out.println("End Build File List")
  }
  def processImages() {
    def start = System.currentTimeMillis()
    if(this.opt.all) {
      def getusfidsSQL = "SELECT ID_PERSON AS USFID FROM IDCARD.ID WHERE ID_IMAGE_FILE_NAME IS NOT NULL GROUP BY ID_PERSON"
      this.idsql.eachRow(getusfidsSQL.toString()) { r ->
        this.processId(r.USFID)
      }
    } else if(this.opt.usfid) {
      def usfid = this.opt.usfid.value as String
      System.out.println(usfid)
      def getusfidsSQL = "SELECT ID.ID_PERSON AS USFID FROM IDCARD.ID WHERE ID.ID_IMAGE_FILE_NAME IS NOT NULL AND ID.ID_PERSON LIKE ? GROUP BY ID.ID_PERSON"
      this.idsql.eachRow(getusfidsSQL.toString(),[ usfid ]) { r ->
        this.processId(r.USFID)
      }
    } else {
      def date = new Date().format('yyyyMMdd') as String
      if (this.opt.date) {
        date = this.opt.date.value as String
      }
      def getusfidsSQL = "SELECT ID_PERSON AS USFID FROM IDCARD.ID WHERE ID_IMAGE_FILE_NAME IS NOT NULL AND ID_ISSUE_DATE > TO_DATE( ? ,'YYYYMMDD') GROUP BY ID_PERSON"
      this.idsql.eachRow(getusfidsSQL.toString(), [ date ]) { r-> 
        this.processId(r.USFID)
      }
    }
    def now = System.currentTimeMillis()
    log.info "${now-start}ms|images: ${this.summary.images}|toPublic: ${this.summary.toPublic}|toPrivate: ${this.summary.toPrivate}|toInactive: ${this.summary.toInactive}"
  }
  def processId(id) {
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
    def oldimages = []
    if(this.idsql.firstRow(activeCardCheckSQL.toString(),[usfid:id]).FOUND) {
      def ac = this.idsql.firstRow(activeCardSQL.toString(),[usfid:id])
      if(this.namssql.firstRow(privacyCheckSQL.toString(),[usfid:id]).found) {
        oldimages.plus([new File("${this.config.inactiveDir}/${id}.jpg"),new File("${this.config.newBaseDir}/${id}.jpg")])
        oldimages.each{ i -> 
          if(i.canRead()) {
            boolean fileMoved = i.renameTo(new File(new File(this.config.privateDir), i.getName()))
          }                  
        }
        oldimages.clear()
        if(this.transferimage(ac.ID_IMAGE_FILE_NAME,"${this.config.privateDir}/${id}.jpg")) {
          this.summary.images++
          this.summary.toPrivate++
        }
      } else {
        oldimages.plus([new File("${this.config.inactiveDir}/${id}.jpg"),new File("${this.config.privateDir}/${id}.jpg")])
        oldimages.each{ i -> 
          if(i.canRead()) {
            boolean fileMoved = i.renameTo(new File(new File(this.config.newBaseDir), i.getName()))
          }                  
        }
        oldimages.clear()
        if(this.transferimage(ac.ID_IMAGE_FILE_NAME,"${this.config.newBaseDir}/${id}.jpg")) {
          this.summary.images++
          this.summary.toPublic++
        }
      }                             
    } else {
      // Move any images from private or public to inactive
      oldimages.plus([new File("${this.config.newBaseDir}/${id}.jpg"),new File("${this.config.privateDir}/${id}.jpg")])
      oldimages.each{ i ->
        if(i.canRead()) {
          boolean fileMoved = i.renameTo(new File(new File(this.config.inactiveDir), i.getName()))
        }
      }
      oldimages.clear()
      boolean found = false
      this.idsql.eachRow(inactiveCardListSQL.toString(),[usfid:id]) { ia ->
        if(!found) {
          found = this.transferimage(ia.ID_IMAGE_FILE_NAME,"${this.config.inactiveDir}/${id}.jpg")
          if(found) { 
            this.summary.images++ 
            this.summary.toInactive++
          }
        }
      }
    }
  }
  def transferimage(srcPath,destPath) {
    def patharr = srcPath.trim().tokenize('\\')
    def fileName = patharr.pop()
    while(patharr.size() > 0) {
      def i = this.fileList.find { it.path =~ /${patharr.join('/')+'/'+fileName}/ }
      if(i.canRead()) {
        BufferedImage imageData = ImageIO.read(i)

        log.debug "${i.absolutePath} => ${destPath}"
        BufferedImage thumbnail = Scalr.resize(imageData, Scalr.Method.SPEED, Scalr.Mode.FIT_TO_HEIGHT, 200, 200)
        def cropX = thumbnail.getWidth() / 2 as int

        // Add a white matte around the image so that we can crop it square and not lose any of the image
        thumbnail = Scalr.pad(thumbnail, 100, java.awt.Color.WHITE)
        thumbnail = Scalr.crop(thumbnail,cropX,100,200,200)
        ImageIO.write(thumbnail, 'JPEG', new File(destPath))
        thumbnail.flush()
        log.debug "Transferring ${i.path} => ${destPath}"
        System.out.println("Transferring ${i.path} => ${destPath}")
        return true;        
      } else {
        System.out.println("Cannot locate image at ${i.path}")
        patharr.remove(0)
      }
    }
    return false
  }
  def buildFileList() {
    this.fileList = []

    def dir = new File(this.config.origBaseDir)
    dir.eachFileRecurse (FileType.FILES) { file ->
      this.fileList << file
    }     
  }
}

