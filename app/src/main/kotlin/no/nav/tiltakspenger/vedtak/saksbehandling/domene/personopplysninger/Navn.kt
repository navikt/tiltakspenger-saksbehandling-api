package no.nav.tiltakspenger.vedtak.saksbehandling.domene.personopplysninger

data class Navn(
    val fornavn: String,
    val mellomnavn: String? = null,
    val etternavn: String,
) {
    val mellomnavnOgEtternavn: String by lazy { "${mellomnavn ?: ""} $etternavn".trim() }
}
