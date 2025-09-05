package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.routes

import no.nav.tiltakspenger.saksbehandling.beregning.infra.dto.MeldeperiodeBeregningDTO
import no.nav.tiltakspenger.saksbehandling.beregning.infra.dto.tilMeldeperiodeBeregningDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.VedtattUtbetaling
import java.time.LocalDateTime

data class VedtattUtbetalingDTO(
    val id: String,
    val vedtakId: String,
    val sakId: String,
    val brukerNavkontorNavn: String?,
    val brukerNavkontorNummer: String,
    val opprettet: LocalDateTime,
    val saksbehandler: String,
    val beslutter: String,
    val meldeperiodeberegninger: List<MeldeperiodeBeregningDTO>,
    val sendtTilUtbetaling: LocalDateTime?,
    val status: String?,
)

fun VedtattUtbetaling.toVedtakUtbetalingDTO() = VedtattUtbetalingDTO(
    id = id.toString(),
    vedtakId = vedtakId.toString(),
    sakId = sakId.toString(),
    brukerNavkontorNavn = brukerNavkontor.kontornavn,
    brukerNavkontorNummer = brukerNavkontor.kontornummer,
    opprettet = opprettet,
    saksbehandler = saksbehandler,
    beslutter = beslutter,
    meldeperiodeberegninger = beregning.beregninger.map { it.tilMeldeperiodeBeregningDTO() },
    sendtTilUtbetaling = sendtTilUtbetaling,
    status = status?.name,
)
