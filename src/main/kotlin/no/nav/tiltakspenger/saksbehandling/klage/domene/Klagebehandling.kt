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
import no.nav.tiltakspenger.saksbehandling.distribusjon.DistribusjonId
import no.nav.tiltakspenger.saksbehandling.felles.Avbrutt
import no.nav.tiltakspenger.saksbehandling.felles.Ventestatus
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.AVBRUTT
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.KLAR_TIL_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.OPPRETTHOLDT
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.OVERSENDT
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.UNDER_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.VEDTATT
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.Brevtekster
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.KlageFormkrav
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.Klageinstanshendelse
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.NyKlagehendelse
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Representerer registrering og vurdering av en klage på et vedtak om tiltakspenger.
 * En klagebehandling har ingen beslutter, da klager avgjøres av en saksbehandler alene. Hvis det fører til medhold, vil en beslutter måtte beslutte selve revurderingen.
 * @param klagensJournalpostId Journalposten som inneholder klagen.
 * @param klagensJournalpostOpprettet Tidspunktet [klagensJournalpostId] ble opprettet.
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
    val klagensJournalpostId: JournalpostId,
    val klagensJournalpostOpprettet: LocalDateTime,
    val status: Klagebehandlingsstatus,
    val resultat: Klagebehandlingsresultat?,
    val formkrav: KlageFormkrav,
    val avbrutt: Avbrutt?,
    val ventestatus: Ventestatus,
) : Behandling {

    val brevtekst: Brevtekster? = resultat?.brevtekst
    val erUnderBehandling = status == UNDER_BEHANDLING

    @Suppress("unused")
    val erKlarTilBehandling: Boolean = status == KLAR_TIL_BEHANDLING
    override val erAvbrutt: Boolean = status == AVBRUTT
    val erVedtatt: Boolean = status == VEDTATT

    /** Dersom klagen er oversendt til klageinstansen eller etterfølgende status. */
    @Suppress("unused")
    val harJournalførtInnstillingsbrev: Boolean = resultat?.harJournalførtInnstillingsbrev == true

    /** Dersom vi har journalført+distribuert innstillingsbrevet og ikke allerede har sendt klagen til klageinstansen */
    val kanOversendeKlageinstans: Boolean = resultat?.kanOversendeKlageinstans == true
    override val erAvsluttet: Boolean = erAvbrutt || erVedtatt
    val erÅpen: Boolean = !erAvsluttet
    val erAvvisning: Boolean = resultat is Klagebehandlingsresultat.Avvist

    /** Dersom resultatet er omgjøring og klagebehandlingen er i en tilstand som kan iverksettes, returneres null; siden det er Rammebehandlingen som avgjør dette. */
    val kanIverksetteVedtak: Boolean? = if (resultat == null) false else resultat.kanIverksetteVedtak(status)
    val kanIverksetteOpprettholdelse = resultat?.kanIverksetteOpprettholdelse(status) ?: false

    @Suppress("unused")
    val erOmgjøring: Boolean = resultat is Klagebehandlingsresultat.Omgjør
    val erOpprettholdt: Boolean = resultat is Klagebehandlingsresultat.Opprettholdt

    /**
     * Hvis resultatet er [Klagebehandlingsresultat.Omgjør] og [Klagebehandlingsresultat.Omgjør.rammebehandlingId] er satt.
     * Merk at dersom rammebehandlingen avbrytes vil denne verdien settes til null.
     */
    val erKnyttetTilRammebehandling: Boolean = resultat?.erKnyttetTilRammebehandling == true
    val rammebehandlingId: BehandlingId? = resultat?.rammebehandlingId

    /**
     * Siden vi i alle tilfeller genererer brevet på nytt, må vi skille på om vi skal akseptere forhåndsvisningens parametre eller ikke.
     * Etter vi har passert et visst punkt i behandlingen, skal ikke saksbehandler kunne påvirke innholdet i brevet lenger.
     * Per tidspunkt har vi bare 2 brev i klagebehandlingen: avvisningsbrev og innstillingsbrev. Så vi kan gjenbruke denne variabelen frem til en behandling har mer enn 1 brev.
     */
    fun skalGenerereBrevKunFraBehandling(): Boolean = resultat?.skalGenerereBrevKunFraBehandling(status) == true

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

    fun oppdaterInnstillingsbrevJournalpost(
        brevdato: LocalDate,
        journalpostId: JournalpostId,
        tidspunkt: LocalDateTime,
    ): Klagebehandling {
        require(resultat is Klagebehandlingsresultat.Opprettholdt) {
            "Kun klagebehandlinger med resultat Opprettholdt kan journalføre innstillingsbrev. sakId=$sakId, saksnummer:$saksnummer, klagebehandlingId=$id"
        }
        return this.copy(
            resultat = resultat.oppdaterInnstillingsbrevJournalpost(brevdato, journalpostId, tidspunkt),
        )
    }

    fun oppdaterInnstillingsbrevDistribusjon(
        distribusjonId: DistribusjonId,
        tidspunkt: LocalDateTime,
    ): Klagebehandling {
        require(resultat is Klagebehandlingsresultat.Opprettholdt) {
            "Kun klagebehandlinger med resultat Opprettholdt kan ha innstillingsbrevdistribusjon. sakId=$sakId, saksnummer:$saksnummer, klagebehandlingId=$id"
        }
        return this.copy(
            resultat = resultat.oppdaterInnstillingsbrevDistribusjon(distribusjonId, tidspunkt),
        )
    }

    fun leggTilKlageinstanshendelse(hendelse: Klageinstanshendelse, sistEndret: LocalDateTime): Klagebehandling {
        // TODO jah: Basert på hendelsestypen, må vi sende tilbake en handling til klagebehandlingen for å oppdatere tilstanden.
        return this.copy(
            resultat = (resultat as Klagebehandlingsresultat.Opprettholdt).leggTilKlageinstanshendelse(hendelse),
            sistEndret = sistEndret,
        )
    }

    init {
        val loggkontekst = "sakId=$sakId, saksnummer:$saksnummer, klagebehandlingId=$id"
        if (formkrav.erAvvisning || resultat == Klagebehandlingsresultat.Avvist) {
            require(resultat is Klagebehandlingsresultat.Avvist && formkrav.erAvvisning) {
                "Klagebehandling som er avvist må ha resultat satt til AVVIST. $loggkontekst"
            }
        }

        when (status) {
            KLAR_TIL_BEHANDLING -> {
                require(saksbehandler == null) {
                    "Klagebehandling som er $status kan ikke ha saksbehandler satt. $loggkontekst"
                }
            }

            UNDER_BEHANDLING -> {
                require(saksbehandler != null) {
                    "Klagebehandling som er $status må ha saksbehandler satt. $loggkontekst"
                }
            }

            AVBRUTT -> {
                require(saksbehandler != null) {
                    "Klagebehandling som er $status må ha saksbehandler satt. $loggkontekst"
                }
            }

            VEDTATT -> {
                require(saksbehandler != null) {
                    "Klagebehandling som er $status må ha saksbehandler satt. $loggkontekst"
                }
                require(iverksattTidspunkt != null) {
                    "Klagebehandling som er $status må ha iverksattTidspunkt satt. $loggkontekst"
                }
                require(resultat != null) {
                    "Klagebehandling som er $status må ha resultat satt. $loggkontekst"
                }
                when (resultat) {
                    is Klagebehandlingsresultat.Omgjør -> require(resultat.rammebehandlingId != null) {
                        "Klagebehandling som er $status med omgjøring må ha rammebehandlingId satt. $loggkontekst"
                    }

                    is Klagebehandlingsresultat.Avvist -> require(!resultat.brevtekst.isNullOrEmpty()) {
                        "Klagebehandling som er $status må ha brevtekst satt. $loggkontekst"
                    }

                    is Klagebehandlingsresultat.Opprettholdt -> throw IllegalArgumentException("Klagebehandling som er $status kan ikke ha resultat satt til OPPRETTHOLDT. $loggkontekst")
                }
            }

            OPPRETTHOLDT -> {
                require(iverksattTidspunkt == null) {
                    "Klagebehandling som er $status kan ikke ha iverksattTidspunkt satt. $loggkontekst"
                }
                require(resultat is Klagebehandlingsresultat.Opprettholdt) {
                    "Klagebehandling til oversending må ha resultat som opprettholdt satt. $loggkontekst"
                }
                require(resultat.iverksattOpprettholdelseTidspunkt != null) {
                    "Klagebehandling til oversending må ha skalOversendesTidspunkt satt. $loggkontekst"
                }
                require(resultat.oversendtKlageinstansenTidspunkt == null) {
                    "Klagebehandling til oversending kan ikke ha oversendtTidspunkt satt. $loggkontekst"
                }
                require(!resultat.brevtekst.isNullOrEmpty()) {
                    "Klagebehandling til oversending må ha brevtekst satt. $loggkontekst"
                }
            }

            OVERSENDT -> {
                require(iverksattTidspunkt == null) {
                    "Klagebehandling som er $status kan ikke ha iverksattTidspunkt satt. $loggkontekst"
                }
                require(resultat is Klagebehandlingsresultat.Opprettholdt) {
                    "Oversendt klagebehandling må ha resultat som opprettholdt satt. $loggkontekst"
                }
                require(resultat.iverksattOpprettholdelseTidspunkt != null) {
                    "Klagebehandling til oversending må ha skalOversendesTidspunkt satt. $loggkontekst"
                }
                require(resultat.oversendtKlageinstansenTidspunkt != null) {
                    "Klagebehandling som er oversendt må ha oversendtTidspunkt satt. $loggkontekst"
                }
                require(!resultat.brevtekst.isNullOrEmpty()) {
                    "Klagebehandling til oversending må ha brevtekst satt. $loggkontekst"
                }
            }
        }
    }

    companion object {
        // Må ligge ved for å kunne opprette Companion extension functions.
    }
}
