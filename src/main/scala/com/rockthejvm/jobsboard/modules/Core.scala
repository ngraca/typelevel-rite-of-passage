package com.rockthejvm.jobsboard.modules

import cats.effect.*
import cats.implicits.*
import cats.effect.kernel.Async
import com.rockthejvm.jobsboard.config.{EmailServiceConfig, SecurityConfig, TokenConfig}
import doobie.util.transactor.Transactor
import org.typelevel.log4cats.Logger
import com.rockthejvm.jobsboard.core.*

final class Core[F[_]] private (val jobs: Jobs[F], val users: Users[F], val auth: Auth[F])

// postgres -> jobs -> core -> httApi -> app
object Core {
  def apply[F[_]: Async: Logger](
      xa: Transactor[F],
      tokenConfig: TokenConfig,
      emailServiceConfig: EmailServiceConfig
  ): Resource[F, Core[F]] =
    val coreF = for {
      jobs   <- LiveJobs[F](xa)
      users  <- LiveUsers[F](xa)
      tokens <- LiveTokens[F](users)(xa, tokenConfig)
      emails <- LiveEmails[F](emailServiceConfig)
      auth   <- LiveAuth[F](users, tokens, emails)
    } yield new Core(jobs, users, auth)

    Resource.eval(coreF)
}
