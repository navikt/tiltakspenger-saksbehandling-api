package no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.felles.OppgaveId
import no.nav.tiltakspenger.saksbehandling.felles.nå
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.Behandlingsstatus.AVBRUTT
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.Behandlingsstatus.KLAR_TIL_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.Behandlingsstatus.KLAR_TIL_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.Behandlingsstatus.UNDER_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.Behandlingsstatus.UNDER_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.Behandlingsstatus.VEDTATT
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.Behandlingstype.FØRSTEGANGSBEHANDLING
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.Behandlingstype.REVURDERING
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.tiltak.Tiltaksdeltagelse
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.tiltak.ValgteTiltaksdeltakelser
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.vilkår.Utfallsperiode
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.vilkår.Utfallsperiode.IKKE_RETT_TIL_TILTAKSPENGER
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.vilkår.Utfallsperiode.RETT_TIL_TILTAKSPENGER
import java.time.LocalDate
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
    val barnetillegg: Barnetillegg?,
    val valgteTiltaksdeltakelser: ValgteTiltaksdeltakelser?,
    val avbrutt: Avbrutt?,
) {
    val erAvsluttet: Boolean by lazy { status == AVBRUTT || status == VEDTATT }
    val erUnderBehandling: Boolean = status == UNDER_BEHANDLING

    val erVedtatt: Boolean = status == VEDTATT

    val maksDagerMedTiltakspengerForPeriode: Int = MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE

    fun inneholderEksternDeltagelseId(eksternDeltagelseId: String): Boolean =
        saksopplysninger.tiltaksdeltagelse.find { it.eksternDeltagelseId == eksternDeltagelseId } != null

    fun getTiltaksdeltagelse(eksternDeltagelseId: String): Tiltaksdeltagelse? = saksopplysninger.getTiltaksdeltagelse(eksternDeltagelseId)

    /**
     * null dersom [virkningsperiode] ikke er satt enda. Typisk i stegene før til beslutning eller ved avslag.
     *
     * Dersom det er en innvilgelse, vil hele utfallsperioden være: [Utfallsperiode.RETT_TIL_TILTAKSPENGER]
     * Dersom det er et avslag, vil den være null.
     * Dersom det er en revurdering stans/opphør, vil hele utfallsperioden være: [Utfallsperiode.IKKE_RETT_TIL_TILTAKSPENGER]
     *
     */
    val utfallsperioder: Periodisering<Utfallsperiode>? by lazy {
        if (virkningsperiode == null) return@lazy null
        when (behandlingstype) {
            FØRSTEGANGSBEHANDLING -> Periodisering<Utfallsperiode>(RETT_TIL_TILTAKSPENGER, virkningsperiode)
            REVURDERING -> Periodisering<Utfallsperiode>(IKKE_RETT_TIL_TILTAKSPENGER, virkningsperiode)
        }
    }

    val erFørstegangsbehandling: Boolean = behandlingstype == FØRSTEGANGSBEHANDLING
    val erRevurdering: Boolean = behandlingstype == REVURDERING

    /** Påkrevd ved førstegangsbehandling/søknadsbehandling, men kan være null ved revurdering. */
    val kravtidspunkt = søknad?.tidsstempelHosOss

    companion object {
        /** Hardkoder denne til 10 for nå. På sikt vil vi la saksbehandler periodisere dette selv, litt på samme måte som barnetillegg. */
        const val MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE: Int = 10

        suspend fun opprettSøknadsbehandling(
            sakId: SakId,
            saksnummer: Saksnummer,
            fnr: Fnr,
            søknad: Søknad,
            saksbehandler: Saksbehandler,
            hentSaksopplysninger: suspend (saksopplysningsperiode: Periode) -> Saksopplysninger,
        ): Either<KanIkkeOppretteBehandling, Behandling> {
            val opprettet = nå()

            /** Kommentar jah: Det kan bli aktuelt at saksbehandler får endre på fraOgMed her. */
            val saksopplysningsperiode: Periode = run {
                // § 11: Tiltakspenger og barnetillegg gis for opptil tre måneder før den måneden da kravet om ytelsen ble satt fram, dersom vilkårene var oppfylt i denne perioden.
                val fraOgMed = søknad.kravdato.withDayOfMonth(1).minusMonths(3)
                // Forskriften gir ingen begrensninger fram i tid. 100 år bør være nok.
                val tilOgMed = fraOgMed.plusYears(100)
                Periode(fraOgMed, tilOgMed)
            }

            val saksopplysninger = hentSaksopplysninger(saksopplysningsperiode)

            if (saksopplysninger.tiltaksdeltagelse.isEmpty()) {
                return KanIkkeOppretteBehandling.IngenRelevanteTiltak.left()
            }

            return Behandling(
                id = BehandlingId.random(),
                saksnummer = saksnummer,
                sakId = sakId,
                fnr = fnr,
                søknad = søknad,
                virkningsperiode = null,
                saksopplysninger = saksopplysninger,
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
                behandlingstype = FØRSTEGANGSBEHANDLING,
                oppgaveId = søknad.oppgaveId,
                saksopplysningsperiode = saksopplysningsperiode,
                barnetillegg = null,
                valgteTiltaksdeltakelser = null,
                avbrutt = null,
            ).right()
        }

        suspend fun opprettRevurdering(
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
                behandlingstype = REVURDERING,
                // her kan man på sikt lagre oppgaveId hvis man oppretter oppgave for revurdering
                oppgaveId = null,
                // Kommentar John: Dersom en revurdering tar utgangspunkt i en søknad, bør denne bestemmes på samme måte som for førstegangsbehandling.
                saksopplysningsperiode = saksopplysningsperiode,
                barnetillegg = null,
                valgteTiltaksdeltakelser = null,
                avbrutt = null,
            )
        }
    }

    /** Saksbehandler/beslutter tar eller overtar behandlingen. */
    fun taBehandling(saksbehandler: Saksbehandler): Behandling {
        return when (this.status) {
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

            AVBRUTT -> throw IllegalArgumentException(
                "Kan ikke ta behandling når behandlingen er AVBRUTT. Behandlingsstatus: ${this.status}. Utøvende saksbehandler: $saksbehandler. Saksbehandler på behandling: ${this.saksbehandler}",
            )
        }
    }

    fun tilBeslutning(
        kommando: SendSøknadsbehandlingTilBeslutningKommando,
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
            barnetillegg = kommando.barnetillegg,
            valgteTiltaksdeltakelser = kommando.valgteTiltaksdeltakelser(this),
        )
    }

    fun sendRevurderingTilBeslutning(
        kommando: SendRevurderingTilBeslutningKommando,
        sisteDagSomGirRett: LocalDate,
    ): Behandling {
        check(status == UNDER_BEHANDLING) {
            "Behandlingen må være under behandling, det innebærer også at en saksbehandler må ta saken før den kan sendes til beslutter. Behandlingsstatus: ${this.status}. Utøvende saksbehandler: $saksbehandler. Saksbehandler på behandling: ${this.saksbehandler}"
        }
        check(kommando.saksbehandler.navIdent == this.saksbehandler) { "Det er ikke lov å sende en annen sin behandling til beslutter" }

        return this.copy(
            status = if (beslutter == null) KLAR_TIL_BESLUTNING else UNDER_BESLUTNING,
            sendtTilBeslutning = nå(),
            begrunnelseVilkårsvurdering = kommando.begrunnelse,
            virkningsperiode = Periode(kommando.stansDato, sisteDagSomGirRett),
        )
    }

    fun iverksett(
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

            KLAR_TIL_BEHANDLING, UNDER_BEHANDLING, KLAR_TIL_BESLUTNING, VEDTATT, AVBRUTT -> throw IllegalStateException(
                "Må ha status UNDER_BESLUTNING for å iverksette. Behandlingsstatus: $status",
            )
        }
    }

    fun sendTilbakeTilBehandling(
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

            KLAR_TIL_BEHANDLING, UNDER_BEHANDLING, KLAR_TIL_BESLUTNING, VEDTATT, AVBRUTT -> throw IllegalStateException(
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

    fun oppdaterBarnetillegg(kommando: OppdaterBarnetilleggKommando): Behandling {
        if (!kommando.saksbehandler.erSaksbehandler()) {
            throw IllegalArgumentException("Kunne ikke oppdatere barnetillegg. Saksbehandler mangler rollen SAKSBEHANDLER. sakId=$sakId, behandlingId=$id")
        }
        if (this.saksbehandler != kommando.saksbehandler.navIdent) {
            throw IllegalArgumentException("Kunne ikke oppdatere barnetillegg. Saksbehandler er ikke satt på behandlingen. sakId=$sakId, behandlingId=$id")
        }
        if (!this.erUnderBehandling) {
            throw IllegalArgumentException("Kunne ikke oppdatere barnetillegg. Behandling er ikke under behandling. sakId=$sakId, behandlingId=$id, status=$status")
        }
        return this.copy(
            barnetillegg = kommando.barnetillegg(this.virkningsperiode),
        )
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

    fun avbryt(avbruttAv: Saksbehandler, begrunnelse: String, tidspunkt: LocalDateTime): Behandling {
        if (this.status == AVBRUTT || avbrutt != null) {
            throw IllegalArgumentException("Behandlingen er allerede avbrutt")
        }
        return this.copy(
            status = AVBRUTT,
            søknad = this.søknad?.avbryt(avbruttAv, begrunnelse, tidspunkt),
            avbrutt = Avbrutt(
                tidspunkt = tidspunkt,
                saksbehandler = avbruttAv.navIdent,
                begrunnelse = begrunnelse,
            ),
        )
    }

    init {
        if (beslutter != null && saksbehandler != null) {
            require(beslutter != saksbehandler) { "Saksbehandler og beslutter kan ikke være samme person" }
        }
        when (behandlingstype) {
            FØRSTEGANGSBEHANDLING -> {
                requireNotNull(søknad) { "Søknad må være satt for førstegangsbehandling" }
            }

            REVURDERING -> {
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
                require(virkningsperiode != null) { "Virkningsperiode må være satt for statusen KLAR_TIL_BESLUTNING" }
                if (barnetillegg != null) {
                    val barnetilleggsperiode = barnetillegg.periodisering.totalePeriode
                    require(barnetilleggsperiode == virkningsperiode) { "Barnetilleggsperioden ($barnetilleggsperiode) må ha samme periode som virkningsperioden($virkningsperiode)" }
                }
                if (behandlingstype == FØRSTEGANGSBEHANDLING) {
                    require(valgteTiltaksdeltakelser != null) { "Valgte tiltaksdeltakelser må være satt for førstegangsbehandling" }
                    require(valgteTiltaksdeltakelser.periodisering.totalePeriode == virkningsperiode) { "Total periode for valgte tiltaksdeltakelser (${valgteTiltaksdeltakelser.periodisering.totalePeriode}) må stemme overens med virkningsperioden ($virkningsperiode)" }
                }
            }

            UNDER_BESLUTNING -> {
                // Vi kan ikke ta saksbehandler av behandlingen før den underkjennes.
                requireNotNull(saksbehandler) { "Behandlingen må ha saksbehandler når status er UNDER_BESLUTNING" }
                requireNotNull(beslutter) { "Behandlingen må tilknyttet en beslutter når status er UNDER_BESLUTNING" }
                require(iverksattTidspunkt == null)
                require(virkningsperiode != null) { "Virkningsperiode må være satt for statusen UNDER_BESLUTNING" }
                if (barnetillegg != null) {
                    val barnetilleggsperiode = barnetillegg.periodisering.totalePeriode
                    require(barnetilleggsperiode == virkningsperiode) { "Barnetilleggsperioden ($barnetilleggsperiode) må ha samme periode som virkningsperioden($virkningsperiode)" }
                }
                if (behandlingstype == FØRSTEGANGSBEHANDLING) {
                    require(valgteTiltaksdeltakelser != null) { "Valgte tiltaksdeltakelser må være satt for førstegangsbehandling" }
                    require(valgteTiltaksdeltakelser.periodisering.totalePeriode == virkningsperiode) { "Total periode for valgte tiltaksdeltakelser (${valgteTiltaksdeltakelser.periodisering.totalePeriode}) må stemme overens med virkningsperioden ($virkningsperiode)" }
                }
            }

            VEDTATT -> {
                // Det er viktig at vi ikke tar saksbehandler og beslutter av behandlingen når status er VEDTATT.
                requireNotNull(beslutter) { "Behandlingen må ha beslutter når status er VEDTATT" }
                requireNotNull(saksbehandler) { "Behandlingen må ha saksbehandler når status er VEDTATT" }
                requireNotNull(iverksattTidspunkt)
                requireNotNull(sendtTilBeslutning)
                require(virkningsperiode != null) { "Virkningsperiode må være satt for statusen VEDTATT" }
                if (barnetillegg != null) {
                    val barnetilleggsperiode = barnetillegg.periodisering.totalePeriode
                    require(barnetilleggsperiode == virkningsperiode) { "Barnetilleggsperioden ($barnetilleggsperiode) må ha samme periode som virkningsperioden($virkningsperiode)" }
                }
                if (behandlingstype == FØRSTEGANGSBEHANDLING) {
                    require(valgteTiltaksdeltakelser != null) { "Valgte tiltaksdeltakelser må være satt for førstegangsbehandling" }
                    require(valgteTiltaksdeltakelser.periodisering.totalePeriode == virkningsperiode) { "Total periode for valgte tiltaksdeltakelser (${valgteTiltaksdeltakelser.periodisering.totalePeriode}) må stemme overens med virkningsperioden ($virkningsperiode)" }
                }
            }

            AVBRUTT -> {
                requireNotNull(avbrutt)
            }
        }
    }
}
