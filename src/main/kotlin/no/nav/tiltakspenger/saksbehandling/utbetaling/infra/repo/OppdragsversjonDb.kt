package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo

import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Oppdragsversjon

fun Oppdragsversjon.tilDb(): Int = when (this) {
    Oppdragsversjon.V2 -> 2
    Oppdragsversjon.V3 -> 3
}

fun Int.tilOppdragsversjon(): Oppdragsversjon = Oppdragsversjon.entries.single { it.tilDb() == this }
