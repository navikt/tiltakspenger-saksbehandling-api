package no.nav.tiltakspenger.saksbehandling.objectmothers

import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.fixedClock

object KlokkeMother {
    // TODO jah: Hver test bør eie sin egen klokke for å unngå deling av state mellom tester.
    val clock = TikkendeKlokke(fixedClock)
}
