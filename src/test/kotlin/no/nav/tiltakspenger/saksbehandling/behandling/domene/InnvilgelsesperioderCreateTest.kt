package no.nav.tiltakspenger.saksbehandling.behandling.domene

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.juni
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.til
import no.nav.tiltakspenger.libs.periodisering.tilIkkeTomPeriodisering
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksopplysninger
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.tiltaksdeltakelse
import org.junit.jupiter.api.Test

// Denne kan slettes når Innvilgelsesperioder.create fjernes
// Brukes kun i en overgangsfase og til migrering
class InnvilgelsesperioderCreateTest {
    val innvilgelsesperiodeTotal = 1.januar(2025) til 30.juni(2025)

    @Test
    fun `Skal opprette en innvilgelsesperiode`() {
        val saksopplysninger = saksopplysninger(
            fom = innvilgelsesperiodeTotal.fraOgMed,
            tom = innvilgelsesperiodeTotal.tilOgMed,
            tiltaksdeltakelse = listOf(
                tiltaksdeltakelse(
                    eksternTiltaksdeltakelseId = "asdf",
                    fom = innvilgelsesperiodeTotal.fraOgMed,
                    tom = innvilgelsesperiodeTotal.tilOgMed,
                ),
            ),
        )

        val innvilgelsesperioder = Innvilgelsesperioder.create(
            saksopplysninger = saksopplysninger,
            innvilgelsesperiode = innvilgelsesperiodeTotal,
            antallDagerPerMeldeperiode = listOf(innvilgelsesperiodeTotal to AntallDagerForMeldeperiode(10)),
            tiltaksdeltakelser = listOf(innvilgelsesperiodeTotal to "asdf"),
        )

        innvilgelsesperioder shouldBe Innvilgelsesperioder(
            listOf(
                PeriodeMedVerdi(
                    verdi = Innvilgelsesperiode(
                        periode = innvilgelsesperiodeTotal,
                        valgtTiltaksdeltakelse = saksopplysninger.tiltaksdeltakelser.first(),
                        antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(10),
                    ),
                    periode = innvilgelsesperiodeTotal,
                ),
            ).tilIkkeTomPeriodisering(),
        )
    }

    @Test
    fun `Skal opprette to innvilgelsesperioder med ulikt antall dager per meldeperiode`() {
        val saksopplysninger = saksopplysninger(
            fom = innvilgelsesperiodeTotal.fraOgMed,
            tom = innvilgelsesperiodeTotal.tilOgMed,
            tiltaksdeltakelse = listOf(
                tiltaksdeltakelse(
                    eksternTiltaksdeltakelseId = "asdf",
                    fom = innvilgelsesperiodeTotal.fraOgMed,
                    tom = innvilgelsesperiodeTotal.tilOgMed,
                ),
            ),
        )

        val førstePeriode = innvilgelsesperiodeTotal.fraOgMed til 10.januar(2025)
        val andrePeriode = 11.januar(2025) til innvilgelsesperiodeTotal.tilOgMed

        val innvilgelsesperioder = Innvilgelsesperioder.create(
            saksopplysninger = saksopplysninger,
            innvilgelsesperiode = innvilgelsesperiodeTotal,
            antallDagerPerMeldeperiode = listOf(
                førstePeriode to AntallDagerForMeldeperiode(10),
                andrePeriode to AntallDagerForMeldeperiode(9),
            ),
            tiltaksdeltakelser = listOf(innvilgelsesperiodeTotal to "asdf"),
        )

        innvilgelsesperioder shouldBe Innvilgelsesperioder(
            listOf(
                PeriodeMedVerdi(
                    verdi = Innvilgelsesperiode(
                        periode = førstePeriode,
                        valgtTiltaksdeltakelse = saksopplysninger.tiltaksdeltakelser.first(),
                        antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(10),
                    ),
                    periode = førstePeriode,
                ),
                PeriodeMedVerdi(
                    verdi = Innvilgelsesperiode(
                        periode = andrePeriode,
                        valgtTiltaksdeltakelse = saksopplysninger.tiltaksdeltakelser.first(),
                        antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(9),
                    ),
                    periode = andrePeriode,
                ),
            ).tilIkkeTomPeriodisering(),
        )
    }

