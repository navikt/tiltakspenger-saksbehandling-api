package no.nav.tiltakspenger.saksbehandling.sak

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.leggSammen
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlinger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.TiltaksdeltagelseDetErSøktTiltakspengerFor
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.TiltaksdeltagelserDetErSøktTiltakspengerFor
import no.nav.tiltakspenger.saksbehandling.behandling.service.avslutt.AvbrytSøknadOgBehandlingCommand
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregninger
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerRolle
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandletAutomatisk
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
import java.time.LocalDate
import java.time.LocalDateTime

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
    val meldeperiodeBeregninger: MeldeperiodeBeregninger = meldekortBehandlinger.meldeperiodeBeregninger

    /** Nåtilstand. Tar utgangspunkt i tidslinja på saken og henter den siste innvilget dagen. */
    val førsteDagSomGirRett = vedtaksliste.førsteDagSomGirRett

    /** Nåtilstand. Tar utgangspunkt i tidslinja på saken og henter den siste innvilget dagen. */
    val sisteDagSomGirRett = vedtaksliste.sisteDagSomGirRett

    val revurderinger = behandlinger.revurderinger

    /** Henter fra siste godkjente meldekort */
    @Suppress("unused")
    val sisteNavkontor: Navkontor? by lazy {
        meldekortBehandlinger.sisteGodkjenteMeldekort?.navkontor
    }

    val barnetilleggsperioder: Periodisering<AntallBarn> by lazy { vedtaksliste.barnetilleggsperioder }

    val tiltakstypeperioder: Periodisering<TiltakstypeSomGirRett> by lazy { vedtaksliste.tiltakstypeperioder }

    /** Et førstegangsvedtak defineres som den første søknadsbehandlingen som førte til innvilgelse. */
    val harFørstegangsvedtak: Boolean by lazy { this.vedtaksliste.harFørstegangsvedtak }

    /**
     * Periodene det er søkt om, slått sammen overlappende og tilstøtende perioder.
     * Merk at tiltaksdeltagelsesperiodene er som de så ut på søknadstidspunktet og kan ha endret seg i etterkant.
     * Man kan bare søke på en tiltaksdeltagelse per søknad.
     */
    val søknadsperioder: List<Periode> by lazy {
        soknader.map { it.tiltaksdeltagelseperiodeDetErSøktOm() }.leggSammen(godtaOverlapp = true)
    }

    val tiltaksdeltagelserIderDetErSøktPå: List<String> by lazy {
        soknader.map { it.tiltak.id }.sorted().distinct()
    }

    /**
     * Ikke basert på tidslinje.
     * Dette er summen av alle vedtak sine tiltaksdeltagelseperioder, filtrert på tiltaksdeltagelseId, slått sammen overlappende og tilstøtende perioder.
     */
    val tiltaksdeltagelsesperioderFraVedtak: List<Periode> by lazy {
        vedtaksliste.value
            .flatMap { it.behandling.saksopplysninger.tiltaksdeltagelse }
            .filter { it.eksternDeltagelseId in tiltaksdeltagelserIderDetErSøktPå }
            .map { Periode(it.deltagelseFraOgMed!!, it.deltagelseTilOgMed!!) }
            .leggSammen(godtaOverlapp = true)
    }

    /**
     * Sakens totale saksopplysningsperiode/innhentingsperiode.
     * Denne vil omfavne sakens totale behandlingsgrunnlag.
     * Dette vil være summen av periodene til tiltaksdeltagelsene det er søkt for.
     * Merk at perioden til tiltaksdeltagelsen kan ha endret seg etter søknaden.
     * Saksopplysningsperioden er summen av disse, uavhengig av vedtakstype.
     * Bruker den største perioden (summen) av tiltaksdeltagelsen på søknaden og vedtaket.
     * I de fleste tilfeller bør en behandling bruke en undermengde av denne perioden.
     *
     * Dette er ikke nødvendigvis en sammenhengende periode. Eksempelvis kan to tiltaksdeltagelser være spredt med f.eks. flere år i mellom.
     * Det gir oss ikke behandlingsgrunnlag for perioden det ikke var tiltaksdeltagelse.
     */
