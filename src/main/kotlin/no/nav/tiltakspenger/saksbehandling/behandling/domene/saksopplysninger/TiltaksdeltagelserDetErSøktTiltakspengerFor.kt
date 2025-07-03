package no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger

/**
 * Oversikt over hvilke tiltaksdeltagelser en bruker har søkt på, sortert på kravtidspunktet.
 * Trenger ikke være sammenhengende.
 * Kan inneholde overlappende tiltaksdeltagelser, men de vil ha forskjellige kravtidspunkt.
 * Slik som de så ut på søknadstidspunktet.
 *
 * Brukes for å utlede hvilke tiltaksdeltagelser som er relevante for en behandling.
 */
data class TiltaksdeltagelserDetErSøktTiltakspengerFor(
    val value: List<TiltaksdeltagelseDetErSøktTiltakspengerFor>,
) : List<TiltaksdeltagelseDetErSøktTiltakspengerFor> by value {

    init {
        value.zipWithNext { a, b -> a.kravtidspunkt <= b.kravtidspunkt }
    }

    /** TiltaksdeltagelseIden. Uavhengig av kildesystem. */
    val ider: List<String> by lazy { value.map { it.søknadstiltak.id }.distinct() }
}
