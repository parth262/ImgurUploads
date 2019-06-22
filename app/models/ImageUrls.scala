package models

import java.util.Base64
import java.util.UUID.nameUUIDFromBytes

import play.api.Logger
import play.api.libs.ws._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class ImageUrls(ws: WSClient, imgurConfig: Map[String, String])(implicit val ec: ExecutionContext) {
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
    request.get().onComplete {
      case Success(response) =>
        logger.info("Image successfully downloaded")
        logger.info("Uploading image to imgur")
        response.status match {
          case 200 => uploadImageToImgur(response.bodyAsBytes.toArray)
          case status => logger.error(s"Error fetching image from url $url with status $status")
        }
      case Failure(_) => logger.error(s"Error fetching image from $url")
    }
  }

  private def uploadImageToImgur(imageBytes: Array[Byte]): Unit = {
    val futureResponse = ws.url(imgurConfig("uploadUrl"))
      .addHttpHeaders("Authorization" -> s"Client-ID ${imgurConfig("id")}",
        "Content-Type" -> "application/x-www-form-urlencoded")
      .post(Map("image" -> new String(Base64.getEncoder.encode(imageBytes))))
    futureResponse.onComplete {
      case Success(response) =>
        response.status match {
          case 200 => logger.debug(response.json.toString())
          case status => logger.error(s"Error uploading to imgur with status $status")
        }
      case Failure(error) => logger.error(s"Error => $error")
    }
  }

}
