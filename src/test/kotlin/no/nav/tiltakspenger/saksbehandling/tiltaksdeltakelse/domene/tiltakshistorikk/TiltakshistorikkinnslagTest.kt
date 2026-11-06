package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.tiltakshistorikk

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.personopplysning.Tilknytningstittel
import no.nav.tiltakspenger.libs.common.personopplysning.Virksomhetsnavn
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Arrangør
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.medPeriode
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.tilLibsDeltakelse
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TiltakshistorikkinnslagTest {

    private fun nåtilstand(tilOgMed: LocalDate? = LocalDate.of(2026, 3, 31)): Tiltaksdeltakelse.GirRett =
        ObjectMother.tiltaksdeltakelseTac(
            eksternTiltaksdeltakelseId = "ekstern-id",
            fom = LocalDate.of(2026, 1, 1),
            tom = LocalDate.of(2026, 3, 31),
        ).medPeriode(fraOgMed = LocalDate.of(2026, 1, 1), tilOgMed = tilOgMed).tilLibsDeltakelse()

    private fun Tiltaksdeltakelse.GirRett.medTittelOgArrangør(): Tiltaksdeltakelse.GirRett {
        val tittel = Tilknytningstittel("Kurs i sveising")
        val arrangør = Arrangør(hovedenhet = Virksomhetsnavn("Hovedenhet AS"), underenhet = Virksomhetsnavn("Underenhet AS"))
        return when (this) {
            is Tiltaksdeltakelse.GirRett.MedPeriode -> copy(tittel = tittel, arrangør = arrangør)
            is Tiltaksdeltakelse.GirRett.UtenPeriode -> copy(tittel = tittel, arrangør = arrangør)
        }
    }

    @Test
    fun `er endret når det ikke finnes noe forrige innslag`() {
        nåtilstand().erEndretFra(null) shouldBe true
    }

    @Test
    fun `er endret når et lagret felt er endret`() {
        nåtilstand(tilOgMed = LocalDate.of(2026, 4, 30)).erEndretFra(nåtilstand()) shouldBe true
    }

    @Test
    fun `tittel og arrangør regnes ikke som endringer`() {
        nåtilstand().medTittelOgArrangør().erEndretFra(nåtilstand()) shouldBe false
    }

    @Test
    fun `er ikke endret når verdien er lik, med og uten periode`() {
        nåtilstand().erEndretFra(nåtilstand()) shouldBe false
        nåtilstand(tilOgMed = null).medTittelOgArrangør().erEndretFra(nåtilstand(tilOgMed = null)) shouldBe false
    }
}
