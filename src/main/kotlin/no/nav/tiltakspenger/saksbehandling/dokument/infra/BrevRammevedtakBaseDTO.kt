@file:Suppress("unused")

package no.nav.tiltakspenger.saksbehandling.dokument.infra

sealed interface BrevRammevedtakBaseDTO {
    val personalia: BrevPersonaliaDTO
    val saksnummer: String
    val saksbehandlerNavn: String
    val beslutterNavn: String?

    // Dette er vår dato, det brukes typisk når bruker klager på vedtaksbrev på dato ...
    val datoForUtsending: String
    val tilleggstekst: String?
    val forhandsvisning: Boolean
}

sealed interface BrevRammevedtakInnvilgelseBaseDTO : BrevRammevedtakBaseDTO {
    val harBarnetillegg: Boolean
    val satser: List<SatserDTO>
    val innvilgelsesperioder: BrevInnvilgelsesperioderDTO
    val barnetillegg: List<BrevBarnetilleggDTO>

    data class SatserDTO(
        val år: Int,
        val ordinær: Int,
        val barnetillegg: Int,
    )
}

/**
 * @param antallDagerPerMeldeperiode null når antallet varierer i perioden; pdfgenrs lager dagteksten, og saksbehandler skriver den i tilleggsteksten når den mangler.
 */
data class BrevInnvilgelsesperioderDTO(
    val antallDagerPerMeldeperiode: Int?,
    val perioder: List<BrevPeriodeDTO>,
)

/** @param antallBarn pdfgenrs skriver antallet med bokstaver. */
data class BrevBarnetilleggDTO(
    val antallBarn: Int,
    val periode: BrevPeriodeDTO,
)
