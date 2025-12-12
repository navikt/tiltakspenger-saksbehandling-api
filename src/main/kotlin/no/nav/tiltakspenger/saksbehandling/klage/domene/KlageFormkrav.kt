package no.nav.tiltakspenger.saksbehandling.klage.domene

import no.nav.tiltakspenger.libs.common.Ulid

/**
 * @param vedtakDetKlagesPå Id til vedtaket som klages på. Kan være null hvis klagen ikke gjelder et spesifikt vedtak, i så fall vil det bli en avvisning.
 */
data class KlageFormkrav(
    val vedtakDetKlagesPå: Ulid?,
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
