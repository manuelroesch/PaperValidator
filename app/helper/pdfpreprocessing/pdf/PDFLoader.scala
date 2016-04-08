package helper.pdfpreprocessing.pdf

import java.io.File

import helper.pdfpreprocessing.entities.{Paper, Journal}
import helper.pdfpreprocessing.util.FileUtils

/**
 * Created by pdeboer on 16/10/15.
 */
class PDFLoader(path: File) {
	def papers = {
		path.listFiles().par.flatMap(journalDir => {
			val resultForJournalFolder = Option(journalDir.listFiles()).getOrElse(Array.empty[File]).par.map(paperFile => {
				extractPaper(journalFromFolder(journalDir), paperFile)
			}).filter(_.isDefined)

			if (resultForJournalFolder.nonEmpty) resultForJournalFolder else List(extractPaper(journalFromFolder(path), journalDir))
		}).filter(_.isDefined).map(_.get).toArray
	}

	private def journalFromFolder(journalDir: File): Journal = {
		val journal = Journal(journalDir.getName, journalDir)
		journal
	}

	private def extractPaper(journal: Journal, paperFile: File): Option[Paper] = {
		if (paperFile.getName.endsWith(".pdf"))
			Some(Paper(FileUtils.filenameWithoutExtension(paperFile), paperFile, journal))
		else None
	}
}
