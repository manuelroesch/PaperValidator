package helper

import java.awt.Color
import java.io.{BufferedOutputStream, ByteArrayOutputStream, FileOutputStream}

import helper.pdfpreprocessing.PreprocessPDF
import helper.pdfpreprocessing.pdf.{PDFTextExtractor, TextHighlight}
import models._
import org.apache.pdfbox.pdmodel.PDDocument
import play.api.Configuration

import scala.collection.mutable.ListBuffer
import scala.io.Source
import scala.util.matching.Regex

/**
  * Created by manuel on 20.07.2016.
  */
object PaperAnnotator {

  def annotatePaper(configuration: Configuration,answerService: AnswerService, papersService: PapersService,
                    conferenceSettingsService: ConferenceSettingsService, paperResultService: PaperResultService,
                    paperMethodService: PaperMethodService, paper: Papers, glossaryWithIDMode: Boolean) = {
    val fileBasePath = configuration.getString("highlighter.pdfSourceDir").get+"/"+Commons.getSecretHash(paper.secret)+"/"
    val paperPDF = new java.io.File(fileBasePath + paper.name)

    var m2aResult : List[PaperResult] = List()
    m2aResult = M2AResultHelper.addMethodsAndAssumptions(paper.id.get,m2aResult,papersService,answerService,conferenceSettingsService)
    val methods = paperMethodService.findByPaperId(paper.id.get)
    val answers = answerService.findByPaperId(paper.id.get,true)
    val results = paperResultService.findByPaperId(paper.id.get)
    PaperProcessingManager.writePaperLog("Creating Annotation PDF\n",paper.secret)
    try {
      val annotationList: ListBuffer[String] = ListBuffer()
      val pdDoc: PDDocument = PDDocument.load(paperPDF)

      val textHighlighter = new TextHighlight("UTF-8")
      textHighlighter.initialize(pdDoc)
      if(!glossaryWithIDMode) {
        answers.foreach(a => {
          if(a.isCheckedBefore>=0.5 && a.isRelated>0.5) {
            /*val methodPos = a.method.substring(a.method.lastIndexOf("_") + 1).split(":")
            val methodSize = a.method.substring(0, a.method.lastIndexOf("_")).length
            if (!annotationList.contains(methodPos(0) + ":" + methodPos(1))) {
              textHighlighter.highlight(methodPos(1).toInt, methodPos(1).toInt + methodSize, Color.yellow,
                methodPos(0).toInt, 5, true, false)
              annotationList += methodPos(0) + ":" + methodPos(1)
            }*/
            val assumptionParsed = a.assumption.split("/")
            val assumptionPos = assumptionParsed(2).split(":")
            val assumptionSize = assumptionParsed(1).length
            if (!annotationList.contains(assumptionPos(0) + ":" + assumptionPos(1))) {
              textHighlighter.highlight(assumptionPos(1).toInt, assumptionPos(1).toInt + assumptionSize, Color.yellow,
                assumptionPos(0).toInt, 10, true, false,"",false)
              annotationList += assumptionPos(0) + ":" + assumptionPos(1)
            }
          }
        })
        m2aResult.sortBy(m2a => {m2a.symbol}).reverse.foreach(m2a => {
         if(m2a.symbol == PaperResult.SYMBOL_ERROR) {
           m2aFlag2Annotation(m2a, annotationList, methods, textHighlighter, Color.red)
         } else if (m2a.symbol == PaperResult.SYMBOL_WARNING) {
           m2aFlag2Annotation(m2a, annotationList, methods, textHighlighter, Color.yellow)
         } else {
           m2aFlag2Annotation(m2a, annotationList, methods, textHighlighter, Color.green)
         }
        })
        val colorList = List(new Color(22, 160, 133), new Color(39, 174, 96), new Color(41, 128, 185),
          new Color(142, 68, 173), new Color(44, 62, 80), new Color(243, 156, 18), new Color(211, 84, 0),
          new Color(192, 57, 43), new Color(189, 195, 199), new Color(127, 140, 141))
        results.foreach(r => {
          if (r.position != "") {
            r.position.split(",").foreach(p => {
              val position = p.split(":|-")
              if (position.length == 3) {
                if (!annotationList.contains(position(0) + ":" + position(1))) {
                  textHighlighter.highlight(position(1).toInt, position(2).toInt, colorList(r.resultType % 1000 / 10),
                    position(0).toInt, 2, true, false,"",false)
                  annotationList += position(0) + ":" + position(1)
                }
              }
            })
          }
        })
      }
      addGlossaryAnnotation(paper, textHighlighter, glossaryWithIDMode)
      val byteArrayOutputStream = new ByteArrayOutputStream()
      if (pdDoc != null) {
        pdDoc.save(byteArrayOutputStream)
        pdDoc.close()
      }
      val outFile = new java.io.File(fileBasePath + "annotated-" + paper.name.replace("/",""))
      Some(new BufferedOutputStream(new FileOutputStream(outFile))).foreach(o => {
        o.write(byteArrayOutputStream.toByteArray)
        o.close()
      })
      PaperProcessingManager.writePaperLog("Annotation PDF ready!\n",paper.secret)
    } catch {
      case e: Throwable => {
        PaperProcessingManager.writePaperLog(e.toString,paper.secret)
        println("couldn't highlight pdf", e)
      }
    }
  }

  def m2aFlag2Annotation(m2a: PaperResult, annotationList:ListBuffer[String], methods:List[PaperMethod], textHighlighter: TextHighlight, color: Color) = {
    methods.foreach(m => {
      if(m.method == m2a.descr.replace(" ","").split("<")(0)) {
        if (!annotationList.contains(m.pos)) {
          textHighlighter.highlight(m.pos.split(":")(1).toInt, m.pos.split(":")(1).toInt + m.method.length, color,
            m.pos.split(":")(0).toInt, 10, true, false, m2a.descr.replaceAll("<.*>","->"),false)
          annotationList += m.pos
        } else {
          textHighlighter.highlight(m.pos.split(":")(1).toInt, m.pos.split(":")(1).toInt + m.method.length, color,
            m.pos.split(":")(0).toInt, 10, true, false, m2a.descr.replaceAll("<.*>","->"),true)
        }
      }
    })
  }

  val glossary = Source.fromFile("statterms/glossary.txt", "UTF-8").getLines().toList
  val REGEX_GLOSSARY = new Regex(glossary.mkString("[^a-z]|[^a-z]").replace(" ", "\\s+").replace("'", "."))
  def addGlossaryAnnotation(paper:Papers, textHighlighter: TextHighlight, glossaryWithIdMode: Boolean) = {
    val paperLink = PreprocessPDF.INPUT_DIR + "/" + Commons.getSecretHash(paper.secret) + "/" + paper.name
    val textList = new PDFTextExtractor(paperLink).pages.map(_.toLowerCase())
    textList.zipWithIndex.foreach{case(text,page) => {
      REGEX_GLOSSARY.findAllMatchIn(text).foreach(m => {
        textHighlighter.highlight(m.start(0), m.end(0), Color.black,page, 1, true, glossaryWithIdMode,"",false)
      })
    }}
  }

}
