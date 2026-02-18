package no.nav.tiltakspenger.saksbehandling.klage.infra.http

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Hjemmel.ArbeidsmarkedslovenHjemmel
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Hjemmel.FolketrygdlovenHjemmel
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Hjemmel.ForeldelseslovenHjemmel
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Hjemmel.ForvaltningslovenHjemmel
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Hjemmel.TiltakspengeforskriftenHjemmel
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

/**
 * https://github.com/navikt/klage-kodeverk/blob/077caf7d5402898e0bead6b723ce4f9443adc85b/src/main/kotlin/no/nav/klage/kodeverk/hjemmel/YtelseToHjemler.kt#L1320
 */
fun Klagehjemler.tilHjemlerDto(): List<String> {
    return this.map {
        when (it) {
            ArbeidsmarkedslovenHjemmel.ARBEIDSMARKEDSLOVEN_2 -> "ARBML_2"
            ArbeidsmarkedslovenHjemmel.ARBEIDSMARKEDSLOVEN_13 -> "ARBML_13"
            ArbeidsmarkedslovenHjemmel.ARBEIDSMARKEDSLOVEN_13_LØNN -> "ARBML_13_LOENN"
            ArbeidsmarkedslovenHjemmel.ARBEIDSMARKEDSLOVEN_13_L4 -> "ARBML_13_4"
            ArbeidsmarkedslovenHjemmel.ARBEIDSMARKEDSLOVEN_15 -> "ARBML_15"
            ArbeidsmarkedslovenHjemmel.ARBEIDSMARKEDSLOVEN_17 -> "ARBML_17"
            ArbeidsmarkedslovenHjemmel.ARBEIDSMARKEDSLOVEN_22 -> "ARBML_22"
            FolketrygdlovenHjemmel.FOLKETRYGDLOVEN_22_15 -> "FTRL_22_15"
            FolketrygdlovenHjemmel.FOLKETRYGDLOVEN_22_17_A -> "FTRL_22_17A"
            ForeldelseslovenHjemmel.FORELDELSESLOVEN_10 -> "FORELDELSESLOVEN_10"
            ForeldelseslovenHjemmel.FORELDELSESLOVEN_2_OG_3 -> "FORELDELSESLOVEN_2_OG_3"
            ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_11 -> "FVL_11"
            ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_17 -> "FVL_17"
            ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_18_OG_19 -> "FVL_18_OG_19"
            ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_28 -> "FVL_28"
            ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_30 -> "FVL_30"
            ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_31 -> "FVL_31"
            ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_32 -> "FVL_32"
            ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_35 -> "FVL_35"
            ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_41 -> "FVL_41"
            ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_42 -> "FVL_42"
            TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_2 -> "FS_TIP_2"
            TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_3 -> "FS_TIP_3"
            TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_5 -> "FS_TIP_5"
            TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_6 -> "FS_TIP_6"
            TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_7 -> "FS_TIP_7"
            TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_8 -> "FS_TIP_8"
            TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_9 -> "FS_TIP_9"
            TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_10 -> "FS_TIP_10"
            TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_11 -> "FS_TIP_11"
        }
    }
}
