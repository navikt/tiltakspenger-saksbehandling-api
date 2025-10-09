package no.nav.tiltakspenger.saksbehandling.person

import no.nav.tiltakspenger.libs.common.Fnr
import java.time.LocalDate

data class BarnMedSkjerming(val barn: EnkelPerson, val erSkjermet: Boolean) {
    val harAdresseBeskyttelse = barn.fortrolig || barn.strengtFortrolig || barn.strengtFortroligUtland
    val fnr: Fnr = barn.fnr
    val fødselsdato: LocalDate = barn.fødselsdato
    val fornavn: String? = if (harAdresseBeskyttelse) null else barn.fornavn
    val mellomnavn: String? = if (harAdresseBeskyttelse) null else barn.mellomnavn
    val etternavn: String? = if (harAdresseBeskyttelse) null else barn.etternavn
    val fortrolig: Boolean = barn.fortrolig
    val strengtFortrolig: Boolean = barn.strengtFortrolig
    val strengtFortroligUtland = barn.strengtFortroligUtland
    val skjermet: Boolean = erSkjermet
    val dødsdato: LocalDate? = barn.dødsdato
}
