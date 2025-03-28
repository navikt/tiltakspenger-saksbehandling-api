package no.nav.tiltakspenger.saksbehandling.person

data class Navn(
    val fornavn: String,
    val mellomnavn: String? = null,
    val etternavn: String,
) {
    val mellomnavnOgEtternavn: String by lazy { "${mellomnavn ?: ""} $etternavn".trim() }
}
