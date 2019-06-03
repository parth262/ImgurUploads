package models

import java.util.Base64
import java.util.UUID.nameUUIDFromBytes
import play.api.Logger
import play.api.libs.ws._

import scala.concurrent.ExecutionContext

class ImageUrls(ws: WSClient, ec: ExecutionContext, imgurConfig: Map[String, String]) {
  val logger = Logger.logger
  def processUrls(urls: Seq[String]): String = {
    val jobId = nameUUIDFromBytes(urls.mkString(";").getBytes).toString
    urls.foreach(url => {
      processImage(url)
    })
    jobId
  }

  private def processImage(url: String): Unit = {
    logger.info("Downloading image in memory")
    val request = ws.url(url)
    val futureResponse = request.get()
    futureResponse.onComplete(response => {
      if (response.isSuccess) {
        logger.info("Image successfully downloaded")
        logger.info("Uploading image to imgur")
        uploadImageToImgur(response.get.bodyAsBytes.toArray)
      } else {
        logger.error(s"Error fetching image from $url")
      }
    })(ec)
  }

  private def uploadImageToImgur(imageBytes: Array[Byte]): Unit = {
    val futureResponse = ws.url(imgurConfig("uploadUrl"))
      .addHttpHeaders("Authorization" -> s"Client-ID ${imgurConfig("id")}",
        "Content-Type" -> "application/x-www-form-urlencoded")
      .post(Map("image" -> new String(Base64.getEncoder.encode(imageBytes))))
    futureResponse.onComplete(response => {
      if (response.isSuccess) {
        val completedResponse = response.get.json.toString()
        logger.debug(completedResponse)
      } else {
        logger.error(s"Error => ${response.get.json \ "data" \ "error" get toString}")
      }
    })(ec)
  }

}
