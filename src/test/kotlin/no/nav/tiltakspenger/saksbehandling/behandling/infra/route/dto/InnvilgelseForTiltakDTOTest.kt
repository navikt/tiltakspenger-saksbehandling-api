package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import arrow.core.nonEmptyListOf
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.periode.toDTO
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.InnvilgelsesperiodeKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.InnvilgelsesperiodeVerdi
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.behandling.domene.reversertOpphør.ReversertOpphør
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltakelser
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseIntern
import org.junit.jupiter.api.Test

class InnvilgelseForTiltakDTOTest {

    private val januar = 1.januar(2025) til 31.januar(2025)
    private val februar = 1.februar(2025) til 28.februar(2025)
    private val mars = 1.mars(2025) til 31.mars(2025)
    private val april = 1.april(2025) til 30.april(2025)

    private val amo = ObjectMother.tiltaksdeltakelse(periode = 1.januar(2025) til 30.april(2025))
    private val arbeidstrening = ObjectMother.tiltaksdeltakelse(periode = 1.januar(2025) til 30.april(2025))
    private val tiltaksdeltakelser = Tiltaksdeltakelser(listOf(amo, arbeidstrening))

    private fun tiltak(
        tiltaksdeltakelse: TiltaksdeltakelseIntern,
        vararg perioder: Periode,
        erReversertOpphør: Boolean = false,
    ) = InnvilgelseForTiltakDTO(
        internDeltakelseId = tiltaksdeltakelse.internDeltakelseId.toString(),
        erReversertOpphør = erReversertOpphør,
        perioder = perioder.map { InnvilgelsesperiodeForTiltakDTO(periode = it.toDTO()) },
    )

    private fun request(
        vararg tiltak: InnvilgelseForTiltakDTO,
        antallDager: List<Pair<Periode, Int>>,
    ) = InnvilgelsesperioderDTO(
        tiltak = tiltak.toList(),
        antallDagerPerMeldeperiode = antallDager.map { (periode, dager) ->
            AntallDagerPerMeldeperiodeDTO(periode = periode.toDTO(), antallDagerPerMeldeperiode = dager)
        },
    )

    private fun kommando(
        periode: Periode,
        vararg tiltaksdeltakelser: TiltaksdeltakelseIntern,
        dager: Int,
        erReversertOpphør: Boolean = false,
    ) = InnvilgelsesperiodeKommando(
        periode = periode,
        internDeltakelseIder = nonEmptyListOf(tiltaksdeltakelser.first(), *tiltaksdeltakelser.drop(1).toTypedArray()).map { it.internDeltakelseId },
        antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(dager),
        erReversertOpphør = erReversertOpphør,
    )

    private fun innvilgelsesperiode(
        periode: Periode,
        vararg tiltaksdeltakelser: TiltaksdeltakelseIntern,
        dager: Int,
        reversertOpphør: ReversertOpphør? = null,
    ): PeriodeMedVerdi<InnvilgelsesperiodeVerdi> = ObjectMother.innvilgelsesperiode(
        periode = periode,
        tiltaksdeltakelser = nonEmptyListOf(tiltaksdeltakelser.first(), *tiltaksdeltakelser.drop(1).toTypedArray()),
        antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(dager),
        reversertOpphør = reversertOpphør,
    )

    /** Slik domenet blir når requesten lagres, se [no.nav.tiltakspenger.saksbehandling.behandling.domene.tilInnvilgelsesperiodeVerdi]. */
    private fun InnvilgelsesperioderDTO.tilDomene(): Innvilgelsesperioder = Innvilgelsesperioder(
        tilKommando().map { kommando, _ ->
            InnvilgelsesperiodeVerdi(
                tiltaksdeltakelser = kommando.internDeltakelseIder.map { tiltaksdeltakelser.getTiltaksdeltakelse(it)!! },
                antallDagerPerMeldeperiode = kommando.antallDagerPerMeldeperiode,
                reversertOpphør = null,
            )
        },
    )

