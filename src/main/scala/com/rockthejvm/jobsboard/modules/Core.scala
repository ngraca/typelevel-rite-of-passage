package com.rockthejvm.jobsboard.modules

import cats.effect.*
import cats.implicits.*
import cats.effect.kernel.Async
import doobie.util.transactor.Transactor
import org.typelevel.log4cats.Logger

import com.rockthejvm.jobsboard.core.*

final class Core[F[_]] private (val jobs: Jobs[F])

// postgres -> jobs -> core -> httApi -> app
object Core {

  def apply[F[_]: Async: Logger](xa: Transactor[F]): Resource[F, Core[F]] =
    Resource
      .eval(LiveJobs[F](xa))
      .map(jobs => new Core(jobs))
}
