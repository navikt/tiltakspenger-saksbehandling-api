package no.nav.tiltakspenger.saksbehandling.klage.domene

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.dokument.GenererKlageAvvisningsbrev
import no.nav.tiltakspenger.saksbehandling.dokument.KunneIkkeGenererePdf
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.AVBRUTT
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.KLAR_TIL_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.UNDER_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.klage.domene.avbryt.AvbrytKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.avbryt.KanIkkeAvbryteKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.Brevtekster
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.KlagebehandlingBrevKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.KanIkkeOppdatereKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.KlageFormkrav
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.OppdaterKlagebehandlingFormkravKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.opprett.OpprettKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Representerer registrering og vurdering av en klage på et vedtak om tiltakspenger.
 * En klagebehandling har ingen beslutter, da klager avgjøres av en saksbehandler alene. Hvis det fører til medhold, vil en beslutter måtte beslutte selve revurderingen.
 *
 * @param brevtekst Brukes både for avvisning og innstilling.
 */
data class Klagebehandling(
    val id: KlagebehandlingId,
    val sakId: SakId,
    val saksnummer: Saksnummer,
    val fnr: Fnr,
    val opprettet: LocalDateTime,
    val sistEndret: LocalDateTime,
    val saksbehandler: String?,
    val journalpostId: JournalpostId,
    val journalpostOpprettet: LocalDateTime,
    val status: Klagebehandlingsstatus,
    val resultat: Klagebehandlingsresultat?,
    val formkrav: KlageFormkrav,
    val brevtekst: Brevtekster?,
) {
    val erUnderBehandling = status == UNDER_BEHANDLING
    val erKlarTilBehandling = status == KLAR_TIL_BEHANDLING
    val erAvbrutt = status == AVBRUTT

    val kanOppdateres = erKlarTilBehandling || erUnderBehandling

    val erÅpen = erKlarTilBehandling || erUnderBehandling

    // TODO jah: Utvid med iverksatt/innstilt når det er på plass
    val erAvsluttet = erAvbrutt

    val erAvvisning = formkrav.erAvvisning

    fun erSaksbehandlerPåBehandlingen(saksbehandler: Saksbehandler): Boolean {
        return this.saksbehandler == saksbehandler.navIdent
    }

    fun oppdaterFormkrav(
        kommando: OppdaterKlagebehandlingFormkravKommando,
        journalpostOpprettet: LocalDateTime,
        clock: Clock,
    ): Either<KanIkkeOppdatereKlagebehandling, Klagebehandling> {
        if (!kanOppdateres) return KanIkkeOppdatereKlagebehandling.KanIkkeOppdateres.left()
        if (!erSaksbehandlerPåBehandlingen(kommando.saksbehandler)) {
            return KanIkkeOppdatereKlagebehandling.SaksbehandlerMismatch(
                forventetSaksbehandler = this.saksbehandler!!,
                faktiskSaksbehandler = kommando.saksbehandler.navIdent,
            ).left()
        }
        return this.copy(
            sistEndret = nå(clock),
            formkrav = kommando.toKlageFormkrav(),
            journalpostId = kommando.journalpostId,
            journalpostOpprettet = journalpostOpprettet,
        ).right()
    }

    fun oppdaterBrevtekst(
        kommando: KlagebehandlingBrevKommando,
        clock: Clock,
    ): Either<KanIkkeOppdatereKlagebehandling, Klagebehandling> {
        if (!kanOppdateres) return KanIkkeOppdatereKlagebehandling.KanIkkeOppdateres.left()
        if (!erSaksbehandlerPåBehandlingen(kommando.saksbehandler)) {
            return KanIkkeOppdatereKlagebehandling.SaksbehandlerMismatch(
                forventetSaksbehandler = this.saksbehandler!!,
                faktiskSaksbehandler = kommando.saksbehandler.navIdent,
            ).left()
        }
        return this.copy(
            sistEndret = nå(clock),
            brevtekst = kommando.brevtekster,
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
        require(erAvvisning) {
            "Kan kun generere klagebrev for avvisning når formkrav er avvisning"
        }
        // TODO jah: if(erIverksatt) return genererBrev(genererAvvisningsbrev)
        val saksbehandler: String = when (status) {
            KLAR_TIL_BEHANDLING -> "-"
            UNDER_BEHANDLING -> this.saksbehandler!!
            AVBRUTT -> this.saksbehandler ?: "-"
        }
        val erSaksbehandlerPåBehandlingen = this.erSaksbehandlerPåBehandlingen(kommando.saksbehandler)
        val tilleggstekst: Brevtekster = when (status) {
            KLAR_TIL_BEHANDLING -> brevtekst ?: Brevtekster.empty
            UNDER_BEHANDLING -> if (erSaksbehandlerPåBehandlingen) {
                kommando.brevtekster
            } else {
                (
                    brevtekst
                        ?: Brevtekster.empty
                    )
            }

            AVBRUTT -> this.brevtekst ?: Brevtekster.empty
        }
        return genererAvvisningsbrev(
            saksnummer,
            fnr,
            saksbehandler,
            tilleggstekst,
            // TODO jah: Bytt til if(erIverksatt) false else true når iverksatt er på plass
            true,
        )
    }

    /**
     * Kun til bruk av systemet når det skal genereres endelig brev.
     */
    suspend fun genererBrev(
        genererAvvisningsbrev: GenererKlageAvvisningsbrev,
    ): Either<KunneIkkeGenererePdf, PdfOgJson> {
        // TODO jah: require(erIverksatt)
        return genererAvvisningsbrev(
            saksnummer,
            fnr,
            saksbehandler!!,
            brevtekst!!,
            false,
        )
    }

    fun avbryt(
        kommando: AvbrytKlagebehandlingKommando,
        clock: Clock,
    ): Either<KanIkkeAvbryteKlagebehandling, Klagebehandling> {
        require(!erAvsluttet) {
            "Klagebehandling er allerede avsluttet og kan ikke avbrytes. klagebehandlingId=$id"
        }
        if (saksbehandler != kommando.saksbehandler.navIdent) {
            return KanIkkeAvbryteKlagebehandling.SaksbehandlerMismatch(
                forventetSaksbehandler = this.saksbehandler!!,
                faktiskSaksbehandler = kommando.saksbehandler.navIdent,
            ).left()
        }
        return this.copy(
            sistEndret = nå(clock),
            status = AVBRUTT,
        ).right()
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
                resultat = if (formkrav.erAvvisning) Klagebehandlingsresultat.AVVIST else null,
                brevtekst = null,
            )
        }
    }

    init {
        if (formkrav.erAvvisning) {
            require(resultat == Klagebehandlingsresultat.AVVIST) {
                "Klagebehandling som er avvist må ha resultat satt til AVVIST"
            }
        } else {
            require(resultat == null) {
                "Klagebehandling som ikke er avvist kan ikke ha resultat satt ved opprettelse"
            }
        }
        when (status) {
            KLAR_TIL_BEHANDLING -> {
                require(saksbehandler == null) {
                    "Klagebehandling som er $status kan ikke ha saksbehandler satt"
                }
            }

            UNDER_BEHANDLING -> {
                require(saksbehandler != null) {
                    "Klagebehandling som er $status må ha saksbehandler satt"
                }
            }

            AVBRUTT -> {
                require(saksbehandler != null) {
                    "Klagebehandling som er $status må ha saksbehandler satt"
                }
            }
        }
    }
}
