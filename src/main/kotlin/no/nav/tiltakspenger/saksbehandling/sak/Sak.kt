package no.nav.tiltakspenger.saksbehandling.sak

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlinger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlinger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.TiltaksdeltakelseDetErSøktTiltakspengerFor
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.TiltaksdeltakelserDetErSøktTiltakspengerFor
import no.nav.tiltakspenger.saksbehandling.behandling.service.avslutt.AvbrytSøknadOgBehandlingCommand
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningerVedtatt
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandletAutomatisk
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldekortbehandlinger
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldekortvedtak
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldekortvedtaksliste
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalinger
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtaksliste
import no.nav.tiltakspenger.saksbehandling.vedtak.Vedtaksliste
import java.time.Clock
import java.time.LocalDateTime

data class Sak(
    val id: SakId,
    val fnr: Fnr,
    val saksnummer: Saksnummer,
    val behandlinger: Behandlinger,
    val vedtaksliste: Vedtaksliste,
    val meldeperiodeKjeder: MeldeperiodeKjeder,
    val brukersMeldekort: List<BrukersMeldekort>,
    val søknader: List<Søknad>,
    val kanSendeInnHelgForMeldekort: Boolean,
) {
    val utbetalinger: Utbetalinger by lazy {
        Utbetalinger(
            rammevedtaksliste.utbetalinger
                .plus(meldekortvedtaksliste.utbetalinger)
                .sortedBy { it.opprettet },
        )
    }
    val rammevedtaksliste: Rammevedtaksliste = vedtaksliste.rammevedtaksliste
    val meldekortvedtaksliste: Meldekortvedtaksliste = vedtaksliste.meldekortvedtaksliste
    val rammebehandlinger: Rammebehandlinger = behandlinger.rammebehandlinger
    val meldekortbehandlinger: Meldekortbehandlinger = behandlinger.meldekortbehandlinger

    val meldeperiodeBeregninger: MeldeperiodeBeregningerVedtatt by lazy {
        vedtaksliste.meldeperiodeBeregninger
    }

    /** Nåtilstand. Tar utgangspunkt i tidslinja på saken og henter den siste innvilget dagen. */
    val førsteDagSomGirRett = rammevedtaksliste.førsteDagSomGirRett

    /** Nåtilstand. Tar utgangspunkt i tidslinja på saken og henter den siste innvilget dagen. */
    val sisteDagSomGirRett = rammevedtaksliste.sisteDagSomGirRett

    val revurderinger = rammebehandlinger.revurderinger

    val barnetilleggsperioder: Periodisering<AntallBarn> by lazy { rammevedtaksliste.barnetilleggsperioder }

    val tiltakstypeperioder: Periodisering<TiltakstypeSomGirRett> by lazy { rammevedtaksliste.tiltakstypeperioder }

    /** Et førstegangsvedtak defineres som den første søknadsbehandlingen som førte til innvilgelse. */
    val harFørstegangsvedtak: Boolean by lazy { this.vedtaksliste.harFørstegangsvedtak }

    val tiltaksdeltakelserDetErSøktTiltakspengerFor by lazy {
        TiltaksdeltakelserDetErSøktTiltakspengerFor(
            this.søknader.mapNotNull { søknad ->
                søknad.tiltak?.let { tiltak ->
                    TiltaksdeltakelseDetErSøktTiltakspengerFor(tiltak, søknad.tidsstempelHosOss)
                }
            },
        )
    }

    val apneSoknadsbehandlinger = rammebehandlinger
        .filterIsInstance<Søknadsbehandling>()
        .filterNot { it.erAvsluttet }

    fun hentMeldekortBehandling(meldekortId: MeldekortId): MeldekortBehandling? {
        return meldekortbehandlinger.hentMeldekortBehandling(meldekortId)
    }

    fun hentSisteMeldeperiodeForKjede(kjedeId: MeldeperiodeKjedeId): Meldeperiode {
        return meldeperiodeKjeder.hentSisteMeldeperiodeForKjede(kjedeId)
    }

    fun hentRammebehandling(behandlingId: BehandlingId): Rammebehandling? =
        rammebehandlinger.hentBehandling(behandlingId)

    fun harSoknadUnderBehandling(): Boolean {
        val avsluttedeSoknadsbehandlinger = rammebehandlinger
            .filterIsInstance<Søknadsbehandling>()
            .filter { it.erAvsluttet }
        val apneSoknader = søknader.filterNot { it.erAvbrutt }
        return apneSoknader.any { soknad ->
            avsluttedeSoknadsbehandlinger.find { it.søknad.id == soknad.id } == null ||
                apneSoknadsbehandlinger.find { it.søknad.id == soknad.id } != null
        }
    }

    fun erSisteVersjonAvMeldeperiode(meldeperiode: Meldeperiode): Boolean {
        return meldeperiodeKjeder.erSisteVersjonAvMeldeperiode(meldeperiode)
    }

    fun avbrytSøknadOgBehandling(
        command: AvbrytSøknadOgBehandlingCommand,
        avbruttTidspunkt: LocalDateTime,
    ): Triple<Sak, Søknad?, Rammebehandling?> {
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
    ): Triple<Sak, Søknad?, Rammebehandling> {
        val behandling: Rammebehandling = this.hentRammebehandling(command.behandlingId!!)!!
        val avbruttBehandling = behandling.avbryt(command.avsluttetAv, command.begrunnelse, avbruttTidspunkt)
        val avbruttSøknad = if (behandling is Søknadsbehandling) {
            behandling.søknad.avbryt(command.avsluttetAv, command.begrunnelse, avbruttTidspunkt)
        } else {
            null
        }

        val oppdatertSak = this.copy(
            søknader = if (avbruttSøknad != null) this.søknader.map { if (it.id == command.søknadId) avbruttSøknad else it } else this.søknader,
            behandlinger = this.behandlinger.oppdaterRammebehandling(avbruttBehandling),
        )

        return Triple(oppdatertSak, avbruttSøknad, avbruttBehandling)
    }

    private fun avbrytSøknad(
        command: AvbrytSøknadOgBehandlingCommand,
        avbruttTidspunkt: LocalDateTime,
    ): Pair<Sak, Søknad> {
        val søknad = this.søknader.single { it.id == command.søknadId }
        val avbruttSøknad = søknad.avbryt(command.avsluttetAv, command.begrunnelse, avbruttTidspunkt)
        val oppdatertSak = this.copy(
            søknader = this.søknader.map { if (it.id == command.søknadId) avbruttSøknad else it },
        )
        return Pair(oppdatertSak, avbruttSøknad)
    }

    fun genererMeldeperioder(clock: Clock): Pair<Sak, List<Meldeperiode>> {
        return this.meldeperiodeKjeder
            .genererMeldeperioder(this.rammevedtaksliste, clock)
            .let { this.copy(meldeperiodeKjeder = it.first) to it.second }
    }

    fun leggTilSøknadsbehandling(behandling: Søknadsbehandling): Sak {
        return this.copy(behandlinger = this.behandlinger.leggTilSøknadsbehandling(behandling))
    }

    fun leggTilRevurdering(revurdering: Revurdering): Sak {
        return this.copy(behandlinger = this.behandlinger.leggTilRevurdering(revurdering))
    }

    fun leggTilMeldekortbehandling(behandling: MeldekortUnderBehandling): Sak {
        return this.copy(behandlinger = this.behandlinger.leggTilMeldekortUnderBehandling(behandling))
    }

    fun leggTilMeldekortbehandling(behandling: MeldekortBehandletAutomatisk): Sak {
        return this.copy(behandlinger = this.behandlinger.leggTilMeldekortBehandletAutomatisk(behandling))
    }

    // TODO jah: Bør kun oppdatere den behandlingen som er endret, ikke hele settet
    fun oppdaterMeldekortbehandlinger(behandlinger: Meldekortbehandlinger): Sak {
        return this.copy(behandlinger = this.behandlinger.copy(meldekortbehandlinger = behandlinger))
    }

    fun oppdaterMeldekortbehandling(behandling: MeldekortBehandling): Sak {
        return this.copy(behandlinger = this.behandlinger.oppdaterMeldekortbehandling(behandling))
    }

    fun oppdaterRammebehandling(behandling: Rammebehandling): Sak {
        return this.copy(behandlinger = this.behandlinger.oppdaterRammebehandling(behandling))
    }

    fun oppdaterSøknadsbehandling(behandling: Søknadsbehandling): Sak {
        return this.oppdaterRammebehandling(behandling)
    }

    fun oppdaterRevurdering(behandling: Revurdering): Sak {
        return this.oppdaterRammebehandling(behandling)
    }

    fun leggTilMeldekortvedtak(vedtak: Meldekortvedtak): Sak {
        return this.copy(vedtaksliste = this.vedtaksliste.leggTilMeldekortvedtak(vedtak))
    }

    fun leggTilRammevedtak(vedtak: Rammevedtak): Sak {
        return this.copy(vedtaksliste = this.vedtaksliste.leggTilRammevedtak(vedtak))
    }

    fun hentRammevedtakForId(rammevedtakId: VedtakId): Rammevedtak {
        return rammevedtaksliste.hentRammevedtakForId(rammevedtakId)
    }

    fun oppdaterKanSendeInnHelgForMeldekort(kanSendeInnHelgForMeldekort: Boolean): Sak =
        this.copy(kanSendeInnHelgForMeldekort = kanSendeInnHelgForMeldekort)

    /**
     * Tar kun med vedtak som innvilger.
     */
    fun erRammevedtakGjeldendeForHeleSinPeriode(rammevedtakId: VedtakId): Boolean {
        val rammevedtak = hentRammevedtakForId(rammevedtakId)
        val treff = this.rammevedtaksliste.innvilgetTidslinje.filter {
            it.verdi.id == rammevedtakId
        }
        if (treff.size != 1) return false
        return treff.single().periode == rammevedtak.periode
    }
}
