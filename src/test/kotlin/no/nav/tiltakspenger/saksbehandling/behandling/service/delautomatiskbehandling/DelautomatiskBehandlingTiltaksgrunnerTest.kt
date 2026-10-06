package no.nav.tiltakspenger.saksbehandling.behandling.service.delautomatiskbehandling

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ManueltBehandlesGrunn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ManueltBehandlesGrunn.SAKSOPPLYSNING_FANT_IKKE_TILTAK
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ManueltBehandlesGrunn.SAKSOPPLYSNING_MANGLER_FULLSTENDIG_PERIODE
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ManueltBehandlesGrunn.SAKSOPPLYSNING_MINDRE_ENN_14_DAGER_MELLOM_TILTAK_OG_SOKNAD
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ManueltBehandlesGrunn.SAKSOPPLYSNING_OVERLAPPENDE_TILTAK
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ManueltBehandlesGrunn.SAKSOPPLYSNING_TILTAK_MANGLER_DELTAKELSESMENGDE
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ManueltBehandlesGrunn.SAKSOPPLYSNING_TILTAK_MANGLER_PERIODE
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ManueltBehandlesGrunn.SAKSOPPLYSNING_TILTAK_MER_ENN_FEM_DAGER_PER_UKE
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ManueltBehandlesGrunn.SAKSOPPLYSNING_ULIK_TILTAKSPERIODE
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadPåSakId
import no.nav.tiltakspenger.saksbehandling.søknad.domene.InnvilgbarSøknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * Grunnene som stammer fra søknadstiltaket slik det ser ut i registeret når behandlingen opprettes.
 * Søknaden fryser tiltaket slik bruker så det, mens saksopplysningene hentes på nytt; testene lar de to spriker.
 */
class DelautomatiskBehandlingTiltaksgrunnerTest {

    /** Uten periode på søknadstiltaket mangler også saksopplysningene en fullstendig periode. */
    @Test
    fun `søknadstiltaket finnes ikke lenger i registeret`() {
        withTestApplicationContext { tac ->
            val søknadstiltak = tac.tiltaksdeltakelse()
            val (sak, søknad) = opprettSakOgSøknad(tac, tiltaksdeltakelse = søknadstiltak)
            tac.oppdaterTiltaksdeltakelse(sak.fnr, null)

            behandleAutomatisk(tac, søknad) shouldBe listOf(SAKSOPPLYSNING_FANT_IKKE_TILTAK, SAKSOPPLYSNING_MANGLER_FULLSTENDIG_PERIODE)
        }
    }

    @Test
    fun `søknadstiltaket mangler sluttdato i registeret`() {
        withTestApplicationContext { tac ->
            val søknadstiltak = tac.tiltaksdeltakelse()
            val (sak, søknad) = opprettSakOgSøknad(tac, tiltaksdeltakelse = søknadstiltak)
            tac.oppdaterTiltaksdeltakelse(sak.fnr, søknadstiltak.copy(deltakelseTilOgMed = null))

            behandleAutomatisk(tac, søknad) shouldBe listOf(SAKSOPPLYSNING_TILTAK_MANGLER_PERIODE, SAKSOPPLYSNING_MANGLER_FULLSTENDIG_PERIODE)
        }
    }

