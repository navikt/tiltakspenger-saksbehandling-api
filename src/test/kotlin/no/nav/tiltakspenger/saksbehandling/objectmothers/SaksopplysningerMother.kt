package no.nav.tiltakspenger.saksbehandling.objectmothers

import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.Tiltaksdeltagelse
import no.nav.tiltakspenger.saksbehandling.ytelser.domene.Ytelse
import java.time.LocalDate

interface SaksopplysningerMother {
    fun saksopplysninger(
        fom: LocalDate = 1.januar(2023),
        tom: LocalDate = 31.mars(2023),
        fødselsdato: LocalDate = ObjectMother.fødselsdato(),
        tiltaksdeltagelse: Tiltaksdeltagelse = ObjectMother.tiltaksdeltagelse(fom = fom, tom = tom),
        ytelser: List<Ytelse> = emptyList(),
    ): Saksopplysninger {
        return Saksopplysninger(
            fødselsdato = fødselsdato,
            tiltaksdeltagelse = listOf(tiltaksdeltagelse),
            periode = Periode(fom, tom),
            ytelser = ytelser,
        )
    }
}
