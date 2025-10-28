package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.RammebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilBehandlingerDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.MeldeperiodeKjedeDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.toMeldeperiodeKjederDTO
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.infra.route.SøknadDTO
import no.nav.tiltakspenger.saksbehandling.søknad.infra.route.toSøknadDTO
import java.time.Clock
import java.time.LocalDate

/**
 * @property førsteDagSomGirRett Dersom vi ikke har vedtak vil denne være null.
 */
data class SakDTO(
    val saksnummer: String,
    val sakId: String,
    val fnr: String,
    val behandlingsoversikt: List<SaksoversiktDTO>,
    val meldeperiodeKjeder: List<MeldeperiodeKjedeDTO>,
    val førsteDagSomGirRett: LocalDate?,
    val sisteDagSomGirRett: LocalDate?,
    val søknader: List<SøknadDTO>,
    val behandlinger: List<RammebehandlingDTO>,
    val tidslinje: List<RammevedtakDTO>,
    val utbetalingstidslinje: List<UtbetalingstidslinjeMeldeperiodeDTO>,
    val kanSendeInnHelgForMeldekort: Boolean,
)

fun Sak.toSakDTO(clock: Clock) = SakDTO(
    saksnummer = saksnummer.verdi,
    sakId = id.toString(),
    fnr = fnr.verdi,
    behandlingsoversikt = (
        rammebehandlinger.åpneBehandlinger.toSaksoversiktDTO() +
            this.søknader
                .filter { soknad ->
                    !soknad.erAvbrutt && rammebehandlinger.søknadsbehandlinger.none { it.søknad.id == soknad.id }
                }
                .toSaksoversiktDTO()
        ).sortedBy { it.opprettet },
    meldeperiodeKjeder = toMeldeperiodeKjederDTO(clock = clock),
    førsteDagSomGirRett = førsteDagSomGirRett,
    sisteDagSomGirRett = sisteDagSomGirRett,
    søknader = søknader.toSøknadDTO(),
    behandlinger = this.tilBehandlingerDTO(),
    tidslinje = rammevedtaksliste.tidslinje.perioderMedVerdi.map { it.tilPeriodisertRammevedtakDTO() },
    utbetalingstidslinje = this.tilUtbetalingstidslinjeMeldeperiodeDTO(),
    kanSendeInnHelgForMeldekort = kanSendeInnHelgForMeldekort,
)
