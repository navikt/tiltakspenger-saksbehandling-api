package no.nav.tiltakspenger.saksbehandling.behandling.domene

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.ValgteTiltaksdeltakelser
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class BehandlingResultatKtTest {
    @Nested
    inner class NullstilleResultatVedSaksopplysninger {
        val periode = Periode(1.januar(2023), 31.mars(2023))

        private val valgteTiltaksdeltakelser = ValgteTiltaksdeltakelser(
            SammenhengendePeriodisering(PeriodeMedVerdi(ObjectMother.tiltaksdeltakelse(), periode)),
        )
        private val matchendeSaksopplysning = ObjectMother.saksopplysninger(
            fom = periode.fraOgMed,
            tom = periode.tilOgMed,
        )
        private val endretSaksopplysning = ObjectMother.saksopplysninger(
            tiltaksdeltakelse = listOf(ObjectMother.tiltaksdeltakelse(eksternTiltaksdeltakelseId = "annen-id")),
        )

        @Test
        fun `skal nullstille resultet ved endring av saksopplysninger`() {
            val actual = skalNullstilleResultatVedNyeSaksopplysninger(
                valgteTiltaksdeltakelser,
                ObjectMother.saksopplysninger(
                    tiltaksdeltakelse = listOf(
                        ObjectMother.tiltaksdeltakelse(eksternTiltaksdeltakelseId = "annen-id"),
                        ObjectMother.tiltaksdeltakelse(eksternTiltaksdeltakelseId = "annen-id2"),
                    ),
                ),
            )

            actual shouldBe true
        }

        @Test
        fun `skal nullstille resultet ved endring av innhold for saksopplysningene`() {
            val actual = skalNullstilleResultatVedNyeSaksopplysninger(
                valgteTiltaksdeltakelser,
                endretSaksopplysning,
            )

            actual shouldBe true
        }

        @Test
        fun `skal ikke nullstille resultet ved endring av saksopplysninger`() {
            val actual = skalNullstilleResultatVedNyeSaksopplysninger(
                valgteTiltaksdeltakelser,
                matchendeSaksopplysning,
            )

            actual shouldBe false
        }
    }
}
