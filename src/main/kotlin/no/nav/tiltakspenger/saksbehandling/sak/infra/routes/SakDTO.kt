package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.BehandlingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilBehandlingerDTO
import no.nav.tiltakspenger.saksbehandling.beregning.infra.dto.BeløpDTO
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
    val behandlinger: List<BehandlingDTO>,
    val tidslinje: List<RammevedtakDTO>,
    val utbetalingstidslinje: List<UtbetalingstidslinjeMeldeperiodeDTO>,
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
    førsteDagSomGirRett = førsteDagSomGirRett,
    sisteDagSomGirRett = sisteDagSomGirRett,
    søknader = soknader.toSøknadDTO(),
    behandlinger = this.tilBehandlingerDTO(),
    tidslinje = vedtaksliste.tidslinje.perioderMedVerdi.map { it.tilPeriodisertRammevedtakDTO() },
    utbetalingstidslinje = utbetalinger.tidslinje.perioderMedVerdi.flatMap { (utbetaling, periode) ->
        utbetaling.beregning.beregninger
            .filter { it.periode.overlapperMed(periode) }
            .map { meldeperiodeberegning ->
                UtbetalingstidslinjeMeldeperiodeDTO(
                    kjedeId = meldeperiodeberegning.kjedeId.verdi,
                    periode = meldeperiodeberegning.periode.toDTO(),
                    beløp = BeløpDTO(
                        totalt = meldeperiodeberegning.totalBeløp,
                        ordinært = meldeperiodeberegning.ordinærBeløp,
                        barnetillegg = meldeperiodeberegning.barnetilleggBeløp,
                    ),
                )
            }
    },
)
