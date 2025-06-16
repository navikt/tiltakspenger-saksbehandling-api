package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.BehandlingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilBehandlingerDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.MeldeperiodeKjedeDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.toMeldeperiodeKjederDTO
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.infra.route.SøknadDTO
import no.nav.tiltakspenger.saksbehandling.søknad.infra.route.toSøknadDTO
import java.time.Clock
import java.time.LocalDate

/**
 * @property førsteLovligeStansdato Dersom vi ikke har vedtak vil denne være null. Hvis vi ikke har utbetalt, vil den være første dag i saksperioden. Dersom vi har utbetalt, vil den være dagen etter siste utbetalte dag.
 */
data class SakDTO(
    val saksnummer: String,
    val sakId: String,
    val fnr: String,
    val behandlingsoversikt: List<SaksoversiktDTO>,
    val meldeperiodeKjeder: List<MeldeperiodeKjedeDTO>,
    val førsteLovligeStansdato: LocalDate?,
    val sisteDagSomGirRett: LocalDate?,
    val søknader: List<SøknadDTO>,
    val behandlinger: List<BehandlingDTO>,
)

fun Sak.toSakDTO(clock: Clock) = SakDTO(
    saksnummer = saksnummer.verdi,
    sakId = id.toString(),
    fnr = fnr.verdi,
    behandlingsoversikt = (
        behandlinger.hentÅpneBehandlinger().toSaksoversiktDTO() +
            this.soknader
                .filter { soknad ->
                    !soknad.erAvbrutt && behandlinger.søknadsbehandlinger.none { it.søknad.id == soknad.id }
                }
                .toSaksoversiktDTO()
        ).sortedBy { it.opprettet },
    meldeperiodeKjeder = toMeldeperiodeKjederDTO(clock = clock),
    førsteLovligeStansdato = førsteLovligeStansdato(),
    sisteDagSomGirRett = sisteDagSomGirRett,
    søknader = soknader.toSøknadDTO(),
    behandlinger = behandlinger.tilBehandlingerDTO(),
)
