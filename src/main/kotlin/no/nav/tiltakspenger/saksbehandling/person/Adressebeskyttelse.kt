package no.nav.tiltakspenger.saksbehandling.person

/** Graderingen av adressen til en person, med de samme verdiene som i PDL. */
enum class Adressebeskyttelse {
    STRENGT_FORTROLIG_UTLAND,
    STRENGT_FORTROLIG,
    FORTROLIG,
    UGRADERT,
    ;

    val erGradert: Boolean get() = this != UGRADERT
    val erKode6: Boolean get() = this == STRENGT_FORTROLIG || this == STRENGT_FORTROLIG_UTLAND
    val erKode7: Boolean get() = this == FORTROLIG
}
