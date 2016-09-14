package helper

import models._

import scala.collection.mutable.ListBuffer
import scala.util.control.Breaks._

/**
  * Created by manuel on 23.08.2016.
  */
object M2AResultHelper {

  def addMethodsAndAssumptions(id:Int, results: List[PaperResult], papersService: PapersService, answerService: AnswerService,
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
              val m2aDescr = m2a.method+" <span class='glyphicon glyphicon-arrow-right'></span> "+m2a.assumption
              var m2aResult = "Related: <b>"+ (m2a.isRelated > 0.5) + "</b>, " +
                "Checked before: <b>" + (m2a.isCheckedBefore > 0.5) + "</b>"
              var symbol = PaperResult.SYMBOL_ERROR
              if(m2a.isRelated > 0.5 && m2a.isCheckedBefore > 0.5) {
                symbol = PaperResult.SYMBOL_OK
              } else if(m2a.isRelated > 0.5) {
                symbol = PaperResult.SYMBOL_WARNING
                m2aResult += getM2AFlagText(confSetting)
              } else {
                m2aResult += getM2AFlagText(confSetting)
              }
              allResults = allResults :+ new PaperResult(Some(1L),id,PaperResult.TYPE_M2A,m2aDescr,m2aResult,symbol,"")
              conferenceSettings.remove(i)
              break
            }
          }
        }}
      }
    })
    conferenceSettings.foreach(confSetting => {
      if(confSetting.flag.get != ConferenceSettings.FLAG_IGNORE){
        val m2aDescr = confSetting.methodName+" <span class='glyphicon glyphicon-arrow-right'></span> "+
          confSetting.assumptionName
        var m2aResult = "Not Found in Paper"
        m2aResult += getM2AFlagText(confSetting)
        var symbol = PaperResult.SYMBOL_ERROR
        if(confSetting.flag.get==ConferenceSettings.FLAG_EXPECT) {
          symbol = PaperResult.SYMBOL_WARNING
        }
        allResults = allResults :+ new PaperResult(Some(1L),id,PaperResult.TYPE_M2A,m2aDescr,m2aResult,symbol,"")
      }
    })
    allResults
  }


  def getM2AFlagText(confSetting: ConferenceSettings): String = {
    confSetting.flag.get match {
      case ConferenceSettings.FLAG_REQUIRE =>
        "<span class='m2aFlag text-danger glyphicon glyphicon-flag'><span>"
      case ConferenceSettings.FLAG_EXPECT =>
        "<span class='m2aFlag text-warning glyphicon glyphicon-flag'><span>"
      case ConferenceSettings.FLAG_IGNORE =>
        "<span class='m2aFlag text-muted glyphicon glyphicon-flag'><span>"
    }
  }

}