    @Test
    fun `Skal opprette to innvilgelsesperioder med ulike tiltaksdeltakelser`() {
        val førstePeriode = innvilgelsesperiodeTotal.fraOgMed til 10.januar(2025)
        val andrePeriode = 11.januar(2025) til innvilgelsesperiodeTotal.tilOgMed

        val saksopplysninger = saksopplysninger(
            fom = innvilgelsesperiodeTotal.fraOgMed,
            tom = innvilgelsesperiodeTotal.tilOgMed,
            tiltaksdeltakelse = listOf(
                tiltaksdeltakelse(
                    eksternTiltaksdeltakelseId = "asdf",
                    fom = førstePeriode.fraOgMed,
                    tom = førstePeriode.tilOgMed,
                ),
                tiltaksdeltakelse(
                    eksternTiltaksdeltakelseId = "qwer",
                    fom = andrePeriode.fraOgMed,
                    tom = andrePeriode.tilOgMed,
                ),
            ),
        )

        val innvilgelsesperioder = Innvilgelsesperioder.create(
            saksopplysninger = saksopplysninger,
            innvilgelsesperiode = innvilgelsesperiodeTotal,
            antallDagerPerMeldeperiode = listOf(
                innvilgelsesperiodeTotal to AntallDagerForMeldeperiode(10),
            ),
            tiltaksdeltakelser = listOf(førstePeriode to "asdf", andrePeriode to "qwer"),
        )

        innvilgelsesperioder shouldBe Innvilgelsesperioder(
            listOf(
                PeriodeMedVerdi(
                    verdi = Innvilgelsesperiode(
                        periode = førstePeriode,
                        valgtTiltaksdeltakelse = saksopplysninger.tiltaksdeltakelser[0],
                        antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(10),
                    ),
                    periode = førstePeriode,
                ),
                PeriodeMedVerdi(
                    verdi = Innvilgelsesperiode(
                        periode = andrePeriode,
                        valgtTiltaksdeltakelse = saksopplysninger.tiltaksdeltakelser[1],
                        antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(10),
                    ),
                    periode = andrePeriode,
                ),
            ).tilIkkeTomPeriodisering(),
        )
    }

    @Test
    fun `Skal opprette tre innvilgelsesperioder med ulike tiltaksdeltakelser og antall dager`() {
        val førsteTiltaksperiode = innvilgelsesperiodeTotal.fraOgMed til 10.januar(2025)
        val andreTiltaksPeriode = 11.januar(2025) til innvilgelsesperiodeTotal.tilOgMed

        val førsteAntallDagerPeriode = innvilgelsesperiodeTotal.fraOgMed til 11.januar(2025)
        val andreAntallDagerPeriode = 12.januar(2025) til innvilgelsesperiodeTotal.tilOgMed

        val saksopplysninger = saksopplysninger(
            fom = innvilgelsesperiodeTotal.fraOgMed,
            tom = innvilgelsesperiodeTotal.tilOgMed,
            tiltaksdeltakelse = listOf(
                tiltaksdeltakelse(
                    eksternTiltaksdeltakelseId = "asdf",
                    fom = førsteTiltaksperiode.fraOgMed,
                    tom = førsteTiltaksperiode.tilOgMed,
                ),
                tiltaksdeltakelse(
                    eksternTiltaksdeltakelseId = "qwer",
                    fom = andreTiltaksPeriode.fraOgMed,
                    tom = andreTiltaksPeriode.tilOgMed,
                ),
            ),
        )

        val innvilgelsesperioder = Innvilgelsesperioder.create(
            saksopplysninger = saksopplysninger,
            innvilgelsesperiode = innvilgelsesperiodeTotal,
            antallDagerPerMeldeperiode = listOf(
                førsteAntallDagerPeriode to AntallDagerForMeldeperiode(10),
                andreAntallDagerPeriode to AntallDagerForMeldeperiode(9),
            ),
            tiltaksdeltakelser = listOf(førsteTiltaksperiode to "asdf", andreTiltaksPeriode to "qwer"),
        )

        innvilgelsesperioder shouldBe Innvilgelsesperioder(
            listOf(
                Innvilgelsesperiode(
                    periode = innvilgelsesperiodeTotal.fraOgMed til 10.januar(2025),
                    valgtTiltaksdeltakelse = saksopplysninger.tiltaksdeltakelser[0],
                    antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(10),
                ).tilPeriodeMedVerdi(),

                Innvilgelsesperiode(
                    periode = 11.januar(2025) til 11.januar(2025),
                    valgtTiltaksdeltakelse = saksopplysninger.tiltaksdeltakelser[1],
                    antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(10),
                ).tilPeriodeMedVerdi(),

                Innvilgelsesperiode(
                    periode = 12.januar(2025) til innvilgelsesperiodeTotal.tilOgMed,
                    valgtTiltaksdeltakelse = saksopplysninger.tiltaksdeltakelser[1],
                    antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(9),
                ).tilPeriodeMedVerdi(),
            ).tilIkkeTomPeriodisering(),
        )
    }

