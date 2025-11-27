package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.RammebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilBehandlingerDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.MeldeperiodeKjedeDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.toMeldeperiodeKjederDTO
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.vedtak.infra.route.RammevedtakDTO
import no.nav.tiltakspenger.saksbehandling.vedtak.infra.route.TidslinjeDTO
import no.nav.tiltakspenger.saksbehandling.vedtak.infra.route.tilRammevedtakDTO
import no.nav.tiltakspenger.saksbehandling.vedtak.infra.route.tilRammevedtakInnvilgetTidslinjeDTO
import no.nav.tiltakspenger.saksbehandling.vedtak.infra.route.tilRammevedtakTidslinjeDTO
import java.time.Clock
import java.time.LocalDate

/**
 * @property førsteDagSomGirRett Dersom vi ikke har en innvilget gjeldende periode, vil denne være null.
 * @property sisteDagSomGirRett Dersom vi ikke har en innvilget gjeldende periode, vil denne være null.
 * @property tidslinje Tidslinje med alle gjeldende rammevedtak. Avslag er aldri gjeldende.
 * @property innvilgetTidslinje Tidslinje med alle gjeldende innvilgede rammevedtak. Avslag, stans og rene opphør er aldri innvilgede.
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
    val innvilgetTidslinje: TidslinjeDTO,
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
    innvilgetTidslinje = rammevedtaksliste.tilRammevedtakInnvilgetTidslinjeDTO(),
    alleRammevedtak = rammevedtaksliste.map { it.tilRammevedtakDTO() },
    utbetalingstidslinje = this.tilUtbetalingstidslinjeMeldeperiodeDTO(),
    kanSendeInnHelgForMeldekort = kanSendeInnHelgForMeldekort,
)
