package no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId

/**
 * @param erKlagefristenOverholdt Hvis true, skal erUnntakForKlagefrist være null. Hvis false, må erUnntakForKlagefrist være satt.
 */
data class OppdaterKlagebehandlingFormkravKommando(
    val sakId: SakId,
    val klagebehandlingId: KlagebehandlingId,
    val saksbehandler: Saksbehandler,
    val journalpostId: JournalpostId,
    val vedtakDetKlagesPå: VedtakId?,
    val erKlagerPartISaken: Boolean,
    val klagesDetPåKonkreteElementerIVedtaket: Boolean,
    val erKlagefristenOverholdt: Boolean,
    val erUnntakForKlagefrist: KlagefristUnntakSvarord?,
    val erKlagenSignert: Boolean,
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
        )
    }

    init {
        if (erKlagefristenOverholdt) {
            require(erUnntakForKlagefrist == null) {
                "Hvis klagefristen er overholdt, skal ikke unntak for klagefrist være satt. sakId: $sakId, klagebehandlingId: $klagebehandlingId"
            }
        }
        if (!erKlagefristenOverholdt) {
            require(erUnntakForKlagefrist != null) {
                "Hvis klagefristen ikke er overholdt, må unntak for klagefrist være satt sakId: $sakId, klagebehandlingId: $klagebehandlingId"
            }
        }
    }
}
