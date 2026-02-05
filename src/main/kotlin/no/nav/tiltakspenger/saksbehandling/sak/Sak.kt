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
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltakelser
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.TiltaksdeltakelserDetErSøktTiltakspengerFor
import no.nav.tiltakspenger.saksbehandling.behandling.service.avslutt.AvbrytRammebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningerVedtatt
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagevedtaksliste
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
    val klagevedtaksliste: Klagevedtaksliste = vedtaksliste.klagevedtaksliste
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

    fun hentRammebehandling(rammebehandlingId: BehandlingId): Rammebehandling? =
        rammebehandlinger.hentRammebehandling(rammebehandlingId)

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
        command: AvbrytRammebehandlingKommando,
        avbruttTidspunkt: LocalDateTime,
    ): Triple<Sak, Søknad?, Rammebehandling> {
        val rammebehandling: Rammebehandling = this.hentRammebehandling(command.behandlingId)!!
        val skalAvbryteSøknad =
            rammebehandling is Søknadsbehandling && this.rammebehandlinger.filter { it.id != rammebehandling.id }.none { it is Søknadsbehandling && it.søknad.id == rammebehandling.søknad.id && !it.erAvbrutt }
        val avbruttBehandling = rammebehandling.avbryt(
            avbruttAv = command.avsluttetAv,
            begrunnelse = command.begrunnelse,
            tidspunkt = avbruttTidspunkt,
            skalAvbryteSøknad = skalAvbryteSøknad,
        )
        val avbruttSøknad = if (skalAvbryteSøknad) (avbruttBehandling as Søknadsbehandling).søknad else null

        val oppdatertSak = this.copy(
            søknader = if (avbruttSøknad != null) oppdaterSøknad(avbruttSøknad) else this.søknader,
            behandlinger = this.behandlinger.oppdaterRammebehandling(avbruttBehandling),
        )
        return Triple(oppdatertSak, avbruttSøknad, avbruttBehandling)
    }

    fun oppdaterSøknad(søknad: Søknad): List<Søknad> {
        require(this.søknader.count { it.id == søknad.id } == 1) { "Søknad med id ${søknad.id} finnes ikke på saken og kan derfor ikke oppdateres." }
        return this.søknader.map {
            if (it.id == søknad.id) søknad else it
        }
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

    /**
     * Oppdaterer også klagebehandlinger dersom rammebehandlingen har en koblet klagebehandling.
     */
    fun oppdaterRammebehandling(behandling: Rammebehandling): Sak {
        return this.copy(behandlinger = this.behandlinger.oppdaterRammebehandling(behandling))
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
        return hentRammevedtakForId(rammevedtakId).omgjortGrad == null
    }

    // Et meldeperiode har ikke informasjon om tiltaksdeltakelsen, så vi må hente det fra rammevedtakene som gjelder for dette meldekortvedtaket.
    // Det er mulig at flere rammevedtak gjelder for samme meldekortvedtak, f.eks. ved revurdering.
    // Ved flere rammevedtak kan de inneholde de samme tiltaksdeltakelsene.
    // Derfor må vi gruppere på internDeltakelseId og ta den nyeste.
    fun hentNyesteTiltaksdeltakelserForRammevedtakIder(rammevedtakIder: List<VedtakId>): Tiltaksdeltakelser =
        rammevedtakIder
            .map { this.hentRammevedtakForId(it) }
            .mapNotNull { vedtak -> vedtak.valgteTiltaksdeltakelser?.let { vedtak.opprettet to it } }
            .flatMap { (opprettet, deltakelser) -> deltakelser.verdier.map { opprettet to it } }
            .groupBy { it.second.internDeltakelseId }
            .map { (_, verdi) -> verdi.maxBy { it.first }.second }
            .let { Tiltaksdeltakelser(it) }
}