    @Test
    fun `overlappende tiltak deles opp i delperioder med tiltaksdeltakelsene i rekkefølgen fra requesten og antall dager for delperioden`() {
        val kommando = request(
            tiltak(arbeidstrening, 1.februar(2025) til 30.april(2025)),
            tiltak(amo, 1.januar(2025) til 31.mars(2025)),
            antallDager = listOf(januar to 5, (1.februar(2025) til 31.mars(2025)) to 8, april to 3),
        ).tilKommando()

        kommando.verdier shouldBe listOf(
            kommando(januar, amo, dager = 5),
            kommando(1.februar(2025) til 31.mars(2025), arbeidstrening, amo, dager = 8),
            kommando(april, arbeidstrening, dager = 3),
        )
    }

    @Test
    fun `én periode for antall dager kan dekke flere delperioder, og tilstøtende delperioder slås ikke sammen`() {
        val kommando = request(
            tiltak(amo, januar, februar),
            tiltak(arbeidstrening),
            antallDager = listOf((1.januar(2025) til 28.februar(2025)) to 5),
        ).tilKommando()

        kommando.verdier shouldBe listOf(
            kommando(januar, amo, dager = 5),
            kommando(februar, amo, dager = 5),
        )
    }

    @Test
    fun `perioder uten tiltak blir hull`() {
        val kommando = request(
            tiltak(amo, januar),
            tiltak(arbeidstrening, april),
            antallDager = listOf(januar to 5, april to 3),
        ).tilKommando()

        kommando.verdier shouldBe listOf(
            kommando(januar, amo, dager = 5),
            kommando(april, arbeidstrening, dager = 3),
        )
    }

    @Test
    fun `reversert opphør tilstøtende vanlige perioder beholder flagget sitt`() {
        val kommando = request(
            tiltak(amo, januar),
            tiltak(amo, februar, erReversertOpphør = true),
            antallDager = listOf((1.januar(2025) til 28.februar(2025)) to 5),
        ).tilKommando()

        kommando.verdier shouldBe listOf(
            kommando(januar, amo, dager = 5),
            kommando(februar, amo, dager = 5, erReversertOpphør = true),
        )
    }

    @Test
    fun `kaster når periodene for samme tiltak overlapper`() {
        shouldThrow<IllegalArgumentException> {
            request(
                tiltak(amo, 1.januar(2025) til 28.februar(2025), februar),
                antallDager = listOf((1.januar(2025) til 28.februar(2025)) to 5),
            ).tilKommando()
        }
        shouldThrow<IllegalArgumentException> {
            request(
                tiltak(amo, januar),
                tiltak(amo, januar, erReversertOpphør = true),
                antallDager = listOf(januar to 5),
            ).tilKommando()
        }
    }

    @Test
    fun `kaster når reversert opphør overlapper perioder som ikke er reversert opphør`() {
        shouldThrow<IllegalArgumentException> {
            request(
                tiltak(amo, 1.januar(2025) til 28.februar(2025)),
                tiltak(arbeidstrening, februar, erReversertOpphør = true),
                antallDager = listOf((1.januar(2025) til 28.februar(2025)) to 5),
            ).tilKommando()
        }
    }

    @Test
    fun `kaster når det ikke finnes noen perioder`() {
        shouldThrow<IllegalArgumentException> { request(antallDager = emptyList()).tilKommando() }
        shouldThrow<IllegalArgumentException> { request(tiltak(amo), antallDager = listOf(januar to 5)).tilKommando() }
    }

    @Test
    fun `kaster når antall dager ikke passer med delperiodene`() {
        val tiltak = arrayOf(
            tiltak(amo, 1.januar(2025) til 31.mars(2025)),
            tiltak(arbeidstrening, 1.februar(2025) til 30.april(2025)),
        )

        // Perioden for antall dager deler delperioden februar til mars i to.
        shouldThrow<IllegalArgumentException> {
            request(*tiltak, antallDager = listOf((1.januar(2025) til 14.februar(2025)) to 5, (15.februar(2025) til 30.april(2025)) to 8)).tilKommando()
        }
        // Delperioden for april mangler antall dager.
        shouldThrow<IllegalArgumentException> {
            request(*tiltak, antallDager = listOf(januar to 5, (1.februar(2025) til 31.mars(2025)) to 8)).tilKommando()
        }
        // Periodene for antall dager overlapper.
        shouldThrow<IllegalArgumentException> {
            request(*tiltak, antallDager = listOf((1.januar(2025) til 30.april(2025)) to 5, januar to 8)).tilKommando()
        }
        // Perioden for antall dager dekker hullet mellom tiltakene.
        shouldThrow<IllegalArgumentException> {
            request(tiltak(amo, januar), tiltak(arbeidstrening, april), antallDager = listOf((1.januar(2025) til 30.april(2025)) to 5)).tilKommando()
        }
    }

