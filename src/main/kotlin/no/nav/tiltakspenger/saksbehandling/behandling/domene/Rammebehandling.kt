package no.nav.tiltakspenger.saksbehandling.behandling.domene

import arrow.core.Either
import arrow.core.getOrElse
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
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.AVBRUTT
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.KLAR_TIL_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.UNDER_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.UNDER_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.VEDTATT
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.søknadsbehandling.KanIkkeSendeTilBeslutter
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.overta.KunneIkkeOvertaBehandling
import no.nav.tiltakspenger.saksbehandling.behandling.service.delautomatiskbehandling.AUTOMATISK_SAKSBEHANDLER
import no.nav.tiltakspenger.saksbehandling.felles.Attestering
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringer
import no.nav.tiltakspenger.saksbehandling.felles.Avbrutt
import no.nav.tiltakspenger.saksbehandling.felles.Ventestatus
import no.nav.tiltakspenger.saksbehandling.felles.krevBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerRolle
import no.nav.tiltakspenger.saksbehandling.infra.setup.AUTOMATISK_SAKSBEHANDLER_ID
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjørRammevedtak
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.ValgteTiltaksdeltakelser
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering
import java.time.Clock
import java.time.LocalDateTime

const val DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE: Int = 10

/**
 * En rammebehandling fører til et [no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak]. Dette er en vurderingen av søknaden og inngangsvilkårene - om en bruker har rett til tiltangspenger i en gitt periode.
 * Dette gjelder både søknadsbehandling (innvilgelse og avslag) og revurdering (endring og omgjøring, inkl. stans/opphør, innvilgelse/forlengelse)
 * Se [no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling] for behandling av meldekort innenfor et [no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak].
 */
sealed interface Rammebehandling : Behandling {
    override val id: BehandlingId
    val status: Rammebehandlingsstatus
    override val opprettet: LocalDateTime

    val sistEndret: LocalDateTime
    override val iverksattTidspunkt: LocalDateTime?
    val sendtTilDatadeling: LocalDateTime?
    override val sakId: SakId

    override val saksnummer: Saksnummer
    override val fnr: Fnr
    val saksopplysninger: Saksopplysninger

    override val saksbehandler: String?
    override val beslutter: String?
    override val sendtTilBeslutning: LocalDateTime?
    override val attesteringer: Attesteringer

    val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?
    val avbrutt: Avbrutt?
    val ventestatus: Ventestatus
    val venterTil: LocalDateTime?
    val resultat: BehandlingResultat?
    val virkningsperiode: Periode?
    val innvilgelsesperiode: Periode?
    val begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering?

    val valgteTiltaksdeltakelser: ValgteTiltaksdeltakelser?
    val barnetillegg: Barnetillegg?

    val antallDagerPerMeldeperiode: SammenhengendePeriodisering<AntallDagerForMeldeperiode>?

    val behandlingstype: Behandlingstype
        get() = when (this) {
            is Revurdering -> Behandlingstype.REVURDERING
            is Søknadsbehandling -> Behandlingstype.SØKNADSBEHANDLING
        }

    val erUnderAutomatiskBehandling: Boolean get() = status == UNDER_AUTOMATISK_BEHANDLING
    val erUnderBehandling: Boolean get() = status == UNDER_BEHANDLING || status == UNDER_AUTOMATISK_BEHANDLING
    override val erAvbrutt: Boolean get() = status == AVBRUTT
    val erVedtatt: Boolean get() = status == VEDTATT
    override val erAvsluttet: Boolean get() = erAvbrutt || erVedtatt

    val saksopplysningsperiode: Periode? get() = saksopplysninger.periode

    val omgjørRammevedtak: OmgjørRammevedtak

    val utbetaling: BehandlingUtbetaling?

    fun inneholderSaksopplysningerEksternDeltakelseId(eksternDeltakelseId: String): Boolean =
        saksopplysninger.tiltaksdeltakelser.find { it.eksternDeltakelseId == eksternDeltakelseId } != null

    fun getTiltaksdeltakelse(eksternDeltakelseId: String): Tiltaksdeltakelse? =
        saksopplysninger.getTiltaksdeltakelse(eksternDeltakelseId)

    fun avbryt(avbruttAv: Saksbehandler, begrunnelse: String, tidspunkt: LocalDateTime): Rammebehandling

    fun erFerdigutfylt(): Boolean

