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
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.InnvilgelsesperiodeKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.InnvilgelsesperiodeVerdi
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.behandling.domene.InnvilgetTiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.behandling.domene.InnvilgetTiltaksdeltakelseKommando
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

    private fun tiltak(
        tiltaksdeltakelse: TiltaksdeltakelseIntern,
        vararg perioder: Pair<Periode, Int>,
        erReversertOpphør: Boolean = false,
    ) = InnvilgelseForTiltakDTO(
        internDeltakelseId = tiltaksdeltakelse.internDeltakelseId.toString(),
        erReversertOpphør = erReversertOpphør,
        perioder = perioder.map { (periode, dager) ->
            InnvilgelsesperiodeForTiltakDTO(periode = periode.toDTO(), antallDagerPerMeldeperiode = dager)
        },
    )

    private fun kommando(tiltaksdeltakelse: TiltaksdeltakelseIntern, dager: Int) = InnvilgetTiltaksdeltakelseKommando(
        internDeltakelseId = tiltaksdeltakelse.internDeltakelseId,
        antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(dager),
    )

    private fun innvilget(tiltaksdeltakelse: TiltaksdeltakelseIntern, dager: Int) = InnvilgetTiltaksdeltakelse(
        tiltaksdeltakelse = tiltaksdeltakelse,
        antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(dager),
    )

    @Test
    fun `overlappende tiltak deles opp i delperioder med tiltaksdeltakelsene i rekkefølgen fra requesten`() {
        val kommando = listOf(
            tiltak(arbeidstrening, (1.februar(2025) til 30.april(2025)) to 3),
            tiltak(amo, (1.januar(2025) til 31.mars(2025)) to 5),
        ).tilKommando()

        kommando.verdier shouldBe listOf(
            InnvilgelsesperiodeKommando(januar, nonEmptyListOf(kommando(amo, 5))),
            InnvilgelsesperiodeKommando(1.februar(2025) til 31.mars(2025), nonEmptyListOf(kommando(arbeidstrening, 3), kommando(amo, 5))),
            InnvilgelsesperiodeKommando(april, nonEmptyListOf(kommando(arbeidstrening, 3))),
        )
    }

    @Test
    fun `tilstøtende delperioder med like tiltaksdeltakelser slås sammen, og tiltak uten perioder ignoreres`() {
        val kommando = listOf(
            tiltak(amo, januar to 5, februar to 5, mars to 4),
            tiltak(arbeidstrening),
        ).tilKommando()

        kommando.verdier shouldBe listOf(
            InnvilgelsesperiodeKommando(1.januar(2025) til 28.februar(2025), nonEmptyListOf(kommando(amo, 5))),
            InnvilgelsesperiodeKommando(mars, nonEmptyListOf(kommando(amo, 4))),
        )
    }

    @Test
    fun `perioder uten tiltak blir hull`() {
        val kommando = listOf(
            tiltak(amo, januar to 5),
            tiltak(arbeidstrening, april to 3),
        ).tilKommando()

        kommando.verdier shouldBe listOf(
            InnvilgelsesperiodeKommando(januar, nonEmptyListOf(kommando(amo, 5))),
            InnvilgelsesperiodeKommando(april, nonEmptyListOf(kommando(arbeidstrening, 3))),
        )
    }

    @Test
    fun `reversert opphør tilstøtende vanlige perioder beholder flagget sitt`() {
        val kommando = listOf(
            tiltak(amo, januar to 5),
            tiltak(amo, februar to 5, erReversertOpphør = true),
        ).tilKommando()

        kommando.verdier shouldBe listOf(
            InnvilgelsesperiodeKommando(januar, nonEmptyListOf(kommando(amo, 5))),
            InnvilgelsesperiodeKommando(februar, nonEmptyListOf(kommando(amo, 5)), erReversertOpphør = true),
        )
    }

    @Test
    fun `kaster når periodene for samme tiltak overlapper`() {
        shouldThrow<IllegalArgumentException> {
            listOf(tiltak(amo, (1.januar(2025) til 28.februar(2025)) to 5, februar to 3)).tilKommando()
        }
        shouldThrow<IllegalArgumentException> {
            listOf(
                tiltak(amo, januar to 5),
                tiltak(amo, januar to 5, erReversertOpphør = true),
            ).tilKommando()
        }
    }

    @Test
    fun `kaster når reversert opphør overlapper perioder som ikke er reversert opphør`() {
        shouldThrow<IllegalArgumentException> {
            listOf(
                tiltak(amo, (1.januar(2025) til 28.februar(2025)) to 5),
                tiltak(arbeidstrening, februar to 3, erReversertOpphør = true),
            ).tilKommando()
        }
    }

    @Test
    fun `kaster når det ikke finnes noen perioder`() {
        shouldThrow<IllegalArgumentException> { emptyList<InnvilgelseForTiltakDTO>().tilKommando() }
        shouldThrow<IllegalArgumentException> { listOf(tiltak(amo)).tilKommando() }
    }

    @Test
    fun `respons grupperes per tiltak med eget antall dager, og tilstøtende perioder med likt antall dager slås sammen`() {
        val innvilgelsesperioder = ObjectMother.innvilgelsesperioder(
            ObjectMother.innvilgelsesperiode(januar, nonEmptyListOf(innvilget(amo, 5))),
            ObjectMother.innvilgelsesperiode(februar, nonEmptyListOf(innvilget(amo, 5), innvilget(arbeidstrening, 3))),
            ObjectMother.innvilgelsesperiode(mars, nonEmptyListOf(innvilget(arbeidstrening, 3), innvilget(amo, 4))),
        )

        innvilgelsesperioder.tilDTO(Tiltaksdeltakelser(listOf(amo, arbeidstrening))) shouldBe listOf(
            tiltak(amo, (1.januar(2025) til 28.februar(2025)) to 5, mars to 4),
            tiltak(arbeidstrening, (1.februar(2025) til 31.mars(2025)) to 3),
        )
    }

    @Test
    fun `respons har egen innvilgelse for reversert opphør, uten validering mot saksopplysningene`() {
        val innvilgelsesperioder = ObjectMother.innvilgelsesperioder(
            ObjectMother.innvilgelsesperiode(januar, nonEmptyListOf(innvilget(amo, 5))),
            ObjectMother.innvilgelsesperiode(februar, nonEmptyListOf(innvilget(amo, 5)), ReversertOpphør(VedtakId.random())),
            ObjectMother.innvilgelsesperiode(mars, nonEmptyListOf(innvilget(amo, 5)), ReversertOpphør(VedtakId.random())),
        )

        innvilgelsesperioder.tilDTO(Tiltaksdeltakelser.empty()) shouldBe listOf(
            InnvilgelseForTiltakDTO(
                internDeltakelseId = amo.internDeltakelseId.toString(),
                perioder = listOf(
                    InnvilgelsesperiodeForTiltakDTO(
                        periode = januar.toDTO(),
                        antallDagerPerMeldeperiode = 5,
                        ugyldigTiltaksdeltakelse = UgyldigTiltaksdeltakelseDTO(UgyldigTiltaksdeltakelseDTO.Type.FINNES_IKKE, null),
                    ),
                ),
            ),
            tiltak(amo, (1.februar(2025) til 31.mars(2025)) to 5, erReversertOpphør = true),
        )
    }

    @Test
    fun `request til domene og tilbake til respons gir de samme innvilgelsene`() {
        val request = listOf(
            tiltak(arbeidstrening, februar to 3, april to 2),
            tiltak(amo, (1.januar(2025) til 31.mars(2025)) to 5),
        )
        val tiltaksdeltakelser = Tiltaksdeltakelser(listOf(amo, arbeidstrening))

        val innvilgelsesperioder = Innvilgelsesperioder(
            request.tilKommando().map { kommando, _ ->
                InnvilgelsesperiodeVerdi(
                    tiltaksdeltakelser = kommando.tiltaksdeltakelser.map {
                        innvilget(tiltaksdeltakelser.getTiltaksdeltakelse(it.internDeltakelseId)!!, it.antallDagerPerMeldeperiode.value)
                    },
                    reversertOpphør = null,
                )
            },
        )

        innvilgelsesperioder.tilDTO(tiltaksdeltakelser).sortedBy { it.internDeltakelseId } shouldBe request.sortedBy { it.internDeltakelseId }
    }
}
