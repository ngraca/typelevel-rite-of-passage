package com.rockthejvm.jobsboard.fixtures

import cats.effect.IO
import com.rockthejvm.jobsboard.core.Users
import com.rockthejvm.jobsboard.domain.user.{NewUserInfo, Role, User}

/*
rockthejvm => $2a$10$fTtSkEzG7/OrqJdqx3N.Su/yLrQ7MPaoS6A/m8WS/KdA/.1ESZ5jO
riccardorulez => $2a$10$Mgvd7UROYFj9W4hjMFp.EeCs3yVAcD/MiXislIj5Mt9RWQ1q3VedS
simplepassword => $2a$10$SdBnsTC27YrKeJnvzfu/JOp7c97.T4KZaRDJzCSMTcu5ZZf5om.Wa
riccardorocks => $2a$10$ohXojPX1Tyi7j9Hi8eOjl.FOXc/L02Q351iUSwVuhNab/nsQumkG6

 */

trait UserFixture {

  val mockedUsers: Users[IO] = new Users[IO] {
    override def find(email: String): IO[Option[User]] =
      if (email == danielEmail) IO.pure(Some(Daniel))
      else IO.pure(None)

    override def create(user: User): IO[String] = IO.pure(user.email)

    override def update(user: User): IO[Option[User]] = IO.pure(Some(user))

    override def delete(email: String): IO[Boolean] = IO.pure(true)
  }

  val Daniel = User(
    "daniel@rockthejvm.com",
    "$2a$10$fTtSkEzG7/OrqJdqx3N.Su/yLrQ7MPaoS6A/m8WS/KdA/.1ESZ5jO",
    Some("Daniel"),
    Some("Ciocirlan"),
    Some("Rock the JVM"),
    Role.ADMIN
  )
  val danielEmail    = Daniel.email
  val danielPassword = "rockthejvm"

  val Riccardo = User(
    "riccardo@rockthejvm.com",
    "$2a$10$Mgvd7UROYFj9W4hjMFp.EeCs3yVAcD/MiXislIj5Mt9RWQ1q3VedS",
    Some("Riccardo"),
    Some("Cardin"),
    Some("Rock the JVM"),
    Role.RECRUITER
  )
  val riccardoEmail    = Riccardo.email
  val riccardoPassword = "riccardorulez"

  val NewUser = User(
    "newuser@gmail.com",
    "$2a$10$SdBnsTC27YrKeJnvzfu/JOp7c97.T4KZaRDJzCSMTcu5ZZf5om.Wa",
    Some("John"),
    Some("Doe"),
    Some("Some Company"),
    Role.RECRUITER
  )

  val UpdatedRiccardo = User(
    "riccardo@rockthejvm.com",
    "$2a$10$ohXojPX1Tyi7j9Hi8eOjl.FOXc/L02Q351iUSwVuhNab/nsQumkG6",
    Some("RICCARDO"),
    Some("CARDIN"),
    Some("Adobe"),
    Role.RECRUITER
  )

  val NewUserDaniel = NewUserInfo(
    danielEmail,
    danielEmail,
    Some("Daniel"),
    Some("Ciocirlan"),
    Some("Rock the JVM")
  )

  val NewUserRiccardo = NewUserInfo(
    riccardoEmail,
    riccardoPassword,
    Some("Riccardo"),
    Some("Cardin"),
    Some("Rock the JVM")
  )
}
