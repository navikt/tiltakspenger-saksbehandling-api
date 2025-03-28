package no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.personopplysninger

import no.nav.tiltakspenger.libs.common.Fnr

data class EnkelPersonMedSkjerming(val enkelPerson: EnkelPerson, val erSkjermet: Boolean) {
    val fnr: Fnr = enkelPerson.fnr
    val fornavn: String = enkelPerson.fornavn
    val mellomnavn: String? = enkelPerson.mellomnavn
    val etternavn: String = enkelPerson.etternavn
    val fortrolig: Boolean = enkelPerson.fortrolig
    val strengtFortrolig: Boolean = enkelPerson.strengtFortrolig
    val strengtFortroligUtland = enkelPerson.strengtFortroligUtland
    val skjermet: Boolean = erSkjermet
}
