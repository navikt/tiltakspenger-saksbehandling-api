package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.konverter

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.juni
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetBody
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.forlengelse.ForlengerRammevedtak
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Omgjøringsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.RevurderingsresultatType
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.infra.route.rammebehandlingJson
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgStartRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.konverterRevurdering
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendRevurderingTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingForSakId
import org.junit.jupiter.api.Test

class KonverterRevurderingRouteTest {

    private fun feil(melding: String, kode: String) = ForventetRespons(
        status = 400,
        body = ForventetBody.Json("""{"melding":"$melding","kode":"$kode"}"""),
        contentType = "application/json; charset=UTF-8",
    )

    @Test
    fun `stans konverteres til innvilgelse, og resultatet nullstilles`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, _, stans) = iverksettSøknadsbehandlingOgStartRevurderingStans(tac)
            oppdaterRevurderingStans(
                tac = tac,
                sakId = sak.id,
                behandlingId = stans.id,
                begrunnelseVilkårsvurdering = "begrunnelse",
                fritekstTilVedtaksbrev = "fritekst",
            )

            val (_, revurdering, sakJson) = konverterRevurdering(
                tac = tac,
                sakId = sak.id,
                behandlingId = stans.id,
                revurderingType = "REVURDERING_INNVILGELSE",
            )!!

            revurdering.resultat shouldBe Revurderingsresultat.Innvilgelse.empty
            revurdering.utbetaling.shouldBeNull()
            revurdering.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
            revurdering.begrunnelseVilkårsvurdering?.verdi shouldBe "begrunnelse"
            revurdering.fritekstTilVedtaksbrev?.verdi shouldBe "fritekst"

            val behandlingJson = sakJson.rammebehandlingJson(stans.id)
            behandlingJson["resultat"].asString() shouldBe "REVURDERING_INNVILGELSE"
            behandlingJson["gyldigeKommandoer"].values().map { it.asString() } shouldContain "KonverterRevurdering"
        }
    }

    @Test
    fun `stans konverteres til omgjøring uten valgt resultat`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, _, stans) = iverksettSøknadsbehandlingOgStartRevurderingStans(tac)

            val (_, revurdering) = konverterRevurdering(
                tac = tac,
                sakId = sak.id,
                behandlingId = stans.id,
                revurderingType = "OMGJØRING",
            )!!

            revurdering.resultat.shouldBeInstanceOf<Omgjøringsresultat.OmgjøringIkkeValgt>()
        }
    }

    @Test
    fun `stans konverteres til forlengelse av et vedtak som kan forlenges`() {
        withTestApplicationContextAndPostgres { tac ->
            val vedtaksperiode = 1.januar(2025) til 31.mars(2025)
            val deltakelse = tac.tiltaksdeltakelse(1.januar(2025) til 30.juni(2025))
            val (sak, _, vedtak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = deltakelse,
                innvilgelsesperioder = innvilgelsesperioder(vedtaksperiode, deltakelse),
            )
            val (_, stans) = startRevurderingForSakId(tac, sak.id, RevurderingsresultatType.STANS)!!

            val (_, revurdering) = konverterRevurdering(
                tac = tac,
                sakId = sak.id,
                behandlingId = stans.id,
                revurderingType = "FORLENGELSE",
                rammevedtakIdSomForlenges = vedtak.id,
            )!!

            revurdering.resultat.shouldBeInstanceOf<Revurderingsresultat.Forlengelse>()
                .forlenger shouldBe ForlengerRammevedtak(vedtak.id, 1.januar(2025).plusMonths(3))
        }
    }

    @Test
    fun `forlengelse av et vedtak som ikke kan forlenges avvises`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, _, stans) = iverksettSøknadsbehandlingOgStartRevurderingStans(tac)
            val ukjentVedtakId = VedtakId.random()

            konverterRevurdering(
                tac = tac,
                sakId = sak.id,
                behandlingId = stans.id,
                revurderingType = "FORLENGELSE",
                rammevedtakIdSomForlenges = ukjentVedtakId,
                forventet = feil(
                    "Vedtaket kan ikke forlenges fordi det ikke er innvilget helt frem til slutten av vedtaksperioden, eller fordi dagen etter dekkes av et annet vedtak.",
                    "vedtak_kan_ikke_forlenges",
                ),
            ).shouldBeNull()
        }
    }

    @Test
    fun `forlengelse uten vedtak som forlenges avvises`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, _, stans) = iverksettSøknadsbehandlingOgStartRevurderingStans(tac)

            konverterRevurdering(
                tac = tac,
                sakId = sak.id,
                behandlingId = stans.id,
                revurderingType = "FORLENGELSE",
                forventet = feil(
                    "rammevedtakIdSomForlenges må være satt for å konvertere til forlengelse.",
                    "mangler_rammevedtak_som_forlenges",
                ),
            ).shouldBeNull()
        }
    }

    @Test
    fun `konvertering til samme type avvises`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, _, stans) = iverksettSøknadsbehandlingOgStartRevurderingStans(tac)

            konverterRevurdering(
                tac = tac,
                sakId = sak.id,
                behandlingId = stans.id,
                revurderingType = "STANS",
                forventet = feil("Revurderingen er allerede av denne typen.", "har_allerede_typen"),
            ).shouldBeNull()
        }
    }

    @Test
    fun `annen saksbehandler enn den tildelte kan ikke konvertere`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, _, stans) = iverksettSøknadsbehandlingOgStartRevurderingStans(tac)
            val annenSaksbehandler = ObjectMother.saksbehandler(navIdent = "Z999999")

            konverterRevurdering(
                tac = tac,
                sakId = sak.id,
                behandlingId = stans.id,
                revurderingType = "OMGJØRING",
                saksbehandler = annenSaksbehandler,
                forventet = feil(
                    "Du kan ikke utføre handlinger på en behandling som ikke er tildelt deg. Behandlingen er tildelt ${stans.saksbehandler}",
                    "behandling_eies_av_annen_saksbehandler",
                ),
            ).shouldBeNull()
            (tac.behandlingContext.rammebehandlingRepo.hent(stans.id) as Revurdering).resultat
                .shouldBeInstanceOf<Revurderingsresultat.Stans>()
        }
    }

    @Test
    fun `revurdering som er sendt til beslutning kan ikke konverteres`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, _, stans) = iverksettSøknadsbehandlingOgStartRevurderingStans(tac)
            oppdaterRevurderingStans(
                tac = tac,
                sakId = sak.id,
                behandlingId = stans.id,
                begrunnelseVilkårsvurdering = "begrunnelse",
                fritekstTilVedtaksbrev = "fritekst",
            )
            sendRevurderingTilBeslutningForBehandlingId(tac, sak.id, stans.id)

            konverterRevurdering(
                tac = tac,
                sakId = sak.id,
                behandlingId = stans.id,
                revurderingType = "OMGJØRING",
                forventet = feil(
                    "Revurderingen må være under behandling for å kunne konverteres. Status er KLAR_TIL_BESLUTNING.",
                    "må_være_under_behandling",
                ),
            ).shouldBeNull()
        }
    }

    @Test
    fun `søknadsbehandling kan ikke konverteres`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, søknadsbehandling) = opprettSøknadsbehandlingUnderBehandling(tac)

            konverterRevurdering(
                tac = tac,
                sakId = sak.id,
                behandlingId = søknadsbehandling.id,
                revurderingType = "STANS",
                forventet = feil("Bare revurderinger kan konverteres.", "må_være_revurdering"),
            ).shouldBeNull()
        }
    }
}
