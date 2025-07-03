package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http

import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.utsjekk.kontrakter.iverksett.ForrigeIverksettingV2Dto
import no.nav.utsjekk.kontrakter.iverksett.UtbetalingV2Dto

/**
 * Kommentar jah: Ser ikke simuleringstypene i kontrakter: https://github.com/navikt/utsjekk-kontrakter/
 * Se også: https://github.com/navikt/helved-utbetaling/blob/main/apps/utsjekk/main/utsjekk/simulering/SimuleringDto.kt#L81
 */
private data class SimuleringRequestDTO(
    val sakId: String,
    val behandlingId: String,
    val personident: String,
    val saksbehandlerId: String,
    val utbetalinger: List<UtbetalingV2Dto>,
    val forrigeIverksetting: ForrigeIverksettingV2Dto?,
)

fun MeldekortBehandling.toSimuleringRequest(
    brukersNavkontor: Navkontor,
    forrigeUtbetalingJson: String?,
    forrigeVedtakId: VedtakId?,
): String {
    return SimuleringRequestDTO(
        sakId = this.saksnummer.toString(),
        // Merk at vi ikke har vedtakId på dette tidspunktet, så behandlingId får være dekkende.
        behandlingId = this.id.uuidPart(),
        personident = this.fnr.verdi,
        saksbehandlerId = this.saksbehandler!!,
        utbetalinger = this.beregning!!.tilUtbetalingerDTO(
            brukersNavkontor = brukersNavkontor,
            forrigeUtbetalingJson = forrigeUtbetalingJson,
        ),
        forrigeIverksetting = forrigeVedtakId?.uuidPart()?.let { ForrigeIverksettingV2Dto(it) },
    ).let { serialize(it) }
}
