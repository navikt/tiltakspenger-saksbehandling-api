package no.nav.tiltakspenger.saksbehandling.objectmothers

import no.nav.tiltakspenger.saksbehandling.felles.januar
import no.nav.tiltakspenger.saksbehandling.felles.mars
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.tiltak.Tiltaksdeltagelse
import java.time.LocalDate

interface SaksopplysningerMother {
    fun saksopplysninger(
        fom: LocalDate = 1.januar(2023),
        tom: LocalDate = 31.mars(2023),
        fødselsdato: LocalDate = ObjectMother.fødselsdato(),
        tiltaksdeltagelse: Tiltaksdeltagelse = ObjectMother.tiltaksdeltagelse(fom = fom, tom = tom),
    ): Saksopplysninger {
        return Saksopplysninger(
            fødselsdato = fødselsdato,
            tiltaksdeltagelse = listOf(tiltaksdeltagelse),
        )
    }
}
