package no.nav.tiltakspenger.saksbehandling.klage.infra.repo

import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.KlageFormkrav
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.KlagefristUnntakSvarord

private data class KlagebehandlingFormkravDbJson(
    val vedtakDetKlagesPå: String?,
    val erKlagerPartISaken: Boolean,
    val klagesDetPåKonkreteElementerIVedtaket: Boolean,
    val erKlagefristenOverholdt: Boolean,
    val erUnntakForKlagefrist: KlagefristUnntakSvarordDb?,
    val erKlagenSignert: Boolean,
) {
    fun toDomain(): KlageFormkrav {
        return KlageFormkrav(
            vedtakDetKlagesPå = vedtakDetKlagesPå?.let { VedtakId.fromString(it) },
            erKlagerPartISaken = erKlagerPartISaken,
            klagesDetPåKonkreteElementerIVedtaket = klagesDetPåKonkreteElementerIVedtaket,
            erKlagefristenOverholdt = erKlagefristenOverholdt,
            erUnntakForKlagefrist = erUnntakForKlagefrist?.toDomain(),
            erKlagenSignert = erKlagenSignert,
        )
    }
}

enum class KlagefristUnntakSvarordDb {
    JA_KLAGER_KAN_IKKE_LASTES_FOR_Å_HA_SENDT_INN_ETTER_FRISTEN,
    JA_AV_SÆRLIGE_GRUNNER,
    NEI,
    ;

    fun toDomain(): KlagefristUnntakSvarord =
        when (this) {
            JA_KLAGER_KAN_IKKE_LASTES_FOR_Å_HA_SENDT_INN_ETTER_FRISTEN -> KlagefristUnntakSvarord.JA_KLAGER_KAN_IKKE_LASTES_FOR_Å_HA_SENDT_INN_ETTER_FRISTEN
            JA_AV_SÆRLIGE_GRUNNER -> KlagefristUnntakSvarord.JA_AV_SÆRLIGE_GRUNNER
            NEI -> KlagefristUnntakSvarord.NEI
        }

    companion object {
        fun toDbDto(svarord: KlagefristUnntakSvarord): KlagefristUnntakSvarordDb =
            when (svarord) {
                KlagefristUnntakSvarord.JA_KLAGER_KAN_IKKE_LASTES_FOR_Å_HA_SENDT_INN_ETTER_FRISTEN -> JA_KLAGER_KAN_IKKE_LASTES_FOR_Å_HA_SENDT_INN_ETTER_FRISTEN
                KlagefristUnntakSvarord.JA_AV_SÆRLIGE_GRUNNER -> JA_AV_SÆRLIGE_GRUNNER
                KlagefristUnntakSvarord.NEI -> NEI
            }
    }
}

fun KlageFormkrav.toDbJson(): String {
    return KlagebehandlingFormkravDbJson(
        vedtakDetKlagesPå = vedtakDetKlagesPå?.toString(),
        erKlagerPartISaken = erKlagerPartISaken,
        klagesDetPåKonkreteElementerIVedtaket = klagesDetPåKonkreteElementerIVedtaket,
        erKlagefristenOverholdt = erKlagefristenOverholdt,
        erUnntakForKlagefrist = erUnntakForKlagefrist?.let { KlagefristUnntakSvarordDb.toDbDto(it) },
        erKlagenSignert = erKlagenSignert,
    ).let { serialize(it) }
}

fun String.toKlageFormkrav(): KlageFormkrav {
    return deserialize<KlagebehandlingFormkravDbJson>(this).toDomain()
}