    fun settPåVent(
        endretAv: Saksbehandler,
        begrunnelse: String,
        clock: Clock,
        venterTil: LocalDateTime? = null,
    ): Rammebehandling {
        when (status) {
            UNDER_AUTOMATISK_BEHANDLING,
            UNDER_BEHANDLING,
            UNDER_BESLUTNING,
            -> {
                require(begrunnelse.isNotBlank()) { "Du må oppgi en grunn for at behandlingen settes på vent." }
                if (status == UNDER_BEHANDLING) {
                    krevSaksbehandlerRolle(endretAv)
                    require(this.saksbehandler == endretAv.navIdent) { "Du må være saksbehandler på behandlingen for å kunne sette den på vent." }
                }
                if (status == UNDER_BESLUTNING) {
                    krevBeslutterRolle(endretAv)
                    require(this.beslutter == endretAv.navIdent) { "Du må være beslutter på behandlingen for å kunne sette den på vent." }
                }

                return when (this) {
                    is Søknadsbehandling -> this.copy(
                        ventestatus = ventestatus.leggTil(
                            tidspunkt = nå(clock),
                            endretAv = endretAv.navIdent,
                            begrunnelse = begrunnelse,
                            erSattPåVent = true,
                            status = status,
                        ),
                        venterTil = venterTil,
                        sistEndret = nå(clock),
                    )

                    is Revurdering -> this.copy(
                        ventestatus = ventestatus.leggTil(
                            tidspunkt = nå(clock),
                            endretAv = endretAv.navIdent,
                            begrunnelse = begrunnelse,
                            erSattPåVent = true,
                            status = status,
                        ),
                        venterTil = venterTil,
                        sistEndret = nå(clock),
                    )
                }
            }

            KLAR_TIL_BEHANDLING,
            KLAR_TIL_BESLUTNING,
            VEDTATT,
            AVBRUTT,
            -> throw IllegalStateException("Kan ikke sette behandling på vent som har status ${status.name}")
        }
    }

    /**
     * Kan kun gjenoppta en behandling som er satt på vent.
     * @param hentSaksopplysninger Henter saksopplysninger på nytt dersom denne ikke er null.
     */
    suspend fun gjenoppta(
        endretAv: Saksbehandler,
        clock: Clock,
        hentSaksopplysninger: (suspend () -> Saksopplysninger)?,
    ): Either<KunneIkkeOppdatereSaksopplysninger, Rammebehandling> {
        require(ventestatus.erSattPåVent) { "Behandlingen er ikke satt på vent" }

        val nå = nå(clock)
        suspend fun gjenopptaBehandling(
            overta: Boolean,
            hentSaksopplysninger: (suspend () -> Saksopplysninger)?,
        ): Either<KunneIkkeOppdatereSaksopplysninger, Rammebehandling> {
            val oppdatertVentestatus = ventestatus.leggTil(
                tidspunkt = nå,
                endretAv = endretAv.navIdent,
                erSattPåVent = false,
                status = status,
            )
            return when (this) {
                is Søknadsbehandling -> this.copy(ventestatus = oppdatertVentestatus, venterTil = null, sistEndret = nå)
                is Revurdering -> this.copy(ventestatus = oppdatertVentestatus, venterTil = null, sistEndret = nå)
            }.let {
                if (overta) {
                    if (it.saksbehandler == null) {
                        it.taBehandling(endretAv, clock)
                    } else {
                        it.overta(endretAv, clock).getOrNull()!!
                    }
                } else {
                    it
                }
            }.let {
                if (hentSaksopplysninger != null) {
                    it.oppdaterSaksopplysninger(endretAv, hentSaksopplysninger())
                } else {
                    it.right()
                }
            }
        }
        return when (status) {
            VEDTATT, AVBRUTT -> throw IllegalStateException("Kan ikke gjenoppta behandling som har status ${status.name}")
            KLAR_TIL_BEHANDLING -> {
                krevSaksbehandlerRolle(endretAv)
                gjenopptaBehandling(true, hentSaksopplysninger)
            }

            UNDER_BEHANDLING -> {
                krevSaksbehandlerRolle(endretAv)
                require(this.saksbehandler == endretAv.navIdent) { "Du må være saksbehandler på behandlingen for å kunne gjenoppta den." }
                gjenopptaBehandling(false, hentSaksopplysninger)
            }

            UNDER_AUTOMATISK_BEHANDLING -> {
                if (endretAv == AUTOMATISK_SAKSBEHANDLER) {
                    gjenopptaBehandling(false, null)
                } else {
                    krevSaksbehandlerRolle(endretAv)
                    gjenopptaBehandling(true, hentSaksopplysninger)
                }
            }

            KLAR_TIL_BESLUTNING -> {
                krevBeslutterRolle(endretAv)
                gjenopptaBehandling(true, null)
            }

            UNDER_BESLUTNING -> {
                krevBeslutterRolle(endretAv)
                require(this.beslutter == endretAv.navIdent) { "Du må være beslutter på behandlingen for å kunne gjenoppta den." }
                gjenopptaBehandling(false, null)
            }
        }
    }

