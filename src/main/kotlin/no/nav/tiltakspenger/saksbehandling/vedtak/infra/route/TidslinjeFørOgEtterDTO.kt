package no.nav.tiltakspenger.saksbehandling.vedtak.infra.route

import no.nav.tiltakspenger.libs.periode.PeriodeDTO
import no.nav.tiltakspenger.libs.periode.toDTO
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.vedtak.TidslinjeFørOgEtter
import no.nav.tiltakspenger.saksbehandling.vedtak.Tidslinjeresultat
import no.nav.tiltakspenger.saksbehandling.vedtak.Tidslinjeverdi

/**
 * Vedtakstidslinjen innenfor vedtaksperioden til en rammebehandling, før og etter at behandlingen er iverksatt.
 * Perioder uten element i [før] eller [etter] gir ikke rett til tiltakspenger.
 */
data class TidslinjeFørOgEtterDTO(
    val periode: PeriodeDTO,
    val før: List<TidslinjeFørOgEtterElementDTO>,
    val etter: List<TidslinjeFørOgEtterElementDTO>,
    val harEndring: Boolean,
)

/**
 * @param rammevedtakId Null dersom behandlingen ikke er iverksatt.
 * @param antallDagerPerMeldeperiode Kun satt ved innvilgelse.
 * @param antallBarn Kun satt ved innvilgelse.
 */
data class TidslinjeFørOgEtterElementDTO(
    val periode: PeriodeDTO,
    val tidslinjeResultat: TidslinjeResultat,
    val behandlingId: String,
    val rammevedtakId: String?,
    val antallDagerPerMeldeperiode: Int?,
    val antallBarn: Int?,
)

fun TidslinjeFørOgEtter.tilTidslinjeFørOgEtterDTO(): TidslinjeFørOgEtterDTO {
    return TidslinjeFørOgEtterDTO(
        periode = periode.toDTO(),
        før = før.tilElementerDTO(),
        etter = etter.tilElementerDTO(),
        harEndring = harEndring,
    )
}

private fun Periodisering<Tidslinjeverdi>.tilElementerDTO(): List<TidslinjeFørOgEtterElementDTO> {
    return perioderMedVerdi.map { (verdi, periode) ->
        TidslinjeFørOgEtterElementDTO(
            periode = periode.toDTO(),
            tidslinjeResultat = verdi.resultat.tilDTO(),
            behandlingId = verdi.rammebehandlingId.toString(),
            rammevedtakId = verdi.rammevedtakId?.toString(),
            antallDagerPerMeldeperiode = verdi.antallDagerPerMeldeperiode?.value,
            antallBarn = verdi.antallBarn?.value,
        )
    }
}

private fun Tidslinjeresultat.tilDTO(): TidslinjeResultat {
    return when (this) {
        Tidslinjeresultat.SØKNADSBEHANDLING_INNVILGELSE -> TidslinjeResultat.SØKNADSBEHANDLING_INNVILGELSE
        Tidslinjeresultat.REVURDERING_INNVILGELSE -> TidslinjeResultat.REVURDERING_INNVILGELSE
        Tidslinjeresultat.FORLENGELSE -> TidslinjeResultat.FORLENGELSE
        Tidslinjeresultat.OMGJØRING_INNVILGELSE -> TidslinjeResultat.OMGJØRING_INNVILGELSE
        Tidslinjeresultat.OMGJØRING_OPPHØR -> TidslinjeResultat.OMGJØRING_OPPHØR
        Tidslinjeresultat.STANS -> TidslinjeResultat.STANS
    }
}
