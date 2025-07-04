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
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.AVBRUTT
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.KLAR_TIL_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.KLAR_TIL_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.UNDER_AUTOMATISK_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.UNDER_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.UNDER_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.VEDTATT
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.overta.KunneIkkeOvertaBehandling
import no.nav.tiltakspenger.saksbehandling.felles.Attestering
import no.nav.tiltakspenger.saksbehandling.felles.Avbrutt
import no.nav.tiltakspenger.saksbehandling.felles.Utfallsperiode
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.TilgangException
import no.nav.tiltakspenger.saksbehandling.felles.krevBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerRolle
import no.nav.tiltakspenger.saksbehandling.infra.setup.AUTOMATISK_SAKSBEHANDLER_ID
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.Tiltaksdeltagelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.ValgteTiltaksdeltakelser
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

    val saksnummer: Saksnummer
    val fnr: Fnr
    val saksopplysninger: Saksopplysninger

    val saksbehandler: String?
    val beslutter: String?
    val sendtTilBeslutning: LocalDateTime?
    val attesteringer: List<Attestering>

    val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?
    val avbrutt: Avbrutt?
    val resultat: BehandlingResultat?
    val virkningsperiode: Periode?
    val begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering?

    val valgteTiltaksdeltakelser: ValgteTiltaksdeltakelser?
    val barnetillegg: Barnetillegg?
    val utfallsperioder: SammenhengendePeriodisering<Utfallsperiode>?
    val antallDagerPerMeldeperiode: SammenhengendePeriodisering<AntallDagerForMeldeperiode>?

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

    val saksopplysningsperiode: Periode get() = saksopplysninger.periode

    fun inneholderEksternDeltagelseId(eksternDeltagelseId: String): Boolean =
        saksopplysninger.tiltaksdeltagelse.find { it.eksternDeltagelseId == eksternDeltagelseId } != null

    fun getTiltaksdeltagelse(eksternDeltagelseId: String): Tiltaksdeltagelse? =
        saksopplysninger.getTiltaksdeltagelse(eksternDeltagelseId)

    fun avbryt(avbruttAv: Saksbehandler, begrunnelse: String, tidspunkt: LocalDateTime): Behandling

    fun leggTilbakeBehandling(saksbehandler: Saksbehandler): Behandling {
        return when (status) {
            UNDER_BEHANDLING -> {
                krevSaksbehandlerRolle(saksbehandler)
                require(this.saksbehandler == saksbehandler.navIdent) {
                    "Kan bare legge tilbake behandling dersom saksbehandler selv er på behandlingen"
                }

                when (this) {
                    is Søknadsbehandling -> this.copy(saksbehandler = null, status = KLAR_TIL_BEHANDLING)
                    is Revurdering -> this.copy(saksbehandler = null, status = KLAR_TIL_BEHANDLING)
                }
            }

            UNDER_BESLUTNING -> {
                krevBeslutterRolle(saksbehandler)
                require(this.beslutter == saksbehandler.navIdent) {
                    "Kan bare legge tilbake behandling dersom saksbehandler selv er på behandlingen"
                }

                when (this) {
                    is Søknadsbehandling -> this.copy(beslutter = null, status = KLAR_TIL_BESLUTNING)
                    is Revurdering -> this.copy(beslutter = null, status = KLAR_TIL_BESLUTNING)
                }
            }

            KLAR_TIL_BESLUTNING -> throw IllegalStateException("Kan ikke legge tilbake behandling som er klar til beslutning")
            KLAR_TIL_BEHANDLING -> throw IllegalStateException("Kan ikke legge tilbake behandling som ikke er påbegynt")
            VEDTATT, AVBRUTT, UNDER_AUTOMATISK_BEHANDLING -> {
                throw IllegalArgumentException(
                    "Kan ikke legge tilbake behandling når behandlingen er ${this.status}. Utøvende saksbehandler: $saksbehandler. Saksbehandler på behandling: ${this.saksbehandler}",
                )
            }
        }
    }

    /** Saksbehandler/beslutter tar behandlingen. */
    fun taBehandling(saksbehandler: Saksbehandler): Behandling {
        return when (status) {
            KLAR_TIL_BEHANDLING -> {
                krevSaksbehandlerRolle(saksbehandler)

                require(this.saksbehandler == null) { "Saksbehandler skal ikke kunne være satt på behandlingen dersom den er KLAR_TIL_BEHANDLING" }

                when (this) {
                    is Søknadsbehandling -> this.copy(saksbehandler = saksbehandler.navIdent, status = UNDER_BEHANDLING)
                    is Revurdering -> this.copy(saksbehandler = saksbehandler.navIdent, status = UNDER_BEHANDLING)
                }
            }

            KLAR_TIL_BESLUTNING -> {
                check(saksbehandler.navIdent != this.saksbehandler) {
                    "Beslutter ($saksbehandler) kan ikke være den samme som saksbehandleren (${this.saksbehandler}"
                }
                krevBeslutterRolle(saksbehandler)
                require(this.beslutter == null) { "Behandlingen har en eksisterende beslutter. For å overta behandlingen, bruk overta() - behandlingsId: ${this.id}" }

                when (this) {
                    is Søknadsbehandling -> this.copy(beslutter = saksbehandler.navIdent, status = UNDER_BESLUTNING)
                    is Revurdering -> this.copy(beslutter = saksbehandler.navIdent, status = UNDER_BESLUTNING)
                }
            }

            UNDER_BEHANDLING -> throw IllegalStateException("Skal kun kunne ta behandlingen dersom det er registrert en saksbehandler fra før. For å overta behandlingen, skal andre operasjoner bli brukt")
            UNDER_BESLUTNING -> throw IllegalStateException("Skal kun kunne ta behandlingen dersom det er registrert en beslutter fra før. For å overta behandlingen, skal andre operasjoner bli brukt")
            VEDTATT, AVBRUTT, UNDER_AUTOMATISK_BEHANDLING -> {
                throw IllegalArgumentException(
                    "Kan ikke ta behandling når behandlingen har status $status. Utøvende saksbehandler: $saksbehandler. Saksbehandler på behandling: ${this.saksbehandler}",
                )
            }
        }
    }

    /** Saksbehandler/beslutter overtar behandlingen. */
    fun overta(saksbehandler: Saksbehandler, clock: Clock): Either<KunneIkkeOvertaBehandling, Behandling> {
        val nåTidMinus1Time = LocalDateTime.now(clock).minusHours(1)
        val erSistEndretMindreEnn1TimeSiden = this.sistEndret.isAfter(nåTidMinus1Time)

        if (erSistEndretMindreEnn1TimeSiden) {
            return KunneIkkeOvertaBehandling.BehandlingenErUnderAktivBehandling.left()
        }

        val oppdatertSistEndret = LocalDateTime.now(clock)

        return when (status) {
            UNDER_BEHANDLING -> {
                krevSaksbehandlerRolle(saksbehandler)
                if (this.saksbehandler == null) {
                    return KunneIkkeOvertaBehandling.BehandlingenErIkkeKnyttetTilEnSaksbehandlerForÅOverta.left()
                }

                // dersom det er beslutteren som overtar behandlingen, skal dem nulles ut som beslutter
                val beslutter = if (this.beslutter == saksbehandler.navIdent) null else this.beslutter

                when (this) {
                    is Søknadsbehandling -> this.copy(
                        saksbehandler = saksbehandler.navIdent,
                        beslutter = beslutter,
                        sistEndret = oppdatertSistEndret,
                    )

                    is Revurdering -> this.copy(
                        saksbehandler = saksbehandler.navIdent,
                        beslutter = beslutter,
                        sistEndret = oppdatertSistEndret,
                    )
                }.right()
            }

            UNDER_BESLUTNING -> {
                krevBeslutterRolle(saksbehandler)
                if (this.beslutter == null) {
                    return KunneIkkeOvertaBehandling.BehandlingenErIkkeKnyttetTilEnBeslutterForÅOverta.left()
                }
                if (this.saksbehandler == saksbehandler.navIdent) {
                    return KunneIkkeOvertaBehandling.SaksbehandlerOgBeslutterKanIkkeVæreDenSamme.left()
                }

                when (this) {
                    is Søknadsbehandling -> this.copy(
                        beslutter = saksbehandler.navIdent,
                        sistEndret = oppdatertSistEndret,
                    )

                    is Revurdering -> this.copy(
                        beslutter = saksbehandler.navIdent,
                        sistEndret = oppdatertSistEndret,
                    )
                }.right()
            }

            KLAR_TIL_BEHANDLING -> KunneIkkeOvertaBehandling.BehandlingenMåVæreUnderBehandlingForÅOverta.left()
            KLAR_TIL_BESLUTNING -> KunneIkkeOvertaBehandling.BehandlingenMåVæreUnderBeslutningForÅOverta.left()
            UNDER_AUTOMATISK_BEHANDLING -> KunneIkkeOvertaBehandling.BehandlingenKanIkkeVæreUnderAutomatiskBehandling.left()

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
                krevBeslutterRolle(utøvendeBeslutter)
                check(this.beslutter == utøvendeBeslutter.navIdent) { "Kan ikke iverksette en behandling man ikke er beslutter på" }
                check(!this.attesteringer.any { it.isGodkjent() }) {
                    "Behandlingen er allerede godkjent"
                }

                val attesteringer = attesteringer + attestering
                val iverksattTidspunkt = nå(clock)

                when (this) {
                    is Søknadsbehandling -> this.copy(
                        status = VEDTATT,
                        attesteringer = attesteringer,
                        iverksattTidspunkt = iverksattTidspunkt,
                    )

                    is Revurdering -> this.copy(
                        status = VEDTATT,
                        attesteringer = attesteringer,
                        iverksattTidspunkt = iverksattTidspunkt,
                    )
                }
            }

            KLAR_TIL_BEHANDLING,
            UNDER_BEHANDLING,
            KLAR_TIL_BESLUTNING,
            VEDTATT,
            AVBRUTT,
            UNDER_AUTOMATISK_BEHANDLING,
            -> throw IllegalStateException(
                "Må ha status UNDER_BESLUTNING for å iverksette. Behandlingsstatus: $status",
            )
        }
    }

    /**
     * Sjekker om [utøvendeBeslutter] har BESLUTTER-rollen og at det er beslutteren som har saken.
     * Hvis saken har blitt behandlet automatisk fjernes automatisk saksbehandler og flagget som sier at
     * den har blitt behandlet automatisk ved underkjenning.
     */
    fun sendTilbakeTilBehandling(
        utøvendeBeslutter: Saksbehandler,
        attestering: Attestering,
    ): Behandling {
        return when (status) {
            UNDER_BESLUTNING -> {
                krevBeslutterRolle(utøvendeBeslutter)
                check(this.beslutter == utøvendeBeslutter.navIdent) {
                    "Kun beslutter som har saken kan sende tilbake"
                }
                check(!this.attesteringer.any { it.isGodkjent() }) {
                    "Behandlingen er allerede godkjent"
                }

                val attesteringer = attesteringer + attestering

                when (this) {
                    is Søknadsbehandling -> this.copy(
                        status = if (automatiskSaksbehandlet) {
                            KLAR_TIL_BEHANDLING
                        } else {
                            UNDER_BEHANDLING
                        },
                        attesteringer = attesteringer,
                        saksbehandler = if (automatiskSaksbehandlet) {
                            null
                        } else {
                            saksbehandler
                        },
                        automatiskSaksbehandlet = false,
                    )

                    is Revurdering -> this.copy(
                        status = UNDER_BEHANDLING,
                        attesteringer = attesteringer,
                    )
                }
            }

            KLAR_TIL_BEHANDLING, UNDER_BEHANDLING, KLAR_TIL_BESLUTNING, VEDTATT, AVBRUTT, UNDER_AUTOMATISK_BEHANDLING -> throw IllegalStateException(
                "Må ha status UNDER_BESLUTNING for å sende tilbake. Behandlingsstatus: $status",
            )
        }
    }

    /**
     * @param periode må være en 14 dagers meldeperiode fra mandag til søndag.
     * @throws NullPointerException dersom [antallDagerPerMeldeperiode] er null
     * @return den høyeste verdien som overlapper perioden eller null dersom ingen overlapper
     */
    fun finnAntallDagerForMeldeperiode(periode: Periode): AntallDagerForMeldeperiode? {
        return antallDagerPerMeldeperiode!!.finnAntallDagerForMeldeperiode(periode)
    }

    fun oppdaterBegrunnelseVilkårsvurdering(
        saksbehandler: Saksbehandler,
        begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering,
        clock: Clock,
    ): Either<KunneIkkeOppdatereBegrunnelseVilkårsvurdering, Behandling> {
        return validerKanOppdatere(saksbehandler, "Kunne ikke oppdatere begrunnelse/vilkårsvurdering").mapLeft {
            KunneIkkeOppdatereBegrunnelseVilkårsvurdering.KunneIkkeOppdatereBehandling(it)
        }.map {
            when (this) {
                is Søknadsbehandling -> this.copy(begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering, sistEndret = nå(clock))
                is Revurdering -> this.copy(begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering, sistEndret = nå(clock))
            }
        }
    }

    fun oppdaterSaksopplysninger(
        saksbehandler: Saksbehandler,
        oppdaterteSaksopplysninger: Saksopplysninger,
    ): Either<KunneIkkeOppdatereSaksopplysninger, Behandling> {
        return validerKanOppdatere(saksbehandler, "Kunne ikke oppdatere saksopplysinger").mapLeft {
            KunneIkkeOppdatereSaksopplysninger.KunneIkkeOppdatereBehandling(it)
        }.map {
            when (this) {
                is Søknadsbehandling -> this.copy(saksopplysninger = oppdaterteSaksopplysninger)
                is Revurdering -> this.copy(saksopplysninger = oppdaterteSaksopplysninger)
            }
        }
    }

    fun oppdaterFritekstTilVedtaksbrev(
        saksbehandler: Saksbehandler,
        fritekstTilVedtaksbrev: FritekstTilVedtaksbrev,
        clock: Clock,
    ): Either<KunneIkkeOppdatereFritekstTilVedtaksbrev, Behandling> {
        return validerKanOppdatere(saksbehandler, "Kunne ikke oppdatere fritekst til vedtaksbrev").mapLeft {
            KunneIkkeOppdatereFritekstTilVedtaksbrev.KunneIkkeOppdatereBehandling(it)
        }.map {
            when (this) {
                is Søknadsbehandling -> this.copy(fritekstTilVedtaksbrev = fritekstTilVedtaksbrev, sistEndret = nå(clock))
                is Revurdering -> this.copy(fritekstTilVedtaksbrev = fritekstTilVedtaksbrev, sistEndret = nå(clock))
            }
        }
    }

    fun validerKanOppdatere(
        saksbehandler: Saksbehandler,
        errorMsg: String,
    ): Either<Valideringsfeil, Unit> {
        if (!saksbehandler.erSaksbehandler()) {
            throw TilgangException("$errorMsg - Saksbehandler ${saksbehandler.navIdent} mangler rollen SAKSBEHANDLER - sakId=$sakId, behandlingId=$id")
        }
        if (this.saksbehandler != null && this.saksbehandler != saksbehandler.navIdent) {
            return Valideringsfeil.UtdøvendeSaksbehandlerErIkkePåBehandlingen(this.saksbehandler!!).left()
        }
        if (!this.erUnderBehandling) {
            return Valideringsfeil.BehandlingenErIkkeUnderBehandling.left()
        }

        return Unit.right()
    }

    fun init() {
        if (beslutter != null && saksbehandler != null) {
            require(beslutter != saksbehandler) { "Saksbehandler og beslutter kan ikke være samme person" }
        }
        when (status) {
            UNDER_AUTOMATISK_BEHANDLING -> {
                require(saksbehandler == AUTOMATISK_SAKSBEHANDLER_ID) {
                    "Behandlingen må være tildelt $AUTOMATISK_SAKSBEHANDLER_ID når statusen er UNDER_AUTOMATISK_BEHANDLING"
                }
                require(iverksattTidspunkt == null)
                require(beslutter == null)
            }

            KLAR_TIL_BEHANDLING -> {
                require(saksbehandler == null) {
                    "Behandlingen kan ikke være tilknyttet en saksbehandler når statusen er KLAR_TIL_BEHANDLING"
                }
                require(iverksattTidspunkt == null)

                if (this is Revurdering || (this is Søknadsbehandling && attesteringer.isEmpty())) {
                    require(beslutter == null) { "Beslutter kan ikke være tilknyttet behandlingen dersom det ikke er en underkjent automatisk behandlet søknadsbehandling" }
                }
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
                    require(resultat != null) { "Behandlingsresultat må være satt dersom det er gjort attesteringer på behandlingen" }
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
                require(this.resultat != null) { "Behandlingsresultat må være satt for statusen KLAR_TIL_BESLUTNING" }
            }

            UNDER_BESLUTNING -> {
                // Vi kan ikke ta saksbehandler av behandlingen før den underkjennes.
                requireNotNull(saksbehandler) { "Behandlingen må ha saksbehandler når status er UNDER_BESLUTNING" }
                requireNotNull(beslutter) { "Behandlingen må tilknyttet en beslutter når status er UNDER_BESLUTNING" }
                require(iverksattTidspunkt == null)
                require(virkningsperiode != null) { "Virkningsperiode må være satt for statusen UNDER_BESLUTNING" }
                require(this.resultat != null) { "Behandlingsresultat må være satt for statusen UNDER_BESLUTNING" }
            }

            VEDTATT -> {
                // Det er viktig at vi ikke tar saksbehandler og beslutter av behandlingen når status er VEDTATT.
                requireNotNull(beslutter) { "Behandlingen må ha beslutter når status er VEDTATT" }
                requireNotNull(saksbehandler) { "Behandlingen må ha saksbehandler når status er VEDTATT" }
                requireNotNull(iverksattTidspunkt)
                requireNotNull(sendtTilBeslutning)
                require(virkningsperiode != null) { "Virkningsperiode må være satt for statusen VEDTATT" }
                require(this.resultat != null) { "Behandlingsresultat må være satt for statusen VEDTATT" }
            }

            AVBRUTT -> {
                requireNotNull(avbrutt)
            }
        }
    }
}