    fun leggTilbakeBehandling(saksbehandler: Saksbehandler, clock: Clock): Rammebehandling {
        return when (status) {
            UNDER_BEHANDLING -> {
                krevSaksbehandlerRolle(saksbehandler)
                require(this.saksbehandler == saksbehandler.navIdent) {
                    "Kan bare legge tilbake behandling dersom saksbehandler selv er på behandlingen"
                }

                when (this) {
                    is Søknadsbehandling -> this.copy(
                        saksbehandler = null,
                        status = KLAR_TIL_BEHANDLING,
                        sistEndret = nå(clock),
                    )

                    is Revurdering -> this.copy(
                        saksbehandler = null,
                        status = KLAR_TIL_BEHANDLING,
                        sistEndret = nå(clock),
                    )
                }
            }

            UNDER_BESLUTNING -> {
                krevBeslutterRolle(saksbehandler)
                require(this.beslutter == saksbehandler.navIdent) {
                    "Kan bare legge tilbake behandling dersom saksbehandler selv er på behandlingen"
                }

                when (this) {
                    is Søknadsbehandling -> this.copy(
                        beslutter = null,
                        status = KLAR_TIL_BESLUTNING,
                        sistEndret = nå(clock),
                    )

                    is Revurdering -> this.copy(
                        beslutter = null,
                        status = KLAR_TIL_BESLUTNING,
                        sistEndret = nå(clock),
                    )
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
    fun taBehandling(saksbehandler: Saksbehandler, clock: Clock): Rammebehandling {
        return when (status) {
            KLAR_TIL_BEHANDLING -> {
                krevSaksbehandlerRolle(saksbehandler)

                require(this.saksbehandler == null) { "Saksbehandler skal ikke kunne være satt på behandlingen dersom den er KLAR_TIL_BEHANDLING" }

                when (this) {
                    is Søknadsbehandling -> this.copy(
                        saksbehandler = saksbehandler.navIdent,
                        beslutter = if (saksbehandler.navIdent == beslutter) null else beslutter,
                        status = UNDER_BEHANDLING,
                        sistEndret = nå(clock),
                    )

                    is Revurdering -> this.copy(
                        saksbehandler = saksbehandler.navIdent,
                        beslutter = if (saksbehandler.navIdent == beslutter) null else beslutter,
                        status = UNDER_BEHANDLING,
                        sistEndret = nå(clock),
                    )
                }
            }

            KLAR_TIL_BESLUTNING -> {
                check(saksbehandler.navIdent != this.saksbehandler) {
                    "Beslutter ($saksbehandler) kan ikke være den samme som saksbehandleren (${this.saksbehandler}"
                }
                krevBeslutterRolle(saksbehandler)
                require(this.beslutter == null) { "Behandlingen har en eksisterende beslutter. For å overta behandlingen, bruk overta() - behandlingsId: ${this.id}" }

                when (this) {
                    is Søknadsbehandling -> this.copy(
                        beslutter = saksbehandler.navIdent,
                        status = UNDER_BESLUTNING,
                        sistEndret = nå(clock),
                    )

                    is Revurdering -> this.copy(
                        beslutter = saksbehandler.navIdent,
                        status = UNDER_BESLUTNING,
                        sistEndret = nå(clock),
                    )
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
    fun overta(saksbehandler: Saksbehandler, clock: Clock): Either<KunneIkkeOvertaBehandling, Rammebehandling> {
        val nåTidMinus1Minutt = LocalDateTime.now(clock).minusMinutes(1)
        val erSistEndretMindreEnn1MinuttSiden = this.sistEndret.isAfter(nåTidMinus1Minutt)

        if (erSistEndretMindreEnn1MinuttSiden) {
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
            UNDER_AUTOMATISK_BEHANDLING -> {
                if (this.saksbehandler != AUTOMATISK_SAKSBEHANDLER_ID || !ventestatus.erSattPåVent) {
                    return KunneIkkeOvertaBehandling.BehandlingenKanIkkeVæreUnderAutomatiskBehandling.left()
                }
                krevSaksbehandlerRolle(saksbehandler)
                return when (this) {
                    is Søknadsbehandling -> this.copy(
                        status = UNDER_BEHANDLING,
                        saksbehandler = saksbehandler.navIdent,
                        sistEndret = oppdatertSistEndret,
                    )

                    is Revurdering -> return KunneIkkeOvertaBehandling.BehandlingenKanIkkeVæreUnderAutomatiskBehandling.left()
                }.right()
            }

            VEDTATT,
            AVBRUTT,
            -> KunneIkkeOvertaBehandling.BehandlingenKanIkkeVæreVedtattEllerAvbrutt.left()
        }
    }

    fun tilBeslutning(
        kommando: SendBehandlingTilBeslutningKommando,
        clock: Clock,
    ): Either<KanIkkeSendeTilBeslutter, Rammebehandling> {
        validerKanSendeTilBeslutning(kommando.saksbehandler).onLeft { return it.left() }

        val status = if (beslutter == null) KLAR_TIL_BESLUTNING else UNDER_BESLUTNING
        val sendtTilBeslutning = nå(clock)

        return when (this) {
            is Revurdering -> this.copy(
                status = status,
                sendtTilBeslutning = sendtTilBeslutning,
                sistEndret = sendtTilBeslutning,
            )

            is Søknadsbehandling -> this.copy(
                status = status,
                sendtTilBeslutning = sendtTilBeslutning,
                sistEndret = sendtTilBeslutning,
            )
        }.right()
    }

    fun iverksett(
        utøvendeBeslutter: Saksbehandler,
        attestering: Attestering,
        clock: Clock,
    ): Rammebehandling {
        require(virkningsperiode != null) { "virkningsperiode må være satt ved iverksetting" }

        return when (status) {
            UNDER_BESLUTNING -> {
                krevBeslutterRolle(utøvendeBeslutter)
                check(this.beslutter == utøvendeBeslutter.navIdent) { "Kan ikke iverksette en behandling man ikke er beslutter på" }
                check(!this.attesteringer.any { it.isGodkjent() }) {
                    "Behandlingen er allerede godkjent"
                }
                check(!ventestatus.erSattPåVent) { "Behandlingen må gjenopptas før den kan iverksettes." }

                val attesteringer = attesteringer.leggTil(attestering)
                val iverksattTidspunkt = nå(clock)

                when (this) {
                    is Søknadsbehandling -> this.copy(
                        status = VEDTATT,
                        attesteringer = attesteringer,
                        iverksattTidspunkt = iverksattTidspunkt,
                        sistEndret = iverksattTidspunkt,
                    )

                    is Revurdering -> this.copy(
                        status = VEDTATT,
                        attesteringer = attesteringer,
                        iverksattTidspunkt = iverksattTidspunkt,
                        sistEndret = iverksattTidspunkt,
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
    fun underkjenn(
        utøvendeBeslutter: Saksbehandler,
        attestering: Attestering,
        clock: Clock,
    ): Rammebehandling {
        return when (status) {
            UNDER_BESLUTNING -> {
                krevBeslutterRolle(utøvendeBeslutter)
                check(this.beslutter == utøvendeBeslutter.navIdent) {
                    "Kun beslutter som har saken kan sende tilbake"
                }
                check(!this.attesteringer.any { it.isGodkjent() }) {
                    "Behandlingen er allerede godkjent"
                }
                check(!ventestatus.erSattPåVent) { "Behandlingen må gjenopptas før den kan underkjennes." }

                val attesteringer = attesteringer.leggTil(attestering)

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
                        sistEndret = nå(clock),
                    )

                    is Revurdering -> this.copy(
                        status = UNDER_BEHANDLING,
                        attesteringer = attesteringer,
                        sistEndret = nå(clock),
                    )
                }
            }

            KLAR_TIL_BEHANDLING, UNDER_BEHANDLING, KLAR_TIL_BESLUTNING, VEDTATT, AVBRUTT, UNDER_AUTOMATISK_BEHANDLING -> throw IllegalStateException(
                "Må ha status UNDER_BESLUTNING for å sende tilbake. Behandlingsstatus: $status",
            )
        }
    }

    /**
     * Validerer saksbehandler og behandlingens tilstand.
     * Validerer ikke om saksbehandler har tilgang til personen.
     */
    fun oppdaterSaksopplysninger(
        saksbehandler: Saksbehandler,
        nyeSaksopplysninger: Saksopplysninger,
    ): Either<KunneIkkeOppdatereSaksopplysninger, Rammebehandling> {
        return validerKanOppdatere(saksbehandler).mapLeft {
            KunneIkkeOppdatereSaksopplysninger.KunneIkkeOppdatereBehandling(it)
        }.map {
            when (this) {
                is Søknadsbehandling -> this.copy(
                    saksopplysninger = nyeSaksopplysninger,
                    resultat = this.resultat?.oppdaterSaksopplysninger(nyeSaksopplysninger)?.getOrElse {
                        return it.left()
                    },
                )

                is Revurdering -> this.copy(
                    saksopplysninger = nyeSaksopplysninger,
                    resultat = this.resultat.oppdaterSaksopplysninger(nyeSaksopplysninger).getOrElse {
                        return it.left()
                    },
                )
            }
        }
    }

    fun validerKanOppdatere(saksbehandler: Saksbehandler): Either<KanIkkeOppdatereBehandling, Unit> {
        if (this.saksbehandler != null && this.saksbehandler != saksbehandler.navIdent) {
            return KanIkkeOppdatereBehandling.BehandlingenEiesAvAnnenSaksbehandler(this.saksbehandler!!).left()
        }
        if (!this.erUnderBehandling) {
            return KanIkkeOppdatereBehandling.MåVæreUnderBehandling.left()
        }

        return Unit.right()
    }

    fun validerKanSendeTilBeslutning(saksbehandler: Saksbehandler): Either<KanIkkeSendeTilBeslutter, Unit> {
        if (this.saksbehandler != null && this.saksbehandler != saksbehandler.navIdent) {
            return KanIkkeSendeTilBeslutter.BehandlingenEiesAvAnnenSaksbehandler(this.saksbehandler!!).left()
        }
        if (status != UNDER_BEHANDLING && status != UNDER_AUTOMATISK_BEHANDLING) {
            return KanIkkeSendeTilBeslutter.MåVæreUnderBehandlingEllerAutomatisk.left()
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
                require(this is Søknadsbehandling) {
                    "Kun søknadsbehandlinger kan være under automatisk behandling"
                }
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
                }
                // Vi kan ikke kreve at resultatet er satt dersom den har vært underkjent, siden hentOpplysninger kan resette saksoplysninger og implisitt resultatet.
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
                require(erFerdigutfylt())
            }

            UNDER_BESLUTNING -> {
                // Vi kan ikke ta saksbehandler av behandlingen før den underkjennes.
                requireNotNull(saksbehandler) { "Behandlingen må ha saksbehandler når status er UNDER_BESLUTNING" }
                requireNotNull(beslutter) { "Behandlingen må tilknyttet en beslutter når status er UNDER_BESLUTNING" }
                require(iverksattTidspunkt == null)
                require(virkningsperiode != null) { "Virkningsperiode må være satt for statusen UNDER_BESLUTNING" }
                require(this.resultat != null) { "Behandlingsresultat må være satt for statusen UNDER_BESLUTNING" }
                require(erFerdigutfylt())
            }

            VEDTATT -> {
                // Det er viktig at vi ikke tar saksbehandler og beslutter av behandlingen når status er VEDTATT.
                requireNotNull(beslutter) { "Behandlingen må ha beslutter når status er VEDTATT" }
                requireNotNull(saksbehandler) { "Behandlingen må ha saksbehandler når status er VEDTATT" }
                requireNotNull(iverksattTidspunkt)
                requireNotNull(sendtTilBeslutning)
                require(virkningsperiode != null) { "Virkningsperiode må være satt for statusen VEDTATT" }
                require(this.resultat != null) { "Behandlingsresultat må være satt for statusen VEDTATT" }
                require(erFerdigutfylt())
            }

            AVBRUTT -> {
                requireNotNull(avbrutt)
            }
        }
    }

    fun oppdaterSimulering(nySimulering: Simulering?): Rammebehandling
}