    @Test
    fun `Skal opprette fem innvilgelsesperioder med ulike tiltaksdeltakelser og antall dager`() {
        val saksopplysninger = saksopplysninger(
            fom = innvilgelsesperiodeTotal.fraOgMed,
            tom = innvilgelsesperiodeTotal.tilOgMed,
            tiltaksdeltakelse = listOf(
                tiltaksdeltakelse(
                    eksternTiltaksdeltakelseId = "asdf",
                    fom = innvilgelsesperiodeTotal.fraOgMed,
                    tom = 31.mai(2025),
                ),
                tiltaksdeltakelse(
                    eksternTiltaksdeltakelseId = "qwer",
                    fom = 1.mars(2025),
                    tom = innvilgelsesperiodeTotal.tilOgMed,
                ),
            ),
        )

        val førsteTiltaksperiode = innvilgelsesperiodeTotal.fraOgMed til 30.april(2025) to "asdf"
        val andreTiltaksPeriode = 1.mai(2025) til innvilgelsesperiodeTotal.tilOgMed to "qwer"

        val førsteAntallDagerPeriode = innvilgelsesperiodeTotal.fraOgMed til 10.januar(2025) to AntallDagerForMeldeperiode(6)
        val andreAntallDagerPeriode = 11.januar(2025) til 20.januar(2025) to AntallDagerForMeldeperiode(10)
        val tredjeAntallDagerPeriode = 21.januar(2025) til 31.mai(2025) to AntallDagerForMeldeperiode(4)
        val fjerdeAntallDagerPeriode = 1.juni(2025) til innvilgelsesperiodeTotal.tilOgMed to AntallDagerForMeldeperiode(2)

        val innvilgelsesperioder = Innvilgelsesperioder.create(
            saksopplysninger = saksopplysninger,
            innvilgelsesperiode = innvilgelsesperiodeTotal,
            antallDagerPerMeldeperiode = listOf(
                førsteAntallDagerPeriode,
                andreAntallDagerPeriode,
                tredjeAntallDagerPeriode,
                fjerdeAntallDagerPeriode,
            ),
            tiltaksdeltakelser = listOf(
                førsteTiltaksperiode,
                andreTiltaksPeriode,
            ),
        )

        innvilgelsesperioder shouldBe Innvilgelsesperioder(
            listOf(
                Innvilgelsesperiode(
                    periode = innvilgelsesperiodeTotal.fraOgMed til 10.januar(2025),
                    valgtTiltaksdeltakelse = saksopplysninger.tiltaksdeltakelser[0],
                    antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(6),
                ).tilPeriodeMedVerdi(),

                Innvilgelsesperiode(
                    periode = 11.januar(2025) til 20.januar(2025),
                    valgtTiltaksdeltakelse = saksopplysninger.tiltaksdeltakelser[0],
                    antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(10),
                ).tilPeriodeMedVerdi(),

                Innvilgelsesperiode(
                    periode = 21.januar(2025) til 30.april(2025),
                    valgtTiltaksdeltakelse = saksopplysninger.tiltaksdeltakelser[0],
                    antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(4),
                ).tilPeriodeMedVerdi(),

                Innvilgelsesperiode(
                    periode = 1.mai(2025) til 31.mai(2025),
                    valgtTiltaksdeltakelse = saksopplysninger.tiltaksdeltakelser[1],
                    antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(4),
                ).tilPeriodeMedVerdi(),

                Innvilgelsesperiode(
                    periode = 1.juni(2025) til innvilgelsesperiodeTotal.tilOgMed,
                    valgtTiltaksdeltakelse = saksopplysninger.tiltaksdeltakelser[1],
                    antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(2),
                ).tilPeriodeMedVerdi(),
            ).tilIkkeTomPeriodisering(),
        )
    }

