package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.iverksett

import arrow.core.nonEmptyListOf
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.periodisering.til
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.toBarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.OppdaterRevurderingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.ValgtHjemmelForStansDTO
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.RammevedtakDTOJson
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.barnetillegg
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioderDTO
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.hentSakForSaksnummer
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgRevurderingOmgjøring
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgStartRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgStartRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendRevurderingInnvilgelseTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendRevurderingStansTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taBehandling
import no.nav.tiltakspenger.saksbehandling.vedtak.infra.routes.shouldBeEqualToRammevedtakDTO
import no.nav.tiltakspenger.saksbehandling.vedtak.infra.routes.shouldBeEqualToRammevedtakDTOinnvilgelse
import org.json.JSONObject
import org.junit.jupiter.api.Test

internal class IverksettRevurderingTest {
    @Test
    fun `kan iverksette revurdering stans`() {
        withTestApplicationContext { tac ->
            val (sak, _, rammevedtakSøknadsbehandling, revurdering) = iverksettSøknadsbehandlingOgStartRevurderingStans(
                tac,
            )

            oppdaterBehandling(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                oppdaterBehandlingDTO = OppdaterRevurderingDTO.Stans(
                    begrunnelseVilkårsvurdering = null,
                    fritekstTilVedtaksbrev = null,
                    valgteHjemler = nonEmptyListOf(ValgtHjemmelForStansDTO.Alder),
                    stansFraOgMed = rammevedtakSøknadsbehandling.behandling.vedtaksperiode!!.fraOgMed,
                    stansTilOgMed = null,
                    harValgtStansFraFørsteDagSomGirRett = false,
                    harValgtStansTilSisteDagSomGirRett = true,
                ),
            )

            sendRevurderingStansTilBeslutningForBehandlingId(
                tac,
                sak.id,
                revurdering.id,
            )
            taBehandling(tac, sak.id, revurdering.id, saksbehandler = ObjectMother.beslutter())
            iverksettForBehandlingId(tac, sak.id, revurdering.id)
        }
    }

    @Test
    fun `kan iverksette revurdering innvilgelsesperiode fremover`() {
        withTestApplicationContext { tac ->
            val søknadsbehandlingVedtaksperiode = 1.til(10.april(2025))
            val revurderingInnvilgelsesperiode = søknadsbehandlingVedtaksperiode.plusTilOgMed(14L)

            val (sak, _, _, revurdering) = iverksettSøknadsbehandlingOgStartRevurderingInnvilgelse(
                tac,
                søknadsbehandlingInnvilgelsesperiode = søknadsbehandlingVedtaksperiode,
                revurderingVedtaksperiode = revurderingInnvilgelsesperiode,
            )

            val barnetillegg = barnetillegg(
                begrunnelse = Begrunnelse.create("barnetillegg begrunnelse"),
                periode = revurderingInnvilgelsesperiode,
                antallBarn = AntallBarn(1),
            )

            oppdaterBehandling(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                oppdaterBehandlingDTO = OppdaterRevurderingDTO.Innvilgelse(
                    fritekstTilVedtaksbrev = "ny brevtekst",
                    begrunnelseVilkårsvurdering = "ny begrunnelse",
                    innvilgelsesperioder = revurdering.innvilgelsesperioderDTO(revurderingInnvilgelsesperiode),
                    barnetillegg = barnetillegg.toBarnetilleggDTO(),
                ),
            )

            sendRevurderingInnvilgelseTilBeslutningForBehandlingId(
                tac,
                sak.id,
                revurdering.id,
            )
            taBehandling(tac, sak.id, revurdering.id, saksbehandler = ObjectMother.beslutter())
            iverksettForBehandlingId(tac, sak.id, revurdering.id)
        }
    }

    @Test
    fun `kan iverksette revurdering innvilgelsesperiode bakover`() {
        withTestApplicationContext { tac ->
            val søknadsbehandlingVedtaksperiode = 1.til(10.april(2025))
            val revurderingInnvilgelsesperiode = søknadsbehandlingVedtaksperiode.minusFraOgMed(14L)

            val (sak, _, _, revurdering) = iverksettSøknadsbehandlingOgStartRevurderingInnvilgelse(
                tac,
                søknadsbehandlingInnvilgelsesperiode = søknadsbehandlingVedtaksperiode,
                revurderingVedtaksperiode = revurderingInnvilgelsesperiode,
            )

            val barnetillegg = barnetillegg(
                begrunnelse = Begrunnelse.create("barnetillegg begrunnelse"),
                periode = revurderingInnvilgelsesperiode,
                antallBarn = AntallBarn(1),
            )

            oppdaterBehandling(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                oppdaterBehandlingDTO = OppdaterRevurderingDTO.Innvilgelse(
                    fritekstTilVedtaksbrev = "ny brevtekst",
                    begrunnelseVilkårsvurdering = "ny begrunnelse",
                    innvilgelsesperioder = revurdering.innvilgelsesperioderDTO(
                        revurderingInnvilgelsesperiode,
                    ),
                    barnetillegg = barnetillegg.toBarnetilleggDTO(),
                ),
            )

            sendRevurderingInnvilgelseTilBeslutningForBehandlingId(
                tac,
                sak.id,
                revurdering.id,
            )
            taBehandling(tac, sak.id, revurdering.id, saksbehandler = ObjectMother.beslutter())
            iverksettForBehandlingId(tac, sak.id, revurdering.id)
        }
    }

