package no.nav.tiltakspenger.saksbehandling.klage.infra.repo

import no.nav.tiltakspenger.libs.common.Ulid
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.KlageFormkrav

private data class KlagebehandlingFormkravDbJson(
    val vedtakDetKlagesPå: String?,
    val erKlagerPartISaken: Boolean,
    val klagesDetPåKonkreteElementerIVedtaket: Boolean,
    val erKlagefristenOverholdt: Boolean,
    val erKlagenSignert: Boolean,
) {
    fun toDomain(): KlageFormkrav {
        return KlageFormkrav(
            vedtakDetKlagesPå = vedtakDetKlagesPå?.let { VedtakId.fromString(it) },
            erKlagerPartISaken = erKlagerPartISaken,
            klagesDetPåKonkreteElementerIVedtaket = klagesDetPåKonkreteElementerIVedtaket,
            erKlagefristenOverholdt = erKlagefristenOverholdt,
            erKlagenSignert = erKlagenSignert,
        )
    }
}

fun KlageFormkrav.toDbJson(): String {
    return KlagebehandlingFormkravDbJson(
        vedtakDetKlagesPå = vedtakDetKlagesPå?.toString(),
        erKlagerPartISaken = erKlagerPartISaken,
        klagesDetPåKonkreteElementerIVedtaket = klagesDetPåKonkreteElementerIVedtaket,
        erKlagefristenOverholdt = erKlagefristenOverholdt,
        erKlagenSignert = erKlagenSignert,
    ).let { serialize(it) }
}

fun String.toKlageFormkrav(): KlageFormkrav {
    return deserialize<KlagebehandlingFormkravDbJson>(this).toDomain()
}
