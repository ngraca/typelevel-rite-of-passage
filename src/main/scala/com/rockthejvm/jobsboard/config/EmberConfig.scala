package com.rockthejvm.jobsboard.config

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.* 
import com.comcast.ip4s.Port
import com.comcast.ip4s.Host
import pureconfig.error.CannotConvert

// genreates given configReader: ConfigReader[EmberConfig]
final case class EmberConfig(host: Host, port: Port) derives ConfigReader

object EmberConfig {
  // need givev ConfigReader[Host] + given ConfigReader[Port] => compiler generates ConfigReader[EmberConfig]
  given hostReader: ConfigReader[Host] = ConfigReader[String].emap { hostString =>
    Host
      .fromString(hostString)
      .toRight(
        CannotConvert(hostString, Host.getClass.toString, s"Invalid host string: $hostString")
      )
  }

  given porttReader: ConfigReader[Port] = ConfigReader[Int].emap { portInt =>
    Port
      .fromInt(portInt)
      .toRight(
        CannotConvert(portInt.toString, Port.getClass.toString, s"Invalid port number: $portInt")
      )
  }
}