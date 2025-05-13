package no.nav.tiltakspenger.saksbehandling.statistikk.vedtak

import no.nav.tiltakspenger.saksbehandling.vedtak.Vedtakstype
import java.time.LocalDate
import java.util.UUID

// Team Spenn (DVH) bruker denne tabellen, samt statistikk_utbetaling. De har beskrevet
// behovet sitt her: https://confluence.adeo.no/display/DVH/Datamodell+for+tiltakspenger
data class StatistikkStønadDTO(
    // tilfeldig id
    val id: UUID,
    // fnr
    val brukerId: String,

    val sakId: String,
    val saksnummer: String,
    val resultat: VedtakStatistikkResultat,
    val sakDato: LocalDate,
    // sakFraDato og sakTilDato er de samme som datoene for vedtaket siden sak ikke har periode lenger
    val sakFraDato: LocalDate,
    val sakTilDato: LocalDate,
    // IND
    val ytelse: String,

    val søknadId: String?,
    val søknadDato: LocalDate?,
    // perioden for tiltaksdeltakelsen det er søkt for
    val søknadFraDato: LocalDate?,
    val søknadTilDato: LocalDate?,

    val vedtakId: String,
    val vedtaksType: String,
    val vedtakDato: LocalDate,
    val vedtakFom: LocalDate,
    val vedtakTom: LocalDate,
    // Brukes av DVH for å identifisere vedtakssystem når de sammenstiller data
    val fagsystem: String = "TPSAK",
    // tiltaksdeltakelser (eksternId) som det er innvilget tiltakspenger for
    val tiltaksdeltakelser: List<String>,
)

enum class VedtakStatistikkResultat {
    Innvilgelse,
    Avslag,
    Stans,
    ;

    companion object {
        fun Vedtakstype.toVedtakStatistikkResultat(): VedtakStatistikkResultat = when (this) {
            Vedtakstype.INNVILGELSE -> Innvilgelse
            Vedtakstype.AVSLAG -> Avslag
            Vedtakstype.STANS -> Stans
        }
    }
}
