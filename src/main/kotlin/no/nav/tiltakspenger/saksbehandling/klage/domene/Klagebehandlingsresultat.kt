package no.nav.tiltakspenger.saksbehandling.klage.domene

import arrow.core.NonEmptyList
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.saksbehandling.distribusjon.DistribusjonId
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.journalpost.DokumentInfoId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.AVBRUTT
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.FERDIGSTILT
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.KLAR_TIL_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.MOTTATT_FRA_KLAGEINSTANS
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.OMGJØRING_ETTER_KLAGEINSTANS
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.OPPRETTHOLDT
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.OVERSENDT
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.OVERSEND_FEILET
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.UNDER_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.VEDTATT
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.Brevtekster
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.Klageinstanshendelse
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.Klageinstanshendelse.KlagebehandlingAvsluttet.KlagehendelseKlagebehandlingAvsluttetUtfall
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.Klageinstanshendelse.OmgjøringskravbehandlingAvsluttet.OmgjøringskravbehandlingAvsluttetUtfall
import no.nav.tiltakspenger.saksbehandling.klage.domene.vurder.KlageOmgjøringsårsak
import no.nav.tiltakspenger.saksbehandling.klage.domene.vurder.VurderOmgjørKlagebehandlingKommando
import java.time.LocalDate
import java.time.LocalDateTime

sealed interface Klagebehandlingsresultat {

    val kanVæreKnyttetTilBehandling: Boolean
    val tilknyttetBehandlingId: List<BehandlingId>
    val brevtekst: Brevtekster?
    val ferdigstiltTidspunkt: LocalDateTime?
    val begrunnelseFerdigstilling: Begrunnelse?

    /** Dersom vi har journalført+distribuert innstillingsbrevet og ikke allerede har sendt klagen til klageinstansen */
    val kanOversendeKlageinstans: Boolean
    val harJournalførtInnstillingsbrev: Boolean
    val harDistribuertInnstillingsbrev: Boolean
    fun kanIverksetteVedtak(status: Klagebehandlingsstatus): Boolean?
    fun kanIverksetteOpprettholdelse(status: Klagebehandlingsstatus): Boolean
    val erKnyttetTilBehandling: Boolean
    val åpenBehandlingId: BehandlingId?
    fun skalGenerereBrevKunFraBehandling(status: Klagebehandlingsstatus): Boolean
    val kanOmgjøresEtterKA: Boolean

    /**
     * @param klagebehandlingId - kun brukt for logging
     */
    fun leggTilNyÅpenBehandling(
        behandlingId: BehandlingId,
        klagebehandlingId: KlagebehandlingId,
    ): Klagebehandlingsresultat

    fun ferdigstill(ferdigstiltTidspunkt: LocalDateTime, begrunnelse: Begrunnelse?): Klagebehandlingsresultat?
    fun nullstillÅpenBehandlingId(): Klagebehandlingsresultat?

