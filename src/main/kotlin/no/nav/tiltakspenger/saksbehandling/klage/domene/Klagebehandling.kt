package no.nav.tiltakspenger.saksbehandling.klage.domene

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.dokument.GenererKlageAvvisningsbrev
import no.nav.tiltakspenger.saksbehandling.dokument.KunneIkkeGenererePdf
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.felles.Avbrutt
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.AVBRUTT
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.KLAR_TIL_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.UNDER_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.VEDTATT
import no.nav.tiltakspenger.saksbehandling.klage.domene.avbryt.AvbrytKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.avbryt.KanIkkeAvbryteKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.Brevtekster
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.KlagebehandlingBrevKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.KanIkkeOppdatereKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.KlageFormkrav
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.OppdaterKlagebehandlingFormkravKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.iverksett.IverksettKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.iverksett.KanIkkeIverksetteKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.opprett.OpprettKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.vurder.KanIkkeVurdereKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.vurder.OmgjørKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.vurder.VurderKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import java.time.Clock
import java.time.LocalDateTime

/**
 * Representerer registrering og vurdering av en klage på et vedtak om tiltakspenger.
 * En klagebehandling har ingen beslutter, da klager avgjøres av en saksbehandler alene. Hvis det fører til medhold, vil en beslutter måtte beslutte selve revurderingen.
 * @param journalpostId Journalposten som inneholder klagen.
 * @param journalpostOpprettet Tidspunktet [journalpostId] ble opprettet.
 */
