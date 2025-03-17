package no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.sak

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
import no.nav.tiltakspenger.saksbehandling.felles.Navkontor
import no.nav.tiltakspenger.saksbehandling.felles.min
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlinger
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.Behandling
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.Behandlinger
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.Søknad
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.vedtak.Vedtaksliste
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.avslutt.AvbrytSøknadOgBehandlingCommand
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalinger
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
    /** Nåtilstand. Dette er sakens totale vedtaksperiode. Vær veldig obs når du bruker denne, fordi den sier ikke noe om antall perioder, om de gir rett eller ikke. */
    val vedtaksperiode: Periode? = vedtaksliste.vedtaksperiode

    /** Nåtilstand. Tar utgangspunkt i tidslinja på saken og henter den siste innvilget dagen. */
    val førsteDagSomGirRett = vedtaksliste.førsteDagSomGirRett

    /** Nåtilstand. Tar utgangspunkt i tidslinja på saken og henter den siste innvilget dagen. */
    @Suppress("unused")
    val sisteDagSomGirRett = vedtaksliste.sisteDagSomGirRett

    /**
     * En sak kan kun ha en førstegangsbehandling, dersom perioden til den vedtatte førstegangsbehandlingen skal utvides eller minskes (den må fortsatt være sammenhengende) må vi revurdere/omgjøre, ikke førstegangsbehandle på nytt.
     * Dersom den nye søknaden ikke overlapper eller tilstøter den gamle perioden, må vi opprette en ny sak som får en ny førstegangsbehandling.
     */
    val ikkeAvbruttFørstegangsbehandlinger: List<Behandling> = behandlinger.ikkeAvbrutteFørstegangsbehandlinger
    val revurderinger = behandlinger.revurderinger

    val førsteSaksopplysningsperiode: Periode? =
        ikkeAvbruttFørstegangsbehandlinger.firstOrNull()?.saksopplysningsperiode

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
    fun hentMeldekortBehandlingerForMeldeperiode(meldeperiodeId: MeldeperiodeId) =
        meldekortBehandlinger.hentMeldekortBehandlingerForMeldeperiode(meldeperiodeId)

    /** Flere behandlinger kan være knyttet til samme versjon av meldeperioden. */
    fun hentMeldekortBehandlingerForKjede(id: MeldeperiodeKjedeId) =
        meldekortBehandlinger.hentMeldekortBehandlingerForKjede(id)

    fun hentSisteMeldekortBehandlingForKjede(id: MeldeperiodeKjedeId) =
        meldekortBehandlinger.hentSisteMeldekortBehandlingForKjede(id)

    fun hentMeldeperiode(id: MeldeperiodeId): Meldeperiode? {
        return meldeperiodeKjeder.hentMeldeperiode(id)
    }

    fun hentSisteMeldeperiodeForKjede(kjedeId: MeldeperiodeKjedeId): Meldeperiode {
        return meldeperiodeKjeder.hentSisteMeldeperiodeForKjede(kjedeId)
    }

    /** Den er kun trygg inntil vi revurderer antall dager. */
    fun hentAntallDager(periode: Periode): Int? = vedtaksliste.hentAntallDager(periode)
    fun hentTynnSak(): TynnSak = TynnSak(this.id, this.fnr, this.saksnummer)

    fun hentBehandling(behandlingId: BehandlingId): Behandling? = behandlinger.hentBehandling(behandlingId)

    fun sisteUtbetalteMeldekortDag(): LocalDate? = meldekortBehandlinger.sisteUtbetalteMeldekortDag

    /**
     * Vil være innenfor [vedtaksperiode]. Dersom vi ikke har et vedtak, vil den være null.
     */
    fun førsteLovligeStansdato(): LocalDate? = sisteUtbetalteMeldekortDag()?.plusDays(1)?.let {
        min(it, vedtaksperiode!!.tilOgMed)
    } ?: vedtaksperiode?.fraOgMed

    fun erSisteVersjonAvMeldeperiode(meldeperiode: Meldeperiode): Boolean {
        return meldeperiodeKjeder.erSisteVersjonAvMeldeperiode(meldeperiode)
    }

    fun finnNærmesteMeldeperiode(dato: LocalDate): Periode = meldeperiodeKjeder.finnNærmesteMeldeperiode(dato)

    fun avbrytSøknadOgBehandling(
        command: AvbrytSøknadOgBehandlingCommand,
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
        command: AvbrytSøknadOgBehandlingCommand,
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
        command: AvbrytSøknadOgBehandlingCommand,
        avbruttTidspunkt: LocalDateTime,
    ): Pair<Sak, Søknad> {
        val søknad = this.soknader.single { it.id == command.søknadId }
        val avbruttSøknad = søknad.avbryt(command.avsluttetAv, command.begrunnelse, avbruttTidspunkt)
        val oppdatertSak = this.copy(
            soknader = this.soknader.map { if (it.id == command.søknadId) avbruttSøknad else it },
        )
        return Pair(oppdatertSak, avbruttSøknad)
    }

    fun genererMeldeperioder(): Pair<Sak, List<Meldeperiode>> {
        val dag = LocalDate.now()
        val ukedag = dag.dayOfWeek.value
        val ikkeGenererEtter = if (ukedag > 4) {
            dag.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        } else {
            dag.with(
                TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY),
            )
        }
        return this.meldeperiodeKjeder.genererMeldeperioder(this.vedtaksliste, ikkeGenererEtter).let {
            this.copy(meldeperiodeKjeder = it.first) to it.second
        }
    }
}
