package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.utsjekk.kontrakter.felles

import com.fasterxml.jackson.annotation.JsonCreator

sealed interface StønadType {
    fun tilFagsystem(): Fagsystem

    companion object {
        @JsonCreator
        @JvmStatic
        fun deserialize(json: String): StønadType? {
            val stønadstypeTiltakspenger = StønadTypeTiltakspenger.entries.find { it.name == json }
            return stønadstypeTiltakspenger
        }
    }
}

enum class StønadTypeTiltakspenger : StønadType {
    ARBEIDSFORBEREDENDE_TRENING,
    ARBEIDSRETTET_REHABILITERING,
    ARBEIDSTRENING,
    AVKLARING,
    DIGITAL_JOBBKLUBB,
    ENKELTPLASS_AMO,
    ENKELTPLASS_VGS_OG_HØYERE_YRKESFAG,
    FORSØK_OPPLÆRING_LENGRE_VARIGHET,
    GRUPPE_AMO,
    GRUPPE_VGS_OG_HØYERE_YRKESFAG,
    HØYERE_UTDANNING,
    INDIVIDUELL_JOBBSTØTTE,
    INDIVIDUELL_KARRIERESTØTTE_UNG,
    JOBBKLUBB,
    OPPFØLGING,
    UTVIDET_OPPFØLGING_I_NAV,
    UTVIDET_OPPFØLGING_I_OPPLÆRING,
    ;

    override fun tilFagsystem(): Fagsystem = Fagsystem.TILTAKSPENGER
}
