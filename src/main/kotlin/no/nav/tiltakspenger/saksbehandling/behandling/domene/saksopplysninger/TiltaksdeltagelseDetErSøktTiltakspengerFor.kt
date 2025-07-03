package no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger

import no.nav.tiltakspenger.saksbehandling.søknad.Søknadstiltak

/**
 * Wrapperklasse for [Søknadstiltak] og kravtidspunkt.
 * Brukes for å avgjøre perioden vi skal hente saksopplysninger for i behandlinger (behandlingsgrunnlag).
 */
data class TiltaksdeltagelseDetErSøktTiltakspengerFor(
    val søknadstiltak: Søknadstiltak,
    val kravdato: LocalDate,
)
