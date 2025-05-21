package no.nav.tiltakspenger.saksbehandling.behandling.domene

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.AVBRUTT
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.KLAR_TIL_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.KLAR_TIL_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.UNDER_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.UNDER_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.VEDTATT
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.overta.KunneIkkeOvertaBehandling
import no.nav.tiltakspenger.saksbehandling.felles.Attestering
import no.nav.tiltakspenger.saksbehandling.felles.Avbrutt
import no.nav.tiltakspenger.saksbehandling.felles.Utfallsperiode
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.søknad.Søknad
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.Tiltaksdeltagelse
import java.time.Clock
import java.time.LocalDateTime

/** Hardkoder denne til 10 for nå. På sikt vil vi la saksbehandler periodisere dette selv, litt på samme måte som barnetillegg. */
const val MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE: Int = 10

sealed interface Behandling {
    val id: BehandlingId
    val status: Behandlingsstatus
    val opprettet: LocalDateTime

    val sistEndret: LocalDateTime
    val iverksattTidspunkt: LocalDateTime?
    val sendtTilDatadeling: LocalDateTime?
    val sakId: SakId
    val oppgaveId: OppgaveId?

    val saksnummer: Saksnummer
    val fnr: Fnr
    val saksopplysninger: Saksopplysninger

    val saksopplysningsperiode: Periode
    val saksbehandler: String?

    val beslutter: String?
    val sendtTilBeslutning: LocalDateTime?
    val attesteringer: List<Attestering>
    val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?
    val avbrutt: Avbrutt?
    val utfall: BehandlingUtfall?
    val virkningsperiode: Periode?
    val begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering?

    val barnetillegg: Barnetillegg?
    val utfallsperioder: Periodisering<Utfallsperiode>?

    val behandlingstype: Behandlingstype
        get() = when (this) {
            is Revurdering -> Behandlingstype.REVURDERING
            is Søknadsbehandling -> Behandlingstype.SØKNADSBEHANDLING
        }

    val erUnderBehandling: Boolean get() = status == UNDER_BEHANDLING
    val erAvbrutt: Boolean get() = status == AVBRUTT
    val erVedtatt: Boolean get() = status == VEDTATT
    val erAvsluttet: Boolean get() = erAvbrutt || erVedtatt
    val maksDagerMedTiltakspengerForPeriode: Int get() = MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE

    fun inneholderEksternDeltagelseId(eksternDeltagelseId: String): Boolean =
        saksopplysninger.tiltaksdeltagelse.find { it.eksternDeltagelseId == eksternDeltagelseId } != null

    fun getTiltaksdeltagelse(eksternDeltagelseId: String): Tiltaksdeltagelse? =
        saksopplysninger.getTiltaksdeltagelse(eksternDeltagelseId)

    fun avbryt(avbruttAv: Saksbehandler, begrunnelse: String, tidspunkt: LocalDateTime): Behandling

    fun leggTilbakeBehandling(saksbehandler: Saksbehandler): Either<KanIkkeLeggeTilbakeBehandling, Behandling> {
        return when (status) {
            KLAR_TIL_BEHANDLING -> throw IllegalStateException("Kan ikke legge tilbake behandling som ikke er påbegynt")
            UNDER_BEHANDLING -> {
                check(saksbehandler.erSaksbehandler()) {
                    "Saksbehandler må ha rolle saksbehandler. Utøvende saksbehandler: $saksbehandler"
                }
                require(this.saksbehandler == saksbehandler.navIdent) {
                    return KanIkkeLeggeTilbakeBehandling.MåVæreSaksbehandlerEllerBeslutterForBehandlingen.left()
                }
                require(this.beslutter == null) { "Beslutter skal ikke kunne være satt på behandlingen dersom den er UNDER_BEHANDLING" }

                when (this) {
                    is Søknadsbehandling -> this.copy(saksbehandler = null, status = KLAR_TIL_BEHANDLING).right()
                    is Revurdering -> this.copy(saksbehandler = null, status = KLAR_TIL_BEHANDLING).right()
                }
            }

            KLAR_TIL_BESLUTNING -> throw IllegalStateException("Kan ikke legge tilbake behandling som er klar til beslutning")
            UNDER_BESLUTNING -> {
                check(saksbehandler.erBeslutter()) {
                    "Saksbehandler må ha beslutterrolle. Utøvende saksbehandler: $saksbehandler"
                }
                require(this.beslutter == saksbehandler.navIdent) {
                    return KanIkkeLeggeTilbakeBehandling.MåVæreSaksbehandlerEllerBeslutterForBehandlingen.left()
                }

                when (this) {
                    is Søknadsbehandling -> this.copy(beslutter = null, status = KLAR_TIL_BESLUTNING).right()
                    is Revurdering -> this.copy(beslutter = null, status = KLAR_TIL_BESLUTNING).right()
                }
            }

            VEDTATT, AVBRUTT -> {
                throw IllegalArgumentException(
                    "Kan ikke legge tilbake behandling når behandlingen er ${this.status}. Utøvende saksbehandler: $saksbehandler. Saksbehandler på behandling: ${this.saksbehandler}",
                )
            }
        }
    }

