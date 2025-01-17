package no.nav.tiltakspenger.vedtak.clients.veilarboppfolging

import no.nav.tiltakspenger.felles.Navkontor

data class HentOppfolgingsenhetResponse(
    val data: ResponseData? = null,
    val errors: List<String> = emptyList(),
)

data class ResponseData(
    val oppfolgingsEnhet: OppfolgingsEnhetsInfo,
)

data class OppfolgingsEnhetsInfo(
    val enhet: EnhetDto?,
)

data class EnhetDto(
    val id: String,
    val navn: String,
) {
    fun toNavkontor(): Navkontor =
        Navkontor(
            kontornummer = id,
            kontornavn = navn,
        )
}
