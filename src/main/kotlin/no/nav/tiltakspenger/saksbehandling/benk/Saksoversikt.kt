package no.nav.tiltakspenger.saksbehandling.benk

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Ulid
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import java.time.LocalDateTime

/**
 * En oversikt over flere søknader og behandlinger på tvers av saker.
 */
data class Saksoversikt(
    val behandlinger: List<BehandlingEllerSøknadForSaksoversikt>,
) : List<BehandlingEllerSøknadForSaksoversikt> by behandlinger {
    fun filter(fn: (BehandlingEllerSøknadForSaksoversikt) -> Boolean): Saksoversikt {
        return Saksoversikt(behandlinger.filter(fn))
    }
}

/**
 * @property id Vi har ikke en fellestype for behandlingId og søknadId, så vi bruker Ulid. Hvis ikke må vi endre denne til en sealed interface.
 * @property kravtidspunkt settes kun for søknad og førstegangsbehandling
 */
data class BehandlingEllerSøknadForSaksoversikt(
    val periode: Periode?,
    val status: Status,
    val underkjent: Boolean?,
    val kravtidspunkt: LocalDateTime?,
    val behandlingstype: BenkBehandlingstype,
    val fnr: Fnr,
    val saksnummer: Saksnummer,
    val id: Ulid,
    val saksbehandler: String?,
    val beslutter: String?,
    val sakId: SakId,
    val opprettet: LocalDateTime,
) {
    sealed interface Status {
        data object Søknad : Status

        data class Behandling(
            val behandlingsstatus: Behandlingsstatus,
        ) : Status
    }
}
