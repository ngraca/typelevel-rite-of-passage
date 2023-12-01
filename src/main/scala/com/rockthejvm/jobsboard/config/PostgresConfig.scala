package com.rockthejvm.jobsboard.config

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.* 

final case class PostgresConfig(nThreads: Int, url: String, pass: String)
  derives ConfigReader