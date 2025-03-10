package no.nav.tiltakspenger.vedtak.repository.meldekort

import no.nav.tiltakspenger.vedtak.meldekort.domene.Satsdag
import java.time.LocalDate

data class SatsdagDbJson(
    val sats: Int,
    val satsRedusert: Int,
    val satsBarnetillegg: Int,
    val satsBarnetilleggRedusert: Int,
    val dato: LocalDate,
)

fun SatsdagDbJson.toSatsdag(): Satsdag = Satsdag(sats = sats, satsRedusert = satsRedusert, satsBarnetillegg = satsBarnetillegg, satsBarnetilleggRedusert = satsBarnetilleggRedusert, dato = dato)

fun Satsdag.toDbJson(): SatsdagDbJson =
    SatsdagDbJson(sats = sats, satsRedusert = satsRedusert, satsBarnetillegg = satsBarnetillegg, satsBarnetilleggRedusert = satsBarnetilleggRedusert, dato = dato)
