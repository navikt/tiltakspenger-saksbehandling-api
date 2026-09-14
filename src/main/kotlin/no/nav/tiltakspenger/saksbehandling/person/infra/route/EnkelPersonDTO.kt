package no.nav.tiltakspenger.saksbehandling.person.infra.route

import no.nav.tiltakspenger.saksbehandling.person.BarnMedSkjerming
import no.nav.tiltakspenger.saksbehandling.person.EnkelPersonMedSkjerming

/**
 * @property fødselsdato Dato på ISO-format.
 * Er en streng og ikke en dato fordi den kan være sladdet.
 * @property dødsdato Dato på ISO-format.
 * Er en streng og ikke en dato fordi den kan være sladdet.
 */
data class EnkelPersonDTO(
    val fnr: String,
    val fødselsdato: String,
    val fornavn: String?,
    val mellomnavn: String?,
    val etternavn: String?,
    val fortrolig: Boolean,
    val strengtFortrolig: Boolean,
    val strengtFortroligUtland: Boolean,
    val skjermet: Boolean,
    val dødsdato: String?,
)

fun EnkelPersonMedSkjerming.toEnkelPersonDTO(): EnkelPersonDTO = EnkelPersonDTO(
    fnr = fnr.verdi,
    fødselsdato = fødselsdato.toString(),
    fornavn = fornavn,
    mellomnavn = mellomnavn,
    etternavn = etternavn,
    fortrolig = fortrolig,
    strengtFortrolig = strengtFortrolig,
    strengtFortroligUtland = strengtFortroligUtland,
    skjermet = skjermet,
    dødsdato = dødsdato?.toString(),
)

fun BarnMedSkjerming.toEnkelPersonDTO(): EnkelPersonDTO = EnkelPersonDTO(
    fnr = fnr.verdi,
    fødselsdato = fødselsdato.toString(),
    fornavn = fornavn,
    mellomnavn = mellomnavn,
    etternavn = etternavn,
    fortrolig = fortrolig,
    strengtFortrolig = strengtFortrolig,
    strengtFortroligUtland = strengtFortroligUtland,
    skjermet = skjermet,
    dødsdato = dødsdato?.toString(),
)
