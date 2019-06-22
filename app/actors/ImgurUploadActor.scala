package actors

import java.util.Base64

import akka.actor.{Actor, ActorSystem}
import javax.inject.Inject
import play.api.libs.ws.{WSClient, WSRequest}
import play.api.{Configuration, Logger}

import scala.concurrent.{ExecutionContext, Future}

object ImgurUploadActor {

  case class UploadImage(url: String)

}

class ImgurUploadActor @Inject()(system: ActorSystem, ws: WSClient, config: Configuration)(implicit ec: ExecutionContext) extends Actor {

  import ImgurUploadActor._

  private val logger = Logger.logger
  private val imgurConfig = Map(
    "uploadUrl" -> config.get[String]("imgur.imageUploadUrl"),
    "id" -> config.get[String]("imgur.client.id"),
    "secret" -> config.get[String]("imgur.client.secret")
  )

  val futureResponse: WSRequest = ws.url(imgurConfig("uploadUrl"))
    .addHttpHeaders("Authorization" -> s"Client-ID ${imgurConfig("id")}",
      "Content-Type" -> "application/x-www-form-urlencoded")

  private def uploadImageToImgur(imageBytes: Array[Byte]): Future[Unit] = {
    logger.info("Uploading image to imgur")
    futureResponse
      .post(Map("image" -> new String(Base64.getEncoder.encode(imageBytes))))
      .map(response => {
        response.status match {
          case 200 => logger.debug(response.json.toString())
          case status => logger.error(s"Error uploading to imgur with status $status")
        }
      }).recover {
      case e: Exception => logger.error(s"Error => $e")
    }
  }

  private def downloadImage(url: String): Future[Option[Array[Byte]]] = {
    logger.info("Downloading image in memory")
    val request = ws.url(url)
    request.get().map(response => {
      response.status match {
        case 200 =>
          logger.info("Image successfully downloaded")
          Some(response.bodyAsBytes.toArray)
        case status =>
          logger.error(s"Error fetching image from url $url with status $status")
          None
      }
    }).recover {
      case _: Exception =>
        logger.error(s"Error fetching image from $url")
        None
    }
  }

  override def receive: Receive = {

    case UploadImage(url: String) =>
      downloadImage(url).foreach(value => {
        if(value.isDefined) {
          uploadImageToImgur(value.get)
        } else {
          logger.error("No image to upload")
        }
      })
  }
}
