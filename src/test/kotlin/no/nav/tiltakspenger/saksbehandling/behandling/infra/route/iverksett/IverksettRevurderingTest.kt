package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.iverksett

import arrow.core.nonEmptyListOf
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.libs.periodisering.til
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.toBarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.OppdaterRevurderingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.ValgtHjemmelForStansDTO
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.RammevedtakDTOJson
import no.nav.tiltakspenger.saksbehandling.infra.route.tilAntallDagerPerMeldeperiodeDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.antallDagerPerMeldeperiodeDTO
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.barnetillegg
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.tiltaksdeltakelseDTO
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.hentSakForSaksnummer
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettRevurderingOmgjøring
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendRevurderingInnvilgelseTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendRevurderingStansTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taBehandling
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.route.TiltaksdeltakelsePeriodeDTO
import no.nav.tiltakspenger.saksbehandling.vedtak.infra.routes.shouldBeEqualToRammevedtakDTO
import no.nav.tiltakspenger.saksbehandling.vedtak.infra.routes.shouldBeEqualToRammevedtakDTOinnvilgelse
import org.json.JSONObject
import org.junit.jupiter.api.Test

internal class IverksettRevurderingTest {
    @Test
    fun `kan iverksette revurdering stans`() {
        withTestApplicationContext { tac ->
            val (sak, _, søknadsbehandling, revurdering) = startRevurderingStans(tac)

            oppdaterBehandling(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                oppdaterBehandlingDTO = OppdaterRevurderingDTO.Stans(
                    begrunnelseVilkårsvurdering = null,
                    fritekstTilVedtaksbrev = null,
                    valgteHjemler = nonEmptyListOf(ValgtHjemmelForStansDTO.Alder),
                    stansFraOgMed = søknadsbehandling.virkningsperiode!!.fraOgMed,
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
            val søknadsbehandlingVirkningsperiode = 1.til(10.april(2025))
            val revurderingInnvilgelsesperiode = søknadsbehandlingVirkningsperiode.plusTilOgMed(14L)

            val (sak, _, _, revurdering) = startRevurderingInnvilgelse(
                tac,
                søknadsbehandlingInnvilgelsesperiode = søknadsbehandlingVirkningsperiode,
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
                    valgteTiltaksdeltakelser = revurdering.tiltaksdeltakelseDTO(),
                    innvilgelsesperiode = revurderingInnvilgelsesperiode.toDTO(),
                    barnetillegg = barnetillegg.toBarnetilleggDTO(),
                    antallDagerPerMeldeperiodeForPerioder = revurdering.antallDagerPerMeldeperiodeDTO(
                        revurderingInnvilgelsesperiode,
                    ),
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
            val søknadsbehandlingVirkningsperiode = 1.til(10.april(2025))
            val revurderingInnvilgelsesperiode = søknadsbehandlingVirkningsperiode.minusFraOgMed(14L)

            val (sak, _, _, revurdering) = startRevurderingInnvilgelse(
                tac,
                søknadsbehandlingInnvilgelsesperiode = søknadsbehandlingVirkningsperiode,
                revurderingVedtaksperiode = revurderingInnvilgelsesperiode,
            )

            val tiltaksdeltakelse = revurdering.saksopplysninger.tiltaksdeltakelser.single()

            val barnetillegg = barnetillegg(
                begrunnelse = Begrunnelse.create("barnetillegg begrunnelse"),
                periode = revurderingInnvilgelsesperiode,
                antallBarn = AntallBarn(1),
            )

            val antallDager = SammenhengendePeriodisering(
                AntallDagerForMeldeperiode(DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE),
                revurderingInnvilgelsesperiode,
            )

            oppdaterBehandling(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                oppdaterBehandlingDTO = OppdaterRevurderingDTO.Innvilgelse(
                    fritekstTilVedtaksbrev = "ny brevtekst",
                    begrunnelseVilkårsvurdering = "ny begrunnelse",
                    valgteTiltaksdeltakelser = listOf(
                        TiltaksdeltakelsePeriodeDTO(
                            eksternDeltagelseId = tiltaksdeltakelse.eksternDeltakelseId,
                            periode = tiltaksdeltakelse.periode!!.toDTO(),
                        ),
                    ),
                    innvilgelsesperiode = revurderingInnvilgelsesperiode.toDTO(),
                    barnetillegg = barnetillegg.toBarnetilleggDTO(),
                    antallDagerPerMeldeperiodeForPerioder = antallDager.tilAntallDagerPerMeldeperiodeDTO(),
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
            val (sak, _, søknadsbehandling, revurdering) = startRevurderingStans(tac)

            oppdaterBehandling(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                oppdaterBehandlingDTO = OppdaterRevurderingDTO.Stans(
                    begrunnelseVilkårsvurdering = null,
                    fritekstTilVedtaksbrev = null,
                    valgteHjemler = nonEmptyListOf(ValgtHjemmelForStansDTO.Alder),
                    stansFraOgMed = søknadsbehandling.virkningsperiode!!.fraOgMed,
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
            val (sak, _, søknadsbehandling, revurdering) = iverksettRevurderingInnvilgelse(
                tac = tac,
                søknadsbehandlingInnvilgelsesperiode = 1.til(10.april(2025)),
                revurderingInnvilgelsesperiode = 9.til(11.april(2025)),
            )
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
            val (sak, _, søknadsbehandling, revurdering) = iverksettRevurderingStans(
                tac = tac,
                søknadsbehandlingInnvilgelsesperiode = 1.til(10.april(2025)),
                stansFraOgMed = 5.april(2025),
            )
            val sakDTOJson: JSONObject = hentSakForSaksnummer(tac, sak.saksnummer)!!
            val søknadsbehandlingvedtakDTOJson: RammevedtakDTOJson =
                sakDTOJson.getJSONArray("alleRammevedtak").getJSONObject(0)
            val revurderingvedtakDTOJson: RammevedtakDTOJson =
                sakDTOJson.getJSONArray("alleRammevedtak").getJSONObject(1)
            sak.rammevedtaksliste.size.shouldBe(2)
            søknadsbehandlingvedtakDTOJson.shouldBeEqualToRammevedtakDTOinnvilgelse(
                id = sak.rammevedtaksliste[0].id.toString(),
                behandlingId = søknadsbehandling.id.toString(),
                gjeldendeVedtaksperioder = listOf(1.til(4.april(2025))),
                gjeldendeInnvilgetPerioder = listOf(1.til(4.april(2025))),
                omgjortGrad = "DELVIS",
            )
            revurderingvedtakDTOJson.shouldBeEqualToRammevedtakDTO(
                id = sak.rammevedtaksliste[1].id.toString(),
                behandlingId = revurdering.id.toString(),
                gjeldendeVedtaksperioder = listOf(5.til(10.april(2025))),
                gjeldendeInnvilgetPerioder = emptyList(),
                opprinneligVedtaksperiode = 5.til(10.april(2025)),
                opprinneligInnvilgetPerioder = emptyList(),
                opprettet = "2025-01-01T01:02:33.456789",
                resultat = "STANS",
                barnetillegg = null,
                antallDagerPerMeldeperiode = 0,
                saksbehandler = revurdering.saksbehandler!!,
                beslutter = revurdering.beslutter!!,
                erGjeldende = true,
                vedtaksdato = null,
                omgjortGrad = null,
                omgjøringskommando = """
                    "OMGJØR": {
                      "tvungenOmgjøringsperiode": {
                        "fraOgMed": "2025-04-05",
                        "tilOgMed": "2025-04-10"
                      },
                      "type": "OMGJØR"
                    }
                """.trimIndent(),
                stanskommando = null,
                opphørskommando = null,
            )
        }
    }

    @Test
    fun `verifiser vedtak dto ved revurdering til omgjøring`() {
        withTestApplicationContext { tac ->
            val (sak, _, søknadsbehandling, revurdering) = iverksettRevurderingOmgjøring(tac)
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
