package no.nav.tiltakspenger.saksbehandling.klage.domene

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.saksbehandling.distribusjon.DistribusjonId
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.AVBRUTT
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.KLAR_TIL_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.OPPRETTHOLDT
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.OVERSENDT
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.UNDER_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.VEDTATT
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.Brevtekster
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.Klageinstanshendelse
import no.nav.tiltakspenger.saksbehandling.klage.domene.vurder.KlageOmgjøringsårsak
import no.nav.tiltakspenger.saksbehandling.klage.domene.vurder.VurderOmgjørKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Begrunnelse
import java.time.LocalDate
import java.time.LocalDateTime

sealed interface Klagebehandlingsresultat {

    val rammebehandlingId: BehandlingId?
    val brevtekst: Brevtekster?

    /** Dersom vi har journalført+distribuert innstillingsbrevet og ikke allerede har sendt klagen til klageinstansen */
    val kanOversendeKlageinstans: Boolean
    val harJournalførtInnstillingsbrev: Boolean
    val harDistribuertInnstillingsbrev: Boolean
    fun kanIverksetteVedtak(status: Klagebehandlingsstatus): Boolean?
    fun kanIverksetteOpprettholdelse(status: Klagebehandlingsstatus): Boolean
    val erKnyttetTilRammebehandling: Boolean
    fun skalGenerereBrevKunFraBehandling(status: Klagebehandlingsstatus): Boolean

    /**
     * Merk at en avvisning ikke er det samme som et avslag.
     * Det er et vedtak som kan klages på.
     */
    data class Avvist(
        override val brevtekst: Brevtekster?,
    ) : Klagebehandlingsresultat {

        override val erKnyttetTilRammebehandling = false
        override val rammebehandlingId = null
        override val kanOversendeKlageinstans = false
        override val harJournalførtInnstillingsbrev = false
        override val harDistribuertInnstillingsbrev = false

        /** Merk at generering av brev kan feile i tilstandene KLAR_TIL_BEHANDLING og AVBRUTT hvis [brevtekst] er null. */
        override fun skalGenerereBrevKunFraBehandling(status: Klagebehandlingsstatus): Boolean {
            return when (status) {
                UNDER_BEHANDLING -> false
                KLAR_TIL_BEHANDLING, AVBRUTT, VEDTATT -> true
                OPPRETTHOLDT, OVERSENDT -> throw IllegalStateException("$status er en ugyldig status for Avvist klage.")
            }
        }

        override fun kanIverksetteVedtak(status: Klagebehandlingsstatus): Boolean {
            return status == UNDER_BEHANDLING && !brevtekst.isNullOrEmpty()
        }

        override fun kanIverksetteOpprettholdelse(status: Klagebehandlingsstatus) = false

        fun oppdaterBrevtekst(
            brevtekst: Brevtekster,
        ): Avvist {
            return this.copy(
                brevtekst = brevtekst,
            )
        }

        companion object {
            val empty = Avvist(null)
        }
    }

    /**
     * Grunnen til at dette er et imperativ verb, er at selve klagebehandlingen utfører ikke omgjøringen; det er den følgende rammebehandlingen som gjør.
     * @param rammebehandlingId Genereres av systemet når klagen omgjøres til en rammebehandling. Vil være null ved f.eks. medhold på klage om tilbakekreving.
     */
    data class Omgjør(
        val årsak: KlageOmgjøringsårsak,
        val begrunnelse: Begrunnelse,
        override val rammebehandlingId: BehandlingId?,
    ) : Klagebehandlingsresultat {
        override val kanOversendeKlageinstans = false
        override val brevtekst = null

        override fun skalGenerereBrevKunFraBehandling(status: Klagebehandlingsstatus): Boolean {
            throw IllegalStateException("Omgjort klage skal ikke generere brev, så denne funksjonen skal ikke brukes.")
        }

        override val harJournalførtInnstillingsbrev = false
        override val harDistribuertInnstillingsbrev = false

        /** Brukes av frontend. Man kan ikke iverksette klagebehandlingen*/
        override fun kanIverksetteVedtak(status: Klagebehandlingsstatus): Boolean? {
            if (status != UNDER_BEHANDLING || rammebehandlingId == null) return false
            return null
        }

        override fun kanIverksetteOpprettholdelse(status: Klagebehandlingsstatus) = false

        override val erKnyttetTilRammebehandling = rammebehandlingId != null

        /** Kan oppdatere frem til rammebehandlingen er KLAR_TIL_BESLUTNING */
        fun oppdater(kommando: VurderOmgjørKlagebehandlingKommando): Omgjør {
            return this.copy(
                årsak = kommando.årsak,
                begrunnelse = kommando.begrunnelse,
            )
        }
    }

