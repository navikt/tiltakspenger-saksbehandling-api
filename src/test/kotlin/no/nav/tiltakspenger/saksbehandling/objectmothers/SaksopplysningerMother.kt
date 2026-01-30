package no.nav.tiltakspenger.saksbehandling.objectmothers

import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltakelser
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.TiltakspengevedtakFraArena
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Ytelser
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

interface SaksopplysningerMother {
    fun saksopplysninger(
        fom: LocalDate = 1.januar(2023),
        tom: LocalDate = 31.mars(2023),
        fødselsdato: LocalDate = ObjectMother.fødselsdato(),
        tiltaksdeltakelse: List<Tiltaksdeltakelse> = listOf(ObjectMother.tiltaksdeltakelse(fom = fom, tom = tom)),
        oppslagsperiode: Periode = Periode(fom, tom),
        clock: Clock = ObjectMother.clock,
        oppslagstidspunkt: LocalDateTime = LocalDateTime.now(clock),
        ytelser: Ytelser = Ytelser.fromList(emptyList(), oppslagsperiode, oppslagstidspunkt),
        tiltakspengevedtakFraArena: TiltakspengevedtakFraArena = TiltakspengevedtakFraArena.fromList(
            emptyList(),
            oppslagsperiode,
            oppslagstidspunkt,
        ),
    ): Saksopplysninger {
        return Saksopplysninger(
            fødselsdato = fødselsdato,
            tiltaksdeltakelser = Tiltaksdeltakelser(tiltaksdeltakelse),
            ytelser = ytelser,
            tiltakspengevedtakFraArena = tiltakspengevedtakFraArena,
            oppslagstidspunkt = oppslagstidspunkt,
        )
    }
}
