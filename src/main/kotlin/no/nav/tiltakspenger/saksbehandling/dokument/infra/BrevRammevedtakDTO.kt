package no.nav.tiltakspenger.saksbehandling.dokument.infra

data class BrevPersonaliaDTO(
    val ident: String,
    val fornavn: String,
    val etternavn: String,
)

sealed interface BrevRammevedtakBaseDTO {
    val personalia: BrevPersonaliaDTO
    val saksnummer: String
    val saksbehandlerNavn: String
    val beslutterNavn: String?
    val datoForUtsending: String
    val tilleggstekst: String?
    val forhandsvisning: Boolean
    val kontor: String get() = "Nav Tiltakspenger"
}
