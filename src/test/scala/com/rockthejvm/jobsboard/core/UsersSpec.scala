package com.rockthejvm.jobsboard.core

import cats.effect.*
import cats.effect.testing.scalatest.AsyncIOSpec
import com.rockthejvm.jobsboard.domain.user.User
import doobie.implicits.*
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import com.rockthejvm.jobsboard.fixtures.UserFixture
import org.postgresql.util.PSQLException
import org.scalatest.Inside

class UsersSpec
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with Inside
    with DoobieSpec
    with UserFixture {
  override val initScript: String = "sql/users.sql"
  given logger: Logger[IO]        = Slf4jLogger.getLogger[IO]

  "Users 'algebra'" - {
    "should retrieve a user by email" in {
      transactor.use { xa =>
        val program = for {
          users     <- LiveUsers[IO](xa)
          retrieved <- users.find("riccardo@rockthejvm.com")
        } yield retrieved
        program.asserting(_ shouldBe Some(Riccardo))
      }
    }

    "should return None if the email doesn't exist" in {
      transactor.use { xa =>
        val program = for {
          users     <- LiveUsers[IO](xa)
          retrieved <- users.find("notfound@rockthejvm.com")
        } yield retrieved
        program.asserting(_ shouldBe None)
      }
    }

    "should create a new user" in {
      transactor.use { xa =>
        val program = for {
          users  <- LiveUsers[IO](xa)
          userId <- users.create(NewUser)
          maybeUser <- sql"SELECT * FROM users WHERE email = ${NewUser.email}"
            .query[User]
            .option
            .transact(xa)
        } yield (userId, maybeUser)

        program.asserting { case (userId, maybeUser) =>
          userId shouldBe NewUser.email
          maybeUser shouldBe Some(NewUser)
        }
      }
    }

    "should fail creating a new user if the eamil already exists" in {
      transactor.use { xa =>
        val program = for {
          users  <- LiveUsers[IO](xa)
          userId <- users.create(Daniel).attempt // IO[Either[Throwable, String]]
        } yield userId

        program.asserting { outcome =>
          inside(outcome) {
            case Left(e) => e shouldBe a[PSQLException]
            case _       => fail()
          }
        }
      }
    }

    "should return None when updating a user that does not exist" in {
      transactor.use { xa =>
        val program = for {
          users     <- LiveUsers[IO](xa)
          maybeUser <- users.update(NewUser)
        } yield maybeUser

        program.asserting(_ shouldBe None)
      }
    }

    "should update an existing user" in {
      transactor.use { xa =>
        val program = for {
          users     <- LiveUsers[IO](xa)
          maybeUser <- users.update(UpdatedRiccardo)
        } yield maybeUser

        program.asserting(_ shouldBe Some(UpdatedRiccardo))
      }
    }

    "should delete a user" in {
      transactor.use { xa =>
        val program = for {
          users  <- LiveUsers[IO](xa)
          result <- users.delete("daniel@rockthejvm.com")
          maybeUser <- sql"SELECT * FROM users WHERE email = 'daniel@rockthejvm.com'"
            .query[User]
            .option
            .transact(xa)
        } yield (result, maybeUser)

        program.asserting { case (result, maybeUser) =>
          result shouldBe true
          maybeUser shouldBe None
        }
      }
    }

    "sould NOT delete a user that does not exist" in {
      transactor.use { xa =>
        val program = for {
          users  <- LiveUsers[IO](xa)
          result <- users.delete("nobody@rockthejvm.com")
        } yield result

        program.asserting(_ shouldBe false)
      }
    }
  }
}
