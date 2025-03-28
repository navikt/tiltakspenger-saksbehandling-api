package no.nav.tiltakspenger.saksbehandling.person.infra.route

import no.nav.tiltakspenger.saksbehandling.person.EnkelPersonMedSkjerming

data class EnkelPersonDTO(
    val fnr: String,
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val fortrolig: Boolean,
    val strengtFortrolig: Boolean,
    val strengtFortroligUtland: Boolean,
    val skjermet: Boolean,
)

fun EnkelPersonMedSkjerming.toEnkelPersonDTO(): EnkelPersonDTO = EnkelPersonDTO(
    fnr = fnr.verdi,
    fornavn = fornavn,
    mellomnavn = mellomnavn,
    etternavn = etternavn,
    fortrolig = fortrolig,
    strengtFortrolig = strengtFortrolig,
    strengtFortroligUtland = strengtFortroligUtland,
    skjermet = skjermet,
)
