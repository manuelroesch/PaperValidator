package helper.pdfpreprocessing.util

import java.io.File

import play.api.Logger

/**
  * Created by pdeboer on 16/10/15.
  */
object FileUtils {
	def emptyDir(dir: File): Boolean = {
		dir.listFiles().par.foreach(file => {
			if (file.isDirectory) {
				emptyDir(file)
			}
			file.delete()
		})
		true
	}

	def copyFileIntoDirectory(source: File, destination: String, filename: Option[String] = None, createFolders: Boolean = true): File = {
		if (createFolders) new File(destination).mkdirs()
		val destinationFile = new File(destination + filename.getOrElse(source.getName))

		try {
			org.codehaus.plexus.util.FileUtils.copyFile(source, destinationFile)
			destinationFile
		} catch {
			case e: Exception => {
				Logger.error(s"Cannot copy file $source to $destinationFile", e)
				null
			}
		}
	}

	def filenameWithoutExtension(file: File) = {
		val filename: String = file.getName
		val lastDotIndex = filename.lastIndexOf(".")

		if (filename.length - lastDotIndex > 4)
			filename // doesnt have an extension
		else filename.substring(0, lastDotIndex)
	}
}
