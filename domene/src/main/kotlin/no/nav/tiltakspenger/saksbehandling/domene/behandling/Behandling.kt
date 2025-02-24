package no.nav.tiltakspenger.saksbehandling.domene.behandling

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.felles.OppgaveId
import no.nav.tiltakspenger.felles.nå
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandlingsstatus.KLAR_TIL_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandlingsstatus.KLAR_TIL_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandlingsstatus.UNDER_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandlingsstatus.UNDER_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandlingsstatus.VEDTATT
import no.nav.tiltakspenger.saksbehandling.domene.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.AvklartUtfallForPeriode
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.UtfallForPeriode
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.toAvklartUtfallForPeriode
import java.time.LocalDateTime

/**
 * Revurdering: https://jira.adeo.no/browse/BEGREP-1559
 *
 * Unikt for søknadssbehandling: Søknad? (I følge begrepskatalogen kan en endringssøknad føre til en revurdering (må den da være nullable?), men gitt at det er et helt nytt uavhengig tiltak, er det da en førstegangssøknad, søknad eller endringssøknad?)
 * Unikt for Revurdering:
 *
 * @param saksbehandler Vil bli satt på en behandling ved opprettelse, men i noen tilfeller kan saksbehandler ta seg selv av behandlingen igjen.
 * @param beslutter Vil bli satt når behandlingen avsluttes (iverksett eller avbrytes) eller underkjennes.
 * @param søknad Påkrevd for [Behandlingstype.FØRSTEGANGSBEHANDLING]. Kan være null for [Behandlingstype.REVURDERING]. Må vurdere på sikt om en endringssøknad (samme tiltak) er en ny førstegangssøknad eller en revurdering. Og om en ny søknad (nytt tiltak) er en førstegangssøknad, søknad eller en revurdering.
 * @param virkningsperiode Vil tilsvare innvilgelsesperiode for vedtak som gir rett til tiltakspenger og stansperiode/opphørsperiode for vedtak som fjerner rett til tiltakspenger.
 */
