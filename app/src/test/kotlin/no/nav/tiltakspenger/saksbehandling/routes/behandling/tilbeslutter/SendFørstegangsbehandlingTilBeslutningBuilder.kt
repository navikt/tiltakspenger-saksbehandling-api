package no.nav.tiltakspenger.saksbehandling.routes.behandling.tilbeslutter

import arrow.core.Tuple4
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.path
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.util.url
import no.nav.tiltakspenger.common.TestApplicationContext
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.felles.januar
import no.nav.tiltakspenger.saksbehandling.felles.mars
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBuilder.oppdaterBegrunnelseForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBuilder.oppdaterFritekstForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBuilder.startBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBuilder.taBehanding
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.Søknad
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.sak.Sak

interface SendFørstegangsbehandlingTilBeslutningBuilder {

    /** Oppretter ny sak, søknad og behandling. */
    suspend fun ApplicationTestBuilder.sendFørstegangsbehandlingTilBeslutning(
        tac: TestApplicationContext,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    ): Tuple4<Sak, Søknad, BehandlingId, String> {
        val (sak, søknad, behandling) = startBehandling(tac)
        val sakId = sak.id
        val behandlingId = behandling.id
        oppdaterFritekstForBehandlingId(tac, sakId, behandlingId, saksbehandler)
        oppdaterBegrunnelseForBehandlingId(tac, sakId, behandlingId, saksbehandler)
        taBehanding(tac, behandlingId, saksbehandler)
        return Tuple4(
            sak,
            søknad,
            behandlingId,
            sendFørstegangsbehandlingTilBeslutningForBehandlingId(
                tac,
                sakId,
                behandlingId,
                saksbehandler,
                innvilgelsesperiode = søknad.vurderingsperiode(),
                eksternDeltagelseId = søknad.tiltak.id,
            ),
        )
    }

    /** Forventer at det allerede finnes en behandling med status `UNDER_BEHANDLING` */
    suspend fun ApplicationTestBuilder.sendFørstegangsbehandlingTilBeslutningForBehandlingId(
        tac: TestApplicationContext,
        sakId: SakId,
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        fritekstTilVedtaksbrev: String = "fritekst",
        begrunnelseVilkårsvurdering: String = "begrunnelse",
        innvilgelsesperiode: Periode = Periode(1.januar(2023), 31.mars(2023)),
        eksternDeltagelseId: String,
    ): String {
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
            setBody(
                """
            {
                "fritekstTilVedtaksbrev": "$fritekstTilVedtaksbrev",
                "begrunnelseVilkårsvurdering": "$begrunnelseVilkårsvurdering",
                "innvilgelsesperiode": {
                    "fraOgMed": "${innvilgelsesperiode.fraOgMed}",
                    "tilOgMed": "${innvilgelsesperiode.tilOgMed}"
                },
                "valgteTiltaksdeltakelser": {
                    "valgteTiltaksdeltakelser": [
                        {
                            "eksternDeltagelseId": "$eksternDeltagelseId",
                            "periode": {
                                "fraOgMed": "${innvilgelsesperiode.fraOgMed}",
                                "tilOgMed": "${innvilgelsesperiode.tilOgMed}"
                            }
                        }
                    ]
                }
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
}
