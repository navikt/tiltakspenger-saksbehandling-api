package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.tilbeslutter

import arrow.core.Nel
import arrow.core.Tuple4
import arrow.core.nonEmptyListOf
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.path
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.util.url
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.toBarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterBegrunnelseForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterFritekstForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taBehanding
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.Søknad
import org.intellij.lang.annotations.Language

interface SendRevurderingTilBeslutningBuilder {

    /** Oppretter ny sak, søknad og behandling. */
    @Suppress("unused")
    suspend fun ApplicationTestBuilder.sendRevurderingStansTilBeslutning(
        tac: TestApplicationContext,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    ): Tuple4<Sak, Søknad, BehandlingId, String> {
        val (sak, søknad, behandling) = startRevurderingStans(tac)
        val sakId = sak.id
        val behandlingId = behandling.id
        oppdaterFritekstForBehandlingId(tac, sakId, behandlingId, saksbehandler)
        oppdaterBegrunnelseForBehandlingId(tac, sakId, behandlingId, saksbehandler)
        taBehanding(tac, sak.id, behandlingId, saksbehandler)
        return Tuple4(
            sak,
            søknad,
            behandlingId,
            sendRevurderingStansTilBeslutningForBehandlingId(
                tac,
                sakId,
                behandlingId,
                saksbehandler,
                stansperiode = søknad.vurderingsperiode(),
                valgteHjemler = nonEmptyListOf("Alder"),
            ),
        )
    }

    /** Oppretter ny sak, søknad og behandling. */
    suspend fun ApplicationTestBuilder.sendRevurderingInnvilgelseTilBeslutning(
        tac: TestApplicationContext,
        søknadsbehandlingVirkningsperiode: Periode = Periode(1.april(2025), 10.april(2025)),
        revurderingVirkningsperiode: Periode = søknadsbehandlingVirkningsperiode.plusTilOgMed(14L),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    ): Tuple4<Sak, Søknad, Søknadsbehandling, String> {
        val (sak, søknad, søknadsbehandling, revurdering) = startRevurderingInnvilgelse(
            tac,
            søknadsbehandlingVirkningsperiode = søknadsbehandlingVirkningsperiode,
            revurderingVirkningsperiode = revurderingVirkningsperiode,
        )

        return Tuple4(
            sak,
            søknad,
            søknadsbehandling,
            sendRevurderingInnvilgelseTilBeslutningForBehandlingId(
                tac,
                sak.id,
                revurdering.id,
                saksbehandler,
                innvilgelsesperiode = revurderingVirkningsperiode,
                eksternDeltagelseId = søknad.tiltak.id,
            ),
        )
    }

    /** Forventer at det allerede finnes en behandling med status `UNDER_BEHANDLING` */
    suspend fun ApplicationTestBuilder.sendRevurderingStansTilBeslutningForBehandlingId(
        tac: TestApplicationContext,
        sakId: SakId,
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        fritekstTilVedtaksbrev: String = "fritekst",
        begrunnelseVilkårsvurdering: String = "begrunnelse",
        stansperiode: Periode,
        valgteHjemler: Nel<String>,
    ): String {
        defaultRequest(
            HttpMethod.Companion.Post,
            url {
                protocol = URLProtocol.Companion.HTTPS
                path("/sak/$sakId/revurdering/$behandlingId/sendtilbeslutning")
            },
            jwt = tac.jwtGenerator.createJwtForSaksbehandler(
                saksbehandler = saksbehandler,
            ),
        ) {
            setBody(
                @Language("JSON")
                """
                {
                    "type": "STANS",
                    "begrunnelse": "$begrunnelseVilkårsvurdering",
                    "stans": {
                        "stansFraOgMed": "${stansperiode.fraOgMed}",
                        "valgteHjemler": [${valgteHjemler.joinToString(separator = ",", prefix = "\"", postfix = "\"")}]
                    }
                }
                """.trimIndent(),
            )
        }.apply {
            val bodyAsText = this.bodyAsText()
            withClue(
                """
                    Response details:
                    Status: ${this.status}
                    Content-Type: ${this.contentType()}
                    Body: $bodyAsText
                """.trimMargin(),
            ) {
                status shouldBe HttpStatusCode.Companion.OK
            }
            return bodyAsText
        }
    }

    /** Forventer at det allerede finnes en behandling med status `UNDER_BEHANDLING` */
    suspend fun ApplicationTestBuilder.sendRevurderingInnvilgelseTilBeslutningForBehandlingId(
        tac: TestApplicationContext,
        sakId: SakId,
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        fritekstTilVedtaksbrev: String = "fritekst",
        begrunnelseVilkårsvurdering: String = "begrunnelse",
        eksternDeltagelseId: String = "TA12345",
        barnetillegg: Barnetillegg? = null,
        innvilgelsesperiode: Periode,
        antallDagerPerMeldeperiode: SammenhengendePeriodisering<AntallDagerForMeldeperiode> = SammenhengendePeriodisering(
            AntallDagerForMeldeperiode.default,
            innvilgelsesperiode,
        ),
    ): String {
        defaultRequest(
            HttpMethod.Companion.Post,
            url {
                protocol = URLProtocol.Companion.HTTPS
                path("/sak/$sakId/revurdering/$behandlingId/sendtilbeslutning")
            },
            jwt = tac.jwtGenerator.createJwtForSaksbehandler(
                saksbehandler = saksbehandler,
            ),
        ) {
            setBody(
                //language=JSON
                """
                {
                    "type": "REVURDERING_INNVILGELSE",
                    "begrunnelse": "$begrunnelseVilkårsvurdering",
                    "innvilgelse": {
                        "innvilgelsesperiode": {
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
                         "antallDagerPerMeldeperiodeForPerioder": [
                          {
                            "antallDagerPerMeldeperiode": 10,
                            "periode": {
                              "fraOgMed": "${innvilgelsesperiode.fraOgMed}",
                              "tilOgMed": "${innvilgelsesperiode.tilOgMed}"
                            }
                          }
                        ],
                        "barnetillegg": ${if (barnetillegg == null) null else serialize(barnetillegg.toBarnetilleggDTO())}
                    }
                }
                """.trimIndent(),
            )
        }.apply {
            val bodyAsText = this.bodyAsText()
            withClue(
                """
                    Response details:
                    Status: ${this.status}
                    Content-Type: ${this.contentType()}
                    Body: $bodyAsText
                """.trimMargin(),
            ) {
                status shouldBe HttpStatusCode.Companion.OK
                bodyAsText shouldContain "id"
            }
            return bodyAsText
        }
    }
}