data class Klagebehandling(
    override val id: KlagebehandlingId,
    override val sakId: SakId,
    override val saksnummer: Saksnummer,
    override val fnr: Fnr,
    override val opprettet: LocalDateTime,
    val sistEndret: LocalDateTime,
    override val iverksattTidspunkt: LocalDateTime?,
    override val saksbehandler: String?,
    val journalpostId: JournalpostId,
    val journalpostOpprettet: LocalDateTime,
    val status: Klagebehandlingsstatus,
    val resultat: Klagebehandlingsresultat?,
    val formkrav: KlageFormkrav,
    val avbrutt: Avbrutt?,
) : Behandling {
    val brevtekst: Brevtekster? = resultat?.brevtekst
    val erUnderBehandling = status == UNDER_BEHANDLING

    @Suppress("unused")
    val erKlarTilBehandling = status == KLAR_TIL_BEHANDLING
    override val erAvbrutt = status == AVBRUTT
    val erVedtatt = status == VEDTATT
    override val erAvsluttet = erAvbrutt || erVedtatt
    val erÅpen = !erAvsluttet
    val erAvvisning = resultat is Klagebehandlingsresultat.Avvist
    val erOmgjøring = resultat is Klagebehandlingsresultat.Omgjør

    /**
     * Hvis resultatet er [Klagebehandlingsresultat.Omgjør] og [Klagebehandlingsresultat.Omgjør.rammebehandlingId] er satt.
     * Merk at dersom rammebehandlingen avbrytes vil denne verdien settes til null.
     */
    val erKnyttetTilRammebehandling: Boolean = resultat?.erKnyttetTilRammebehandling == true
    val rammebehandlingId: BehandlingId? = when (val res = resultat) {
        is Klagebehandlingsresultat.Omgjør -> res.rammebehandlingId
        is Klagebehandlingsresultat.Avvist, null -> null
    }

    /** Dette flagget gir ikke så mye mening dersom resultatet er medhold/omgjøring, siden man må spørre Rammebehandlingen om man kanIverksette */
    val kanIverksette: Boolean? by lazy {
        when {
            !erUnderBehandling -> false
            resultat == null -> false
            else -> resultat.kanIverksette
        }
    }

    val kanIkkeIverksetteGrunner: List<String> by lazy {
        val grunner = mutableListOf<String>()
        if (!erUnderBehandling) grunner.add("Klagebehandling er ikke under behandling")
        if (resultat == null) grunner.add("Resultat er ikke satt")
        grunner + (resultat?.kanIkkeIverksetteGrunner ?: emptyList())
    }

    fun erSaksbehandlerPåBehandlingen(saksbehandler: Saksbehandler): Boolean {
        return this.saksbehandler == saksbehandler.navIdent
    }

    fun oppdaterFormkrav(
        kommando: OppdaterKlagebehandlingFormkravKommando,
        journalpostOpprettet: LocalDateTime,
        clock: Clock,
    ): Either<KanIkkeOppdatereKlagebehandling, Klagebehandling> {
        if (!erUnderBehandling) return KanIkkeOppdatereKlagebehandling.KanIkkeOppdateres.left()
        if (!erSaksbehandlerPåBehandlingen(kommando.saksbehandler)) {
            return KanIkkeOppdatereKlagebehandling.SaksbehandlerMismatch(
                forventetSaksbehandler = this.saksbehandler!!,
                faktiskSaksbehandler = kommando.saksbehandler.navIdent,
            ).left()
        }
        val oppdaterteFormkrav = kommando.toKlageFormkrav()
        val tidligereResultat = this.resultat
        val harTilknyttetRammebehandling =
            this.resultat is Klagebehandlingsresultat.Omgjør && this.resultat.rammebehandlingId != null

        if (oppdaterteFormkrav.erAvvisning && harTilknyttetRammebehandling) {
            return KanIkkeOppdatereKlagebehandling.KanIkkeEndreTilAvvisningNårTilknyttetRammebehandling.left()
        }

        return this.copy(
            sistEndret = nå(clock),
            formkrav = oppdaterteFormkrav,
            journalpostId = kommando.journalpostId,
            journalpostOpprettet = journalpostOpprettet,
            resultat = when {
                oppdaterteFormkrav.erAvvisning && tidligereResultat is Klagebehandlingsresultat.Avvist -> tidligereResultat
                oppdaterteFormkrav.erAvvisning -> Klagebehandlingsresultat.Avvist.empty
                resultat is Klagebehandlingsresultat.Omgjør && oppdaterteFormkrav.erOppfyllt -> this.resultat
                else -> null
            },
        ).right()
    }

    fun oppdaterBrevtekst(
        kommando: KlagebehandlingBrevKommando,
        clock: Clock,
    ): Either<KanIkkeOppdatereKlagebehandling, Klagebehandling> {
        if (!erAvvisning || !erUnderBehandling) {
            return KanIkkeOppdatereKlagebehandling.KanIkkeOppdateres.left()
        }
        if (!erSaksbehandlerPåBehandlingen(kommando.saksbehandler)) {
            return KanIkkeOppdatereKlagebehandling.SaksbehandlerMismatch(
                forventetSaksbehandler = this.saksbehandler!!,
                faktiskSaksbehandler = kommando.saksbehandler.navIdent,
            ).left()
        }
        return (resultat as Klagebehandlingsresultat.Avvist).oppdaterBrevtekst(kommando.brevtekster).let {
            this.copy(
                sistEndret = nå(clock),
                resultat = it,
            ).right()
        }
    }

    fun vurder(
        kommando: VurderKlagebehandlingKommando,
        rammebehandlingsstatus: Rammebehandlingsstatus?,
        clock: Clock,
    ): Either<KanIkkeVurdereKlagebehandling, Klagebehandling> {
        require(kommando is OmgjørKlagebehandlingKommando)
        kanOppdatereIDenneStatusen(rammebehandlingsstatus).onLeft {
            return KanIkkeVurdereKlagebehandling.KanIkkeOppdateres(
                it,
            ).left()
        }
        // TODO jah: Denne må nok gjøres om litt når vi legger til opprettholdelse
        if (resultat != null && !erOmgjøring) {
            return KanIkkeVurdereKlagebehandling.KanIkkeOppdateres(
                KanIkkeOppdateres.FeilResultat(
                    forventetResultat = Klagebehandlingsresultat.Omgjør::class.simpleName!!,
                    faktiskResultat = resultat::class.simpleName!!,
                ),
            ).left()
        }
        if (!erSaksbehandlerPåBehandlingen(kommando.saksbehandler)) {
            return KanIkkeVurdereKlagebehandling.SaksbehandlerMismatch(
                forventetSaksbehandler = this.saksbehandler!!,
                faktiskSaksbehandler = kommando.saksbehandler.navIdent,
            ).left()
        }
        return this.copy(
            sistEndret = nå(clock),
            resultat = resultat?.let {
                it as Klagebehandlingsresultat.Omgjør
                it.oppdater(kommando)
            } ?: kommando.tilResultatUtenRammebehandlingId(),
        ).right()
    }

    /**
     * Avgjør selv hvilken type brev som genereres og om det er forhåndsvisning eller endelig generering.
     * @param kommando Blir kun brukt dersom saksbehandler på behandlingen forhåndsviser i tilstanden [UNDER_BEHANDLING].
     */
    suspend fun genererBrev(
        kommando: KlagebehandlingBrevKommando,
        genererAvvisningsbrev: GenererKlageAvvisningsbrev,
    ): Either<KunneIkkeGenererePdf, PdfOgJson> {
        require(resultat is Klagebehandlingsresultat.Avvist) {
            "Kan kun generere klagebrev for avvisning når formkrav er avvisning.sakId=$sakId, saksnummer:$saksnummer, klagebehandlingId=$id"
        }
        if (erVedtatt) return genererBrev(genererAvvisningsbrev)
        val brevtekst = resultat.brevtekst
        val saksbehandler: String = when (status) {
            KLAR_TIL_BEHANDLING -> "-"
            UNDER_BEHANDLING -> this.saksbehandler!!
            AVBRUTT -> this.saksbehandler ?: "-"
            VEDTATT -> throw IllegalStateException("Vi håndterer denne tilstanden over.")
        }
        val erSaksbehandlerPåBehandlingen = this.erSaksbehandlerPåBehandlingen(kommando.saksbehandler)
        val tilleggstekst: Brevtekster = when (status) {
            KLAR_TIL_BEHANDLING -> brevtekst ?: Brevtekster.empty
            UNDER_BEHANDLING -> if (erSaksbehandlerPåBehandlingen) {
                kommando.brevtekster
            } else {
                brevtekst ?: Brevtekster.empty
            }

            AVBRUTT -> brevtekst ?: Brevtekster.empty
            VEDTATT -> throw IllegalStateException("Vi håndterer denne tilstanden over.")
        }
        return genererAvvisningsbrev(
            saksnummer,
            fnr,
            saksbehandler,
            tilleggstekst,
            true,
        )
    }

    /**
     * Kun til bruk av systemet når det skal genereres endelig brev.
     * @throws IllegalArgumentException dersom klagebehandlingen ikke er iverksatt.
     */
    suspend fun genererBrev(
        genererAvvisningsbrev: GenererKlageAvvisningsbrev,
    ): Either<KunneIkkeGenererePdf, PdfOgJson> {
        require(erVedtatt) {
            "Kan kun generere klagebrev for avvisning når klagebehandling er iverksatt.sakId=$sakId, saksnummer:$saksnummer, klagebehandlingId=$id"
        }
        return genererAvvisningsbrev(
            saksnummer,
            fnr,
            saksbehandler!!,
            (resultat as Klagebehandlingsresultat.Avvist).brevtekst!!,
            false,
        )
    }

    fun avbryt(
        kommando: AvbrytKlagebehandlingKommando,
        clock: Clock,
    ): Either<KanIkkeAvbryteKlagebehandling, Klagebehandling> {
        require(!erAvsluttet) {
            "Klagebehandling er allerede avsluttet og kan ikke avbrytes. sakId=$sakId, saksnummer:$saksnummer, klagebehandlingId=$id"
        }
        if (erKnyttetTilRammebehandling) {
            return KanIkkeAvbryteKlagebehandling.KnyttetTilIkkeAvbruttRammebehandling(rammebehandlingId!!).left()
        }
        if (!erSaksbehandlerPåBehandlingen(kommando.saksbehandler)) {
            return KanIkkeAvbryteKlagebehandling.SaksbehandlerMismatch(
                forventetSaksbehandler = this.saksbehandler!!,
                faktiskSaksbehandler = kommando.saksbehandler.navIdent,
            ).left()
        }
        return this.copy(
            sistEndret = nå(clock),
            status = AVBRUTT,
            avbrutt = Avbrutt(
                begrunnelse = kommando.begrunnelse,
                saksbehandler = kommando.saksbehandler.navIdent,
                tidspunkt = nå(clock),
            ),
        ).right()
    }

    /**
     * Ved medhold/omgjøring må rammebehandlingId være satt, rammebehandlingen må være i riktig tilstand og skal kun kalles via [no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.IverksettRammebehandlingService]
     */
    fun iverksett(
        kommando: IverksettKlagebehandlingKommando,
    ): Either<KanIkkeIverksetteKlagebehandling, Klagebehandling> {
        if (!erUnderBehandling) {
            return KanIkkeIverksetteKlagebehandling.MåHaStatusUnderBehandling(status.toString()).left()
        }
        if (kommando.saksbehandler != null && !erSaksbehandlerPåBehandlingen(kommando.saksbehandler)) {
            return KanIkkeIverksetteKlagebehandling.SaksbehandlerMismatch(
                forventetSaksbehandler = this.saksbehandler!!,
                faktiskSaksbehandler = kommando.saksbehandler.navIdent,
            ).left()
        }
        when (resultat) {
            is Klagebehandlingsresultat.Avvist, is Klagebehandlingsresultat.Omgjør -> {
                when (resultat) {
                    is Klagebehandlingsresultat.Avvist -> {
                        if (kommando.iverksettFraRammebehandling) {
                            return KanIkkeIverksetteKlagebehandling.FeilInngang(
                                forventetInngang = "Iverksett klagebehandling utenom rammebehandling",
                                faktiskInngang = "Iverksett klagebehandling fra rammebehandling",
                            ).left()
                        }
                    }

                    is Klagebehandlingsresultat.Omgjør -> {
                        if (!kommando.iverksettFraRammebehandling) {
                            return KanIkkeIverksetteKlagebehandling.FeilInngang(
                                forventetInngang = "Iverksett klagebehandling fra rammebehandling",
                                faktiskInngang = "Iverksett klagebehandling utenom rammebehandling",
                            ).left()
                        }
                    }
                }
                if (kanIverksette == false) {
                    return KanIkkeIverksetteKlagebehandling.AndreGrunner(kanIkkeIverksetteGrunner).left()
                }
            }

            null -> return KanIkkeIverksetteKlagebehandling.FeilResultat(
                forventetResultat = Klagebehandlingsresultat.Avvist::class.simpleName!!,
                faktiskResultat = null,
            ).left()
        }

        return this.copy(
            sistEndret = kommando.iverksattTidspunkt,
            iverksattTidspunkt = kommando.iverksattTidspunkt,
            status = VEDTATT,
        ).right()
    }

    fun oppdaterRammebehandlingId(
        rammebehandlingId: BehandlingId,
    ): Klagebehandling {
        require(resultat is Klagebehandlingsresultat.Omgjør) {
            "Resultatet må være Omgjør, men var $resultat. sakId=$sakId, saksnummer:$saksnummer, klagebehandlingId=$id"
        }
        return this.copy(
            resultat = resultat.oppdaterRammebehandlingId(rammebehandlingId),
        )
    }

    fun fjernRammebehandlingId(
        saksbehandler: Saksbehandler,
        rammmebehandlingId: BehandlingId,
    ): Klagebehandling {
        require(erKnyttetTilRammebehandling) {
            "Klagebehandling er ikke knyttet til en rammebehandling.sakId=$sakId, saksnummer:$saksnummer, klagebehandlingId=$id"
        }
        require(this.status == KLAR_TIL_BEHANDLING || this.status == UNDER_BEHANDLING) {
            "Klagebehandling må være i status KLAR_TIL_BEHANDLING eller UNDER_BEHANDLING for at man kan disassosiere rammebehandling.sakId=$sakId, saksnummer:$saksnummer, klagebehandlingId=$id"
        }
        require(erSaksbehandlerPåBehandlingen(saksbehandler))
        return when (val res = resultat) {
            is Klagebehandlingsresultat.Omgjør -> this.copy(resultat = res.fjernRammebehandlingId(rammmebehandlingId))

            is Klagebehandlingsresultat.Avvist, null -> throw IllegalStateException(
                "Klagebehandling er ikke knyttet til en rammebehandling. sakId=$sakId, saksnummer:$saksnummer, klagebehandlingId=$id",
            )
        }
    }

    companion object {
        fun opprett(
            id: KlagebehandlingId = KlagebehandlingId.random(),
            saksnummer: Saksnummer,
            fnr: Fnr,
            opprettet: LocalDateTime,
            journalpostOpprettet: LocalDateTime,
            kommando: OpprettKlagebehandlingKommando,
        ): Klagebehandling {
            val formkrav = kommando.toKlageFormkrav()
            return Klagebehandling(
                id = id,
                sakId = kommando.sakId,
                saksnummer = saksnummer,
                fnr = fnr,
                opprettet = opprettet,
                sistEndret = opprettet,
                saksbehandler = kommando.saksbehandler.navIdent,
                formkrav = formkrav,
                journalpostOpprettet = journalpostOpprettet,
                journalpostId = kommando.journalpostId,
                status = UNDER_BEHANDLING,
                resultat = if (formkrav.erAvvisning) Klagebehandlingsresultat.Avvist.empty else null,
                iverksattTidspunkt = null,
                avbrutt = null,
            )
        }
    }

    sealed interface KanIkkeOppdateres {
        data class FeilKlagebehandlingsstatus(
            val forventetStatus: Klagebehandlingsstatus,
            val faktiskStatus: Klagebehandlingsstatus,
        ) : KanIkkeOppdateres

        data class FeilRammebehandlingssstatus(
            val forventetStatus: Rammebehandlingsstatus,
            val faktiskStatus: Rammebehandlingsstatus,
        ) : KanIkkeOppdateres

        data class FeilResultat(
            val forventetResultat: String,
            val faktiskResultat: String?,
        ) : KanIkkeOppdateres
    }

    /**
     * Sjekker både [Klagebehandlingsresultat] og [Rammebehandlingsstatus] hvis den er satt.
     */
    fun kanOppdatereIDenneStatusen(rammebehandlingsstatus: Rammebehandlingsstatus?): Either<KanIkkeOppdateres, Unit> {
        return when (this.status) {
            KLAR_TIL_BEHANDLING, AVBRUTT, VEDTATT -> {
                KanIkkeOppdateres.FeilKlagebehandlingsstatus(UNDER_BEHANDLING, this.status).left()
            }

            UNDER_BEHANDLING -> {
                when (rammebehandlingsstatus) {
                    Rammebehandlingsstatus.UNDER_BEHANDLING, null -> Unit.right()
                    Rammebehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING,
                    Rammebehandlingsstatus.AVBRUTT,
                    Rammebehandlingsstatus.KLAR_TIL_BEHANDLING,
                    Rammebehandlingsstatus.KLAR_TIL_BESLUTNING,
                    Rammebehandlingsstatus.UNDER_BESLUTNING,
                    Rammebehandlingsstatus.VEDTATT,
                    -> KanIkkeOppdateres.FeilRammebehandlingssstatus(
                        Rammebehandlingsstatus.UNDER_BEHANDLING,
                        rammebehandlingsstatus,
                    ).left()
                }
            }
        }
    }

    init {
        if (formkrav.erAvvisning || resultat == Klagebehandlingsresultat.Avvist) {
            require(resultat is Klagebehandlingsresultat.Avvist && formkrav.erAvvisning) {
                "Klagebehandling som er avvist må ha resultat satt til AVVIST.sakId=$sakId, saksnummer:$saksnummer, klagebehandlingId=$id"
            }
        }

        when (status) {
            KLAR_TIL_BEHANDLING -> {
                require(saksbehandler == null) {
                    "Klagebehandling som er $status kan ikke ha saksbehandler satt.sakId=$sakId, saksnummer:$saksnummer, klagebehandlingId=$id"
                }
            }

            UNDER_BEHANDLING -> {
                require(saksbehandler != null) {
                    "Klagebehandling som er $status må ha saksbehandler satt.sakId=$sakId, saksnummer:$saksnummer, klagebehandlingId=$id"
                }
            }

            AVBRUTT -> {
                require(saksbehandler != null) {
                    "Klagebehandling som er $status må ha saksbehandler satt.sakId=$sakId, saksnummer:$saksnummer, klagebehandlingId=$id"
                }
            }

            VEDTATT -> {
                require(saksbehandler != null) {
                    "Klagebehandling som er $status må ha saksbehandler satt.sakId=$sakId, saksnummer:$saksnummer, klagebehandlingId=$id"
                }
                require(iverksattTidspunkt != null) {
                    "Klagebehandling som er $status må ha iverksattTidspunkt satt.sakId=$sakId, saksnummer:$saksnummer, klagebehandlingId=$id"
                }
                require(resultat != null) {
                    "Klagebehandling som er $status må ha resultat satt.sakId=$sakId, saksnummer:$saksnummer, klagebehandlingId=$id"
                }
                when (resultat) {
                    is Klagebehandlingsresultat.Omgjør -> require(resultat.rammebehandlingId != null) {
                        "Klagebehandling som er $status med omgjøring må ha rammebehandlingId satt.sakId=$sakId, saksnummer:$saksnummer, klagebehandlingId=$id"
                    }

                    is Klagebehandlingsresultat.Avvist -> require(!resultat.brevtekst.isNullOrEmpty()) {
                        "Klagebehandling som er $status må ha brevtekst satt.sakId=$sakId, saksnummer:$saksnummer, klagebehandlingId=$id"
                    }
                }
            }
        }
    }
}
