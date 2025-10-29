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
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.Tiltaksdeltagelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.ValgteTiltaksdeltakelser
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

    val utbetaling: BehandlingUtbetaling?

    fun inneholderSaksopplysningerEksternDeltagelseId(eksternDeltagelseId: String): Boolean =
        saksopplysninger.tiltaksdeltagelser.find { it.eksternDeltagelseId == eksternDeltagelseId } != null

    fun getTiltaksdeltagelse(eksternDeltagelseId: String): Tiltaksdeltagelse? =
        saksopplysninger.getTiltaksdeltagelse(eksternDeltagelseId)

    fun avbryt(avbruttAv: Saksbehandler, begrunnelse: String, tidspunkt: LocalDateTime): Rammebehandling

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

    fun gjenoppta(
        endretAv: Saksbehandler,
        clock: Clock,
    ): Rammebehandling {
        if (status == UNDER_AUTOMATISK_BEHANDLING && endretAv != AUTOMATISK_SAKSBEHANDLER) {
            krevSaksbehandlerRolle(endretAv)
            require(saksbehandler == AUTOMATISK_SAKSBEHANDLER_ID) { "Kan ikke gjenoppta automatisk behandling som ikke eies av tp-sak" }
            require(ventestatus.erSattPåVent) { "Behandlingen er ikke satt på vent" }
            require(this is Søknadsbehandling) { "Kun søknadsbehandlinger kan være under automatisk behandling" }
            val oppdatertBehandling = overta(endretAv, clock)
                .getOrElse { IllegalStateException("Kan ikke gjenoppta automatisk behandling: Kunne ikke overta behandlingen") } as Søknadsbehandling
            return oppdatertBehandling.copy(
                ventestatus = ventestatus.leggTil(
                    tidspunkt = nå(clock),
                    endretAv = endretAv.navIdent,
                    erSattPåVent = false,
                    status = oppdatertBehandling.status,
                ),
                venterTil = null,
                sistEndret = nå(clock),
            )
        }
        if (status == UNDER_BEHANDLING) {
            krevSaksbehandlerRolle(endretAv)
            require(this.saksbehandler == endretAv.navIdent) { "Du må være saksbehandler på behandlingen for å kunne gjenoppta den." }
        }
        if (status == UNDER_BESLUTNING) {
            krevBeslutterRolle(endretAv)
            require(this.beslutter == endretAv.navIdent) { "Du må være beslutter på behandlingen for å kunne gjenoppta den." }
        }
        require(ventestatus.erSattPåVent) { "Behandlingen er ikke satt på vent" }

        return when (this) {
            is Søknadsbehandling -> this.copy(
                ventestatus = ventestatus.leggTil(
                    tidspunkt = nå(clock),
                    endretAv = endretAv.navIdent,
                    erSattPåVent = false,
                    status = status,
                ),
                venterTil = null,
                sistEndret = nå(clock),
            )

            is Revurdering -> this.copy(
                ventestatus = ventestatus.leggTil(
                    tidspunkt = nå(clock),
                    endretAv = endretAv.navIdent,
                    erSattPåVent = false,
                    status = status,
                ),
                venterTil = null,
                sistEndret = nå(clock),
            )
        }
    }

    fun leggTilbakeBehandling(saksbehandler: Saksbehandler): Rammebehandling {
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
    fun taBehandling(saksbehandler: Saksbehandler): Rammebehandling {
        return when (status) {
            KLAR_TIL_BEHANDLING -> {
                krevSaksbehandlerRolle(saksbehandler)

                require(this.saksbehandler == null) { "Saksbehandler skal ikke kunne være satt på behandlingen dersom den er KLAR_TIL_BEHANDLING" }

                when (this) {
                    is Søknadsbehandling -> this.copy(
                        saksbehandler = saksbehandler.navIdent,
                        beslutter = if (saksbehandler.navIdent == beslutter) null else beslutter,
                        status = UNDER_BEHANDLING,
                    )

                    is Revurdering -> this.copy(
                        saksbehandler = saksbehandler.navIdent,
                        beslutter = if (saksbehandler.navIdent == beslutter) null else beslutter,
                        status = UNDER_BEHANDLING,
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
            )

            is Søknadsbehandling -> this.copy(
                status = status,
                sendtTilBeslutning = sendtTilBeslutning,
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
    fun underkjenn(
        utøvendeBeslutter: Saksbehandler,
        attestering: Attestering,
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
            @Suppress("IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE")
            val skalNullstille = this.saksopplysninger.let { saksopplysninger ->
                if (saksopplysninger.tiltaksdeltagelser.size != nyeSaksopplysninger.tiltaksdeltagelser.size) {
                    true
                } else {
                    (
                        saksopplysninger.tiltaksdeltagelser.sortedBy { it.eksternDeltagelseId }
                            .zip(nyeSaksopplysninger.tiltaksdeltagelser.sortedBy { it.eksternDeltagelseId }) { forrige, nye ->
                                // Vi nullstiller resultatet og virkningsperioden dersom det har kommet nye tiltaksdeltagelser eller noen er fjernet. Nullstiller også dersom periodene har endret seg.
                                forrige.eksternDeltagelseId != nye.eksternDeltagelseId ||
                                    forrige.deltagelseFraOgMed == nye.deltagelseFraOgMed ||
                                    forrige.deltagelseTilOgMed == nye.deltagelseTilOgMed
                            }.any { it }
                        )
                }
            }

            when (this) {
                is Søknadsbehandling -> this.copy(
                    saksopplysninger = nyeSaksopplysninger,
                    resultat = if (skalNullstille) null else this.resultat,
                )

                is Revurdering -> this.copy(
                    saksopplysninger = nyeSaksopplysninger,
                    resultat = if (skalNullstille && this.resultat is RevurderingResultat.Innvilgelse) this.resultat.nullstill() else this.resultat,
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

    fun oppdaterSimulering(nySimulering: Simulering?): Rammebehandling
}
