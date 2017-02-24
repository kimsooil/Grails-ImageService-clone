/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.usf.cims.idcard.ImageFetcher

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
	def ImageFetchHandler(idsql,namssql,config,opt) {
    this.idsql = idsql
    this.namssql = namssql
    this.config = config
    this.opt = opt
  }
  def processImages() {
    if(this.opt.all) {
      def getusfidsSQL = "SELECT ID_PERSON AS USFID FROM IDCARD.ID WHERE ID_IMAGE_FILE_NAME IS NOT NULL GROUP BY ID_PERSON"
      this.idsql.eachRow(getusfidsSQL.toString()) { r ->
        processId(r.USFID).each({ k,v ->
          this.summary[k] += v
        })
      }
    } else if(this.opt.usfid) {
      def usfid = this.opt.usfid.value as String
      System.out.println(usfid)
      def getusfidsSQL = "SELECT ID.ID_PERSON AS USFID FROM IDCARD.ID WHERE ID.ID_IMAGE_FILE_NAME IS NOT NULL AND ID.ID_PERSON LIKE ? GROUP BY ID.ID_PERSON"
      this.idsql.eachRow(getusfidsSQL.toString(),[ usfid ]) { r ->
        processId(r.USFID).each({ k,v ->
          this.summary[k] += v
        })
      }
    } else {
      def date = new Date().format('yyyyMMdd') as String
      if (this.opt.date) {
        date = this.opt.date.value as String
      }
      def getusfidsSQL = "SELECT ID_PERSON AS USFID FROM IDCARD.ID WHERE ID_IMAGE_FILE_NAME IS NOT NULL AND ID_ISSUE_DATE > TO_DATE( ? ,'YYYYMMDD') GROUP BY ID_PERSON"
      this.idsql.eachRow(getusfidsSQL.toString(), [ date ]) { r-> 
        processId(r.USFID).each({ k,v ->
          this.summary[k] += v
        })
      }
    }

  }
  def processId(id) {
    
  }
}

