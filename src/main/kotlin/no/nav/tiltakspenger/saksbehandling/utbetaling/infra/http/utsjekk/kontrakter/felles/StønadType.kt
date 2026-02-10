package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.utsjekk.kontrakter.felles

import com.fasterxml.jackson.annotation.JsonCreator

sealed interface StønadType {
    fun tilFagsystem(): Fagsystem

    companion object {
        @JsonCreator
        @JvmStatic
        fun deserialize(json: String): StønadType? {
            val stønadstypeDagpenger = StønadTypeDagpenger.entries.find { it.name == json }
            val stønadstypeTiltakspenger = StønadTypeTiltakspenger.entries.find { it.name == json }
            val stønadstypeTilleggsstønader = StønadTypeTilleggsstønader.entries.find { it.name == json }
            return stønadstypeDagpenger ?: stønadstypeTiltakspenger ?: stønadstypeTilleggsstønader
        }
    }
}

enum class StønadTypeDagpenger : StønadType {
    DAGPENGER_ARBEIDSSØKER_ORDINÆR,
    DAGPENGER_PERMITTERING_ORDINÆR,
    DAGPENGER_PERMITTERING_FISKEINDUSTRI,
    DAGPENGER_EØS,
    ;

    override fun tilFagsystem(): Fagsystem = Fagsystem.DAGPENGER
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

enum class StønadTypeTilleggsstønader : StønadType {
    TILSYN_BARN_ENSLIG_FORSØRGER,
    TILSYN_BARN_AAP,
    TILSYN_BARN_ETTERLATTE,
    LÆREMIDLER_ENSLIG_FORSØRGER,
    LÆREMIDLER_AAP,
    LÆREMIDLER_ETTERLATTE,
    ;

    override fun tilFagsystem(): Fagsystem = Fagsystem.TILLEGGSSTØNADER
}

enum class StønadTypeAAP : StønadType {
    AAP_UNDER_ARBEIDSAVKLARING,
    ;

    override fun tilFagsystem(): Fagsystem = Fagsystem.AAP
}
