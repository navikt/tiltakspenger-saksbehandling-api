package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.Ulid
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.beregning.Beregning
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingId
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

fun toSimuleringRequest(
    saksnummer: Saksnummer,
    behandlingId: Ulid,
    fnr: Fnr,
    saksbehandler: String,
    beregning: Beregning,
    brukersNavkontor: Navkontor,
    kanSendeInnHelgForMeldekort: Boolean,
    forrigeUtbetalingJson: String?,
    forrigeUtbetalingId: UtbetalingId?,
): String {
    return SimuleringRequestDTO(
        sakId = saksnummer.toString(),
        // Merk at vi ikke har vedtakId på dette tidspunktet, så behandlingId får være dekkende.
        behandlingId = behandlingId.uuidPart(),
        personident = fnr.verdi,
        saksbehandlerId = saksbehandler,
        utbetalinger = beregning.tilUtbetalingerDTO(
            brukersNavkontor = brukersNavkontor,
            forrigeUtbetalingJson = forrigeUtbetalingJson,
            skalUtbetaleHelgPåFredag = kanSendeInnHelgForMeldekort,
        ),
        forrigeIverksetting = forrigeUtbetalingId?.uuidPart()?.let { ForrigeIverksettingV2Dto(it) },
    ).let { serialize(it) }
}
