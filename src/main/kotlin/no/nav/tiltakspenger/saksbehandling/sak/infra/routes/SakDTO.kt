package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.RammebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilBehandlingerDTO
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.KlagebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.KlagevedtakDTO
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.tilKlagebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.tilKlagevedtakDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.MeldekortbehandlingDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.MeldekortvedtakDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.MeldeperiodeKjedeDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.tilMeldekortbehandlingDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.tilMeldeperiodeKjederDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.toDto
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.infra.route.SøknadDTO
import no.nav.tiltakspenger.saksbehandling.søknad.infra.route.toSøknadDTO
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.route.dto.TilbakekrevingBehandlingDTO
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.route.dto.tilTilbakekrevingBehandlingDTO
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
 * @property tidslinje Tidslinje med alle gjeldende rammevedtak.
 * Avslag er aldri gjeldende.
 * @property innvilgetTidslinje Tidslinje med alle gjeldende innvilgede rammevedtak.
 * Avslag, stans og rene opphør er aldri innvilgede.
 */
data class SakDTO(
    val sakId: String,
    val saksnummer: String,
    val fnr: String,

    val førsteDagSomGirRett: LocalDate?,
    val sisteDagSomGirRett: LocalDate?,
    val kanSendeInnHelgForMeldekort: Boolean,

    val søknader: List<SøknadDTO>,

    val åpneBehandlinger: List<ÅpenBehandlingDTO>,

    val rammebehandlinger: List<RammebehandlingDTO>,
    val klagebehandlinger: List<KlagebehandlingDTO>,
    val tilbakekrevinger: List<TilbakekrevingBehandlingDTO>,

    val alleRammevedtak: List<RammevedtakDTO>,
    val alleKlagevedtak: List<KlagevedtakDTO>,

    val meldekortbehandlinger: Map<String, MeldekortbehandlingDTO>,
    val meldekortvedtak: List<MeldekortvedtakDTO>,
    val meldeperiodeKjeder: List<MeldeperiodeKjedeDTO>,

    val tidslinje: TidslinjeDTO,
    val innvilgetTidslinje: TidslinjeDTO,
    val utbetalingstidslinje: List<UtbetalingstidslinjeMeldeperiodeDTO>,
)

fun Sak.toSakDTO(saksbehandler: Saksbehandler, clock: Clock) = SakDTO(
    sakId = this.id.toString(),
    saksnummer = this.saksnummer.verdi,
    fnr = this.fnr.verdi,

    førsteDagSomGirRett = this.førsteDagSomGirRett,
    sisteDagSomGirRett = this.sisteDagSomGirRett,
    kanSendeInnHelgForMeldekort = this.kanSendeInnHelgForMeldekort,

    søknader = this.søknader.map { it.toSøknadDTO() },

    åpneBehandlinger = this.tilÅpneBehandlingerDTO(),

    rammebehandlinger = this.tilBehandlingerDTO(saksbehandler),
    klagebehandlinger = this.behandlinger.klagebehandlinger.map { it.tilKlagebehandlingDTO() },
    tilbakekrevinger = this.tilbakekrevinger.map {
        it.tilTilbakekrevingBehandlingDTO(utbetalinger.hentUtbetaling(it.utbetalingId)!!, saksbehandler)
    },

    alleRammevedtak = this.rammevedtaksliste.map { it.tilRammevedtakDTO() },
    alleKlagevedtak = this.klagevedtaksliste.map { it.tilKlagevedtakDTO() },

    meldekortbehandlinger = this.meldekortbehandlinger.associate {
        it.id.toString() to it.tilMeldekortbehandlingDTO(
            beregninger = this.meldeperiodeBeregninger,
            hentVedtak = this.meldekortvedtaksliste::hentForMeldekortbehandling,
            hentTilbakekreving = this::hentTilbakekrevingForMeldekortbehandling,
            kallendeSaksbehandler = saksbehandler,
        )
    },
    meldekortvedtak = this.vedtaksliste.meldekortvedtaksliste.toDto(),
    meldeperiodeKjeder = this.tilMeldeperiodeKjederDTO(clock),

    tidslinje = this.rammevedtaksliste.tilRammevedtakTidslinjeDTO(),
    innvilgetTidslinje = this.rammevedtaksliste.tilRammevedtakInnvilgetTidslinjeDTO(),
    utbetalingstidslinje = this.tilUtbetalingstidslinjeMeldeperiodeDTO(),
)
