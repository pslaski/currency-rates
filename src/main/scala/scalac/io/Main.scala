package scalac.io

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import scalac.io.config.CurrencyRatesConfig
import scalac.io.currencyapi.client.CurrencyApiClient
import scalac.io.currencyapi.fixer.{FixerClient, FixerService}
import scalac.io.currencyapi.services.CurrencyApiService
import scalac.io.http.{AkkaHttpClient, CurrencyRoutes, HttpClient}
import scalac.io.publisher._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

object Main extends App {

  private implicit val system: ActorSystem = ActorSystem("main-system")
  private implicit val materializer: ActorMaterializer = ActorMaterializer()
  private implicit val executionContext: ExecutionContext = system.dispatcher

  private val logger = Logging(system, this.getClass)

  private val config = ConfigFactory.load()
  private val fixerConfig = CurrencyRatesConfig.getFixerConfig(config)
  private val publishingConfig = CurrencyRatesConfig.getPublishingConfig(config)

  private val httpClient: HttpClient = new AkkaHttpClient()

  private val fixerClient: CurrencyApiClient = new FixerClient(fixerConfig, httpClient)
  private val fixerService: CurrencyApiService = new FixerService(fixerClient)

  private val ratesChangeNotifierService: RatesChangeNotifierService = new RatesChangeNotifierServiceImpl(httpClient, publishingConfig.webhookUri)
  private val ratesObserversManager = system.actorOf(RatesObserversManager.props(fixerClient, ratesChangeNotifierService))
  private val observationService: ObservationService = new ObservationServiceImpl(ratesObserversManager)

  private val currencyRoutes = new CurrencyRoutes(fixerService, observationService).routes

  val serverBinding: Future[Http.ServerBinding] = Http().bindAndHandle(currencyRoutes, "localhost", 9000)

  serverBinding.onComplete {
    case Success(_) =>
      logger.info(s"Server online at http://localhost:9000")
    case Failure(e) =>
      logger.error(e,s"Server could not start!")
      e.printStackTrace()
      system.terminate()
  }

  Await.result(system.whenTerminated, Duration.Inf)
}
