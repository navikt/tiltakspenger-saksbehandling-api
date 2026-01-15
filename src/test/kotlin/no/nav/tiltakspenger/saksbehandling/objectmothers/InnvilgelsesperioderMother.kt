package no.nav.tiltakspenger.saksbehandling.objectmothers

import arrow.core.toNonEmptyListOrThrow
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.tilIkkeTomPeriodisering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Innvilgelsesperioder.InnvilgelsesperiodeVerdi
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.vedtaksperiode
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse

interface InnvilgelsesperioderMother {

    fun innvilgelsesperiode(
        periode: Periode = vedtaksperiode(),
        valgtTiltaksdeltakelse: Tiltaksdeltakelse = tiltaksdeltakelse(fom = periode.fraOgMed, tom = periode.tilOgMed),
        antallDagerPerMeldeperiode: AntallDagerForMeldeperiode = AntallDagerForMeldeperiode.default,
    ): PeriodeMedVerdi<InnvilgelsesperiodeVerdi> {
        return PeriodeMedVerdi(
            verdi = InnvilgelsesperiodeVerdi(
                valgtTiltaksdeltakelse = valgtTiltaksdeltakelse,
                antallDagerPerMeldeperiode = antallDagerPerMeldeperiode,
            ),
            periode = periode,
        )
    }

    fun innvilgelsesperioder(
        perioder: List<PeriodeMedVerdi<InnvilgelsesperiodeVerdi>> = listOf(innvilgelsesperiode()),
    ): Innvilgelsesperioder {
        return Innvilgelsesperioder(
            perioder.toNonEmptyListOrThrow().tilIkkeTomPeriodisering(),
        )
    }

    fun innvilgelsesperioder(
        vararg perioder: PeriodeMedVerdi<InnvilgelsesperiodeVerdi>,
    ): Innvilgelsesperioder {
        return innvilgelsesperioder(perioder.toList())
    }

    fun innvilgelsesperioder(
        periode: Periode = vedtaksperiode(),
        valgtTiltaksdeltakelse: Tiltaksdeltakelse = tiltaksdeltakelse(fom = periode.fraOgMed, tom = periode.tilOgMed),
        antallDagerPerMeldeperiode: AntallDagerForMeldeperiode = AntallDagerForMeldeperiode.default,
    ): Innvilgelsesperioder {
        return Innvilgelsesperioder(
            listOf(
                innvilgelsesperiode(
                    periode,
                    valgtTiltaksdeltakelse,
                    antallDagerPerMeldeperiode,
                ),
            ).tilIkkeTomPeriodisering(),
        )
    }
}