    @Test
    fun `må være beslutter for å iverksette revurdering`() {
        withTestApplicationContext { tac ->
            val (sak, _, rammevedtakSøknadsbehandling, revurdering) = iverksettSøknadsbehandlingOgStartRevurderingStans(
                tac,
            )

            oppdaterBehandling(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                oppdaterBehandlingDTO = OppdaterRevurderingDTO.Stans(
                    begrunnelseVilkårsvurdering = null,
                    fritekstTilVedtaksbrev = null,
                    valgteHjemler = nonEmptyListOf(ValgtHjemmelForStansDTO.Alder),
                    stansFraOgMed = rammevedtakSøknadsbehandling.behandling.vedtaksperiode!!.fraOgMed,
                    stansTilOgMed = null,
                    harValgtStansFraFørsteDagSomGirRett = false,
                    harValgtStansTilSisteDagSomGirRett = true,
                ),
            )

            sendRevurderingStansTilBeslutningForBehandlingId(
                tac,
                sak.id,
                revurdering.id,
            )
            taBehandling(tac, sak.id, revurdering.id, saksbehandler = ObjectMother.beslutter())
            iverksettForBehandlingId(
                tac,
                sak.id,
                revurdering.id,
                beslutter = ObjectMother.saksbehandler(),
                forventetStatus = HttpStatusCode.Forbidden,
            )
        }
    }

