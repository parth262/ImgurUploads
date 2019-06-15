package models

import java.util.Base64
import java.util.UUID.nameUUIDFromBytes

import play.api.Logger
import play.api.libs.ws._

import scala.concurrent.ExecutionContext

class ImageUrls(ws: WSClient, implicit val ec: ExecutionContext, imgurConfig: Map[String, String]) {
  private val logger = Logger.logger

  def processUrls(urls: Seq[String]): String = {
    val jobId = nameUUIDFromBytes(urls.mkString(";").getBytes).toString
    urls foreach processImage
    /*for(url <- urls) {
      processImage(url)
    }*/
    jobId
  }

  private def processImage(url: String): Unit = {
    logger.info("Downloading image in memory")
    val request = ws.url(url)
    request.get().map(response => {
      logger.info("Image successfully downloaded")
      logger.info("Uploading image to imgur")
      response.status match {
        case 200 => uploadImageToImgur(response.bodyAsBytes.toArray)
        case status => logger.error(s"Error fetching image from url $url with status $status")
      }
    }).recover {
      case _: Exception => logger.error(s"Error fetching image from $url")
    }
  }

  private def uploadImageToImgur(imageBytes: Array[Byte]): Unit = {
    val futureResponse = ws.url(imgurConfig("uploadUrl"))
      .addHttpHeaders("Authorization" -> s"Client-ID ${imgurConfig("id")}",
        "Content-Type" -> "application/x-www-form-urlencoded")
      .post(Map("image" -> new String(Base64.getEncoder.encode(imageBytes))))
    futureResponse.map(response => {
      response.status match {
        case 200 => logger.debug(response.json.toString())
        case status => logger.error(s"Error uploading to imgur with status $status")
      }
    }).recover {
      case e: Exception => logger.error(s"Error => $e")
    }
  }

}
