package no.nav.tiltakspenger.saksbehandling.klage.infra.http

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonValue
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import java.time.LocalDate

fun Klagebehandling.toOversendelsesDto(
    journalpostIdVedtak: JournalpostId,
): KlageOversendelseDto {
    return KlageOversendelseDto(
        sakenGjelder = SakenGjelder(
            id = SakenGjelderId(verdi = this.fnr.verdi),
        ),
        fagsak = OversendelsesFagsak(
            fagsakId = this.saksnummer.verdi,
        ),
        kildeReferanse = this.id.toString(),
        dvhReferanse = this.id.toString(),
        // TODO - må legge inn støtte for hjemler i klagebehandlingen
        hjemler = emptyList(),
        tilknyttedeJournalposter = listOf(
            TilknyttetJournalpost(
                type = TilknyttetJournalpostType.BRUKERS_KLAGE,
                journalpostId = this.journalpostId.toString(),
            ),
            TilknyttetJournalpost(
                type = TilknyttetJournalpostType.OPPRINNELIG_VEDTAK,
                journalpostId = journalpostIdVedtak.toString(),
            ),
        ),
        // vi har kun data for journalposten, ikke for når klagen ble mottatt i vedtaksinstansen
        brukersKlageMottattVedtaksinstans = this.journalpostOpprettet.toLocalDate(),
        hindreAutomatiskSvarbrev = null,
    )
}

data class KlageOversendelseDto(
    val sakenGjelder: SakenGjelder,
    val fagsak: OversendelsesFagsak,
    val kildeReferanse: String,
    val dvhReferanse: String?,
    val hjemler: List<OversendelsesHjemmel>,
    val tilknyttedeJournalposter: List<TilknyttetJournalpost>,
    val brukersKlageMottattVedtaksinstans: LocalDate?,
    val hindreAutomatiskSvarbrev: Boolean?,
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
    /** Enum. Gyldige verdier er [PERSON,VIRKSOMHET].
     * TODO - dersom vi skal sette 'klager' på DTO'en, så må type inn i konstruktøren
     */
    @JsonInclude
    val type: String = "PERSON"
}

data class OversendelsesFagsak(
    val fagsakId: String,
) {
    @JsonInclude
    val fagsystem: String = "TILTAKSPENGER"
}

enum class OversendelsesHjemmel(@JsonValue private val verdi: String)

data class TilknyttetJournalpost(
    val type: TilknyttetJournalpostType,
    val journalpostId: String,
)

enum class TilknyttetJournalpostType {
    BRUKERS_SOEKNAD,
    OPPRINNELIG_VEDTAK,
    BRUKERS_KLAGE,
    BRUKERS_ANKE,
    BRUKERS_OMGJOERINGSKRAV,
    BRUKERS_BEGJAERING_OM_GJENOPPTAK,
    OVERSENDELSESBREV,
    KLAGE_VEDTAK,
    ANNET,
}
