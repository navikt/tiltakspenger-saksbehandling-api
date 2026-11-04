package no.nav.tiltakspenger.saksbehandling.behandling.infra.route

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.beslutter
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterSøknadsbehandlingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendSøknadsbehandlingTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taRammebehandlinger
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.toggleKanSendeHelgForMeldekort
import org.junit.jupiter.api.Test

/**
 * En bruker som ikke kan melde helg, kan ikke innvilges for mer enn 10 dager per meldeperiode.
 */
class ForMangeDagerPerMeldeperiodeUtenHelgTest {

    private val forMangeDager = ForventetRespons.json(
        status = 409,
        json = """{"melding":"Kan ikke innvilge mer enn 10 dager per meldeperiode når saken ikke er markert for melding på helgedager","kode":"for_mange_dager_per_meldeperiode_uten_helg"}""",
        contentType = "application/json; charset=UTF-8",
    )

    @Test
    fun `kan ikke oppdatere med mer enn 10 dager per meldeperiode når bruker ikke kan melde helg`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)

            oppdaterSøknadsbehandlingInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = behandling.id,
                innvilgelsesperioder = innvilgelsesperioder(antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(10)),
            )

            oppdaterSøknadsbehandlingInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = behandling.id,
                innvilgelsesperioder = innvilgelsesperioder(antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(11)),
                forventet = forMangeDager,
            )
        }
    }

    @Test
    fun `kan ikke sende til beslutning, iverksette eller slå av helg med mer enn 10 dager per meldeperiode`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)

            toggleKanSendeHelgForMeldekort(tac, sak.id, kanSendeHelg = true)
            oppdaterSøknadsbehandlingInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = behandling.id,
                innvilgelsesperioder = innvilgelsesperioder(antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(14)),
            )

            // Saken har ingen meldeperioder ennå, så helg kan slås av selv om behandlingen har 14 dager.
            toggleKanSendeHelgForMeldekort(tac, sak.id, kanSendeHelg = false)
            sendSøknadsbehandlingTilBeslutningForBehandlingId(tac, sak.id, behandling.id, forventet = forMangeDager)

            toggleKanSendeHelgForMeldekort(tac, sak.id, kanSendeHelg = true)
            sendSøknadsbehandlingTilBeslutningForBehandlingId(tac, sak.id, behandling.id)
            taRammebehandlinger(tac, behandlinger = listOf(sak.id to behandling.id), saksbehandler = beslutter())

            toggleKanSendeHelgForMeldekort(tac, sak.id, kanSendeHelg = false)
            iverksettForBehandlingId(tac, sak.id, behandling.id, forventet = forMangeDager)

            toggleKanSendeHelgForMeldekort(tac, sak.id, kanSendeHelg = true)
            iverksettForBehandlingId(tac, sak.id, behandling.id)

            // Nå har saken meldeperioder med 14 dager, så helg kan ikke lenger slås av.
            toggleKanSendeHelgForMeldekort(
                tac = tac,
                sakId = sak.id,
                kanSendeHelg = false,
                forventet = ForventetRespons(409, contentType = "application/json; charset=UTF-8"),
            )
            tac.sakContext.sakRepo.hentForSakId(sak.id)!!.kanSendeInnHelgForMeldekort shouldBe true
        }
    }
}
