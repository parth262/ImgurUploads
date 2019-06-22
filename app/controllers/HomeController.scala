package controllers

import akka.actor.ActorSystem
import javax.inject._
import models.{ImageUrls, SchemaDefinition}
import play.api.libs.json.{JsObject, JsString, JsValue, Json}
import play.api.libs.ws.WSClient
import play.api.mvc._
import play.api._
import sangria.execution.{ErrorWithResolver, Executor, QueryAnalysisError}
import sangria.marshalling.playJson._
import sangria.parser.{QueryParser, SyntaxError}

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class HomeController @Inject()(system: ActorSystem, ws: WSClient, config: Configuration) extends InjectedController {
  import system.dispatcher

  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index(Array()))
  }

  def uploadImages: Action[JsValue] = Action.async(parse.json) { request =>
    val query = (request.body \ "query").as[String]
    val operation = (request.body \ "operationName").asOpt[String]
    val variables = (request.body \ "variables").toOption.flatMap {
      case JsString(vars) ⇒ Some(parseVariables(vars))
      case obj: JsObject ⇒ Some(obj)
      case _ ⇒ None
    }
    executeQuery(query, variables, operation)
  }

  private def parseVariables(variables: String) =
    if (variables.trim == "" || variables.trim == "null") Json.obj() else Json.parse(variables).as[JsObject]


  private def executeQuery(query: String, variables: Option[JsObject], operation: Option[String]) = {
    val imgurConfig = Map(
      "uploadUrl" -> config.get[String]("imgur.imageUploadUrl"),
      "id" -> config.get[String]("imgur.client.id"),
      "secret" -> config.get[String]("imgur.client.secret")
    )

    QueryParser.parse(query) match {
      case Success(queryAst) =>
        Executor.execute(
          SchemaDefinition.UrlSchema,
          queryAst, new ImageUrls(ws, imgurConfig),
          operationName = operation,
          variables = variables getOrElse Json.obj())
          .map(Ok(_))
          .recover {
            case error: QueryAnalysisError ⇒ BadRequest(error.resolveError)
            case error: ErrorWithResolver ⇒ InternalServerError(error.resolveError)
          }

      case Failure(error: SyntaxError) =>
        Future.successful(BadRequest(s"Syntax error => $error"))

      case Failure(error) =>
        throw error
    }
  }
}
