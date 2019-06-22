package models

import java.util.UUID.nameUUIDFromBytes

import actors.ImgurUploadActor.UploadImage
import akka.actor.ActorRef

import scala.concurrent.ExecutionContext

class ImageUrls(imgurUploadActor: ActorRef)(implicit val ec: ExecutionContext) {

  def processUrls(urls: Seq[String]): String = {
    val jobId = nameUUIDFromBytes(urls.mkString(";").getBytes).toString
    for(url <- urls) {
      imgurUploadActor ! UploadImage(url)
    }
    jobId
  }

}
