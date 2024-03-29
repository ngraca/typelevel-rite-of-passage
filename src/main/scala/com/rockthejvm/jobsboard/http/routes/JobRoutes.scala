package com.rockthejvm.jobsboard.http.routes

import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import org.typelevel.log4cats.Logger
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.*
import cats.effect.*
import cats.implicits.*
import scala.language.implicitConversions

import java.util.UUID
import tsec.authentication.{SecuredRequestHandler, asAuthed}
import com.rockthejvm.jobsboard.domain.job.*
import com.rockthejvm.jobsboard.domain.security.*
import com.rockthejvm.jobsboard.domain.pagination.*
import com.rockthejvm.jobsboard.core.*
import com.rockthejvm.jobsboard.domain.security.AuthRoute
import com.rockthejvm.jobsboard.domain.user.User
import com.rockthejvm.jobsboard.http.responses.*
import com.rockthejvm.jobsboard.http.validation.syntax.*
import com.rockthejvm.jobsboard.logging.syntax.*
import org.http4s.dsl.impl.OptionalQueryParamDecoderMatcher

class JobRoutes[F[_]: Concurrent: Logger: SecuredHandler] private (jobs: Jobs[F])
    extends HttpValidationDsl[F] {

  object OffsetQueryParam extends OptionalQueryParamDecoderMatcher[Int]("offset")
  object LimitQueryParam  extends OptionalQueryParamDecoderMatcher[Int]("limit")

  // POST /jobs?limit=y&offset=z { filters } // TODO add query params and filters
  private val allJobsRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root :? LimitQueryParam(limit) +& OffsetQueryParam(offset) =>
      for {
        filter   <- req.as[JobFilter]
        jobsList <- jobs.all(filter, Pagination(limit, offset))
        resp     <- Ok(jobsList)
      } yield resp
  }

  // GET /jobs/uuid
  private val findJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / UUIDVar(id) =>
    jobs.find(id).flatMap {
      case Some(job) => Ok(job)
      case None      => NotFound(FailureResponse(s"Job $id not found."))
    }
  }

  // POST /jobs/create { jobInfo }
  private val createJobRoute: AuthRoute[F] = { case req @ POST -> Root / "create" asAuthed _ =>
    req.request.validate[JobInfo] { jobInfo =>
      for {
        jobId <- jobs.create("TODO@rockthejvm.com", jobInfo)
        resp  <- Created(jobId)
      } yield resp
    }
  }

  // PUT /jobs/uuid { jobInfo }
  private val updateJobRoute: AuthRoute[F] = { case req @ PUT -> Root / UUIDVar(id) asAuthed user =>
    req.request.validate[JobInfo] { jobInfo =>
      jobs.find(id).flatMap {
        case None =>
          NotFound(FailureResponse(s"Cannot update job $id not found"))
        case Some(job) if user.owns(job) || user.isAdmin =>
          jobs.update(id, jobInfo) *> Ok()
        case _ => NotFound(FailureResponse(s"You can only update your own jobs "))
      }
    }
  }

  // DELETE /jobs/uuid
  private val deleteJobRoute: AuthRoute[F] = {
    case req @ DELETE -> Root / UUIDVar(id) asAuthed user =>
      jobs.find(id).flatMap {
        case None => NotFound(FailureResponse(s"Cannot delete job $id not found"))
        case Some(job) if user.owns(job) || user.isAdmin =>
          jobs.delete(id) *> Ok()
        case _ => io.Forbidden(FailureResponse("You can only delete your own jobs"))
      }
  }

  val unauthedRoutes = allJobsRoute <+> findJobRoute
  val authedRoutes = SecuredHandler[F].liftService(
    createJobRoute.restrictedTo(allRoles) |+|
      updateJobRoute.restrictedTo(allRoles) |+|
      deleteJobRoute.restrictedTo(allRoles)
  )

  val routes = Router(
    "/jobs" -> (unauthedRoutes <+> authedRoutes)
  )

}

object JobRoutes {
  def apply[F[_]: Concurrent: Logger: SecuredHandler](jobs: Jobs[F]) =
    new JobRoutes[F](jobs)
}