    /** Saksbehandler/beslutter tar eller overtar behandlingen. */
    fun taBehandling(saksbehandler: Saksbehandler): Behandling {
        return when (status) {
            KLAR_TIL_BEHANDLING -> {
                check(saksbehandler.erSaksbehandler()) {
                    "Saksbehandler må ha rolle saksbehandler. Utøvende saksbehandler: $saksbehandler"
                }
                require(this.saksbehandler == null) { "Saksbehandler skal ikke kunne være satt på behandlingen dersom den er KLAR_TIL_BEHANDLING" }
                require(this.beslutter == null) { "Beslutter skal ikke kunne være satt på behandlingen dersom den er KLAR_TIL_BEHANDLING" }

                // Meeeeeeh
                when (this) {
                    is Søknadsbehandling -> this.copy(saksbehandler = saksbehandler.navIdent, status = UNDER_BEHANDLING)
                    is Revurdering -> this.copy(saksbehandler = saksbehandler.navIdent, status = UNDER_BEHANDLING)
                }
            }

            UNDER_BEHANDLING -> throw IllegalStateException("Skal kun kunne ta behandlingen dersom det er registrert en saksbehandler fra før. For å overta behandlingen, skal andre operasjoner bli brukt")
            KLAR_TIL_BESLUTNING -> {
                check(saksbehandler.navIdent != this.saksbehandler) {
                    "Beslutter ($saksbehandler) kan ikke være den samme som saksbehandleren (${this.saksbehandler}"
                }
                check(saksbehandler.erBeslutter()) {
                    "Saksbehandler må ha beslutterrolle. Utøvende saksbehandler: $saksbehandler"
                }
                require(this.beslutter == null) { "Behandlingen har en eksisterende beslutter. For å overta behandlingen, bruk overta() - behandlingsId: ${this.id}" }

                when (this) {
                    is Søknadsbehandling -> this.copy(beslutter = saksbehandler.navIdent, status = UNDER_BESLUTNING)
                    is Revurdering -> this.copy(beslutter = saksbehandler.navIdent, status = UNDER_BESLUTNING)
                }
            }

            UNDER_BESLUTNING -> throw IllegalStateException("Skal kun kunne ta behandlingen dersom det er registrert en beslutter fra før. For å overta behandlingen, skal andre operasjoner bli brukt")

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

    fun overta(saksbehandler: Saksbehandler, clock: Clock): Either<KunneIkkeOvertaBehandling, Behandling> {
        return when (status) {
            KLAR_TIL_BEHANDLING -> KunneIkkeOvertaBehandling.BehandlingenMåVæreUnderBehandlingForÅOverta.left()
            UNDER_BEHANDLING -> {
                check(saksbehandler.erSaksbehandler()) {
                    "Saksbehandler må ha rolle saksbehandler. Utøvende saksbehandler: $saksbehandler"
                }
                if (this.saksbehandler == null) {
                    return KunneIkkeOvertaBehandling.BehandlingenErIkkeKnyttetTilEnSaksbehandlerForÅOverta.left()
                }

                when (this) {
                    is Søknadsbehandling -> this.copy(
                        saksbehandler = saksbehandler.navIdent,
                        sistEndret = LocalDateTime.now(clock),
                    ).let {
                        // dersom det er beslutteren som overtar behandlingen, skal dem nulles ut som beslutter
                        if (it.beslutter == saksbehandler.navIdent) it.copy(beslutter = null) else it
                    }

                    is Revurdering -> this.copy(
                        saksbehandler = saksbehandler.navIdent,
                        sistEndret = LocalDateTime.now(clock),
                    ).let {
                        // dersom det er beslutteren som overtar behandlingen, skal dem nulles ut som beslutter
                        if (it.beslutter == saksbehandler.navIdent) it.copy(beslutter = null) else it
                    }
                }.right()
            }

            KLAR_TIL_BESLUTNING -> KunneIkkeOvertaBehandling.BehandlingenMåVæreUnderBeslutningForÅOverta.left()

            UNDER_BESLUTNING -> {
                check(saksbehandler.erBeslutter()) {
                    "Saksbehandler må ha beslutterrolle. Utøvende saksbehandler: $saksbehandler"
                }
                if (this.beslutter == null) {
                    return KunneIkkeOvertaBehandling.BehandlingenErIkkeKnyttetTilEnBeslutterForÅOverta.left()
                }
                if (this.saksbehandler == saksbehandler.navIdent) {
                    return KunneIkkeOvertaBehandling.SaksbehandlerOgBeslutterKanIkkeVæreDenSamme.left()
                }

                when (this) {
                    is Søknadsbehandling -> this.copy(
                        beslutter = saksbehandler.navIdent,
                        sistEndret = LocalDateTime.now(clock),
                    )

                    is Revurdering -> this.copy(
                        beslutter = saksbehandler.navIdent,
                        sistEndret = LocalDateTime.now(clock),
                    )
                }.right()
            }

            VEDTATT,
            AVBRUTT,
            -> KunneIkkeOvertaBehandling.BehandlingenKanIkkeVæreVedtattEllerAvbrutt.left()
        }
    }

    fun iverksett(
        utøvendeBeslutter: Saksbehandler,
        attestering: Attestering,
        clock: Clock,
    ): Behandling {
        require(virkningsperiode != null) { "virkningsperiode må være satt ved iverksetting" }

        return when (status) {
            UNDER_BESLUTNING -> {
                check(utøvendeBeslutter.erBeslutter()) { "utøvende saksbehandler må være beslutter" }
                check(this.beslutter == utøvendeBeslutter.navIdent) { "Kan ikke iverksette en behandling man ikke er beslutter på" }
                check(!this.attesteringer.any { it.isGodkjent() }) {
                    "Behandlingen er allerede godkjent"
                }

                when (this) {
                    is Søknadsbehandling -> this.copy(
                        status = VEDTATT,
                        attesteringer = attesteringer + attestering,
                        iverksattTidspunkt = nå(clock),
                    )

                    is Revurdering -> this.copy(
                        status = VEDTATT,
                        attesteringer = attesteringer + attestering,
                        iverksattTidspunkt = nå(clock),
                    )
                }
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

                when (this) {
                    is Søknadsbehandling -> this.copy(
                        status = UNDER_BEHANDLING,
                        attesteringer = attesteringer + attestering,
                    )

                    is Revurdering -> this.copy(
                        status = UNDER_BEHANDLING,
                        attesteringer = attesteringer + attestering,
                    )
                }
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
        if (virkningsperiode == nyPeriode) {
            return this
        }

        val nyVirkningsperiode = virkningsperiode?.let {
            require(it.inneholderHele(nyPeriode)) {
                "Ny periode ($nyPeriode) må være innenfor vedtakets virkningsperiode ($virkningsperiode)"
            }
            nyPeriode
        }

        return when (this) {
            is Søknadsbehandling -> this.copy(
                virkningsperiode = nyVirkningsperiode,
            )

            is Revurdering -> this.copy(
                virkningsperiode = nyVirkningsperiode,
            )
        }
    }

    fun oppdaterBegrunnelseVilkårsvurdering(
        saksbehandler: Saksbehandler,
        begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering,
    ): Behandling {
        validerKanOppdatere(saksbehandler, "Kunne ikke oppdatere begrunnelse/vilkårsvurdering")

        return when (this) {
            is Søknadsbehandling -> this.copy(begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering)
            is Revurdering -> this.copy(begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering)
        }
    }

    fun oppdaterSaksopplysninger(
        saksbehandler: Saksbehandler,
        oppdaterteSaksopplysninger: Saksopplysninger,
    ): Behandling {
        validerKanOppdatere(saksbehandler, "Kunne ikke oppdatere saksopplysinger")

        return when (this) {
            is Søknadsbehandling -> this.copy(saksopplysninger = oppdaterteSaksopplysninger)
            is Revurdering -> this.copy(saksopplysninger = oppdaterteSaksopplysninger)
        }
    }

    fun oppdaterFritekstTilVedtaksbrev(
        saksbehandler: Saksbehandler,
        fritekstTilVedtaksbrev: FritekstTilVedtaksbrev,
    ): Behandling {
        validerKanOppdatere(saksbehandler, "Kunne ikke oppdatere fritekst til vedtaksbrev")

        return when (this) {
            is Søknadsbehandling -> this.copy(fritekstTilVedtaksbrev = fritekstTilVedtaksbrev)
            is Revurdering -> this.copy(fritekstTilVedtaksbrev = fritekstTilVedtaksbrev)
        }
    }

    fun validerKanOppdatere(saksbehandler: Saksbehandler, errorMsg: String) {
        if (!saksbehandler.erSaksbehandler()) {
            throw IllegalArgumentException("$errorMsg - Saksbehandler ${saksbehandler.navIdent} mangler rollen SAKSBEHANDLER - sakId=$sakId, behandlingId=$id")
        }
        if (this.saksbehandler != saksbehandler.navIdent) {
            throw IllegalArgumentException("$errorMsg - Saksbehandler ${saksbehandler.navIdent} er ikke satt på behandlingen - sakId=$sakId, behandlingId=$id")
        }
        if (!this.erUnderBehandling) {
            throw IllegalStateException("$errorMsg - Behandlingen er ikke under behandling - sakId=$sakId, behandlingId=$id, status=$status")
        }
    }

    fun init() {
        if (beslutter != null && saksbehandler != null) {
            require(beslutter != saksbehandler) { "Saksbehandler og beslutter kan ikke være samme person" }
        }

        when (status) {
            KLAR_TIL_BEHANDLING -> {
                require(saksbehandler == null) {
                    "Behandlingen kan ikke være tilknyttet en saksbehandler når statusen er KLAR_TIL_BEHANDLING"
                }
                // Selvom beslutter har underkjent, må vi kunne ta hen av behandlingen.
                require(iverksattTidspunkt == null)
                require(beslutter == null)
            }

            UNDER_BEHANDLING -> {
                requireNotNull(saksbehandler) {
                    "Behandlingen må være tilknyttet en saksbehandler når status er UNDER_BEHANDLING"
                }
                // Selvom beslutter har underkjent, må vi kunne ta hen av behandlingen.
                require(iverksattTidspunkt == null)
                if (attesteringer.isEmpty()) {
                    require(beslutter == null) { "Beslutter kan ikke være tilknyttet behandlingen dersom det ikke er gjort noen attesteringer" }
                } else {
                    require(utfall != null) { "Behandlingsutfall må være satt dersom det er gjort attesteringer på behandlingen" }
                }
            }

            KLAR_TIL_BESLUTNING -> {
                // Vi kan ikke ta saksbehandler av behandlingen før den underkjennes.
                requireNotNull(saksbehandler) { "Behandlingen må ha saksbehandler når status er KLAR_TIL_BESLUTNING" }
                require(beslutter == null) {
                    "Behandlingen kan ikke være tilknyttet en beslutter når status er KLAR_TIL_BESLUTNING"
                }
                require(iverksattTidspunkt == null)
                require(virkningsperiode != null) { "Virkningsperiode må være satt for statusen KLAR_TIL_BESLUTNING" }
                require(this.utfall != null) { "Behandlingsutfall må være satt for statusen KLAR_TIL_BESLUTNING" }
            }

            UNDER_BESLUTNING -> {
                // Vi kan ikke ta saksbehandler av behandlingen før den underkjennes.
                requireNotNull(saksbehandler) { "Behandlingen må ha saksbehandler når status er UNDER_BESLUTNING" }
                requireNotNull(beslutter) { "Behandlingen må tilknyttet en beslutter når status er UNDER_BESLUTNING" }
                require(iverksattTidspunkt == null)
                require(virkningsperiode != null) { "Virkningsperiode må være satt for statusen UNDER_BESLUTNING" }
                require(this.utfall != null) { "Behandlingsutfall må være satt for statusen UNDER_BESLUTNING" }
            }

            VEDTATT -> {
                // Det er viktig at vi ikke tar saksbehandler og beslutter av behandlingen når status er VEDTATT.
                requireNotNull(beslutter) { "Behandlingen må ha beslutter når status er VEDTATT" }
                requireNotNull(saksbehandler) { "Behandlingen må ha saksbehandler når status er VEDTATT" }
                requireNotNull(iverksattTidspunkt)
                requireNotNull(sendtTilBeslutning)
                require(virkningsperiode != null) { "Virkningsperiode må være satt for statusen VEDTATT" }
                require(this.utfall != null) { "Behandlingsutfall må være satt for statusen VEDTATT" }
            }

            AVBRUTT -> {
                requireNotNull(avbrutt)
            }
        }
    }
}