    /**
     * @param iverksattOpprettholdelseTidspunkt Tidspunktet saksbehandler effektuerte/iverksatte opprettholdelsen. Brevteksten må være ferdigutfylt. Innstillingsbrevet journalføres/distribueres og klagen oversendes til klageinstansen etter dette.
     * @param oversendtKlageinstansenTidspunkt Tidspunktet klagebehandlingen faktisk ble oversendt til Kabal. Dette settes når vi får bekreftelse fra Kabal om at klagebehandlingen er mottatt.
     */
    data class Opprettholdt(
        val hjemler: Klagehjemler,
        override val brevtekst: Brevtekster?,
        val iverksattOpprettholdelseTidspunkt: LocalDateTime?,
        val brevdato: LocalDate?,
        val journalpostIdInnstillingsbrev: JournalpostId?,
        val journalføringstidspunktInnstillingsbrev: LocalDateTime?,
        val distribusjonIdInnstillingsbrev: DistribusjonId?,
        val distribusjonstidspunktInnstillingsbrev: LocalDateTime?,
        val oversendtKlageinstansenTidspunkt: LocalDateTime?,
        val klageinstanshendelser: Klageinstanshendelser,
    ) : Klagebehandlingsresultat {

        override val erKnyttetTilRammebehandling = false
        override val rammebehandlingId = null
        override val harJournalførtInnstillingsbrev: Boolean = journalpostIdInnstillingsbrev != null
        override val harDistribuertInnstillingsbrev: Boolean = distribusjonIdInnstillingsbrev != null
        override val kanOversendeKlageinstans =
            harJournalførtInnstillingsbrev && oversendtKlageinstansenTidspunkt == null

        override fun kanIverksetteVedtak(status: Klagebehandlingsstatus): Boolean {
            // TODO jah: Legg til den etter vi har mappet klageinstans-hendelsen.
            return false
        }

        override fun kanIverksetteOpprettholdelse(status: Klagebehandlingsstatus): Boolean {
            return status == UNDER_BEHANDLING && !brevtekst.isNullOrEmpty() && iverksattOpprettholdelseTidspunkt == null
        }

        /** Merk at generering av brev kan feile i tilstandene KLAR_TIL_BEHANDLING og AVBRUTT hvis [brevtekst] er null. */
        override fun skalGenerereBrevKunFraBehandling(status: Klagebehandlingsstatus): Boolean {
            // Safe guard i tilfelle vi finner på å endre statusen tilbake til UNDER_BEHANDLING etter vi har fått svaret fra klageinstansen.
            if (journalpostIdInnstillingsbrev != null) return true
            return when (status) {
                UNDER_BEHANDLING -> false
                KLAR_TIL_BEHANDLING, AVBRUTT, OPPRETTHOLDT, OVERSENDT -> true
                VEDTATT -> throw IllegalStateException("$status er en ugyldig status for Avvist klage. TODO jah: vi legger til denne statusen når vi håndterer svaret fra klageinstansen.")
            }
        }

        fun oppdaterBrevtekst(brevtekst: Brevtekster): Opprettholdt = this.copy(brevtekst = brevtekst)
        fun oppdaterHjemler(hjemler: Klagehjemler) = this.copy(hjemler = hjemler)

        fun oppdaterOversendtKlageinstansenTidspunkt(tidspunkt: LocalDateTime): Opprettholdt {
            require(iverksattOpprettholdelseTidspunkt != null && oversendtKlageinstansenTidspunkt == null) { "Kan kun sette oversendtTidspunkt én gang" }
            return this.copy(oversendtKlageinstansenTidspunkt = tidspunkt)
        }

        fun oppdaterInnstillingsbrevDistribusjon(
            distribusjonId: DistribusjonId,
            tidspunkt: LocalDateTime,
        ): Opprettholdt {
            require(this.distribusjonIdInnstillingsbrev == null && this.distribusjonstidspunktInnstillingsbrev == null && this.journalpostIdInnstillingsbrev != null) {
                "Kan kun sette distribusjonId og distribusjonstidspunkt én gang, og må ha journalført innstillingsbrevet for å kunne oppdatere distribusjonsinformasjon"
            }
            return copy(
                distribusjonIdInnstillingsbrev = distribusjonId,
                distribusjonstidspunktInnstillingsbrev = tidspunkt,
            )
        }

        fun oppdaterInnstillingsbrevJournalpost(
            brevdato: LocalDate,
            journalpostId: JournalpostId,
            tidspunkt: LocalDateTime,
        ): Opprettholdt {
            require(this.journalpostIdInnstillingsbrev == null && this.journalføringstidspunktInnstillingsbrev == null) {
                "Kan kun sette journalpostId og journalføringstidspunkt én gang"
            }
            return copy(
                brevdato = brevdato,
                journalpostIdInnstillingsbrev = journalpostId,
                journalføringstidspunktInnstillingsbrev = tidspunkt,
            )
        }

        fun leggTilKlageinstanshendelse(hendelse: Klageinstanshendelse): Opprettholdt {
            return copy(klageinstanshendelser = klageinstanshendelser.leggTil(hendelse))
        }

        companion object {
            fun create(hjemler: Klagehjemler): Opprettholdt {
                return Opprettholdt(
                    hjemler = hjemler,
                    brevtekst = null,
                    iverksattOpprettholdelseTidspunkt = null,
                    brevdato = null,
                    oversendtKlageinstansenTidspunkt = null,
                    journalpostIdInnstillingsbrev = null,
                    journalføringstidspunktInnstillingsbrev = null,
                    distribusjonIdInnstillingsbrev = null,
                    distribusjonstidspunktInnstillingsbrev = null,
                    klageinstanshendelser = Klageinstanshendelser.empty(),
                )
            }
        }

        init {
            if (brevtekst != null) {
                require(brevtekst.isNotEmpty()) { "brevtekst kan ikke være tom hvis den er satt" }
            }
            if (iverksattOpprettholdelseTidspunkt != null) {
                require(brevtekst != null) { "brevtekst må være satt hvis oversendtTilKabalTidspunkt er satt" }
            }
            if (oversendtKlageinstansenTidspunkt != null) {
                require(iverksattOpprettholdelseTidspunkt != null) { "iverksattOpprettholdelseTidspunkt må være satt hvis oversendtTilKabalTidspunkt er satt" }
                require(brevtekst != null) { "brevtekst må være satt hvis oversendtTilKabalTidspunkt er satt" }
                require(journalpostIdInnstillingsbrev != null) { "journalpostIdInnstillingsbrev for oversendelsesbrev må være satt hvis oversendtTilKabalTidspunkt er satt" }
                require(journalføringstidspunktInnstillingsbrev != null) { "journalføringstidspunktInnstillingsbrev for oversendelsesbrev må være satt hvis oversendtTilKabalTidspunkt er satt" }
            }
            if (journalføringstidspunktInnstillingsbrev != null) {
                require(journalpostIdInnstillingsbrev != null) { "journalpostIdInnstillingsbrev for oversendelsesbrev må være satt hvis journalføringstidspunkt for oversendelsesbrev er satt" }
                require(iverksattOpprettholdelseTidspunkt != null) { "iverksattOpprettholdelseTidspunkt må være satt hvis journalføringstidspunkt for oversendelsesbrev er satt" }
            }
            if (journalpostIdInnstillingsbrev != null) {
                require(journalføringstidspunktInnstillingsbrev != null) { "journalføringstidspunktInnstillingsbrev for oversendelsesbrev må være satt hvis JournalpostId for oversendelsesbrev er satt" }
            }
        }
    }
}
