@file:Suppress("LongParameterList")

package no.nav.tiltakspenger.saksbehandling.objectmothers

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.saksbehandling.felles.januar
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.personopplysninger.PersonopplysningerSøker
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
        fortrolig: Boolean = false,
        strengtFortrolig: Boolean = false,
        strengtFortroligUtland: Boolean = false,
        kommune: String? = null,
        bydel: String? = null,
    ): PersonopplysningerSøker =
        PersonopplysningerSøker(
            fnr = fnr,
            fødselsdato = fødselsdato,
            fornavn = fornavn,
            mellomnavn = mellomnavn,
            etternavn = etternavn,
            fortrolig = fortrolig,
            strengtFortrolig = strengtFortrolig,
            strengtFortroligUtland = strengtFortroligUtland,
        )

    fun personopplysningMaxFyr(
        fnr: Fnr = Fnr.random(),
        fødselsdato: LocalDate = fødselsdato(),
        fornavn: String = "Kjell",
        mellomnavn: String? = "T.",
        etternavn: String = "Ring",
        fortrolig: Boolean = false,
        strengtFortrolig: Boolean = true,
        strengtFortroligUtland: Boolean = false,
        kommune: String? = "Oslo",
        bydel: String? = "3440",
    ): PersonopplysningerSøker =
        PersonopplysningerSøker(
            fnr = fnr,
            fødselsdato = fødselsdato,
            fornavn = fornavn,
            mellomnavn = mellomnavn,
            etternavn = etternavn,
            fortrolig = fortrolig,
            strengtFortrolig = strengtFortrolig,
            strengtFortroligUtland = strengtFortroligUtland,
        )
}