    @Test
    fun `Skal feile med ikke-sammenhengende antall dager`() {
        val førsteAntallDagerPeriode = innvilgelsesperiodeTotal.fraOgMed til 11.januar(2025)
        val andreAntallDagerPeriode = 13.januar(2025) til innvilgelsesperiodeTotal.tilOgMed

        val saksopplysninger = saksopplysninger(
            fom = innvilgelsesperiodeTotal.fraOgMed,
            tom = innvilgelsesperiodeTotal.tilOgMed,
            tiltaksdeltakelse = listOf(
                tiltaksdeltakelse(
                    eksternTiltaksdeltakelseId = "asdf",
                    fom = innvilgelsesperiodeTotal.fraOgMed,
                    tom = innvilgelsesperiodeTotal.tilOgMed,
                ),
            ),
        )

        shouldThrow<ClassCastException> {
            Innvilgelsesperioder.create(
                saksopplysninger = saksopplysninger,
                innvilgelsesperiode = innvilgelsesperiodeTotal,
                antallDagerPerMeldeperiode = listOf(
                    førsteAntallDagerPeriode to AntallDagerForMeldeperiode(10),
                    andreAntallDagerPeriode to AntallDagerForMeldeperiode(9),
                ),
                tiltaksdeltakelser = listOf(innvilgelsesperiodeTotal to "asdf"),
            )
        }
    }

    @Test
    fun `Skal feile med ikke-sammenhengende tiltaksdeltakelser`() {
        val førsteTiltaksperiode = innvilgelsesperiodeTotal.fraOgMed til 10.januar(2025)
        val andreTiltaksPeriode = 12.januar(2025) til innvilgelsesperiodeTotal.tilOgMed

        val saksopplysninger = saksopplysninger(
            fom = innvilgelsesperiodeTotal.fraOgMed,
            tom = innvilgelsesperiodeTotal.tilOgMed,
            tiltaksdeltakelse = listOf(
                tiltaksdeltakelse(
                    eksternTiltaksdeltakelseId = "asdf",
                    fom = førsteTiltaksperiode.fraOgMed,
                    tom = førsteTiltaksperiode.tilOgMed,
                ),
                tiltaksdeltakelse(
                    eksternTiltaksdeltakelseId = "qwer",
                    fom = andreTiltaksPeriode.fraOgMed,
                    tom = andreTiltaksPeriode.tilOgMed,
                ),
            ),
        )

        shouldThrow<ClassCastException> {
            Innvilgelsesperioder.create(
                saksopplysninger = saksopplysninger,
                innvilgelsesperiode = innvilgelsesperiodeTotal,
                antallDagerPerMeldeperiode = listOf(
                    innvilgelsesperiodeTotal to AntallDagerForMeldeperiode(10),
                ),
                tiltaksdeltakelser = listOf(førsteTiltaksperiode to "asdf", andreTiltaksPeriode to "qwer"),
            )
        }
    }
}
