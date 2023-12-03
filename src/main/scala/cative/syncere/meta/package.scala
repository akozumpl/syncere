package cative.syncere

import java.time.Instant

import cats.Show

given Show[Instant] = Show.fromToString
