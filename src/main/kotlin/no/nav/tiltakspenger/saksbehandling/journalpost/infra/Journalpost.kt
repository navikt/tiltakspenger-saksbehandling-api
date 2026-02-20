package no.nav.tiltakspenger.saksbehandling.journalpost.infra

data class Journalpost(
    val avsenderMottaker: AvsenderMottaker?,
    val datoOpprettet: String?,
    val bruker: Bruker?,
)

data class AvsenderMottaker(
    val id: String?,
    val type: String?,
)

data class Bruker(
    val id: String?,
    val type: String?,
)
