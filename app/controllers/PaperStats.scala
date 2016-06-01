package controllers

import models._
import scala.collection.mutable.ListBuffer
import util.control.Breaks._

/**
  * Created by manuel on 31.05.2016.
  */
object PaperStats {

  def getStats(papers: List[Papers], papersService: PapersService, paperResultService: PaperResultService,
                 answerService: AnswerService, conferenceSettingsService: ConferenceSettingsService): List[PapersWithStats] = {
    papers.map(p => {
      var statsTotal : Map[Int,Int] = Map()
      var statDetails : Map[String,Int] = Map()
      var results = paperResultService.findByPaperId(p.id.get)
      results = addMethodsAndAssumptionsResults(p.id.get,results, papersService, answerService, conferenceSettingsService)
      results.foreach(r => {
        statsTotal += (r.symbol -> (statsTotal.getOrElse(r.symbol,0)+1))
        statDetails += (r.resultType+"-"+r.symbol -> (statDetails.getOrElse(r.resultType+"-"+r.symbol,0)+1))
      })
      new PapersWithStats(p.id,p.name,p.status,p.permutations,p.secret, statsTotal, statDetails)
    })
  }

  def addMethodsAndAssumptionsResults(id:Int,results: List[PaperResult], papersService: PapersService, answerService: AnswerService,
                               conferenceSettingsService: ConferenceSettingsService) : List[PaperResult] = {
    var allResults = results
    val paper = papersService.findById(id).get
    val m2aList = answerService.findByPaperId(paper.id.get)
    val conferenceSettings = conferenceSettingsService.findAllByPaperId(paper.id.get,paper.conferenceId).to[ListBuffer]
    m2aList.foreach(m2a => {
      breakable {
        conferenceSettings.zipWithIndex.foreach{case (confSetting,i) => {
          if(confSetting.flag.get != ConferenceSettings.FLAG_IGNORE) {
            if(m2a.method.toLowerCase() == confSetting.methodName.toLowerCase() &&
              m2a.assumption.toLowerCase() == confSetting.assumptionName.toLowerCase()) {
              var symbol = PaperResult.SYMBOL_ERROR
              if(m2a.isRelated > 0.5 && m2a.isCheckedBefore > 0.5) {
                symbol = PaperResult.SYMBOL_OK
              } else if(m2a.isRelated > 0.5) {
                symbol = PaperResult.SYMBOL_WARNING
              } else {
              }
              allResults = allResults:+ new PaperResult(Some(1L),id,PaperResult.TYPE_M2A,"","",symbol)
              conferenceSettings.remove(i)
              break
            }
          }
        }}
      }
    })
    conferenceSettings.foreach(confSetting => {
      if(confSetting.flag.get != ConferenceSettings.FLAG_IGNORE){
        var symbol = PaperResult.SYMBOL_ERROR
        if(confSetting.flag.get==ConferenceSettings.FLAG_EXPECT) {
          symbol = PaperResult.SYMBOL_WARNING
        }
        allResults = allResults:+ new PaperResult(Some(1L),id,PaperResult.TYPE_M2A,"","",symbol)
      }
    })
    allResults
  }

}
