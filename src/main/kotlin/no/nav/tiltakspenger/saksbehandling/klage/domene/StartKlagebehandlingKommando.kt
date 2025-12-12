package no.nav.tiltakspenger.saksbehandling.klage.domene

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.Ulid
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import java.time.LocalDateTime

data class StartKlagebehandlingKommando(
    val sakId: SakId,
    val saksbehandler: Saksbehandler,
    val journalpostId: JournalpostId,
    val vedtakDetKlagesPå: VedtakId?,
    val erKlagerPartISaken: Boolean,
    val klagesDetPåKonkreteElementerIVedtaket: Boolean,
    val erKlagefristenOverholdt: Boolean,
    val erKlagenSignert: Boolean,
    val correlationId: CorrelationId,
)
