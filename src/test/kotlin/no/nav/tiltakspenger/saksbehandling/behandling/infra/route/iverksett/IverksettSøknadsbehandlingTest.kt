package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.iverksett

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.instanceOf
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.SøknadsbehandlingType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Søknadsbehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.infra.route.RammevedtakDTOJson
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.vedtaksperiode
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.hentSakForSaksnummer
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettAutomatiskBehandletSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendSøknadsbehandlingTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taBehandling
import no.nav.tiltakspenger.saksbehandling.vedtak.infra.routes.shouldBeEqualToRammevedtakDTOavslag
import no.nav.tiltakspenger.saksbehandling.vedtak.infra.routes.shouldBeEqualToRammevedtakDTOinnvilgelse
import org.json.JSONObject
import org.junit.jupiter.api.Test

class IverksettSøknadsbehandlingTest {
    @Test
    fun `iverksett endrer status på behandlingen`() = runTest {
        withTestApplicationContext { tac ->
            val (_, _, rammevedtak) = this.iverksettSøknadsbehandling(tac)
            val behandling = rammevedtak.rammebehandling
            behandling.vedtaksperiode.shouldNotBeNull()
            behandling.status shouldBe Rammebehandlingsstatus.VEDTATT
        }
    }

    @Test
    fun `iverksett - kan iverksette en automatisk behandlet behandling`() = runTest {
        withTestApplicationContext { tac ->
            val (_, _, rammevedtakSøknadsbehandling) = this.iverksettAutomatiskBehandletSøknadsbehandling(tac)
            val søknadsbehandling = rammevedtakSøknadsbehandling.rammebehandling as Søknadsbehandling
            søknadsbehandling.vedtaksperiode.shouldNotBeNull()
            søknadsbehandling.status shouldBe Rammebehandlingsstatus.VEDTATT
        }
    }

    @Test
    fun `iverksett - avslag på søknad`() = runTest {
        withTestApplicationContext { tac ->
            val (_, _, rammevedtak) = this.iverksettSøknadsbehandling(
                tac,
                resultat = SøknadsbehandlingType.AVSLAG,
            )
            val behandling = rammevedtak.rammebehandling
            behandling.vedtaksperiode.shouldNotBeNull()
            behandling.status shouldBe Rammebehandlingsstatus.VEDTATT
            behandling.resultat shouldBe instanceOf<`Søknadsbehandlingsresultat`.Avslag>()
        }
    }

    @Test
    fun `iverksett - verifiser avslag vedtak dto`() = runTest {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContext(clock = clock) { tac ->
            val (sak, søknad, rammevedtakSøknadsbehandling) = this.iverksettSøknadsbehandling(
                tac,
                resultat = SøknadsbehandlingType.AVSLAG,
            )
            val søknadsbehandling = rammevedtakSøknadsbehandling.rammebehandling
            val sakDTOJson: JSONObject = hentSakForSaksnummer(tac, søknadsbehandling.saksnummer)!!
            val rammevedtakDTOJson: RammevedtakDTOJson = sakDTOJson.getJSONArray("alleRammevedtak").getJSONObject(0)
            rammevedtakDTOJson.shouldBeEqualToRammevedtakDTOavslag(
                id = sak.rammevedtaksliste.single().id.toString(),
                behandlingId = søknadsbehandling.id.toString(),
                opprettet = "2025-01-01T01:02:20.456789",
                opprinneligVedtaksperiode = søknad.tiltaksdeltakelseperiodeDetErSøktOm()!!,
            )
        }
    }

    @Test
    fun `iverksett - verifiser innvilgelse vedtak dto`() = runTest {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContext(clock = clock) { tac ->
            val (_, _, rammevedtakSøknadsbehandling) = this.iverksettSøknadsbehandling(
                tac,
                resultat = SøknadsbehandlingType.INNVILGELSE,
            )
            val søknadsbehandling = rammevedtakSøknadsbehandling.rammebehandling
            val sakDTOJson: JSONObject = hentSakForSaksnummer(tac, søknadsbehandling.saksnummer)!!
            val rammevedtakDTOJson: RammevedtakDTOJson = sakDTOJson.getJSONArray("alleRammevedtak").getJSONObject(0)
            rammevedtakDTOJson.shouldBeEqualToRammevedtakDTOinnvilgelse(
                id = rammevedtakSøknadsbehandling.id.toString(),
                behandlingId = søknadsbehandling.id.toString(),
                opprettet = "2025-01-01T01:02:20.456789",
                opprinneligVedtaksperiode = vedtaksperiode(),
            )
        }
    }

    @Test
    fun `iverksett - feilmelding hvis behandlingen ikke er tildelt beslutter`() = runTest {
        withTestApplicationContext { tac ->
            val saksbehandler = saksbehandler()
            val beslutter = ObjectMother.beslutter()
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse(tac, saksbehandler = saksbehandler)
            val behandlingId = behandling.id
            tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).also {
                it.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
                it.saksbehandler shouldBe saksbehandler.navIdent
                it.beslutter shouldBe null
            }
            sendSøknadsbehandlingTilBeslutningForBehandlingId(
                tac,
                sak.id,
                behandlingId,
                saksbehandler,
            )
            taBehandling(tac, sak.id, behandlingId, beslutter)

            iverksettForBehandlingId(
                tac,
                sak.id,
                behandlingId,
                ObjectMother.beslutter(navIdent = "B999999"),
                forventetStatus = HttpStatusCode.BadRequest,
                // language=JSON
                forventetJsonBody = """
                    {
                      "melding" : "Du kan ikke utføre handlinger på en behandling som ikke er tildelt deg. Behandlingen er tildelt B12345",
                      "kode" : "behandling_eies_av_annen_saksbehandler"
                    }
                """.trimIndent(),
            ) shouldBe null
        }
    }

    @Test
    fun `iverksett - ikke tilgang hvis beslutter ikke har beslutter-rolle`() = runTest {
        withTestApplicationContext { tac ->
            val saksbehandler = saksbehandler()
            val beslutter = ObjectMother.beslutter()
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse(tac, saksbehandler = saksbehandler)
            val behandlingId = behandling.id
            tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).also {
                it.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
                it.saksbehandler shouldBe saksbehandler.navIdent
                it.beslutter shouldBe null
            }
            sendSøknadsbehandlingTilBeslutningForBehandlingId(
                tac,
                sak.id,
                behandlingId,
                saksbehandler,
            )
            taBehandling(tac, sak.id, behandlingId, beslutter)

            iverksettForBehandlingId(
                tac = tac,
                sakId = sak.id,
                behandlingId = behandlingId,
                beslutter = saksbehandler,
                forventetStatus = HttpStatusCode.Forbidden,
                // language=JSON
                forventetJsonBody = """
                    {
                      "melding" : "Saksbehandler Z12345 mangler rollen BESLUTTER. Saksbehandlers roller: Saksbehandlerroller(value=[SAKSBEHANDLER])",
                      "kode" : "tilgang_nektet_krev_rolle"
                    }
                """.trimIndent(),
            ) shouldBe null
        }
    }
}
