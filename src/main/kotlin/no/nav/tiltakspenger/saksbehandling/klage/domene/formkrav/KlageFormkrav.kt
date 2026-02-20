package no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav

import no.nav.tiltakspenger.libs.common.VedtakId
import java.time.LocalDate

/**
 * @param vedtakDetKlagesPå Id til vedtaket som klages på. Kan være null hvis klagen ikke gjelder et spesifikt vedtak, i så fall vil det bli en avvisning.
 * @param innsendingsdato Datoen klagen formelt ble overlevert til Nav. Et eksempel er dagen man postet brevet. Denne datoen brukes for å vurdere om klagefristen er overholdt.
 * Se også: https://lovdata.no/lov/1967-02-10/§29 til og med §33
 */
data class KlageFormkrav(
    val vedtakDetKlagesPå: VedtakId?,
    val erKlagerPartISaken: Boolean,
    val klagesDetPåKonkreteElementerIVedtaket: Boolean,
    val erKlagefristenOverholdt: Boolean,
    val erUnntakForKlagefrist: KlagefristUnntakSvarord?,
    val erKlagenSignert: Boolean,
    val innsendingsdato: LocalDate,
    val innsendingskilde: KlageInnsendingskilde,
) {
    val erAvvisning: Boolean by lazy {
        if (vedtakDetKlagesPå == null) return@lazy true
        if (!erKlagerPartISaken) return@lazy true
        if (!klagesDetPåKonkreteElementerIVedtaket) return@lazy true
        if (!erKlagenSignert) return@lazy true
        if (!erKlagefristenOverholdt && (erUnntakForKlagefrist!! == KlagefristUnntakSvarord.NEI)) return@lazy true
        false
    }

    val erOppfyllt: Boolean by lazy {
        !erAvvisning
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
