package no.nav.tiltakspenger.saksbehandling.klage.domene

import no.nav.tiltakspenger.libs.common.Ulid
import no.nav.tiltakspenger.libs.common.VedtakId

/**
 * @param vedtakDetKlagesPå Id til vedtaket som klages på. Kan være null hvis klagen ikke gjelder et spesifikt vedtak, i så fall vil det bli en avvisning.
 */
data class KlageFormkrav(
    val vedtakDetKlagesPå: VedtakId?,
    val erKlagerPartISaken: Boolean,
    val klagesDetPåKonkreteElementerIVedtaket: Boolean,
    val erKlagefristenOverholdt: Boolean,
    val erKlagenSignert: Boolean,
) {
    val erAvvisning: Boolean by lazy {
        vedtakDetKlagesPå == null ||
            !erKlagerPartISaken ||
            !klagesDetPåKonkreteElementerIVedtaket ||
            !erKlagefristenOverholdt ||
            !erKlagenSignert
    }
}
