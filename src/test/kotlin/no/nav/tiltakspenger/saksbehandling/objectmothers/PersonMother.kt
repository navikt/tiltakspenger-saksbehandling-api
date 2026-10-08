@file:Suppress("LongParameterList")

package no.nav.tiltakspenger.saksbehandling.objectmothers

import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.person.Adressebeskyttelse
import no.nav.tiltakspenger.saksbehandling.person.EnkelPerson
import java.time.LocalDate

interface PersonMother {
    /** Felles default fødselsdato for testdatatypene */
    fun fødselsdato(): LocalDate = 1.januar(2001)

    fun personopplysningKjedeligFyr(
        fnr: Fnr = Fnr.random(),
        fødselsdato: LocalDate = fødselsdato(),
        fornavn: String = "Fornavn",
        mellomnavn: String? = null,
        etternavn: String = "Etternavn",
        adressebeskyttelse: Adressebeskyttelse = Adressebeskyttelse.UGRADERT,
        kommune: String? = null,
        bydel: String? = null,
    ): EnkelPerson =
        EnkelPerson(
            fnr = fnr,
            fødselsdato = fødselsdato,
            fornavn = fornavn,
            mellomnavn = mellomnavn,
            etternavn = etternavn,
            adressebeskyttelse = adressebeskyttelse,
            dødsdato = null,
        )

    fun personopplysningMaxFyr(
        fnr: Fnr = Fnr.random(),
        fødselsdato: LocalDate = fødselsdato(),
        fornavn: String = "Kjell",
        mellomnavn: String? = "T.",
        etternavn: String = "Ring",
        adressebeskyttelse: Adressebeskyttelse = Adressebeskyttelse.STRENGT_FORTROLIG,
        kommune: String? = "Oslo",
        bydel: String? = "3440",
    ): EnkelPerson =
        EnkelPerson(
            fnr = fnr,
            fødselsdato = fødselsdato,
            fornavn = fornavn,
            mellomnavn = mellomnavn,
            etternavn = etternavn,
            adressebeskyttelse = adressebeskyttelse,
            dødsdato = null,
        )
}
