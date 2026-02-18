package no.nav.tiltakspenger.saksbehandling.klage.infra.http

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Hjemmel
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagehjemler
import java.time.LocalDate

fun Klagebehandling.toOversendelsesDto(
    journalpostIdVedtak: JournalpostId,
): KlageOversendelseDto {
    val resultat = this.resultat as Klagebehandlingsresultat.Opprettholdt
    return KlageOversendelseDto(
        sakenGjelder = SakenGjelder(
            id = SakenGjelderId(verdi = this.fnr.verdi),
        ),
        fagsak = OversendelsesFagsak(
            fagsakId = this.saksnummer.verdi,
        ),
        kildeReferanse = this.id.toString(),
        dvhReferanse = this.id.toString(),
        hjemler = resultat.hjemler.tilHjemlerDto(),
        tilknyttedeJournalposter = listOf(
            TilknyttetJournalpost(
                type = TilknyttetJournalpostType.BRUKERS_KLAGE,
                journalpostId = this.klagensJournalpostId.toString(),
            ),
            TilknyttetJournalpost(
                type = TilknyttetJournalpostType.OPPRINNELIG_VEDTAK,
                journalpostId = journalpostIdVedtak.toString(),
            ),
            TilknyttetJournalpost(
                type = TilknyttetJournalpostType.OVERSENDELSESBREV,
                // journalpostIdInnstillingsbrev skal være satt før jobben sender til KA.
                journalpostId = resultat.journalpostIdInnstillingsbrev!!.toString(),
            ),
        ),
        brukersKlageMottattVedtaksinstans = this.formkrav.innsendingsdato,
    )
}

data class KlageOversendelseDto(
    val sakenGjelder: SakenGjelder,
    val fagsak: OversendelsesFagsak,
    val kildeReferanse: String,
    val dvhReferanse: String?,
    val hjemler: List<String>,
    val tilknyttedeJournalposter: List<TilknyttetJournalpost>,
    val brukersKlageMottattVedtaksinstans: LocalDate?,
) {
    @JsonInclude
    val type: String = "KLAGE"

    @JsonInclude
    val forrigeBehandlendeEnhet = "0387"

    /**
     * https://github.com/navikt/klage-kodeverk/blob/be40830223d5b0aadf16ff1bf6991645f1431d59/src/main/kotlin/no/nav/klage/kodeverk/ytelse/Ytelse.kt#L43
     */
    @JsonInclude
    val ytelse: String = "TIL_TIP"
}

data class SakenGjelder(
    val id: SakenGjelderId,
)

data class SakenGjelderId(
    /**
     * Fødselsnummer
     */
    val verdi: String,
) {
    @JsonInclude
    val type: String = "PERSON"
}

data class OversendelsesFagsak(
    val fagsakId: String,
) {
    @JsonInclude
    val fagsystem: String = "TILTAKSPENGER"
}

data class TilknyttetJournalpost(
    val type: TilknyttetJournalpostType,
    val journalpostId: String,
)

enum class TilknyttetJournalpostType {
    BRUKERS_SOEKNAD,
    OPPRINNELIG_VEDTAK,
    BRUKERS_KLAGE,
    OVERSENDELSESBREV,
    KLAGE_VEDTAK,
    ANNET,
}

fun Klagehjemler.tilHjemlerDto(): List<String> {
    return this.map {
        when (it) {
            // TODO jah klage: Synkroniser med det Kabal-gjengen legger inn hos seg.
            Hjemmel.ArbeidsmarkedslovenHjemmel.ARBEIDSMARKEDSLOVEN_13 -> "ARBEIDSMARKEDSLOVEN_13"

            Hjemmel.ArbeidsmarkedslovenHjemmel.ARBEIDSMARKEDSLOVEN_13_L1 -> "ARBEIDSMARKEDSLOVEN_13_L1"

            Hjemmel.ArbeidsmarkedslovenHjemmel.ARBEIDSMARKEDSLOVEN_13_L4 -> "ARBEIDSMARKEDSLOVEN_13_L4"

            Hjemmel.ArbeidsmarkedslovenHjemmel.ARBEIDSMARKEDSLOVEN_15 -> "ARBEIDSMARKEDSLOVEN_15"

            Hjemmel.ArbeidsmarkedslovenHjemmel.ARBEIDSMARKEDSLOVEN_17 -> "ARBEIDSMARKEDSLOVEN_17"

            Hjemmel.ArbeidsmarkedslovenHjemmel.ARBEIDSMARKEDSLOVEN_2 -> "ARBEIDSMARKEDSLOVEN_2"

            Hjemmel.ArbeidsmarkedslovenHjemmel.ARBEIDSMARKEDSLOVEN_22 -> "ARBEIDSMARKEDSLOVEN_22"

            Hjemmel.FolketrygdlovenHjemmel.FOLKETRYGDLOVEN_22_15 -> "FOLKETRYGDLOVEN_22_15"

            Hjemmel.FolketrygdlovenHjemmel.FOLKETRYGDLOVEN_22_17_A -> "FOLKETRYGDLOVEN_22_17_A"

            Hjemmel.ForeldelseslovenHjemmel.FORELDELSESLOVEN_10 -> "FORELDELSESLOVEN_10"

            Hjemmel.ForeldelseslovenHjemmel.FORELDELSESLOVEN_2_OG_3 -> "FORELDELSESLOVEN_2_OG_3"

            Hjemmel.ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_11 -> "FORVALTNINGSLOVEN_11"

            Hjemmel.ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_17 -> "FORVALTNINGSLOVEN_17"

            Hjemmel.ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_18_OG_19 -> "FORVALTNINGSLOVEN_18_OG_19"

            Hjemmel.ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_28 -> "FORVALTNINGSLOVEN_28"

            Hjemmel.ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_30 -> "FORVALTNINGSLOVEN_30"

            Hjemmel.ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_31 -> "FORVALTNINGSLOVEN_31"

            Hjemmel.ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_32 -> "FORVALTNINGSLOVEN_32"

            Hjemmel.ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_35 -> "FORVALTNINGSLOVEN_35"

            Hjemmel.ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_41 -> "FORVALTNINGSLOVEN_41"

            Hjemmel.ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_42 -> "FORVALTNINGSLOVEN_42"

            Hjemmel.TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_10 -> "TILTAKSPENGEFORSKRIFTEN_10"

            Hjemmel.TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_11 -> "TILTAKSPENGEFORSKRIFTEN_11"

            Hjemmel.TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_2 -> "TILTAKSPENGEFORSKRIFTEN_2"

            Hjemmel.TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_3 -> "TILTAKSPENGEFORSKRIFTEN_3"

            Hjemmel.TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_5 -> "TILTAKSPENGEFORSKRIFTEN_5"

            Hjemmel.TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_6 -> "TILTAKSPENGEFORSKRIFTEN_6"

            Hjemmel.TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_7 -> "TILTAKSPENGEFORSKRIFTEN_7"

            Hjemmel.TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_8 -> "TILTAKSPENGEFORSKRIFTEN_8"

            Hjemmel.TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_9 -> "TILTAKSPENGEFORSKRIFTEN_9"
        }
    }
}
