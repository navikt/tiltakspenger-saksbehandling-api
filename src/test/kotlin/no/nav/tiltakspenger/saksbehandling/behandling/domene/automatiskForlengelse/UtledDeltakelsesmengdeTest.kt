package no.nav.tiltakspenger.saksbehandling.behandling.domene.automatiskForlengelse

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ManueltBehandlesGrunn
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.Deltakelsestilstand
import org.junit.jupiter.api.Test

class UtledDeltakelsesmengdeTest {

    private fun deltakelsesmengde(antallDagerPerUke: Float?, deltakelseProsent: Float?) = Deltakelsestilstand(
        deltakelseFraOgMed = null,
        deltakelseTilOgMed = null,
        deltakelseStatus = TiltakDeltakerstatus.Deltar,
        deltakelseProsent = deltakelseProsent,
        antallDagerPerUke = antallDagerPerUke,
    ).utledDeltakelsesmengde()

    private fun Either<ManueltBehandlesGrunn, UtledetDeltakelsesmengde>.skalGi(antallDagerPerMeldeperiode: Int, beskrivelse: String) {
        this shouldBe UtledetDeltakelsesmengde(AntallDagerForMeldeperiode(antallDagerPerMeldeperiode), beskrivelse).right()
    }

    private fun Either<ManueltBehandlesGrunn, UtledetDeltakelsesmengde>.skalOverlates(grunn: ManueltBehandlesGrunn) {
        this shouldBe grunn.left()
    }

    @Test
    fun `dager per uke brukes når det er satt, selv om prosenten sier noe annet`() {
        deltakelsesmengde(antallDagerPerUke = 3F, deltakelseProsent = 100F).skalGi(6, "3 dager per uke")
    }

    @Test
    fun `halve dager per uke gir et helt antall dager per meldeperiode`() {
        deltakelsesmengde(antallDagerPerUke = 2.5F, deltakelseProsent = null).skalGi(5, "2,5 dager per uke")
    }

    @Test
    fun `prosent brukes når dager per uke ikke er satt, der 100 prosent er 5 dager per uke`() {
        deltakelsesmengde(antallDagerPerUke = null, deltakelseProsent = 100F).skalGi(10, "100 prosent")
        deltakelsesmengde(antallDagerPerUke = null, deltakelseProsent = 80F).skalGi(8, "80 prosent")
        deltakelsesmengde(antallDagerPerUke = 0F, deltakelseProsent = 50F).skalGi(5, "50 prosent")
        deltakelsesmengde(antallDagerPerUke = null, deltakelseProsent = 10F).skalGi(1, "10 prosent")
    }

    @Test
    fun `prosent som ikke gir hele dager per meldeperiode - overlates til saksbehandler`() {
        deltakelsesmengde(antallDagerPerUke = null, deltakelseProsent = 33.3F)
            .skalOverlates(ManueltBehandlesGrunn.FORLENGELSE_DELTAKELSESMENGDE_GIR_IKKE_HELE_DAGER)
        deltakelsesmengde(antallDagerPerUke = null, deltakelseProsent = 25F)
            .skalOverlates(ManueltBehandlesGrunn.FORLENGELSE_DELTAKELSESMENGDE_GIR_IKKE_HELE_DAGER)
        deltakelsesmengde(antallDagerPerUke = null, deltakelseProsent = 2F)
            .skalOverlates(ManueltBehandlesGrunn.FORLENGELSE_DELTAKELSESMENGDE_GIR_IKKE_HELE_DAGER)
    }

    @Test
    fun `dager per uke som ikke gir hele dager per meldeperiode - overlates til saksbehandler`() {
        deltakelsesmengde(antallDagerPerUke = 2.25F, deltakelseProsent = 100F)
            .skalOverlates(ManueltBehandlesGrunn.FORLENGELSE_DELTAKELSESMENGDE_GIR_IKKE_HELE_DAGER)
    }

    @Test
    fun `kan ikke utledes uten dager per uke og prosent`() {
        deltakelsesmengde(antallDagerPerUke = null, deltakelseProsent = null)
            .skalOverlates(ManueltBehandlesGrunn.FORLENGELSE_KAN_IKKE_UTLEDE_DELTAKELSESMENGDE)
        deltakelsesmengde(antallDagerPerUke = 0F, deltakelseProsent = 0F)
            .skalOverlates(ManueltBehandlesGrunn.FORLENGELSE_KAN_IKKE_UTLEDE_DELTAKELSESMENGDE)
    }

    @Test
    fun `kan ikke utledes når deltakelsesmengden er mer enn fulltid`() {
        deltakelsesmengde(antallDagerPerUke = 5.5F, deltakelseProsent = null)
            .skalOverlates(ManueltBehandlesGrunn.FORLENGELSE_KAN_IKKE_UTLEDE_DELTAKELSESMENGDE)
        deltakelsesmengde(antallDagerPerUke = null, deltakelseProsent = 120F)
            .skalOverlates(ManueltBehandlesGrunn.FORLENGELSE_KAN_IKKE_UTLEDE_DELTAKELSESMENGDE)
    }
}
