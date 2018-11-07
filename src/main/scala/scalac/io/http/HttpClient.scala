package scalac.io.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}

import scala.concurrent.Future

trait HttpClient {

  def sendRequest(request: HttpRequest): Future[HttpResponse]

}

//simple client, can use pool if needed
class AkkaHttpClient(implicit actorSystem: ActorSystem) extends HttpClient {
  override def sendRequest(request: HttpRequest): Future[HttpResponse] = Http().singleRequest(request)
}