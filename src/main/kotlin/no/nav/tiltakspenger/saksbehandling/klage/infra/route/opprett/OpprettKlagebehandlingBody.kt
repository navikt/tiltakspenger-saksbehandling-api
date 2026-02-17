package no.nav.tiltakspenger.saksbehandling.klage.infra.route.opprett

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.opprett.OpprettKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.formkrav.KlageInnsendingskildeDto
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.formkrav.KlagefristUnntakSvarordDto
import java.time.LocalDate

data class OpprettKlagebehandlingBody(
    val journalpostId: String,
    val vedtakDetKlagesPå: String?,
    val erKlagerPartISaken: Boolean,
    val klagesDetPåKonkreteElementerIVedtaket: Boolean,
    val erKlagefristenOverholdt: Boolean,
    val erUnntakForKlagefrist: KlagefristUnntakSvarordDto?,
    val erKlagenSignert: Boolean,
    val innsendingsdato: LocalDate,
    val innsendingskilde: KlageInnsendingskildeDto,
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
            erUnntakForKlagefrist = erUnntakForKlagefrist?.toDomain(),
            erKlagenSignert = erKlagenSignert,
            innsendingsdato = innsendingsdato,
            innsendingskilde = innsendingskilde.toDomain(),
            correlationId = correlationId,
        )
    }
}
