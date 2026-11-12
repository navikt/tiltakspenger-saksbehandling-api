package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import arrow.core.nonEmptyListOf
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.InnvilgelsesperiodeKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.InnvilgetTiltaksdeltakelseKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdater.KanIkkeOppdatereBehandling
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterSaksopplysningerForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingKlarTilBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import org.junit.jupiter.api.Test

/** API-et støtter foreløpig bare én tiltaksdeltakelse per innvilgelsesperiode, så flere tiltaksdeltakelser testes mot servicen. */
class OppdaterRammebehandlingServiceTest {

    @Test
    fun `kan innvilge flere overlappende tiltaksdeltakelser i samme innvilgelsesperiode`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)
            // En ny søknad for et annet, overlappende tiltak gjør at søknadsbehandlingen får med begge tiltaksdeltakelsene.
            opprettSøknadsbehandlingKlarTilBehandling(tac = tac, sakId = sak.id, fnr = sak.fnr)
            val (_, medToDeltakelser) = oppdaterSaksopplysningerForBehandlingId(tac, sak.id, behandling.id)
            val (første, andre) = medToDeltakelser.saksopplysninger.tiltaksdeltakelser.value.also { it.size shouldBe 2 }
            val periode = første.lukketPeriode!!

            val (_, oppdatert) = tac.behandlingContext.oppdaterRammebehandlingService.oppdater(
                ObjectMother.oppdaterSøknadsbehandlingInnvilgelseKommando(
                    sakId = sak.id,
                    behandlingId = behandling.id,
                    innvilgelsesperioder = listOf(
                        InnvilgelsesperiodeKommando(
                            periode = periode,
                            tiltaksdeltakelser = nonEmptyListOf(
                                InnvilgetTiltaksdeltakelseKommando(andre.internDeltakelseId, AntallDagerForMeldeperiode(6)),
                                InnvilgetTiltaksdeltakelseKommando(første.internDeltakelseId, AntallDagerForMeldeperiode(4)),
                            ),
                        ),
                    ),
                    barnetillegg = Barnetillegg.utenBarnetillegg(nonEmptyListOf(periode)),
                ),
            ).getOrFail()

            val innvilgelsesperiode = oppdatert.innvilgelsesperioder!!.periodisering.verdier.single()
            innvilgelsesperiode.valgteTiltaksdeltakelser shouldBe nonEmptyListOf(andre, første)
            innvilgelsesperiode.antallDagerPerMeldeperiode shouldBe AntallDagerForMeldeperiode(10)
        }
    }

    @Test
    fun `kan ikke innvilge for flere dager summert enn det er dager i en meldeperiode`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)
            opprettSøknadsbehandlingKlarTilBehandling(tac = tac, sakId = sak.id, fnr = sak.fnr)
            val (_, medToDeltakelser) = oppdaterSaksopplysningerForBehandlingId(tac, sak.id, behandling.id)
            val (første, andre) = medToDeltakelser.saksopplysninger.tiltaksdeltakelser.value.also { it.size shouldBe 2 }
            val periode = første.lukketPeriode!!

            tac.behandlingContext.oppdaterRammebehandlingService.oppdater(
                ObjectMother.oppdaterSøknadsbehandlingInnvilgelseKommando(
                    sakId = sak.id,
                    behandlingId = behandling.id,
                    innvilgelsesperioder = listOf(
                        InnvilgelsesperiodeKommando(
                            periode = periode,
                            tiltaksdeltakelser = nonEmptyListOf(
                                InnvilgetTiltaksdeltakelseKommando(første.internDeltakelseId, AntallDagerForMeldeperiode(10)),
                                InnvilgetTiltaksdeltakelseKommando(andre.internDeltakelseId, AntallDagerForMeldeperiode(5)),
                            ),
                        ),
                    ),
                    barnetillegg = Barnetillegg.utenBarnetillegg(nonEmptyListOf(periode)),
                ),
            ).leftOrNull() shouldBe KanIkkeOppdatereBehandling.ForMangeDagerPerMeldeperiode(nonEmptyListOf(periode))

            tac.behandlingContext.rammebehandlingRepo.hent(behandling.id) shouldBe medToDeltakelser
        }
    }
}
