package no.nav.tiltakspenger.saksbehandling.klage.domene

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import arrow.core.toNonEmptyListOrThrow
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.felles.Avbrutt
import no.nav.tiltakspenger.saksbehandling.felles.Ventestatus
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.AVBRUTT
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.KLAR_TIL_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.OVERSENDT
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.UNDER_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.VEDTATT
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.Brevtekster
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.KlageFormkrav
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
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
    val ventestatus: Ventestatus,
) : Behandling {
    val brevtekst: Brevtekster? = resultat?.brevtekst
    val erUnderBehandling = status == UNDER_BEHANDLING

    @Suppress("unused")
    val erKlarTilBehandling = status == KLAR_TIL_BEHANDLING
    override val erAvbrutt = status == AVBRUTT
    val erVedtatt = status == VEDTATT
    val erOversendt = status == OVERSENDT
    override val erAvsluttet = erAvbrutt || erVedtatt
    val erÅpen = !erAvsluttet
    val erAvvisning = resultat is Klagebehandlingsresultat.Avvist
    val erOmgjøring = resultat is Klagebehandlingsresultat.Omgjør
    val erOpprettholdt = resultat is Klagebehandlingsresultat.Opprettholdt

    /**
     * Hvis resultatet er [Klagebehandlingsresultat.Omgjør] og [Klagebehandlingsresultat.Omgjør.rammebehandlingId] er satt.
     * Merk at dersom rammebehandlingen avbrytes vil denne verdien settes til null.
     */
    val erKnyttetTilRammebehandling: Boolean = resultat?.erKnyttetTilRammebehandling == true
    val rammebehandlingId: BehandlingId? = when (val res = resultat) {
        is Klagebehandlingsresultat.Omgjør -> res.rammebehandlingId
        is Klagebehandlingsresultat.Avvist, is Klagebehandlingsresultat.Opprettholdt, null -> null
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

    /**
     * Sjekker både [Klagebehandlingsresultat] og [Rammebehandlingsstatus] hvis den er satt.
     */
    fun kanOppdatereIDenneStatusen(
        rammebehandlingsstatus: Rammebehandlingsstatus?,
        kanVæreUnderBehandling: Boolean = true,
        kanVæreKlarTilBehandling: Boolean = false,
    ): Either<KanIkkeOppdatereKlagebehandling, Unit> {
        val forventetKlagebehandlingsstatuser = listOfNotNull(
            if (kanVæreUnderBehandling) UNDER_BEHANDLING else null,
            if (kanVæreKlarTilBehandling) KLAR_TIL_BEHANDLING else null,
        ).toNonEmptyListOrThrow()
        val forventetRammebehandlingstatuser = listOfNotNull(
            if (kanVæreUnderBehandling) Rammebehandlingsstatus.UNDER_BEHANDLING else null,
            if (kanVæreKlarTilBehandling) Rammebehandlingsstatus.KLAR_TIL_BEHANDLING else null,
        ).toNonEmptyListOrThrow()
        if (!forventetKlagebehandlingsstatuser.contains(this.status)) {
            return KanIkkeOppdatereKlagebehandling.FeilKlagebehandlingsstatus(
                forventetStatus = forventetKlagebehandlingsstatuser,
                faktiskStatus = this.status,
            ).left()
        }
        if (rammebehandlingsstatus != null && !forventetRammebehandlingstatuser.contains(rammebehandlingsstatus)) {
            return KanIkkeOppdatereKlagebehandling.FeilRammebehandlingssstatus(
                forventetStatus = forventetRammebehandlingstatuser,
                faktiskStatus = rammebehandlingsstatus,
            ).left()
        }
        return Unit.right()
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

                    is Klagebehandlingsresultat.Opprettholdt -> throw IllegalArgumentException("Klagebehandling som er $status kan ikke ha resultat satt til OPPRETHOLDT.sakId=$sakId, saksnummer:$saksnummer, klagebehandlingId=$id")
                }
            }

            OVERSENDT -> {
                require(saksbehandler != null) {
                    "Klagebehandling som er $status må ha saksbehandler satt.sakId=$sakId, saksnummer:$saksnummer, klagebehandlingId=$id"
                }
                require(iverksattTidspunkt != null) {
                    "Klagebehandling som er $status må ha iverksattTidspunkt satt.sakId=$sakId, saksnummer:$saksnummer, klagebehandlingId=$id"
                }
                require(resultat is Klagebehandlingsresultat.Opprettholdt) {
                    "Klagebehandling som er $status må ha resultat som opprettholdt satt.sakId=$sakId, saksnummer:$saksnummer, klagebehandlingId=$id"
                }
                require(rammebehandlingId == null) {
                    "Klagebehandling som er $status skal ikke være knyttet en rammebehandling. sakId=$sakId, saksnummer:$saksnummer, klagebehandlingId=$id"
                }
            }
        }
    }

    companion object {
        // Må ligge ved for å kunne opprette Companion extension functions.
    }
}
