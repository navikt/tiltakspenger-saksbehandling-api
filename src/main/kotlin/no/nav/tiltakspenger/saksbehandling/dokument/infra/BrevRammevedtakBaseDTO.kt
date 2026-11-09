@file:Suppress("unused")

package no.nav.tiltakspenger.saksbehandling.dokument.infra

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
 */
data class BrevInnvilgelsesperioderDTO(
    val antallDagerPerMeldeperiode: Int?,
)

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
