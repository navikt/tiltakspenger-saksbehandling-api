@file:Suppress("unused")

package no.nav.tiltakspenger.saksbehandling.dokument.infra

import no.nav.tiltakspenger.saksbehandling.behandling.domene.Innvilgelsesperioder
import java.time.LocalDate

sealed interface BrevRammevedtakBaseDTO {
    val personalia: BrevPersonaliaDTO
    val saksnummer: String
    val saksbehandlerNavn: String
    val beslutterNavn: String?

    // Dette er vår dato, det brukes typisk når bruker klager på vedtaksbrev på dato ...
    val datoForUtsending: LocalDate
    val tilleggstekst: String?
    val forhandsvisning: Boolean
}

sealed interface BrevRammevedtakInnvilgelseBaseDTO : BrevRammevedtakBaseDTO {
    val innvilgelsesperioder: BrevInnvilgelsesperioderDTO
    val perioder: List<PeriodeInnvilgelseDTO>
    val sisteKjenteSatsår: Int
}

/**
 * @param antallDagerPerMeldeperiode null når antallet varierer i perioden; pdfgenrs lager dagteksten, og saksbehandler skriver den i tilleggsteksten når den mangler.
 * @param tiltak Navnene på tiltakstypene det innvilges for, i rekkefølgen de først er innvilget, uten duplikater.
 * pdfgenrs bruker dem i «fordi du deltar på …».
 */
data class BrevInnvilgelsesperioderDTO(
    val antallDagerPerMeldeperiode: Int?,
    val tiltak: List<String>,
)

fun Innvilgelsesperioder.tilBrevInnvilgelsesperioder(): BrevInnvilgelsesperioderDTO {
    return BrevInnvilgelsesperioderDTO(
        antallDagerPerMeldeperiode = antallDagerPerMeldeperiode.verdier.map { it.value }.distinct().singleOrNull(),
        tiltak = valgteTiltaksdeltagelser.verdier
            .flatten()
            .map { it.typeNavn }
            .distinct(),
    )
}

data class PeriodeInnvilgelseDTO(
    val periode: BrevPeriodeDTO,
    val beløp: List<PeriodeBeløpInnvilgelseDTO>,
)

data class PeriodeBeløpInnvilgelseDTO(
    val periode: BrevPeriodeDTO,
    val antallBarn: Int,
    val tiltakspenger: Int,
    val barnetilleggBeløp: Int,
    val total: Int,
)
