package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.RammebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilBehandlingerDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.MeldeperiodeKjedeDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.toMeldeperiodeKjederDTO
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.vedtak.infra.route.RammevedtakDTO
import no.nav.tiltakspenger.saksbehandling.vedtak.infra.route.TidslinjeDTO
import no.nav.tiltakspenger.saksbehandling.vedtak.infra.route.tilRammevedtakDTO
import no.nav.tiltakspenger.saksbehandling.vedtak.infra.route.tilRammevedtakTidslinjeDTO
import java.time.Clock
import java.time.LocalDate

/**
 * @property førsteDagSomGirRett Dersom vi ikke har vedtak vil denne være null.
 */
data class SakDTO(
    val saksnummer: String,
    val sakId: String,
    val fnr: String,
    val åpneBehandlinger: List<ÅpenBehandlingDTO>,
    val meldeperiodeKjeder: List<MeldeperiodeKjedeDTO>,
    val førsteDagSomGirRett: LocalDate?,
    val sisteDagSomGirRett: LocalDate?,
    val behandlinger: List<RammebehandlingDTO>,
    val tidslinje: TidslinjeDTO,
    val alleRammevedtak: List<RammevedtakDTO>,
    val utbetalingstidslinje: List<UtbetalingstidslinjeMeldeperiodeDTO>,
    val kanSendeInnHelgForMeldekort: Boolean,
)

fun Sak.toSakDTO(clock: Clock) = SakDTO(
    saksnummer = saksnummer.verdi,
    sakId = id.toString(),
    fnr = fnr.verdi,
    åpneBehandlinger = tilÅpneBehandlingerDTO(),
    meldeperiodeKjeder = toMeldeperiodeKjederDTO(clock = clock),
    førsteDagSomGirRett = førsteDagSomGirRett,
    sisteDagSomGirRett = sisteDagSomGirRett,
    behandlinger = this.tilBehandlingerDTO(),
    tidslinje = rammevedtaksliste.tilRammevedtakTidslinjeDTO(),
    alleRammevedtak = rammevedtaksliste.map { it.tilRammevedtakDTO() },
    utbetalingstidslinje = this.tilUtbetalingstidslinjeMeldeperiodeDTO(),
    kanSendeInnHelgForMeldekort = kanSendeInnHelgForMeldekort,
)
