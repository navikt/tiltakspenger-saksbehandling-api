package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.iverksett

import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForStansEllerOpphør
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.infra.route.RammevedtakDTOJson
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.objectmothers.DEFAULT_TILTAK_DELTAKELSE_INTERN_ID
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.barnetillegg
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.vedtaksperiode
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.hentSakForSaksnummer
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOmgjøringInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgStartRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgStartRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendRevurderingTilBeslutningForBehandlingId
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

            oppdaterRevurderingStans(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                begrunnelseVilkårsvurdering = null,
                fritekstTilVedtaksbrev = null,
                valgteHjemler = setOf(HjemmelForStansEllerOpphør.Alder),
                stansFraOgMed = rammevedtakSøknadsbehandling.rammebehandling.vedtaksperiode!!.fraOgMed,
                harValgtStansFraFørsteDagSomGirRett = false,
            )

            sendRevurderingTilBeslutningForBehandlingId(
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
                søknadsbehandlingInnvilgelsesperioder = innvilgelsesperioder(søknadsbehandlingVedtaksperiode),
                oppdatertTiltaksdeltakelse = tiltaksdeltakelse(revurderingInnvilgelsesperiode),
            )

            val barnetillegg = barnetillegg(
                begrunnelse = Begrunnelse.create("barnetillegg begrunnelse"),
                periode = revurderingInnvilgelsesperiode,
                antallBarn = AntallBarn(1),
            )

            oppdaterRevurderingInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                fritekstTilVedtaksbrev = "ny brevtekst",
                begrunnelseVilkårsvurdering = "ny begrunnelse",
                innvilgelsesperioder = innvilgelsesperioder(revurderingInnvilgelsesperiode),
                barnetillegg = barnetillegg,
            )

            sendRevurderingTilBeslutningForBehandlingId(
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
                søknadsbehandlingInnvilgelsesperioder = innvilgelsesperioder(søknadsbehandlingVedtaksperiode),
                oppdatertTiltaksdeltakelse = tiltaksdeltakelse(revurderingInnvilgelsesperiode),
            )

            val barnetillegg = barnetillegg(
                begrunnelse = Begrunnelse.create("barnetillegg begrunnelse"),
                periode = revurderingInnvilgelsesperiode,
                antallBarn = AntallBarn(1),
            )

            oppdaterRevurderingInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                fritekstTilVedtaksbrev = "ny brevtekst",
                begrunnelseVilkårsvurdering = "ny begrunnelse",
                innvilgelsesperioder = innvilgelsesperioder(revurderingInnvilgelsesperiode),
                barnetillegg = barnetillegg,
            )

            sendRevurderingTilBeslutningForBehandlingId(
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

            oppdaterRevurderingStans(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                begrunnelseVilkårsvurdering = null,
                fritekstTilVedtaksbrev = null,
                valgteHjemler = setOf(HjemmelForStansEllerOpphør.Alder),
                stansFraOgMed = rammevedtakSøknadsbehandling.rammebehandling.vedtaksperiode!!.fraOgMed,
                harValgtStansFraFørsteDagSomGirRett = false,
            )

            sendRevurderingTilBeslutningForBehandlingId(
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
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContext(clock = clock) { tac ->
            val (sak, _, rammevedtakSøknadsbehandling, rammevedtakRevurdering) = iverksettSøknadsbehandlingOgRevurderingInnvilgelse(
                tac = tac,
                søknadsbehandlingInnvilgelsesperioder = innvilgelsesperioder(1.til(10.april(2025))),
                revurderingInnvilgelsesperioder = innvilgelsesperioder(9.til(11.april(2025))),
            )
            val søknadsbehandling = rammevedtakSøknadsbehandling.rammebehandling as Søknadsbehandling
            val revurdering = rammevedtakRevurdering.rammebehandling as Revurdering
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
                opprettet = "2025-01-01T01:02:19.456789",
                opprinneligVedtaksperiode = 1.til(10.april(2025)),
                innvilgelsesperioder = """
                    [
                        {
                            "internDeltakelseId": "$DEFAULT_TILTAK_DELTAKELSE_INTERN_ID",
                            "periode": {
                                "fraOgMed": "2025-04-01",
                                "tilOgMed": "2025-04-10"
                            },
                            "antallDagerPerMeldeperiode": 10
                        }
                    ]
                """.trimIndent(),
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
            )
            revurderingvedtakDTOJson.shouldBeEqualToRammevedtakDTOinnvilgelse(
                id = sak.rammevedtaksliste[1].id.toString(),
                behandlingId = revurdering.id.toString(),
                gjeldendeVedtaksperioder = listOf(9.til(11.april(2025))),
                gjeldendeInnvilgetPerioder = listOf(9.til(11.april(2025))),
                opprinneligVedtaksperiode = 9.til(11.april(2025)),
                opprinneligInnvilgetPerioder = listOf(9.til(11.april(2025))),
                opprettet = "2025-01-01T01:02:34.456789",
                resultat = "REVURDERING_INNVILGELSE",
                innvilgelsesperioder = """
                    [
                        {
                            "internDeltakelseId": "$DEFAULT_TILTAK_DELTAKELSE_INTERN_ID",
                            "periode": {
                                "fraOgMed": "2025-04-09",
                                "tilOgMed": "2025-04-11"
                            },
                            "antallDagerPerMeldeperiode": 10
                        }
                    ]
                """.trimIndent(),
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
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContext(clock = clock) { tac ->
            val (sak, _, rammevedtakSøknadsbehandling, rammevedtakRevurdering) = iverksettSøknadsbehandlingOgRevurderingStans(
                tac = tac,
                stansFraOgMed = 5.januar(2023),
            )
            val søknadsbehandling = rammevedtakSøknadsbehandling.rammebehandling as Søknadsbehandling
            val revurdering = rammevedtakRevurdering.rammebehandling as Revurdering
            val sakDTOJson: JSONObject = hentSakForSaksnummer(tac, sak.saksnummer)!!
            val søknadsbehandlingvedtakDTOJson: RammevedtakDTOJson =
                sakDTOJson.getJSONArray("alleRammevedtak").getJSONObject(0)
            val revurderingvedtakDTOJson: RammevedtakDTOJson =
                sakDTOJson.getJSONArray("alleRammevedtak").getJSONObject(1)
            sak.rammevedtaksliste.size.shouldBe(2)
            søknadsbehandlingvedtakDTOJson.shouldBeEqualToRammevedtakDTOinnvilgelse(
                id = rammevedtakSøknadsbehandling.id.toString(),
                behandlingId = søknadsbehandling.id.toString(),
                gjeldendeVedtaksperioder = listOf(1.til(4.januar(2023))),
                gjeldendeInnvilgetPerioder = listOf(1.til(4.januar(2023))),
                omgjortGrad = "DELVIS",
                opprettet = "2025-01-01T01:02:20.456789",
            )
            revurderingvedtakDTOJson.shouldBeEqualToRammevedtakDTO(
                id = rammevedtakRevurdering.id.toString(),
                behandlingId = revurdering.id.toString(),
                gjeldendeVedtaksperioder = listOf(5.januar(2023) til 31.mars(2023)),
                gjeldendeInnvilgetPerioder = emptyList(),
                opprinneligVedtaksperiode = 5.januar(2023) til 31.mars(2023),
                opprinneligInnvilgetPerioder = emptyList(),
                opprettet = "2025-01-01T01:02:42.456789",
                resultat = "STANS",
                barnetillegg = null,
                innvilgelsesperioder = null,
                saksbehandler = rammevedtakRevurdering.saksbehandler,
                beslutter = rammevedtakRevurdering.beslutter,
                erGjeldende = true,
                vedtaksdato = null,
                omgjortGrad = null,
                stanskommando = null,
                opphørskommando = null,
                omgjøringskommando = """
                    "OMGJØR": {
                      "perioderSomKanOmgjøres": [
                        {
                            "fraOgMed": "2023-01-05",
                            "tilOgMed": "2023-03-31"
                        }
                      ],
                      "type": "OMGJØR"
                    } 
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `verifiser vedtak dto ved revurdering til omgjøring`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContext(clock = clock) { tac ->
            val (sak, _, rammevedtakSøknadsbehandling, rammevedtakRevurdering, _) = iverksettSøknadsbehandlingOgOmgjøringInnvilgelse(
                tac,
            )!!
            val innvilgelsesperiode = vedtaksperiode()
            val søknadsbehandling = rammevedtakSøknadsbehandling.rammebehandling as Søknadsbehandling
            val revurdering = rammevedtakRevurdering.rammebehandling as Revurdering
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
                opprettet = "2025-01-01T01:02:20.456789",
            )
            revurderingvedtakDTOJson.shouldBeEqualToRammevedtakDTO(
                id = sak.rammevedtaksliste[1].id.toString(),
                behandlingId = revurdering.id.toString(),
                gjeldendeVedtaksperioder = listOf(innvilgelsesperiode),
                gjeldendeInnvilgetPerioder = listOf(innvilgelsesperiode),
                opprinneligVedtaksperiode = innvilgelsesperiode,
                opprinneligInnvilgetPerioder = listOf(innvilgelsesperiode),
                opprettet = "2025-01-01T01:02:42.456789",
                resultat = "OMGJØRING",
                saksbehandler = revurdering.saksbehandler!!,
                beslutter = revurdering.beslutter!!,
                erGjeldende = true,
                vedtaksdato = null,
                innvilgelsesperioder = """[
                    {
                        "internDeltakelseId": "$DEFAULT_TILTAK_DELTAKELSE_INTERN_ID",
                        "periode": {
                            "fraOgMed": "2023-01-01",
                            "tilOgMed": "2023-03-31"
                        },
                        "antallDagerPerMeldeperiode": 10
                    }
                ]""",
                barnetillegg = """
                    {
                        "begrunnelse": null,
                        "perioder": [
                          {
                            "antallBarn": 0,
                            "periode": {
                            "fraOgMed": "2023-01-01",
                            "tilOgMed": "2023-03-31"
                            }
                          }
                        ]
                      }
                """.trimIndent(),
                omgjortGrad = null,
                omgjøringskommando = """
                    "OMGJØR":{
                      "perioderSomKanOmgjøres": [{
                        "fraOgMed": "2023-01-01",
                        "tilOgMed": "2023-03-31"
                      }],
                      "type": "OMGJØR"
                    }
                """.trimIndent(),
                stanskommando = """
                    "STANS": {
                        "tidligsteFraOgMedDato": "2023-01-01",
                        "type": "STANS",
                        "tvungenStansTilOgMedDato": "2023-03-31"
                    }
                """.trimIndent(),
                opphørskommando = """"OPPHØR": {
                      "innvilgelsesperioder": [
                        {
                          "fraOgMed": "2023-01-01",
                          "tilOgMed": "2023-03-31"
                        }
                      ],
                      "type": "OPPHØR"
                    }
                """.trimIndent(),
            )
        }
    }
}
