package no.nav.tiltakspenger.saksbehandling.objectmothers

import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltagelser
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.TiltakspengevedtakFraArena
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Ytelser
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.Tiltaksdeltagelse
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

interface SaksopplysningerMother {
    fun saksopplysninger(
        fom: LocalDate = 1.januar(2023),
        tom: LocalDate = 31.mars(2023),
        fødselsdato: LocalDate = ObjectMother.fødselsdato(),
        tiltaksdeltagelse: Tiltaksdeltagelse = ObjectMother.tiltaksdeltagelse(fom = fom, tom = tom),
        oppslagsperiode: Periode = tiltaksdeltagelse.periode!!,
        clock: Clock = ObjectMother.clock,
        oppslagstidspunkt: LocalDateTime = LocalDateTime.now(clock),
        ytelser: Ytelser = Ytelser.fromList(emptyList(), oppslagsperiode, oppslagstidspunkt),
        tiltakspengevedtakFraArena: TiltakspengevedtakFraArena = TiltakspengevedtakFraArena.fromList(emptyList(), oppslagsperiode, oppslagstidspunkt),
    ): Saksopplysninger {
        return Saksopplysninger(
            fødselsdato = fødselsdato,
            tiltaksdeltagelser = Tiltaksdeltagelser(listOf(tiltaksdeltagelse)),
            periode = Periode(fom, tom),
            ytelser = ytelser,
            tiltakspengevedtakFraArena = tiltakspengevedtakFraArena,
        )
    }
}
