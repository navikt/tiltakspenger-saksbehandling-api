package no.nav.tiltakspenger.saksbehandling.klage.infra.route

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.OppdaterKlagebehandlingFormkravKommando

data class OppdaterKlagebehandlingFormkravBody(
    val journalpostId: String,
    val vedtakDetKlagesPå: String?,
    val erKlagerPartISaken: Boolean,
    val klagesDetPåKonkreteElementerIVedtaket: Boolean,
    val erKlagefristenOverholdt: Boolean,
    val erUnntakForKlagefrist: KlagefristUnntakSvarordDto?,
    val erKlagenSignert: Boolean,
) {
    fun tilKommando(
        sakId: SakId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
        klagebehandlingId: KlagebehandlingId,
    ): OppdaterKlagebehandlingFormkravKommando {
        return OppdaterKlagebehandlingFormkravKommando(
            sakId = sakId,
            klagebehandlingId = klagebehandlingId,
            saksbehandler = saksbehandler,
            journalpostId = JournalpostId(journalpostId),
            vedtakDetKlagesPå = vedtakDetKlagesPå?.let { VedtakId.fromString(it) },
            erKlagerPartISaken = erKlagerPartISaken,
            klagesDetPåKonkreteElementerIVedtaket = klagesDetPåKonkreteElementerIVedtaket,
            erKlagefristenOverholdt = erKlagefristenOverholdt,
            erUnntakForKlagefrist = erUnntakForKlagefrist?.toDomain(),
            erKlagenSignert = erKlagenSignert,
            correlationId = correlationId,
        )
    }
}
