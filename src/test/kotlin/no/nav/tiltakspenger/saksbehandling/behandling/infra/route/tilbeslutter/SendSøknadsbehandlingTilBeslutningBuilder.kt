package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.tilbeslutter

import arrow.core.Tuple4
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.path
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.util.url
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.april
import no.nav.tiltakspenger.libs.periodisering.januar
import no.nav.tiltakspenger.libs.periodisering.mars
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingType
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterBegrunnelseForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterFritekstForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.Søknad
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.route.toDTO

interface SendSøknadsbehandlingTilBeslutningBuilder {

    /** Oppretter ny sak, søknad og behandling. */
    suspend fun ApplicationTestBuilder.sendSøknadsbehandlingTilBeslutning(
        tac: TestApplicationContext,
        fnr: Fnr = Fnr.random(),
        virkingsperiode: Periode = Periode(1.april(2025), 10.april(2025)),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        resultat: SøknadsbehandlingType = SøknadsbehandlingType.INNVILGELSE,
        antallDagerPerMeldeperiode: Periodisering<AntallDagerForMeldeperiode> = Periodisering(
            PeriodeMedVerdi(
                AntallDagerForMeldeperiode(MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE),
                virkingsperiode,
            ),
        ),
    ): Tuple4<Sak, Søknad, BehandlingId, String> {
        val (sak, søknad, behandling) = startSøknadsbehandling(tac, fnr, virkingsperiode, saksbehandler)
        val sakId = sak.id
        val behandlingId = behandling.id
        oppdaterFritekstForBehandlingId(tac, sakId, behandlingId, saksbehandler)
        oppdaterBegrunnelseForBehandlingId(tac, sakId, behandlingId, saksbehandler)
        return Tuple4(
            sak,
            søknad,
            behandlingId,
            sendSøknadsbehandlingTilBeslutningForBehandlingId(
                tac = tac,
                sakId = sakId,
                behandlingId = behandlingId,
                saksbehandler = saksbehandler,
                innvilgelsesperiode = søknad.vurderingsperiode(),
                eksternDeltagelseId = søknad.tiltak.id,
                resultat = resultat,
                antallDagerPerMeldeperiode = antallDagerPerMeldeperiode,
            ),
        )
    }

    /** Forventer at det allerede finnes en behandling med status `UNDER_BEHANDLING` */
    suspend fun ApplicationTestBuilder.sendSøknadsbehandlingTilBeslutningForBehandlingId(
        tac: TestApplicationContext,
        sakId: SakId,
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        fritekstTilVedtaksbrev: String = "fritekst",
        begrunnelseVilkårsvurdering: String = "begrunnelse",
        innvilgelsesperiode: Periode = Periode(1.januar(2023), 31.mars(2023)),
        eksternDeltagelseId: String,
        resultat: SøknadsbehandlingType = SøknadsbehandlingType.INNVILGELSE,
        antallDagerPerMeldeperiode: Periodisering<AntallDagerForMeldeperiode> = Periodisering(
            PeriodeMedVerdi(
                AntallDagerForMeldeperiode(MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE),
                innvilgelsesperiode,
            ),
        ),
    ): String {
        val avslagsgrunner = if (resultat == SøknadsbehandlingType.AVSLAG) {
            listOf(
                Avslagsgrunnlag.DeltarIkkePåArbeidsmarkedstiltak,
                Avslagsgrunnlag.Alder,
            ).joinToString(prefix = "[", postfix = "]") { "\"${it}\"" }
        } else {
            "null"
        }

        defaultRequest(
            HttpMethod.Post,
            url {
                protocol = URLProtocol.HTTPS
                path("/sak/$sakId/behandling/$behandlingId/sendtilbeslutning")
            },
            jwt = tac.jwtGenerator.createJwtForSaksbehandler(
                saksbehandler = saksbehandler,
            ),
        ) {
            //language=JSON
            setBody(
                """
            {
                "fritekstTilVedtaksbrev": "$fritekstTilVedtaksbrev",
                "begrunnelseVilkårsvurdering": "$begrunnelseVilkårsvurdering",
                "behandlingsperiode": {
                    "fraOgMed": "${innvilgelsesperiode.fraOgMed}",
                    "tilOgMed": "${innvilgelsesperiode.tilOgMed}"
                },
                "valgteTiltaksdeltakelser": [
                    {
                        "eksternDeltagelseId": "$eksternDeltagelseId",
                        "periode": {
                            "fraOgMed": "${innvilgelsesperiode.fraOgMed}",
                            "tilOgMed": "${innvilgelsesperiode.tilOgMed}"
                        }
                    }
                ],
                "resultat": "${resultat.name}",
                "avslagsgrunner": $avslagsgrunner,
                "antallDagerPerMeldeperiodeForPerioder": ${serialize(antallDagerPerMeldeperiode.toDTO())}
            }
                """.trimIndent(),
            )
        }.apply {
            val bodyAsText = this.bodyAsText()
            withClue(
                "Response details:\n" + "Status: ${this.status}\n" + "Content-Type: ${this.contentType()}\n" + "Body: $bodyAsText\n",
            ) {
                status shouldBe HttpStatusCode.OK
            }
            return bodyAsText
        }
    }

    suspend fun ApplicationTestBuilder.sendSøknadsbehandlingTilBeslutningReturnerRespons(
        tac: TestApplicationContext,
        sakId: SakId,
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        fritekstTilVedtaksbrev: String = "fritekst",
        begrunnelseVilkårsvurdering: String = "begrunnelse",
        innvilgelsesperiode: Periode = Periode(1.januar(2023), 31.mars(2023)),
        eksternDeltagelseId: String,
    ): HttpResponse {
        defaultRequest(
            HttpMethod.Post,
            url {
                protocol = URLProtocol.HTTPS
                path("/sak/$sakId/behandling/$behandlingId/sendtilbeslutning")
            },
            jwt = tac.jwtGenerator.createJwtForSaksbehandler(
                saksbehandler = saksbehandler,
            ),
        ) {
            //language=JSON
            setBody(
                """
            {
                "fritekstTilVedtaksbrev": "$fritekstTilVedtaksbrev",
                "begrunnelseVilkårsvurdering": "$begrunnelseVilkårsvurdering",
                "behandlingsperiode": {
                    "fraOgMed": "${innvilgelsesperiode.fraOgMed}",
                    "tilOgMed": "${innvilgelsesperiode.tilOgMed}"
                },
                "valgteTiltaksdeltakelser": [
                    {
                        "eksternDeltagelseId": "$eksternDeltagelseId",
                        "periode": {
                            "fraOgMed": "${innvilgelsesperiode.fraOgMed}",
                            "tilOgMed": "${innvilgelsesperiode.tilOgMed}"
                        }
                    }
                ],
                "antallDagerPerMeldeperiode": 10,
                "resultat": "INNVILGELSE",
                "avslagsgrunner": null
            }
                """.trimIndent(),
            )
        }.apply {
            return this
        }
    }
}
