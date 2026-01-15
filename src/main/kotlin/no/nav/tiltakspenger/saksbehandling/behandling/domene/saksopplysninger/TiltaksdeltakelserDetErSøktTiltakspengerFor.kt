package no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger

import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadstiltak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import java.time.LocalDateTime

/**
 * Oversikt over hvilke tiltaksdeltakelser en bruker har søkt på, sortert på kravtidspunktet.
 * Trenger ikke være sammenhengende.
 * Kan inneholde overlappende tiltaksdeltakelser, men de vil ha forskjellige kravtidspunkt.
 * Slik som de så ut på søknadstidspunktet.
 *
 * Brukes for å utlede hvilke tiltaksdeltakelser som er relevante for en behandling.
 */
data class TiltaksdeltakelserDetErSøktTiltakspengerFor(
    val value: List<TiltaksdeltakelseDetErSøktTiltakspengerFor>,
) : List<TiltaksdeltakelseDetErSøktTiltakspengerFor> by value {

    constructor(value: TiltaksdeltakelseDetErSøktTiltakspengerFor) : this(listOf(value))
    constructor(søknadstiltak: Søknadstiltak, kravtidspunkt: LocalDateTime) :
        this(listOf(TiltaksdeltakelseDetErSøktTiltakspengerFor(søknadstiltak, kravtidspunkt)))

    init {
        value.zipWithNext { a, b -> a.kravtidspunkt <= b.kravtidspunkt }
    }

    /** Intern tiltaksdeltakerId. */
    val ider: List<TiltaksdeltakerId> by lazy { value.map { it.søknadstiltak.tiltaksdeltakerId }.distinct() }

    /** Ekstern tiltaksdeltakelse-id. Uavhengig av kildesystem. */
    val eksterneIder: List<String> by lazy { value.map { it.søknadstiltak.id }.distinct() }

    companion object {
        fun empty() = TiltaksdeltakelserDetErSøktTiltakspengerFor(emptyList())
    }
}