//    val saksopplysningsperiode: List<Periode> by lazy {
//        søknadsperioder.leggSammenMed(tiltaksdeltagelsesperioderFraVedtak, godtaOverlapp = true)
//    }

    val tiltaksdeltagelserDetErSøktTiltakspengerFor by lazy {
        TiltaksdeltagelserDetErSøktTiltakspengerFor(
            this.soknader.map {
                TiltaksdeltagelseDetErSøktTiltakspengerFor(it.tiltak, it.kravdato)
            },
        )
    }

    fun hentMeldekortBehandling(meldekortId: MeldekortId): MeldekortBehandling? {
        return meldekortBehandlinger.hentMeldekortBehandling(meldekortId)
    }

    fun hentSisteMeldekortBehandlingForKjede(id: MeldeperiodeKjedeId): MeldekortBehandling? {
        return meldekortBehandlinger.hentSisteMeldekortBehandlingForKjede(id)
    }

    fun hentSisteMeldeperiodeForKjede(kjedeId: MeldeperiodeKjedeId): Meldeperiode {
        return meldeperiodeKjeder.hentSisteMeldeperiodeForKjede(kjedeId)
    }

    fun hentBehandling(behandlingId: BehandlingId): Behandling? = behandlinger.hentBehandling(behandlingId)

    fun sisteUtbetalteMeldekortDag(): LocalDate? = meldekortBehandlinger.sisteUtbetalteMeldekortDag

    fun harSoknadUnderBehandling(): Boolean {
        val avsluttedeSoknadsbehandlinger = behandlinger
            .filterIsInstance<Søknadsbehandling>()
            .filter { it.erAvsluttet }
        val apneSoknadsbehandlinger = behandlinger
            .filterIsInstance<Søknadsbehandling>()
            .filterNot { it.erAvsluttet }
        val apneSoknader = soknader.filterNot { it.erAvbrutt }
        return apneSoknader.any { soknad ->
            avsluttedeSoknadsbehandlinger.find { it.søknad.id == soknad.id } == null ||
                apneSoknadsbehandlinger.find { it.søknad.id == soknad.id } != null
        }
    }

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

    fun avbrytSøknadOgBehandling(
        command: AvbrytSøknadOgBehandlingCommand,
        avbruttTidspunkt: LocalDateTime,
    ): Triple<Sak, Søknad?, Behandling?> {
        krevSaksbehandlerRolle(command.avsluttetAv)
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
        val avbruttSøknad =
            if (behandling is Søknadsbehandling) {
                behandling.søknad.avbryt(command.avsluttetAv, command.begrunnelse, avbruttTidspunkt)
            } else {
                null
            }

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

    fun genererMeldeperioder(clock: Clock): Pair<Sak, List<Meldeperiode>> {
        return this.meldeperiodeKjeder
            .genererMeldeperioder(this.vedtaksliste, clock)
            .let { this.copy(meldeperiodeKjeder = it.first) to it.second }
    }

    fun leggTilMeldekortbehandling(behandling: MeldekortUnderBehandling): Sak {
        return this.copy(meldekortBehandlinger = this.meldekortBehandlinger.leggTil(behandling))
    }

    fun leggTilMeldekortbehandling(behandling: MeldekortBehandletAutomatisk): Sak {
        return this.copy(meldekortBehandlinger = this.meldekortBehandlinger.leggTil(behandling))
    }

    fun oppdaterMeldekortbehandlinger(behandlinger: MeldekortBehandlinger): Sak {
        return this.copy(meldekortBehandlinger = behandlinger)
    }

    fun oppdaterMeldekortbehandling(behandling: MeldekortBehandling): Sak {
        return this.copy(meldekortBehandlinger = this.meldekortBehandlinger.oppdaterMeldekortbehandling(behandling))
    }

    fun leggTilUtbetalingsvedtak(utbetalingsvedtak: Utbetalingsvedtak): Sak {
        return this.copy(utbetalinger = this.utbetalinger.leggTil(utbetalingsvedtak))
    }
}
