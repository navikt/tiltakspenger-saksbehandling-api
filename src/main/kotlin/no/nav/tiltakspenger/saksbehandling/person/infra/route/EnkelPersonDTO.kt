package no.nav.tiltakspenger.saksbehandling.person.infra.route

import no.nav.tiltakspenger.saksbehandling.person.BarnMedSkjerming
import no.nav.tiltakspenger.saksbehandling.person.EnkelPersonMedSkjerming
import java.time.LocalDate

data class EnkelPersonDTO(
    val fnr: String,
    val fødselsdato: LocalDate,
    val fornavn: String?,
    val mellomnavn: String?,
    val etternavn: String?,
    val fortrolig: Boolean,
    val strengtFortrolig: Boolean,
    val strengtFortroligUtland: Boolean,
    val skjermet: Boolean,
    val dødsdato: LocalDate?,
)

fun EnkelPersonMedSkjerming.toEnkelPersonDTO(): EnkelPersonDTO = EnkelPersonDTO(
    fnr = fnr.verdi,
    fødselsdato = fødselsdato,
    fornavn = fornavn,
    mellomnavn = mellomnavn,
    etternavn = etternavn,
    fortrolig = fortrolig,
    strengtFortrolig = strengtFortrolig,
    strengtFortroligUtland = strengtFortroligUtland,
    skjermet = skjermet,
    dødsdato = dødsdato,
)

fun BarnMedSkjerming.toEnkelPersonDTO(): EnkelPersonDTO = EnkelPersonDTO(
    fnr = fnr.verdi,
    fødselsdato = fødselsdato,
    fornavn = fornavn,
    mellomnavn = mellomnavn,
    etternavn = etternavn,
    fortrolig = fortrolig,
    strengtFortrolig = strengtFortrolig,
    strengtFortroligUtland = strengtFortroligUtland,
    skjermet = skjermet,
    dødsdato = dødsdato,
)