data class Behandling(
    val id: BehandlingId,
    val sakId: SakId,
    val saksnummer: Saksnummer,
    val fnr: Fnr,
    val virkningsperiode: Periode?,
    val søknad: Søknad?,
    val saksbehandler: String?,
    val sendtTilBeslutning: LocalDateTime?,
    val beslutter: String?,
    val saksopplysninger: Saksopplysninger,
    val status: Behandlingsstatus,
    val attesteringer: List<Attestering>,
    val opprettet: LocalDateTime,
    val iverksattTidspunkt: LocalDateTime?,
    val sendtTilDatadeling: LocalDateTime?,
    val sistEndret: LocalDateTime,
    val behandlingstype: Behandlingstype,
    val oppgaveId: OppgaveId?,
    val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?,
    val begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering?,
    val saksopplysningsperiode: Periode?,
) {
    val erUnderBehandling: Boolean = status == UNDER_BEHANDLING

    val erVedtatt: Boolean = status == VEDTATT

    /** John og Agnethe har kommet fram til at vi setter denne til 14 dager for meldeperiode i førsteomgang. Hvis det fører til mye feilutbetaling eller lignende må vi la saksbehandler periodisere dette selv, litt på samme måte som barnetillegg. */
    val maksDagerMedTiltakspengerForPeriode: Int = 14

    val tiltaksnavn = saksopplysninger.tiltaksdeltagelse.typeNavn
    val tiltakstype: TiltakstypeSomGirRett = saksopplysninger.tiltaksdeltagelse.typeKode
    val tiltaksid: String = saksopplysninger.tiltaksdeltagelse.eksternDeltagelseId
    val gjennomføringId: String? = saksopplysninger.tiltaksdeltagelse.gjennomføringId

    // Denne er kun en midlertidig løsning for å kunne støtte ny og gammel vilkårsvurdering i EndretTiltaksdeltakerJobb og bør ikke brukes noe annet
    // sted siden vi mangler data for id, deltakelsesprosent og antallDagerPerUke i gammel vilkårsvurdering og dermed bruker noen defaultverdier
    val tiltaksdeltakelse = saksopplysninger.tiltaksdeltagelse

    val utfallsperioder: Periodisering<UtfallForPeriode>
        get() =
            // Dersom det er en innvilgelse, vil hele innvilgelsesperioden være utfallsperoden: OPPFYLT
            // Dersom det er et avslag, vil vi ikke forholde oss til en utfallsperiode. Det skal ikke lages meldeperioder. Og vedtaket skal bare "ignoreres" som regel.
            // Dersom det er en revurdering stans/opphør, vil vi ha en opphørsperiode.
            when (behandlingstype) {
                Behandlingstype.FØRSTEGANGSBEHANDLING -> {
                    Periodisering<UtfallForPeriode>(UtfallForPeriode.OPPFYLT, virkningsperiode!!)
                }

                Behandlingstype.REVURDERING -> {
                    Periodisering<UtfallForPeriode>(UtfallForPeriode.IKKE_OPPFYLT, virkningsperiode!!)
                }
            }

    val avklarteUtfallsperioder: Periodisering<AvklartUtfallForPeriode> get() = utfallsperioder.toAvklartUtfallForPeriode()

    val erFørstegangsbehandling: Boolean = behandlingstype == Behandlingstype.FØRSTEGANGSBEHANDLING
    val erRevurdering: Boolean = behandlingstype == Behandlingstype.REVURDERING

    /** Påkrevd ved førstegangsbehandling/søknadsbehandling, men kan være null ved revurdering. */
    val kravfrist = søknad?.tidsstempelHosOss

    companion object {
        private val logger = mu.KotlinLogging.logger { }

        /**
         * Ny måte og opprette søknadsbehandling på. Støtter ny og enklere vilkårsvurderingsflyt.
         * TODO John + Anders: Rename når vi har slettet den gamle.
         */
        suspend fun opprettSøknadsbehandlingV2(
            sakId: SakId,
            saksnummer: Saksnummer,
            fnr: Fnr,
            søknad: Søknad,
            saksbehandler: Saksbehandler,
            hentSaksopplysninger: suspend (saksopplysningsperiode: Periode) -> Saksopplysninger,
        ): Either<KanIkkeOppretteBehandling, Behandling> {
            if (søknad.barnetillegg.isNotEmpty()) {
                return KanIkkeOppretteBehandling.StøtterIkkeBarnetillegg.left()
            }
            val opprettet = nå()

            /** Kommentar jah: Det kan bli aktuelt at saksbehandler får endre på fraOgMed her. */
            val saksopplysningsperiode: Periode = run {
                // § 11: Tiltakspenger og barnetillegg gis for opptil tre måneder før den måneden da kravet om ytelsen ble satt fram, dersom vilkårene var oppfylt i denne perioden.
                val fraOgMed = søknad.kravdato.withDayOfMonth(1).minusMonths(3)
                // Forskriften gir ingen begrensninger fram i tid. 100 år bør være nok.
                val tilOgMed = fraOgMed.plusYears(100)
                Periode(fraOgMed, tilOgMed)
            }
            return Behandling(
                id = BehandlingId.random(),
                saksnummer = saksnummer,
                sakId = sakId,
                fnr = fnr,
                søknad = søknad,
                virkningsperiode = null,
                saksopplysninger = hentSaksopplysninger(saksopplysningsperiode),
                fritekstTilVedtaksbrev = null,
                begrunnelseVilkårsvurdering = null,
                saksbehandler = saksbehandler.navIdent,
                sendtTilBeslutning = null,
                beslutter = null,
                status = UNDER_BEHANDLING,
                attesteringer = emptyList(),
                opprettet = opprettet,
                iverksattTidspunkt = null,
                sendtTilDatadeling = null,
                sistEndret = opprettet,
                behandlingstype = Behandlingstype.FØRSTEGANGSBEHANDLING,
                oppgaveId = søknad.oppgaveId,
                saksopplysningsperiode = saksopplysningsperiode,
            ).right()
        }

        suspend fun opprettRevurderingV2(
            sakId: SakId,
            saksnummer: Saksnummer,
            fnr: Fnr,
            saksbehandler: Saksbehandler,
            saksopplysningsperiode: Periode,
            hentSaksopplysninger: suspend () -> Saksopplysninger,
        ): Behandling {
            val opprettet = nå()
            return Behandling(
                id = BehandlingId.random(),
                sakId = sakId,
                saksnummer = saksnummer,
                fnr = fnr,
                virkningsperiode = null,
                søknad = null,
                saksbehandler = saksbehandler.navIdent,
                sendtTilBeslutning = null,
                beslutter = null,
                saksopplysninger = hentSaksopplysninger(),
                fritekstTilVedtaksbrev = null,
                begrunnelseVilkårsvurdering = null,
                status = UNDER_BEHANDLING,
                attesteringer = emptyList(),
                opprettet = opprettet,
                iverksattTidspunkt = null,
                sendtTilDatadeling = null,
                sistEndret = opprettet,
                behandlingstype = Behandlingstype.REVURDERING,
                // her kan man på sikt lagre oppgaveId hvis man oppretter oppgave for revurdering
                oppgaveId = null,
                // Kommentar John: Dersom en revurdering tar utgangspunkt i en søknad, bør denne bestemmes på samme måte som for førstegangsbehandling.
                saksopplysningsperiode = saksopplysningsperiode,
            )
        }
    }

    /** Saksbehandler/beslutter tar eller overtar behandlingen. */
    fun taBehandling(saksbehandler: Saksbehandler): Behandling =
        when (this.status) {
            KLAR_TIL_BEHANDLING, UNDER_BEHANDLING -> {
                check(saksbehandler.erSaksbehandler()) {
                    "Saksbehandler må ha rolle saksbehandler. Utøvende saksbehandler: $saksbehandler"
                }
                this.copy(saksbehandler = saksbehandler.navIdent, status = UNDER_BEHANDLING).let {
                    // Dersom utøvende saksbehandler er beslutter, fjern beslutter fra behandlingen.
                    if (it.saksbehandler == it.beslutter) it.copy(beslutter = null) else it
                }
            }

            KLAR_TIL_BESLUTNING, UNDER_BESLUTNING -> {
                check(saksbehandler.navIdent != this.saksbehandler) {
                    "Beslutter ($saksbehandler) kan ikke være den samme som saksbehandleren (${this.saksbehandler}"
                }
                check(saksbehandler.erBeslutter()) {
                    "Saksbehandler må ha beslutterrolle. Utøvende saksbehandler: $saksbehandler"
                }
                this.copy(beslutter = saksbehandler.navIdent, status = UNDER_BESLUTNING)
            }

            VEDTATT -> {
                throw IllegalArgumentException(
                    "Kan ikke ta behandling når behandlingen er VEDTATT. Behandlingsstatus: ${this.status}. Utøvende saksbehandler: $saksbehandler. Saksbehandler på behandling: ${this.saksbehandler}",
                )
            }
        }

    /** Skal renames til tilBeslutning */
    fun tilBeslutningV2(
        kommando: SendBehandlingTilBeslutningKommando,
    ): Behandling {
        check(status == UNDER_BEHANDLING) {
            "Behandlingen må være under behandling, det innebærer også at en saksbehandler må ta saken før den kan sendes til beslutter. Behandlingsstatus: ${this.status}. Utøvende saksbehandler: $saksbehandler. Saksbehandler på behandling: ${this.saksbehandler}"
        }
        check(kommando.saksbehandler.navIdent == this.saksbehandler) { "Det er ikke lov å sende en annen sin behandling til beslutter" }

        return this.copy(
            status = if (beslutter == null) KLAR_TIL_BESLUTNING else UNDER_BESLUTNING,
            sendtTilBeslutning = nå(),
            fritekstTilVedtaksbrev = kommando.fritekstTilVedtaksbrev,
            begrunnelseVilkårsvurdering = kommando.begrunnelseVilkårsvurdering,
            virkningsperiode = kommando.innvilgelsesperiode,
        )
    }

    fun sendRevurderingTilBeslutning(
        kommando: SendRevurderingTilBeslutningKommando,
        vedtaksperiode: Periode,
    ): Behandling {
        check(status == UNDER_BEHANDLING) {
            "Behandlingen må være under behandling, det innebærer også at en saksbehandler må ta saken før den kan sendes til beslutter. Behandlingsstatus: ${this.status}. Utøvende saksbehandler: $saksbehandler. Saksbehandler på behandling: ${this.saksbehandler}"
        }
        check(kommando.saksbehandler.navIdent == this.saksbehandler) { "Det er ikke lov å sende en annen sin behandling til beslutter" }

        return this.copy(
            status = if (beslutter == null) KLAR_TIL_BESLUTNING else UNDER_BESLUTNING,
            sendtTilBeslutning = nå(),
            begrunnelseVilkårsvurdering = kommando.begrunnelse,
            virkningsperiode = Periode(kommando.stansDato, vedtaksperiode.tilOgMed),
        )
    }

    /** Rename til iverksett (gjelder generelt alle v2)  */
    fun iverksettv2(
        utøvendeBeslutter: Saksbehandler,
        attestering: Attestering,
    ): Behandling {
        require(virkningsperiode != null) { "virkningsperiode må være satt ved iverksetting" }

        return when (status) {
            UNDER_BESLUTNING -> {
                check(utøvendeBeslutter.erBeslutter()) { "utøvende saksbehandler må være beslutter" }
                check(this.beslutter == utøvendeBeslutter.navIdent) { "Kan ikke iverksette en behandling man ikke er beslutter på" }
                check(!this.attesteringer.any { it.isGodkjent() }) {
                    "Behandlingen er allerede godkjent"
                }
                this.copy(
                    status = VEDTATT,
                    attesteringer = attesteringer + attestering,
                    iverksattTidspunkt = nå(),
                )
            }

            KLAR_TIL_BEHANDLING, UNDER_BEHANDLING, KLAR_TIL_BESLUTNING, VEDTATT -> throw IllegalStateException(
                "Må ha status UNDER_BESLUTNING for å iverksette. Behandlingsstatus: $status",
            )
        }
    }

    fun sendTilbake(
        utøvendeBeslutter: Saksbehandler,
        attestering: Attestering,
    ): Behandling {
        return when (status) {
            UNDER_BESLUTNING -> {
                check(
                    utøvendeBeslutter.erBeslutter(),
                ) { "utøvende saksbehandler må være beslutter" }
                check(this.beslutter == utøvendeBeslutter.navIdent) {
                    "Kun beslutter som har saken kan sende tilbake"
                }
                check(!this.attesteringer.any { it.isGodkjent() }) {
                    "Behandlingen er allerede godkjent"
                }
                this.copy(status = UNDER_BEHANDLING, attesteringer = attesteringer + attestering)
            }

            KLAR_TIL_BEHANDLING, UNDER_BEHANDLING, KLAR_TIL_BESLUTNING, VEDTATT -> throw IllegalStateException(
                "Må ha status UNDER_BESLUTNING for å sende tilbake. Behandlingsstatus: $status",
            )
        }
    }

    /**
     * Krymper [virkningsperiode] til [nyPeriode].
     * Endrer ikke [Søknad].
     */
    fun krymp(nyPeriode: Periode): Behandling {
        if (virkningsperiode == nyPeriode) return this
        if (virkningsperiode != null) require(virkningsperiode.inneholderHele(nyPeriode)) { "Ny periode ($nyPeriode) må være innenfor vedtakets virkningsperiode ($virkningsperiode)" }
        return this.copy(
            virkningsperiode = if (virkningsperiode != null) nyPeriode else null,
        )
    }

    fun oppdaterSaksopplysninger(
        saksbehandler: Saksbehandler,
        oppdaterteSaksopplysninger: Saksopplysninger,
    ): Behandling {
        if (!saksbehandler.erSaksbehandler()) {
            throw IllegalArgumentException("Kunne ikke oppdatere saksopplysinger. Saksbehandler mangler rollen SAKSBEHANDLER. sakId=$sakId, behandlingId=$id")
        }
        if (this.saksbehandler != saksbehandler.navIdent) {
            throw IllegalArgumentException("Kunne ikke oppdatere saksopplysinger. Saksbehandler er ikke satt på behandlingen. sakId=$sakId, behandlingId=$id")
        }
        if (!this.erUnderBehandling) {
            throw IllegalArgumentException("Kunne ikke oppdatere saksopplysinger. Behandling er ikke under behandling. sakId=$sakId, behandlingId=$id, status=$status")
        }
        return this.copy(saksopplysninger = oppdaterteSaksopplysninger)
    }

    fun oppdaterBegrunnelseVilkårsvurdering(
        saksbehandler: Saksbehandler,
        begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering,
    ): Behandling {
        if (!saksbehandler.erSaksbehandler()) {
            throw IllegalArgumentException("Kunne ikke oppdatere begrunnelse/vilkårsvurdering. Saksbehandler mangler rollen SAKSBEHANDLER. sakId=$sakId, behandlingId=$id")
        }
        if (this.saksbehandler != saksbehandler.navIdent) {
            throw IllegalArgumentException("Kunne ikke oppdatere begrunnelse/vilkårsvurdering. Saksbehandler er ikke satt på behandlingen. sakId=$sakId, behandlingId=$id")
        }
        if (!this.erUnderBehandling) {
            throw IllegalArgumentException("Kunne ikke oppdatere begrunnelse/vilkårsvurdering. Behandling er ikke under behandling. sakId=$sakId, behandlingId=$id, status=$status")
        }
        return this.copy(begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering)
    }

    fun oppdaterFritekstTilVedtaksbrev(
        saksbehandler: Saksbehandler,
        fritekstTilVedtaksbrev: FritekstTilVedtaksbrev,
    ): Behandling {
        if (!saksbehandler.erSaksbehandler()) {
            throw IllegalArgumentException("Kunne ikke oppdatere fritekst til vedtaksbrev. Saksbehandler mangler rollen SAKSBEHANDLER. sakId=$sakId, behandlingId=$id")
        }
        if (this.saksbehandler != saksbehandler.navIdent) {
            throw IllegalArgumentException("Kunne ikke oppdatere fritekst til vedtaksbrev. Saksbehandler er ikke satt på behandlingen. sakId=$sakId, behandlingId=$id")
        }
        if (!this.erUnderBehandling) {
            throw IllegalArgumentException("Kunne ikke oppdatere fritekst til vedtaksbrev. Behandling er ikke under behandling. sakId=$sakId, behandlingId=$id, status=$status")
        }
        return this.copy(fritekstTilVedtaksbrev = fritekstTilVedtaksbrev)
    }

    init {
        if (beslutter != null && saksbehandler != null) {
            require(beslutter != saksbehandler) { "Saksbehandler og beslutter kan ikke være samme person" }
        }
        when (behandlingstype) {
            Behandlingstype.FØRSTEGANGSBEHANDLING -> {
                requireNotNull(søknad) { "Søknad må være satt for førstegangsbehandling" }
                require(søknad.barnetillegg.isEmpty()) {
                    "Barnetillegg er ikke støttet i MVP 1"
                }
            }

            Behandlingstype.REVURDERING -> {
                require(søknad == null) { "Søknad kan ikke være satt for revurdering" }
            }
        }

        when (status) {
            KLAR_TIL_BEHANDLING -> {
                require(saksbehandler == null) {
                    "Behandlingen kan ikke være tilknyttet en saksbehandler når statusen er KLAR_TIL_BEHANDLING"
                }
                // Selvom beslutter har underkjent, må vi kunne ta hen av behandlingen.
                require(iverksattTidspunkt == null)
            }

            UNDER_BEHANDLING -> {
                requireNotNull(saksbehandler) {
                    "Behandlingen må være tilknyttet en saksbehandler når status er UNDER_BEHANDLING"
                }
                // Selvom beslutter har underkjent, må vi kunne ta hen av behandlingen.
                require(iverksattTidspunkt == null)
            }

            KLAR_TIL_BESLUTNING -> {
                // Vi kan ikke ta saksbehandler av behandlingen før den underkjennes.
                requireNotNull(saksbehandler) { "Behandlingen må ha saksbehandler når status er KLAR_TIL_BESLUTNING" }
                require(beslutter == null) {
                    "Behandlingen kan ikke være tilknyttet en beslutter når status er KLAR_TIL_BESLUTNING"
                }
                require(iverksattTidspunkt == null)
            }

            UNDER_BESLUTNING -> {
                // Vi kan ikke ta saksbehandler av behandlingen før den underkjennes.
                requireNotNull(saksbehandler) { "Behandlingen må ha saksbehandler når status er UNDER_BESLUTNING" }
                requireNotNull(beslutter) { "Behandlingen må tilknyttet en beslutter når status er UNDER_BESLUTNING" }
                require(iverksattTidspunkt == null)
            }

            VEDTATT -> {
                // Det er viktig at vi ikke tar saksbehandler og beslutter av behandlingen når status er VEDTATT.
                requireNotNull(beslutter) { "Behandlingen må ha beslutter når status er VEDTATT" }
                requireNotNull(saksbehandler) { "Behandlingen må ha saksbehandler når status er VEDTATT" }
                requireNotNull(iverksattTidspunkt)
                requireNotNull(sendtTilBeslutning)
            }
        }
    }
}
