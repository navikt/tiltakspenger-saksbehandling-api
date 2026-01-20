package no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav

import no.nav.tiltakspenger.libs.common.VedtakId

/**
 * @param vedtakDetKlagesPå Id til vedtaket som klages på. Kan være null hvis klagen ikke gjelder et spesifikt vedtak, i så fall vil det bli en avvisning.
 */
data class KlageFormkrav(
    val vedtakDetKlagesPå: VedtakId?,
    val erKlagerPartISaken: Boolean,
    val klagesDetPåKonkreteElementerIVedtaket: Boolean,
    val erKlagefristenOverholdt: Boolean,
    val erUnntakForKlagefrist: KlagefristUnntakSvarord?,
    val erKlagenSignert: Boolean,
) {
    val erAvvisning: Boolean by lazy {
        if (vedtakDetKlagesPå == null) return@lazy true
        if (!erKlagerPartISaken) return@lazy true
        if (!klagesDetPåKonkreteElementerIVedtaket) return@lazy true
        if (!erKlagenSignert) return@lazy true
        if (!erKlagefristenOverholdt && (erUnntakForKlagefrist!! == KlagefristUnntakSvarord.NEI)) return@lazy true
        false
    }
    init {
        if (erKlagefristenOverholdt) {
            require(erUnntakForKlagefrist == null) {
                "Hvis klagefristen er overholdt, skal ikke unntak for klagefrist være satt."
            }
        }
        if (!erKlagefristenOverholdt) {
            require(erUnntakForKlagefrist != null) {
                "Hvis klagefristen ikke er overholdt, må unntak for klagefrist være satt."
            }
        }
    }
}
