package no.nav.tiltakspenger.saksbehandling.person

import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import java.time.LocalDate

data class EnkelPersonMedSkjerming(val enkelPerson: EnkelPerson, val erSkjermet: Boolean) {
    val fnr: Fnr = enkelPerson.fnr
    val fødselsdato: LocalDate = enkelPerson.fødselsdato
    val fornavn: String = enkelPerson.fornavn
    val mellomnavn: String? = enkelPerson.mellomnavn
    val etternavn: String = enkelPerson.etternavn
    val adressebeskyttelse: Adressebeskyttelse = enkelPerson.adressebeskyttelse
    val skjermet: Boolean = erSkjermet
    val harAdressebeskyttelseEllerSkjerming: Boolean = adressebeskyttelse.erGradert || skjermet
    val dødsdato: LocalDate? = enkelPerson.dødsdato
}
