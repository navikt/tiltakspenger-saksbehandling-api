package no.nav.tiltakspenger.saksbehandling.klage.infra.route

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.opprett.OpprettKlagebehandlingKommando

data class OpprettKlagebehandlingBody(
    val journalpostId: String,
    val vedtakDetKlagesPå: String?,
    val erKlagerPartISaken: Boolean,
    val klagesDetPåKonkreteElementerIVedtaket: Boolean,
    val erKlagefristenOverholdt: Boolean,
    val erKlagenSignert: Boolean,
) {
    fun tilKommando(
        sakId: SakId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): OpprettKlagebehandlingKommando {
        return OpprettKlagebehandlingKommando(
            sakId = sakId,
            saksbehandler = saksbehandler,
            journalpostId = JournalpostId(journalpostId),
            vedtakDetKlagesPå = vedtakDetKlagesPå?.let { VedtakId.fromString(it) },
            erKlagerPartISaken = erKlagerPartISaken,
            klagesDetPåKonkreteElementerIVedtaket = klagesDetPåKonkreteElementerIVedtaket,
            erKlagefristenOverholdt = erKlagefristenOverholdt,
            erKlagenSignert = erKlagenSignert,
            correlationId = correlationId,
        )
    }
}
