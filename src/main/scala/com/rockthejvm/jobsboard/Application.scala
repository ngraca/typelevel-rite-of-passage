package com.rockthejvm.jobsboard

import cats.*
import cats.effect.*
import cats.implicits.*

import com.rockthejvm.jobsboard.config.*
import com.rockthejvm.jobsboard.config.syntax.*
import com.rockthejvm.jobsboard.modules.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pureconfig.ConfigSource
import com.rockthejvm.jobsboard.config.EmberConfig
import pureconfig.error.ConfigReaderException

import com.rockthejvm.jobsboard.modules.*
import org.http4s.ember.server.EmberServerBuilder
/*
  1 - add a plain health endpoint to our app
  2 - add minimal configuration
  3 - basic http server layout
 */
object Application extends IOApp.Simple {

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  override def run: IO[Unit] = ConfigSource.default.loadF[IO, AppConfig].flatMap {
    case AppConfig(postgresConfig, emberConfig) =>
      val appResource = for {
        xa <- Database[IO](postgresConfig)
        core    <- Core[IO](xa)
        httpApi <- HttpApi[IO](core)
        server <- EmberServerBuilder
          .default[IO]
          .withHost(emberConfig.host) // String, need Host
          .withPort(emberConfig.port) // String, need Port
          .withHttpApp(httpApi.endpoints.orNotFound)
          .build
      } yield server

      appResource.use(_ => IO.println("Rock the JVM!") *> IO.never)
  }
}