    @Test
    fun `respons slår sammen perioder for tiltak, unntatt der de samme tiltakene har ulikt antall dager på hver side`() {
        val innvilgelsesperioder = ObjectMother.innvilgelsesperioder(
            innvilgelsesperiode(januar, amo, dager = 5),
            innvilgelsesperiode(februar, amo, arbeidstrening, dager = 5),
            innvilgelsesperiode(mars, arbeidstrening, amo, dager = 4),
        )

        innvilgelsesperioder.tilDTO(tiltaksdeltakelser) shouldBe request(
            tiltak(amo, 1.januar(2025) til 28.februar(2025), mars),
            tiltak(arbeidstrening, februar, mars),
            antallDager = listOf((1.januar(2025) til 28.februar(2025)) to 5, mars to 4),
        )
    }

    @Test
    fun `respons for ett tiltak med ulikt antall dager gir én periode per antall dager, og gir det samme domenet tilbake`() {
        val innvilgelsesperioder = ObjectMother.innvilgelsesperioder(
            innvilgelsesperiode(januar, amo, dager = 5),
            innvilgelsesperiode(februar, amo, dager = 8),
        )

        val respons = innvilgelsesperioder.tilDTO(tiltaksdeltakelser)

        respons shouldBe request(
            tiltak(amo, januar, februar),
            antallDager = listOf(januar to 5, februar to 8),
        )
        respons.tilDomene() shouldBe innvilgelsesperioder
    }

    @Test
    fun `respons slår sammen periodene for et tiltak når et annet tiltak overlapper deler av det, og gir det samme domenet tilbake`() {
        val innvilgelsesperioder = ObjectMother.innvilgelsesperioder(
            innvilgelsesperiode(januar, amo, dager = 5),
            innvilgelsesperiode(februar, amo, arbeidstrening, dager = 8),
        )

        val respons = innvilgelsesperioder.tilDTO(tiltaksdeltakelser)

        respons shouldBe request(
            tiltak(amo, 1.januar(2025) til 28.februar(2025)),
            tiltak(arbeidstrening, februar),
            antallDager = listOf(januar to 5, februar to 8),
        )
        respons.tilDomene() shouldBe innvilgelsesperioder
    }

    @Test
    fun `respons har egen innvilgelse for reversert opphør, uten validering mot saksopplysningene`() {
        val innvilgelsesperioder = ObjectMother.innvilgelsesperioder(
            innvilgelsesperiode(januar, amo, dager = 5),
            innvilgelsesperiode(februar, amo, dager = 5, reversertOpphør = ReversertOpphør(VedtakId.random())),
            innvilgelsesperiode(mars, amo, dager = 5, reversertOpphør = ReversertOpphør(VedtakId.random())),
        )

        innvilgelsesperioder.tilDTO(Tiltaksdeltakelser.empty()) shouldBe request(
            InnvilgelseForTiltakDTO(
                internDeltakelseId = amo.internDeltakelseId.toString(),
                perioder = listOf(
                    InnvilgelsesperiodeForTiltakDTO(
                        periode = januar.toDTO(),
                        ugyldigTiltaksdeltakelse = UgyldigTiltaksdeltakelseDTO(UgyldigTiltaksdeltakelseDTO.Type.FINNES_IKKE, null),
                    ),
                ),
            ),
            tiltak(amo, 1.februar(2025) til 31.mars(2025), erReversertOpphør = true),
            antallDager = listOf((1.januar(2025) til 31.mars(2025)) to 5),
        )
    }

    @Test
    fun `request til domene og tilbake til respons gir den samme requesten`() {
        val request = request(
            tiltak(arbeidstrening, februar, april),
            tiltak(amo, 1.januar(2025) til 31.mars(2025)),
            antallDager = listOf((1.januar(2025) til 28.februar(2025)) to 5, mars to 8, april to 2),
        )

        request.tilDomene().tilDTO(tiltaksdeltakelser) shouldBe request.copy(
            tiltak = request.tiltak.sortedBy { it.internDeltakelseId != amo.internDeltakelseId.toString() },
        )
    }
}
