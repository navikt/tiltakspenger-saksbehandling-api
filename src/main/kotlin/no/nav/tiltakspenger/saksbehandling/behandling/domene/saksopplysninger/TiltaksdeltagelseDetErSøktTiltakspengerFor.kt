package no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger

import no.nav.tiltakspenger.saksbehandling.søknad.Søknadstiltak
import java.time.LocalDateTime

/**
 * Wrapperklasse for [Søknadstiltak] og kravtidspunkt.
 * Brukes for å avgjøre perioden vi skal hente saksopplysninger for i behandlinger (behandlingsgrunnlag).
 */
data class TiltaksdeltagelseDetErSøktTiltakspengerFor(
    val søknadstiltak: Søknadstiltak,
    val kravtidspunkt: LocalDateTime,
)
