package no.nav.tiltakspenger.saksbehandling.behandling.domene

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class RammebehandlingsresultatKtTest {
    @Nested
    inner class NullstilleResultatVedSaksopplysninger {
        val periode = Periode(1.januar(2023), 31.mars(2023))

        private val valgteTiltaksdeltakelser = listOf(ObjectMother.tiltaksdeltakelse())

        private val matchendeSaksopplysning = ObjectMother.saksopplysninger(
            fom = periode.fraOgMed,
            tom = periode.tilOgMed,
        )
        private val annenInternDeltakelseId = TiltaksdeltakerId.random()
        private val annenInternDeltakelseId2 = TiltaksdeltakerId.random()
        private val endretSaksopplysning = ObjectMother.saksopplysninger(
            tiltaksdeltakelse = listOf(ObjectMother.tiltaksdeltakelse(eksternTiltaksdeltakelseId = "annen-id", internDeltakelseId = annenInternDeltakelseId)),
        )

        @Test
        fun `skal nullstille resultet ved endring av saksopplysninger`() {
            val actual = skalNullstilleResultatVedNyeSaksopplysninger(
                valgteTiltaksdeltakelser,
                ObjectMother.saksopplysninger(
                    tiltaksdeltakelse = listOf(
                        ObjectMother.tiltaksdeltakelse(eksternTiltaksdeltakelseId = "annen-id", internDeltakelseId = annenInternDeltakelseId),
                        ObjectMother.tiltaksdeltakelse(eksternTiltaksdeltakelseId = "annen-id2", internDeltakelseId = annenInternDeltakelseId2),
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
