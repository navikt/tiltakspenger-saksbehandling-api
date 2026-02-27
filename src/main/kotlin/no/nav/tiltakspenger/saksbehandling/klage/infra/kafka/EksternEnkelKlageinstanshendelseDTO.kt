package no.nav.tiltakspenger.saksbehandling.klage.infra.kafka

import no.nav.tiltakspenger.libs.json.deserialize

/**
 * https://github.com/navikt/kabal-api/blob/main/src/main/kotlin/no/nav/klage/oppgave/domain/kafka/BehandlingEvent.kt
 * https://github.com/navikt/kabal-api/blob/main/docs/schema/behandling-events.json
 *
 * Konsumeres av [KlageinstansKlagehendelseConsumer].
 * Kun ment brukt i innlesingsfasen. Egen DTO per type når vi senere knytter hendelsen til en sak.
 */
data class EnkelKabalKlagehendelseDTO(
    /** Format: UUID. Beskrivelse: Unik id for eventen som sendte vedtaket fra Kabal. Kan brukes for å oppnå idempotens på mottakersiden. */
    val eventId: String,
    /** Beskrivelse: Ekstern id for klage. Skal stemme overens med id sendt inn. */
    val kildeReferanse: String,
    /** Kilden som sendte inn klagen/anken. Skal stemme overens med kilde sendt inn. Siden alle ytelsene som er koblet på Kabal havner her, vil kun TILTAKSPENGER være interessant for oss. */
    val kilde: String,
    /** Intern referanse fra kabal. Kan i fremtiden brukes for å hente data om vedtak fra Kabal (se Swagger doc) */
    val kabalReferanse: String,
    /**
     * Typen event som har skjedd. Matcher et av feltene i BehandlingDetaljer, typen her vil vise hvilket felt som ikke er null. Kan og vil utvides med flere verdier på sikt.
     * Aktuelle for Tiltakspenger: KLAGEBEHANDLING_AVSLUTTET, BEHANDLING_FEILREGISTRERT, OMGJOERINGSKRAVBEHANDLING_AVSLUTTET. Anke- og trygderettshendelser er ikke aktuelle for tiltakspenger.
     * */
    val type: String,

) {
    val aktuellKilde = "TILTAKSPENGER"
    val erAktuell: Boolean = kilde == aktuellKilde
}

fun String.tilEnkelKabalKlagehendelseDTO(): EnkelKabalKlagehendelseDTO {
    return deserialize<EnkelKabalKlagehendelseDTO>(this)
}
