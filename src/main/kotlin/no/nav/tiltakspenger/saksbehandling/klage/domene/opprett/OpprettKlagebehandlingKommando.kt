package no.nav.tiltakspenger.saksbehandling.klage.domene.opprett

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.KlageFormkrav
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.KlageInnsendingskilde
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.KlagefristUnntakSvarord
import java.time.LocalDate

/**
 * @param erKlagefristenOverholdt Hvis true, skal erUnntakForKlagefrist være null. Hvis false, må erUnntakForKlagefrist være satt.
 */
data class OpprettKlagebehandlingKommando(
    val sakId: SakId,
    val saksbehandler: Saksbehandler,
    val journalpostId: JournalpostId,
    val vedtakDetKlagesPå: VedtakId?,
    val erKlagerPartISaken: Boolean,
    val klagesDetPåKonkreteElementerIVedtaket: Boolean,
    val erKlagefristenOverholdt: Boolean,
    val erUnntakForKlagefrist: KlagefristUnntakSvarord?,
    val erKlagenSignert: Boolean,
    val innsendingsdato: LocalDate,
    val innsendingskilde: KlageInnsendingskilde,
    val correlationId: CorrelationId,
) {
    fun toKlageFormkrav(): KlageFormkrav {
        return KlageFormkrav(
            vedtakDetKlagesPå = vedtakDetKlagesPå,
            erKlagerPartISaken = erKlagerPartISaken,
            klagesDetPåKonkreteElementerIVedtaket = klagesDetPåKonkreteElementerIVedtaket,
            erKlagefristenOverholdt = erKlagefristenOverholdt,
            erUnntakForKlagefrist = erUnntakForKlagefrist,
            erKlagenSignert = erKlagenSignert,
            innsendingsdato = innsendingsdato,
            innsendingskilde = innsendingskilde,
        )
    }

    init {
        if (erKlagefristenOverholdt) {
            require(erUnntakForKlagefrist == null) {
                "Hvis klagefristen er overholdt, skal ikke unntak for klagefrist være satt. sakId: $sakId"
            }
        }
        if (!erKlagefristenOverholdt) {
            require(erUnntakForKlagefrist != null) {
                "Hvis klagefristen ikke er overholdt, må unntak for klagefrist være satt sakId: $sakId"
            }
        }
    }
}