    @Test
    fun `verifiser vedtak dto ved revurdering til innvilgelse`() {
        withTestApplicationContext { tac ->
            val (sak, _, rammevedtakSøknadsbehandling, rammevedtakRevurdering) = iverksettSøknadsbehandlingOgRevurderingInnvilgelse(
                tac = tac,
                søknadsbehandlingInnvilgelsesperiode = 1.til(10.april(2025)),
                revurderingInnvilgelsesperiode = 9.til(11.april(2025)),
            )
            val søknadsbehandling = rammevedtakSøknadsbehandling.behandling as Søknadsbehandling
            val revurdering = rammevedtakRevurdering.behandling as Revurdering
            val sakDTOJson: JSONObject = hentSakForSaksnummer(tac, sak.saksnummer)!!
            val søknadsbehandlingvedtakDTOJson: RammevedtakDTOJson =
                sakDTOJson.getJSONArray("alleRammevedtak").getJSONObject(0)
            val revurderingvedtakDTOJson: RammevedtakDTOJson =
                sakDTOJson.getJSONArray("alleRammevedtak").getJSONObject(1)
            sak.rammevedtaksliste.size.shouldBe(2)
            søknadsbehandlingvedtakDTOJson.shouldBeEqualToRammevedtakDTOinnvilgelse(
                id = sak.rammevedtaksliste[0].id.toString(),
                behandlingId = søknadsbehandling.id.toString(),
                gjeldendeVedtaksperioder = listOf(1.til(8.april(2025))),
                gjeldendeInnvilgetPerioder = listOf(1.til(8.april(2025))),
                omgjortGrad = "DELVIS",
            )
            revurderingvedtakDTOJson.shouldBeEqualToRammevedtakDTOinnvilgelse(
                id = sak.rammevedtaksliste[1].id.toString(),
                behandlingId = revurdering.id.toString(),
                gjeldendeVedtaksperioder = listOf(9.til(11.april(2025))),
                gjeldendeInnvilgetPerioder = listOf(9.til(11.april(2025))),
                opprinneligVedtaksperiode = 9.til(11.april(2025)),
                opprinneligInnvilgetPerioder = listOf(9.til(11.april(2025))),
                opprettet = "2025-01-01T01:02:33.456789",
                resultat = "REVURDERING_INNVILGELSE",
                barnetillegg = """
                    {
                        "begrunnelse": null,
                        "perioder": [
                          {
                            "antallBarn": 0,
                            "periode": {
                              "fraOgMed": "2025-04-09",
                              "tilOgMed": "2025-04-11"
                            }
                          }
                        ]
                      }
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `verifiser vedtak dto ved revurdering til stans`() {
        withTestApplicationContext { tac ->
            val (sak, _, rammevedtakSøknadsbehandling, rammevedtakRevurdering) = iverksettSøknadsbehandlingOgRevurderingStans(
                tac = tac,
                søknadsbehandlingInnvilgelsesperiode = 1.til(10.april(2025)),
                stansFraOgMed = 5.april(2025),
            )
            val søknadsbehandling = rammevedtakSøknadsbehandling.behandling as Søknadsbehandling
            val revurdering = rammevedtakRevurdering.behandling as Revurdering
            val sakDTOJson: JSONObject = hentSakForSaksnummer(tac, sak.saksnummer)!!
            val søknadsbehandlingvedtakDTOJson: RammevedtakDTOJson =
                sakDTOJson.getJSONArray("alleRammevedtak").getJSONObject(0)
            val revurderingvedtakDTOJson: RammevedtakDTOJson =
                sakDTOJson.getJSONArray("alleRammevedtak").getJSONObject(1)
            sak.rammevedtaksliste.size.shouldBe(2)
            søknadsbehandlingvedtakDTOJson.shouldBeEqualToRammevedtakDTOinnvilgelse(
                id = rammevedtakSøknadsbehandling.id.toString(),
                behandlingId = søknadsbehandling.id.toString(),
                gjeldendeVedtaksperioder = listOf(1.til(4.april(2025))),
                gjeldendeInnvilgetPerioder = listOf(1.til(4.april(2025))),
                omgjortGrad = "DELVIS",
            )
            revurderingvedtakDTOJson.shouldBeEqualToRammevedtakDTO(
                id = rammevedtakRevurdering.id.toString(),
                behandlingId = revurdering.id.toString(),
                gjeldendeVedtaksperioder = listOf(5.til(10.april(2025))),
                gjeldendeInnvilgetPerioder = emptyList(),
                opprinneligVedtaksperiode = 5.til(10.april(2025)),
                opprinneligInnvilgetPerioder = emptyList(),
                opprettet = "2025-01-01T01:02:33.456789",
                resultat = "STANS",
                barnetillegg = null,
                antallDagerPerMeldeperiode = 0,
                saksbehandler = rammevedtakRevurdering.saksbehandler,
                beslutter = rammevedtakRevurdering.beslutter,
                erGjeldende = true,
                vedtaksdato = null,
                omgjortGrad = null,
                omgjøringskommando = null,
                stanskommando = null,
                opphørskommando = null,
            )
        }
    }

    @Test
    fun `verifiser vedtak dto ved revurdering til omgjøring`() {
        withTestApplicationContext { tac ->
            val (sak, _, rammevedtakSøknadsbehandling, rammevedtakRevurdering, _) = iverksettSøknadsbehandlingOgRevurderingOmgjøring(
                tac,
            )!!
            val søknadsbehandling = rammevedtakSøknadsbehandling.behandling as Søknadsbehandling
            val revurdering = rammevedtakRevurdering.behandling as Revurdering
            val sakDTOJson: JSONObject = hentSakForSaksnummer(tac, sak.saksnummer)!!
            val søknadsbehandlingvedtakDTOJson: RammevedtakDTOJson =
                sakDTOJson.getJSONArray("alleRammevedtak").getJSONObject(0)
            val revurderingvedtakDTOJson: RammevedtakDTOJson =
                sakDTOJson.getJSONArray("alleRammevedtak").getJSONObject(1)
            sak.rammevedtaksliste.size.shouldBe(2)
            søknadsbehandlingvedtakDTOJson.shouldBeEqualToRammevedtakDTOinnvilgelse(
                id = sak.rammevedtaksliste[0].id.toString(),
                behandlingId = søknadsbehandling.id.toString(),
                gjeldendeVedtaksperioder = emptyList(),
                gjeldendeInnvilgetPerioder = emptyList(),
                erGjeldende = false,
                omgjortGrad = "HELT",
            )
            revurderingvedtakDTOJson.shouldBeEqualToRammevedtakDTO(
                id = sak.rammevedtaksliste[1].id.toString(),
                behandlingId = revurdering.id.toString(),
                gjeldendeVedtaksperioder = listOf(1.til(10.april(2025))),
                gjeldendeInnvilgetPerioder = listOf(1.til(10.april(2025))),
                opprinneligVedtaksperiode = 1.til(10.april(2025)),
                opprinneligInnvilgetPerioder = listOf(1.til(10.april(2025))),
                opprettet = "2025-01-01T01:02:33.456789",
                resultat = "OMGJØRING",
                antallDagerPerMeldeperiode = 10,
                saksbehandler = revurdering.saksbehandler!!,
                beslutter = revurdering.beslutter!!,
                erGjeldende = true,
                vedtaksdato = null,
                barnetillegg = """
                    {
                        "begrunnelse": null,
                        "perioder": [
                          {
                            "antallBarn": 0,
                            "periode": {
                              "fraOgMed": "2025-04-01",
                              "tilOgMed": "2025-04-10"
                            }
                          }
                        ]
                      }
                """.trimIndent(),
                omgjortGrad = null,
                omgjøringskommando = """
                    "OMGJØR": {
                      "tvungenOmgjøringsperiode": {
                        "fraOgMed": "2025-04-01",
                        "tilOgMed": "2025-04-10"
                      },
                      "type": "OMGJØR"
                    }
                """.trimIndent(),
                stanskommando = """
                    "STANS": {
                        "tidligsteFraOgMedDato": "2025-04-01",
                        "type": "STANS",
                        "tvungenStansTilOgMedDato": "2025-04-10"
                    }
                """.trimIndent(),
                opphørskommando = """"OPPHØR": {
                      "innvilgelsesperioder": [
                        {
                          "fraOgMed": "2025-04-01",
                          "tilOgMed": "2025-04-10"
                        }
                      ],
                      "type": "OPPHØR"
                    }
                """.trimIndent(),
            )
        }
    }
}
