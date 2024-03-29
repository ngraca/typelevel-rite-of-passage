package com.rockthejvm.jobsboard.modules

import cats.data.OptionT
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.*
import org.typelevel.log4cats.Logger
import cats.effect.*
import cats.implicits.*
import com.rockthejvm.jobsboard.config.SecurityConfig
import com.rockthejvm.jobsboard.core.Users
import com.rockthejvm.jobsboard.http.routes.*
import com.rockthejvm.jobsboard.domain.security.*
import com.rockthejvm.jobsboard.domain.user.User
import tsec.authentication.{BackingStore, IdentityStore, JWTAuthenticator, SecuredRequestHandler}
import tsec.common.SecureRandomId
import tsec.mac.jca.HMACSHA256

import scala.collection.immutable.Map

class HttpApi[F[_]: Concurrent: Logger] private (core: Core[F], authenticator: Authenticator[F]) {
  given securedHandler: SecuredHandler[F] = SecuredRequestHandler(authenticator)
  private val healthRoutes                = HealthRoutes[F].routes
  private val jobRoutes                   = JobRoutes[F](core.jobs).routes
  private val authRoutes                  = AuthRoutes[F](core.auth, authenticator).routes

  val endpoints = Router(
    "/api" -> (healthRoutes <+> jobRoutes <+> authRoutes)
  )
}

object HttpApi {

  def createAuthenticator[F[_]: Sync](
      users: Users[F],
      securityConfig: SecurityConfig
  ): F[Authenticator[F]] = {
    // 1. identity store: String => OptionT[F, User]
    val idStore: IdentityStore[F, String, User] = (email: String) => OptionT(users.find(email))

    // 2. backing store for JWT tokens: BackingStore[F, id, JwtToken]
    val tokenStoreF = Ref.of[F, Map[SecureRandomId, JwtToken]](Map.empty).map { ref =>
      new BackingStore[F, SecureRandomId, JwtToken] {
        override def get(id: SecureRandomId): OptionT[F, JwtToken] =
          OptionT( /*F[JwtToken]*/ ref.get.map(_.get(id)))

        override def put(elem: JwtToken): F[JwtToken] =
          ref.modify(store => (store + (elem.id -> elem), elem))

        override def update(v: JwtToken): F[JwtToken] =
          put(v)

        override def delete(id: SecureRandomId): F[Unit] =
          ref.modify(store => (store - id, ()))
      }
    }

    // 3. hashing key
    val keyF =
      HMACSHA256.buildKey[F](securityConfig.secret.getBytes("UTF-8")) // TODO move to config

    for {
      key        <- keyF
      tokenStore <- tokenStoreF
      // 4. authenticator
    } yield JWTAuthenticator.backed.inBearerToken(
      expiryDuration = securityConfig.jwtExpiryDuration, // expiration of tokens
      maxIdle = None,                                    // max idle time (optional)
      identityStore = idStore,                           // identity store
      tokenStore = tokenStore,
      signingKey = key // hash key
    )
  }
  def apply[F[_]: Async: Logger](
      core: Core[F],
      securityConfig: SecurityConfig
  ): Resource[F, HttpApi[F]] =
    Resource
      .eval(createAuthenticator(core.users, securityConfig))
      .map(authenticator => new HttpApi[F](core, authenticator))
}
