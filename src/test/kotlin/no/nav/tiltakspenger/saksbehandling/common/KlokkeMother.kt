package no.nav.tiltakspenger.saksbehandling.common

import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.fixedClock

object KlokkeMother {
    val clock = TikkendeKlokke(fixedClock)
}
