package no.nav.tiltakspenger.saksbehandling.routes.behandling.personopplysninger

import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.personopplysninger.EnkelPersonMedSkjerming

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

fun EnkelPersonMedSkjerming.toDTO(): EnkelPersonDTO = EnkelPersonDTO(
    fnr = fnr.verdi,
    fornavn = fornavn,
    mellomnavn = mellomnavn,
    etternavn = etternavn,
    fortrolig = fortrolig,
    strengtFortrolig = strengtFortrolig,
    strengtFortroligUtland = strengtFortroligUtland,
    skjermet = skjermet,
)
