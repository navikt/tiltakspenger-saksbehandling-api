package no.nav.tiltakspenger.vedtak.routes.sak

import no.nav.tiltakspenger.vedtak.routes.behandling.dto.BehandlingDTO
import no.nav.tiltakspenger.vedtak.routes.behandling.dto.SøknadDTO
import no.nav.tiltakspenger.vedtak.routes.behandling.dto.toDTO
import no.nav.tiltakspenger.vedtak.routes.meldekort.dto.MeldeperiodeKjedeDTO
import no.nav.tiltakspenger.vedtak.routes.meldekort.dto.toMeldeperiodeKjederDTO
import no.nav.tiltakspenger.vedtak.saksbehandling.domene.sak.Sak
import java.time.LocalDate

/**
 * @property førsteLovligeStansdato Dersom vi ikke har vedtak vil denne være null. Hvis vi ikke har utbetalt, vil den være første dag i saksperioden. Dersom vi har utbetalt, vil den være dagen etter siste utbetalte dag.
 */
internal data class SakDTO(
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

internal fun Sak.toDTO() = SakDTO(
    saksnummer = saksnummer.verdi,
    sakId = id.toString(),
    fnr = fnr.verdi,
    behandlingsoversikt = (
        behandlinger.hentÅpneBehandlinger().toSaksoversiktDTO() +
            this.soknader.filterNot { it.erAvbrutt }
                .filter { soknad -> behandlinger.none { it.søknad?.id == soknad.id } }
                .toSaksoversiktDTO()
        ).sortedBy { it.opprettet },
    meldeperiodeKjeder = toMeldeperiodeKjederDTO(),
    førsteLovligeStansdato = førsteLovligeStansdato(),
    sisteDagSomGirRett = sisteDagSomGirRett,
    søknader = soknader.toDTO(),
    behandlinger = behandlinger.toDTO(),
)
