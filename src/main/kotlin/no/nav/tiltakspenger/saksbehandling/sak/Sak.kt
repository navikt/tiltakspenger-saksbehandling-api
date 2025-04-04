package no.nav.tiltakspenger.saksbehandling.sak

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.libs.common.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlinger
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlinger
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.søknad.Søknad
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalinger
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsvedtak
import no.nav.tiltakspenger.saksbehandling.vedtak.Vedtaksliste
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters

data class Sak(
    val id: SakId,
    val fnr: Fnr,
    val saksnummer: Saksnummer,
    val behandlinger: Behandlinger,
    val vedtaksliste: Vedtaksliste,
    val meldekortBehandlinger: MeldekortBehandlinger,
    val meldeperiodeKjeder: MeldeperiodeKjeder,
    val brukersMeldekort: List<BrukersMeldekort>,
    val utbetalinger: Utbetalinger,
    val soknader: List<Søknad>,
) {
    val meldeperiodeBeregninger = meldekortBehandlinger.meldeperiodeBeregninger

    /** Nåtilstand. Tar utgangspunkt i tidslinja på saken og henter den siste innvilget dagen. */
    val førsteDagSomGirRett = vedtaksliste.førsteDagSomGirRett

    /** Nåtilstand. Tar utgangspunkt i tidslinja på saken og henter den siste innvilget dagen. */
    @Suppress("unused")
    val sisteDagSomGirRett = vedtaksliste.sisteDagSomGirRett

    val revurderinger = behandlinger.revurderinger

    /** Henter fra siste godkjente meldekort */
    @Suppress("unused")
    val sisteNavkontor: Navkontor? by lazy {
        meldekortBehandlinger.sisteGodkjenteMeldekort?.navkontor
    }

    val barnetilleggsperioder: Periodisering<AntallBarn?> by lazy { vedtaksliste.barnetilleggsperioder }

    val tiltakstypeperioder: Periodisering<TiltakstypeSomGirRett?> by lazy { vedtaksliste.tiltakstypeperioder }

    fun hentMeldekortBehandling(meldekortId: MeldekortId): MeldekortBehandling? {
        return meldekortBehandlinger.hentMeldekortBehandling(meldekortId)
    }

    /** Flere behandlinger kan være knyttet til samme versjon av meldeperioden. */
    @Suppress("unused")
    fun hentMeldekortBehandlingerForMeldeperiode(meldeperiodeId: MeldeperiodeId): List<MeldekortBehandling> {
        return meldekortBehandlinger.hentMeldekortBehandlingerForMeldeperiode(meldeperiodeId)
    }

    fun hentSisteMeldekortBehandlingForKjede(id: MeldeperiodeKjedeId): MeldekortBehandling? {
        return meldekortBehandlinger.hentSisteMeldekortBehandlingForKjede(id)
    }

    fun hentSisteMeldeperiodeForKjede(kjedeId: MeldeperiodeKjedeId): Meldeperiode {
        return meldeperiodeKjeder.hentSisteMeldeperiodeForKjede(kjedeId)
    }
    fun hentBehandling(behandlingId: BehandlingId): Behandling? = behandlinger.hentBehandling(behandlingId)

    fun sisteUtbetalteMeldekortDag(): LocalDate? = meldekortBehandlinger.sisteUtbetalteMeldekortDag

    fun førsteLovligeStansdato(): LocalDate? {
        val innvilgelsesperioder = this.vedtaksliste.innvilgelsesperioder
        if (innvilgelsesperioder.isEmpty()) return null
        val førsteDagSomIkkeErUtbetalt =
            sisteUtbetalteMeldekortDag()?.plusDays(1) ?: innvilgelsesperioder.first().fraOgMed

        return innvilgelsesperioder.firstOrNull {
            it.tilOgMed >= førsteDagSomIkkeErUtbetalt
        }?.tilDager()?.firstOrNull {
            it >= førsteDagSomIkkeErUtbetalt
        }
    }

    fun erSisteVersjonAvMeldeperiode(meldeperiode: Meldeperiode): Boolean {
        return meldeperiodeKjeder.erSisteVersjonAvMeldeperiode(meldeperiode)
    }

    fun finnNærmesteMeldeperiode(dato: LocalDate): Periode = meldeperiodeKjeder.finnNærmesteMeldeperiode(dato)

    fun avbrytSøknadOgBehandling(
        command: no.nav.tiltakspenger.saksbehandling.behandling.service.avslutt.AvbrytSøknadOgBehandlingCommand,
        avbruttTidspunkt: LocalDateTime,
    ): Triple<Sak, Søknad?, Behandling?> {
        if (command.søknadId != null && command.behandlingId != null) {
            return avbrytBehandling(command, avbruttTidspunkt)
        }
        if (command.søknadId != null) {
            val (oppdatertSak, avbruttSøknad) = avbrytSøknad(command, avbruttTidspunkt)
            return Triple(oppdatertSak, avbruttSøknad, null)
        }

        return avbrytBehandling(command, avbruttTidspunkt)
    }

    private fun avbrytBehandling(
        command: no.nav.tiltakspenger.saksbehandling.behandling.service.avslutt.AvbrytSøknadOgBehandlingCommand,
        avbruttTidspunkt: LocalDateTime,
    ): Triple<Sak, Søknad?, Behandling> {
        val behandling = this.hentBehandling(command.behandlingId!!)!!
        val avbruttBehandling = behandling.avbryt(command.avsluttetAv, command.begrunnelse, avbruttTidspunkt)
        val avbruttSøknad = behandling.søknad?.avbryt(command.avsluttetAv, command.begrunnelse, avbruttTidspunkt)

        val oppdatertSak = this.copy(
            soknader = if (avbruttSøknad != null) this.soknader.map { if (it.id == command.søknadId) avbruttSøknad else it } else this.soknader,
            behandlinger = avbruttBehandling.let { this.behandlinger.oppdaterBehandling(it) },
        )
        return Triple(oppdatertSak, avbruttSøknad, avbruttBehandling)
    }

    private fun avbrytSøknad(
        command: no.nav.tiltakspenger.saksbehandling.behandling.service.avslutt.AvbrytSøknadOgBehandlingCommand,
        avbruttTidspunkt: LocalDateTime,
    ): Pair<Sak, Søknad> {
        val søknad = this.soknader.single { it.id == command.søknadId }
        val avbruttSøknad = søknad.avbryt(command.avsluttetAv, command.begrunnelse, avbruttTidspunkt)
        val oppdatertSak = this.copy(
            soknader = this.soknader.map { if (it.id == command.søknadId) avbruttSøknad else it },
        )
        return Pair(oppdatertSak, avbruttSøknad)
    }

    fun genererMeldeperioder(clock: Clock): Pair<Sak, List<Meldeperiode>> {
        val ikkeGenererEtter = ikkeGenererEtter(clock)
        return this.meldeperiodeKjeder.genererMeldeperioder(this.vedtaksliste, ikkeGenererEtter, clock)
            .let { this.copy(meldeperiodeKjeder = it.first) to it.second }
    }

    fun leggTilMeldekortbehandling(behandling: MeldekortUnderBehandling): Sak {
        return this.copy(meldekortBehandlinger = this.meldekortBehandlinger.leggTil(behandling))
    }

    fun oppdaterMeldekortbehandling(behandling: MeldekortBehandling): Sak {
        return this.copy(meldekortBehandlinger = this.meldekortBehandlinger.oppdaterMeldekortbehandling(behandling))
    }

    fun leggTilUtbetalingsvedtak(utbetalingsvedtak: Utbetalingsvedtak): Sak {
        return this.copy(utbetalinger = this.utbetalinger.leggTil(utbetalingsvedtak))
    }

    companion object {
        fun ikkeGenererEtter(clock: Clock): LocalDate {
            val dag = LocalDate.now(clock)
            val ukedag = dag.dayOfWeek.value
            return if (ukedag > 4) {
                dag.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            } else {
                dag.with(
                    TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY),
                )
            }
        }
    }
}
