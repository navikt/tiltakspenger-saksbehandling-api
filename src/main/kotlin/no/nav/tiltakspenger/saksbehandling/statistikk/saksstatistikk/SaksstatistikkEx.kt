package no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk

fun maskerHvisStrengtFortrolig(
    erStrengtFortrolig: Boolean,
    verdi: String?,
): String? {
    return if (verdi != null) {
        return maskerHvisStrengtFortroligStrict(erStrengtFortrolig, verdi)
    } else {
        verdi
    }
}

fun maskerHvisStrengtFortroligStrict(
    erStrengtFortrolig: Boolean,
    verdi: String,
): String {
    return if (erStrengtFortrolig) {
        "-5"
    } else {
        verdi
    }
}
