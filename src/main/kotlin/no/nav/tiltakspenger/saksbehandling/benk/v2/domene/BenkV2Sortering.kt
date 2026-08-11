package no.nav.tiltakspenger.saksbehandling.benk.v2.domene

/**
 * Sorteringen benken ber om, som «kolonne,retning».
 * Kolonnenavnene er benkens egne, og hver fane har sitt eget sett — se enumene under.
 */
data class BenkV2Sortering<K : BenkV2SorteringKolonne>(
    val kolonne: K,
    val retning: BenkV2SorteringRetning,
)

enum class BenkV2SorteringRetning {
    ASC,
    DESC,
    ;

    companion object {
        fun fromString(retning: String?): BenkV2SorteringRetning =
            entries.firstOrNull { it.name.equals(retning, ignoreCase = true) } ?: ASC
    }
}

/**
 * Kolonnene fanene kan sorteres på.
 * [verdi] er strengen frontend sender, og den er en del av API-et: en omdøping her er en API-endring.
 */
sealed interface BenkV2SorteringKolonne {
    val verdi: String
}

enum class BenkSøknaderKolonne(override val verdi: String) : BenkV2SorteringKolonne {
    FNR("fnr"),
    SØKNADSTYPE("søknadstype"),
    STATUS("status"),
    KRAVTIDSPUNKT("kravtidspunkt"),
    RESULTAT("resultat"),
    SIST_ENDRET("sist_endret"),
    SAKSBEHANDLER("saksbehandler"),
    BESLUTTER("beslutter"),
    VENTESTATUS_FRIST("ventestatus_frist"),
}

enum class BenkRevurderingerKolonne(override val verdi: String) : BenkV2SorteringKolonne {
    FNR("fnr"),
    RESULTAT("resultat"),
    STATUS("status"),
    STARTET("startet"),
    SIST_ENDRET("sist_endret"),
    SAKSBEHANDLER("saksbehandler"),
    BESLUTTER("beslutter"),
    VENTESTATUS_FRIST("ventestatus_frist"),
}

enum class BenkMeldekortKolonne(override val verdi: String) : BenkV2SorteringKolonne {
    FNR("fnr"),
    TYPE("type"),
    PERIODE("periode"),
    BELØP("beløp"),
    STATUS("status"),
    MOTTATT("mottatt"),
    SAKSBEHANDLER("saksbehandler"),
    VENTESTATUS_FRIST("ventestatus_frist"),
}

enum class BenkKlageKolonne(override val verdi: String) : BenkV2SorteringKolonne {
    FNR("fnr"),
    RESULTAT("resultat"),
    STATUS("status"),
    KRAVTIDSPUNKT("kravtidspunkt"),
    SIST_ENDRET("sist_endret"),
    SAKSBEHANDLER("saksbehandler"),
    BESLUTTER("beslutter"),
    VENTESTATUS_FRIST("ventestatus_frist"),
}

enum class BenkTilbakekrevingKolonne(override val verdi: String) : BenkV2SorteringKolonne {
    FNR("fnr"),
    BELØP("beløp"),
    KILDE("kilde"),
    STATUS("status"),
    STARTET("startet"),
    SIST_ENDRET("sist_endret"),
    SAKSBEHANDLER("saksbehandler"),
    VENTESTATUS_FRIST("ventestatus_frist"),
    KRAVGRUNNLAG_PERIODE("kravgrunnlag_periode"),
}

/**
 * Parser «kolonne,retning».
 * En ukjent kolonne faller tilbake på [default] framfor å feile, fordi sorteringen kommer fra en url brukeren kan redigere.
 */
fun <K : BenkV2SorteringKolonne> String?.tilSortering(
    kolonner: List<K>,
    default: K,
): BenkV2Sortering<K> {
    val deler = this.orEmpty().split(",")
    return BenkV2Sortering(
        kolonne = kolonner.firstOrNull { it.verdi.equals(deler.getOrNull(0), ignoreCase = true) } ?: default,
        retning = BenkV2SorteringRetning.fromString(deler.getOrNull(1)),
    )
}
