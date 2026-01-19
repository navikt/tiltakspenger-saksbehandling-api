package no.nav.tiltakspenger.saksbehandling.klage.infra.route

import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.KlagefristUnntakSvarord

enum class KlagefristUnntakSvarordDto {
    JA,
    JA_AV_SÆRLIGE_GRUNNER,
    JA_KLAGER_KAN_IKKE_LASTES_FOR_Å_HA_SENDT_INN_ETTER_FRISTEN,
    NEI,
    ;

    fun toDomain(): KlagefristUnntakSvarord = when (this) {
        JA -> KlagefristUnntakSvarord.JA
        JA_AV_SÆRLIGE_GRUNNER -> KlagefristUnntakSvarord.JA_AV_SÆRLIGE_GRUNNER
        JA_KLAGER_KAN_IKKE_LASTES_FOR_Å_HA_SENDT_INN_ETTER_FRISTEN -> KlagefristUnntakSvarord.JA_KLAGER_KAN_IKKE_LASTES_FOR_Å_HA_SENDT_INN_ETTER_FRISTEN
        NEI -> KlagefristUnntakSvarord.NEI
    }
}