    /**
     * Merk at en avvisning ikke er det samme som et avslag.
     * Det er et vedtak som kan klages på.
     */
    data class Avvist(
        override val brevtekst: Brevtekster?,
    ) : Klagebehandlingsresultat {

        override val erKnyttetTilBehandling = false
        override val kanVæreKnyttetTilBehandling: Boolean = false
        override val åpenBehandlingId: BehandlingId? = null
        override val tilknyttetBehandlingId: List<BehandlingId> = emptyList()
        override val kanOversendeKlageinstans = false
        override val harJournalførtInnstillingsbrev = false
        override val harDistribuertInnstillingsbrev = false
        override val kanOmgjøresEtterKA = false
        override val ferdigstiltTidspunkt: LocalDateTime? = null
        override val begrunnelseFerdigstilling = null

        /** Merk at generering av brev kan feile i tilstandene KLAR_TIL_BEHANDLING og AVBRUTT hvis [brevtekst] er null. */
        override fun skalGenerereBrevKunFraBehandling(status: Klagebehandlingsstatus): Boolean {
            return when (status) {
                UNDER_BEHANDLING -> false

                KLAR_TIL_BEHANDLING, AVBRUTT, VEDTATT -> true

                OPPRETTHOLDT, OVERSENDT, OVERSEND_FEILET, MOTTATT_FRA_KLAGEINSTANS, OMGJØRING_ETTER_KLAGEINSTANS, FERDIGSTILT -> throw IllegalStateException(
                    "$status er en ugyldig status for Avvist klage.",
                )
            }
        }

        override fun leggTilNyÅpenBehandling(
            behandlingId: BehandlingId,
            klagebehandlingId: KlagebehandlingId,
        ): Avvist {
            throw IllegalStateException("Avvist klage kan ikke knyttes til behandling. Dette skjedde for klagebehandlingId=$klagebehandlingId")
        }

        /** Avvist klagebehandling kan ikke ferdigstilles, så denne funksjonen returnerer alltid null. */
        override fun ferdigstill(
            ferdigstiltTidspunkt: LocalDateTime,
            begrunnelse: Begrunnelse?,
        ): Klagebehandlingsresultat? {
            return null
        }

        /**
         * resultat avvist har ingen endringer den skal gjøre ved iverksettelse
         */
        override fun nullstillÅpenBehandlingId(): Avvist {
            return this
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
     * Grunnen til at dette er et imperativ verb, er at selve klagebehandlingen utfører ikke omgjøringen; det er den følgende behandlingen som gjør.
     * @param tilknyttetBehandlingId Genereres av systemet når klagen omgjøres til en behandling. Vil være null ved f.eks. medhold på klage om tilbakekreving.
     */
    data class Omgjør(
        val årsak: KlageOmgjøringsårsak,
        val begrunnelse: Begrunnelse,
        override val ferdigstiltTidspunkt: LocalDateTime?,
        override val begrunnelseFerdigstilling: Begrunnelse?,
        override val tilknyttetBehandlingId: List<BehandlingId>,
        override val åpenBehandlingId: BehandlingId?,
    ) : Klagebehandlingsresultat {
        override val kanOversendeKlageinstans = false
        override val brevtekst = null
        override val kanVæreKnyttetTilBehandling: Boolean = true
        override val kanOmgjøresEtterKA = false
        override val harJournalførtInnstillingsbrev = false
        override val harDistribuertInnstillingsbrev = false
        override val erKnyttetTilBehandling = tilknyttetBehandlingId.isNotEmpty()

        override fun skalGenerereBrevKunFraBehandling(status: Klagebehandlingsstatus): Boolean {
            throw IllegalStateException("Omgjort klage skal ikke generere brev, så denne funksjonen skal ikke brukes.")
        }

        override fun leggTilNyÅpenBehandling(
            behandlingId: BehandlingId,
            klagebehandlingId: KlagebehandlingId,
        ): Omgjør {
            require(åpenBehandlingId == null) {
                "Kan kun legge til én åpen behandlingId. Eksisterende åpenBehandlingId var $åpenBehandlingId, forsøkte å legge til $behandlingId for klagebehandlingId $klagebehandlingId. Dersom $åpenBehandlingId har blitt iverksatt/avbrutt, må den fjernes fra feltet åpenBehandlingId"
            }
            return this.copy(
                tilknyttetBehandlingId = this.tilknyttetBehandlingId.plus(behandlingId),
                åpenBehandlingId = behandlingId,
            )
        }

        /** Brukes av frontend. Man kan ikke iverksette klagebehandlingen*/
        override fun kanIverksetteVedtak(status: Klagebehandlingsstatus): Boolean? {
            if (status != UNDER_BEHANDLING || tilknyttetBehandlingId.isEmpty()) return false
            return null
        }

        override fun kanIverksetteOpprettholdelse(status: Klagebehandlingsstatus) = false

        override fun ferdigstill(ferdigstiltTidspunkt: LocalDateTime, begrunnelse: Begrunnelse?): Omgjør {
            require(this.ferdigstiltTidspunkt == null) { "Kan kun sette ferdigstiltTidspunkt én gang" }
            return this.copy(ferdigstiltTidspunkt = ferdigstiltTidspunkt, begrunnelseFerdigstilling = begrunnelse)
        }

        override fun nullstillÅpenBehandlingId(): Omgjør {
            require(åpenBehandlingId != null) {
                "ÅpenBehandlingId skal ikke være null ved iverksettelse av omgjøring. Hvis dette skjer er det en bug som må fikses, eller så må det håndteres som en left."
            }
            return this.copy(åpenBehandlingId = null)
        }

        /** Kan oppdatere frem til behandlingen er KLAR_TIL_BESLUTNING */
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
        val dokumentInfoIder: List<DokumentInfoId>,
        val journalføringstidspunktInnstillingsbrev: LocalDateTime?,
        val distribusjonIdInnstillingsbrev: DistribusjonId?,
        val distribusjonstidspunktInnstillingsbrev: LocalDateTime?,
        val oversendtKlageinstansenTidspunkt: LocalDateTime?,
        val klageinstanshendelser: Klageinstanshendelser,
        override val ferdigstiltTidspunkt: LocalDateTime?,
        override val begrunnelseFerdigstilling: Begrunnelse?,
        override val tilknyttetBehandlingId: List<BehandlingId>,
        override val åpenBehandlingId: BehandlingId?,
    ) : Klagebehandlingsresultat {

        override val erKnyttetTilBehandling = tilknyttetBehandlingId.isNotEmpty()
        override val harJournalførtInnstillingsbrev: Boolean = journalpostIdInnstillingsbrev != null
        override val harDistribuertInnstillingsbrev: Boolean = distribusjonIdInnstillingsbrev != null
        override val kanOversendeKlageinstans =
            harJournalførtInnstillingsbrev && oversendtKlageinstansenTidspunkt == null
        val sisteKlageinstanshendelse = klageinstanshendelser.lastOrNull()

        private val kanVidereBehandlesEtterSvarFraKA = when (sisteKlageinstanshendelse) {
            is Klageinstanshendelse.BehandlingFeilregistrert -> false

            is Klageinstanshendelse.KlagebehandlingAvsluttet -> (
                listOf(
                    KlagehendelseKlagebehandlingAvsluttetUtfall.OPPHEVET,
                    KlagehendelseKlagebehandlingAvsluttetUtfall.MEDHOLD,
                    KlagehendelseKlagebehandlingAvsluttetUtfall.DELVIS_MEDHOLD,
                    KlagehendelseKlagebehandlingAvsluttetUtfall.UGUNST,
                ).contains(sisteKlageinstanshendelse.utfall)
                )

            is Klageinstanshendelse.OmgjøringskravbehandlingAvsluttet -> listOf(OmgjøringskravbehandlingAvsluttetUtfall.UGUNST).contains(
                sisteKlageinstanshendelse.utfall,
            )

            null -> false
        }

        override val kanOmgjøresEtterKA = kanVidereBehandlesEtterSvarFraKA
        override val kanVæreKnyttetTilBehandling = kanVidereBehandlesEtterSvarFraKA

        override fun kanIverksetteVedtak(status: Klagebehandlingsstatus): Boolean? = null

        override fun kanIverksetteOpprettholdelse(status: Klagebehandlingsstatus): Boolean {
            return status == UNDER_BEHANDLING && !brevtekst.isNullOrEmpty() && iverksattOpprettholdelseTidspunkt == null
        }

        /** Merk at generering av brev kan feile i tilstandene KLAR_TIL_BEHANDLING og AVBRUTT hvis [brevtekst] er null. */
        override fun skalGenerereBrevKunFraBehandling(status: Klagebehandlingsstatus): Boolean {
            return when (status) {
                UNDER_BEHANDLING -> false
                KLAR_TIL_BEHANDLING, AVBRUTT, OPPRETTHOLDT, OVERSENDT, OVERSEND_FEILET, MOTTATT_FRA_KLAGEINSTANS, OMGJØRING_ETTER_KLAGEINSTANS, FERDIGSTILT, VEDTATT -> true
                VEDTATT -> throw IllegalStateException("$status er en ugyldig status for Opprettholdt klage. Bruk FERDIGSTILT.")
            }
        }

        override fun leggTilNyÅpenBehandling(
            behandlingId: BehandlingId,
            klagebehandlingId: KlagebehandlingId,
        ): Opprettholdt {
            require(åpenBehandlingId == null) {
                "Kan kun legge til én åpen behandlingId. Eksisterende åpenBehandlingId var $åpenBehandlingId, forsøkte å legge til $behandlingId for klagebehandlingId $klagebehandlingId. Dersom $åpenBehandlingId har blitt iverksatt/avbrutt, må den fjernes fra feltet åpenBehandlingId"
            }
            require(klageinstanshendelser.isNotEmpty()) {
                "Kan kun legge til ny behandlingId dersom klagebehandlingen har mottatt minst én klageinstanshendelse. Dette for å sikre at vi ikke knytter klagebehandlingen til en behandling før vi har mottatt svar fra KA, og dermed unngår at klagebehandlingen blir videre behandlet."
            }
            return this.copy(
                tilknyttetBehandlingId = this.tilknyttetBehandlingId.plus(behandlingId),
                åpenBehandlingId = behandlingId,
            )
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
            dokumentInfoId: NonEmptyList<DokumentInfoId>,
            tidspunkt: LocalDateTime,
        ): Opprettholdt {
            require(this.journalpostIdInnstillingsbrev == null && this.journalføringstidspunktInnstillingsbrev == null) {
                "Kan kun sette journalpostId og journalføringstidspunkt én gang"
            }
            return copy(
                brevdato = brevdato,
                journalpostIdInnstillingsbrev = journalpostId,
                dokumentInfoIder = dokumentInfoId,
                journalføringstidspunktInnstillingsbrev = tidspunkt,
            )
        }

        fun leggTilKlageinstanshendelse(hendelse: Klageinstanshendelse): Opprettholdt {
            return copy(klageinstanshendelser = klageinstanshendelser.leggTil(hendelse))
        }

        override fun ferdigstill(ferdigstiltTidspunkt: LocalDateTime, begrunnelse: Begrunnelse?): Opprettholdt {
            require(this.ferdigstiltTidspunkt == null && klageinstanshendelser.isNotEmpty()) {
                "Kan kun sette ferdigstiltTidspunkt én gang, og må ha mottatt minst én klageinstanshendelse for å kunne oppdatere ferdigstiltTidspunkt"
            }
            return this.copy(ferdigstiltTidspunkt = ferdigstiltTidspunkt, begrunnelseFerdigstilling = begrunnelse)
        }

        override fun nullstillÅpenBehandlingId(): Opprettholdt {
            require(åpenBehandlingId != null) {
                "ÅpenBehandlingId skal ikke være null ved iverksettelse av opprettholdelse. Hvis dette skjer er det en bug som må fikses, eller så må det håndteres som en left."
            }

            return this.copy(åpenBehandlingId = null)
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
                    dokumentInfoIder = emptyList(),
                    journalføringstidspunktInnstillingsbrev = null,
                    distribusjonIdInnstillingsbrev = null,
                    distribusjonstidspunktInnstillingsbrev = null,
                    klageinstanshendelser = Klageinstanshendelser.empty(),
                    ferdigstiltTidspunkt = null,
                    tilknyttetBehandlingId = emptyList(),
                    begrunnelseFerdigstilling = null,
                    åpenBehandlingId = null,
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
