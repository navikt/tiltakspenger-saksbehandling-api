package no.nav.tiltakspenger.saksbehandling.barnetillegg

import arrow.core.nonEmptyListOf
import no.nav.tiltakspenger.libs.periode.Periode

/**
 * Én-periode-varianten finnes kun fordi tester som regel har nøyaktig én periode.
 * Prod tar stilling til flere perioder og kaller [Barnetillegg.utenBarnetillegg] med lista selv.
 */
fun Barnetillegg.Companion.utenBarnetillegg(periode: Periode): Barnetillegg =
    Barnetillegg.utenBarnetillegg(nonEmptyListOf(periode))
