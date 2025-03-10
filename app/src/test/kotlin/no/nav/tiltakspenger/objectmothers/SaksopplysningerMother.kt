package no.nav.tiltakspenger.objectmothers

import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.tiltak.Tiltaksdeltagelse
import java.time.LocalDate

interface SaksopplysningerMother {
    fun saksopplysninger(
        fødselsdato: LocalDate = ObjectMother.fødselsdato(),
        tiltaksdeltagelse: Tiltaksdeltagelse = ObjectMother.tiltaksdeltagelse(),
    ): Saksopplysninger {
        return Saksopplysninger(
            fødselsdato = fødselsdato,
            tiltaksdeltagelse = listOf(tiltaksdeltagelse),
        )
    }
}