    @Test
    fun `søknadstiltaket har fått en annen periode i registeret`() {
        withTestApplicationContext { tac ->
            val søknadstiltak = tac.tiltaksdeltakelse()
            val (sak, søknad) = opprettSakOgSøknad(tac, tiltaksdeltakelse = søknadstiltak)
            tac.oppdaterTiltaksdeltakelse(sak.fnr, søknadstiltak.copy(deltakelseTilOgMed = 28.februar(2023)))

            behandleAutomatisk(tac, søknad) shouldBe listOf(SAKSOPPLYSNING_ULIK_TILTAKSPERIODE)
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `søknadstiltaket mangler både dager per uke og prosent`(nullDager: Boolean) {
        withTestApplicationContext { tac ->
            val søknadstiltak = tac.tiltaksdeltakelse()
            val (sak, søknad) = opprettSakOgSøknad(tac, tiltaksdeltakelse = søknadstiltak)
            tac.oppdaterTiltaksdeltakelse(
                sak.fnr,
                søknadstiltak.copy(
                    antallDagerPerUke = if (nullDager) null else 0F,
                    deltakelseProsent = null,
                    deltidsprosentGjennomforing = null,
                ),
            )

            behandleAutomatisk(tac, søknad) shouldBe listOf(SAKSOPPLYSNING_TILTAK_MANGLER_DELTAKELSESMENGDE)
        }
    }

    @Test
    fun `deltidsprosenten på gjennomføringen regnes som deltakelsesmengde når deltakelsen mangler den`() {
        withTestApplicationContext { tac ->
            val søknadstiltak = tac.tiltaksdeltakelse()
            val (sak, søknad) = opprettSakOgSøknad(tac, tiltaksdeltakelse = søknadstiltak)
            tac.oppdaterTiltaksdeltakelse(
                sak.fnr,
                søknadstiltak.copy(antallDagerPerUke = null, deltakelseProsent = null, deltidsprosentGjennomforing = 100.0),
            )

            behandleAutomatisk(tac, søknad) shouldBe emptyList()
        }
    }

    @Test
    fun `søknadstiltaket har mer enn fem dager per uke`() {
        withTestApplicationContext { tac ->
            val søknadstiltak = tac.tiltaksdeltakelse()
            val (sak, søknad) = opprettSakOgSøknad(tac, tiltaksdeltakelse = søknadstiltak)
            tac.oppdaterTiltaksdeltakelse(sak.fnr, søknadstiltak.copy(antallDagerPerUke = 6F))

            behandleAutomatisk(tac, søknad) shouldBe listOf(SAKSOPPLYSNING_TILTAK_MER_ENN_FEM_DAGER_PER_UKE)
        }
    }

    @Test
    fun `annet søkt tiltak overlapper søknadsperioden`() {
        withTestApplicationContext { tac ->
            val søknadstiltak = tac.tiltaksdeltakelse(1.januar(2023) til 31.mars(2023))
            val (sak, søknad) = opprettSakOgSøknad(tac, tiltaksdeltakelse = søknadstiltak)
            opprettSøknadPåSakId(tac, sak.id, tiltaksdeltakelse = tac.tiltaksdeltakelse(1.mars(2023) til 30.april(2023)))

            behandleAutomatisk(tac, søknad) shouldBe listOf(SAKSOPPLYSNING_OVERLAPPENDE_TILTAK)
        }
    }

    @Test
    fun `annet søkt tiltak uten periode regnes som overlappende`() {
        withTestApplicationContext { tac ->
            val søknadstiltak = tac.tiltaksdeltakelse(1.januar(2023) til 31.mars(2023))
            val (sak, søknad) = opprettSakOgSøknad(tac, tiltaksdeltakelse = søknadstiltak)
            val annet = tac.tiltaksdeltakelse(1.mars(2023) til 30.april(2023))
            opprettSøknadPåSakId(tac, sak.id, tiltaksdeltakelse = annet)
            tac.oppdaterTiltaksdeltakelse(sak.fnr, annet.copy(deltakelseFraOgMed = null, deltakelseTilOgMed = null))

            behandleAutomatisk(tac, søknad) shouldBe listOf(SAKSOPPLYSNING_OVERLAPPENDE_TILTAK)
        }
    }

    /**
     * Saksopplysningene tar bare med tiltak det er søkt om som overlapper søknadstiltaket slik registeret ser det.
     * Et tiltak som starter innen 14 dager etter søknadsperioden kommer derfor bare med når registeret har forlenget søknadstiltaket.
     * Da slår også [ManueltBehandlesGrunn.SAKSOPPLYSNING_ULIK_TILTAKSPERIODE] inn, så 14-dagersregelen gir aldri utslag alene.
     */
    @Test
    fun `annet søkt tiltak starter mindre enn 14 dager etter søknadsperioden`() {
        withTestApplicationContext { tac ->
            val søknadstiltak = tac.tiltaksdeltakelse(1.januar(2023) til 31.mars(2023))
            val (sak, søknad) = opprettSakOgSøknad(tac, tiltaksdeltakelse = søknadstiltak)
            opprettSøknadPåSakId(tac, sak.id, tiltaksdeltakelse = tac.tiltaksdeltakelse(10.april(2023) til 30.april(2023)))
            tac.oppdaterTiltaksdeltakelse(sak.fnr, søknadstiltak.copy(deltakelseTilOgMed = 30.april(2023)))

            behandleAutomatisk(tac, søknad) shouldBe listOf(
                SAKSOPPLYSNING_ULIK_TILTAKSPERIODE,
                SAKSOPPLYSNING_MINDRE_ENN_14_DAGER_MELLOM_TILTAK_OG_SOKNAD,
            )
        }
    }

    /** Oppretter og kjører den automatiske behandlingen av [søknad], og returnerer grunnene til at den må behandles manuelt. */
    private suspend fun ApplicationTestBuilder.behandleAutomatisk(
        tac: TestApplicationContext,
        søknad: Søknad,
    ): List<ManueltBehandlesGrunn> {
        val behandling = tac.behandlingContext.startSøknadsbehandlingService
            .opprettAutomatiskSoknadsbehandling(søknad.shouldBeInstanceOf<InnvilgbarSøknad>(), CorrelationId.generate())
            .getOrFail()
        tac.behandlingContext.delautomatiskBehandlingService.behandleAutomatisk(behandling, CorrelationId.generate())
        val oppdatert = tac.behandlingContext.rammebehandlingRepo.hent(behandling.id) as Søknadsbehandling
        val grunner = oppdatert.manueltBehandlesGrunner
        oppdatert.status shouldBe if (grunner.isEmpty()) Rammebehandlingsstatus.KLAR_TIL_BESLUTNING else Rammebehandlingsstatus.KLAR_TIL_BEHANDLING
        return grunner
    }
}
