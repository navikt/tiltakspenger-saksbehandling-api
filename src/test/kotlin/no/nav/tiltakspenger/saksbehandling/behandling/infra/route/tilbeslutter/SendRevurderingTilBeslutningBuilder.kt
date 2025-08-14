package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.tilbeslutter

import arrow.core.Tuple4
import arrow.core.nonEmptyListOf
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
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
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.OppdaterRevurderingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.ValgtHjemmelForStansDTO
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taBehanding
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.Søknad
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.route.TiltaksdeltakelsePeriodeDTO
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.route.toDTO

interface SendRevurderingTilBeslutningBuilder {

    /** Oppretter ny sak, søknad og behandling. */
    @Suppress("unused")
    suspend fun ApplicationTestBuilder.sendRevurderingStansTilBeslutning(
        tac: TestApplicationContext,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    ): Tuple4<Sak, Søknad, BehandlingId, String> {
        val (sak, søknad, søknadsbehandling, revurdering) = startRevurderingStans(tac)
        val sakId = sak.id
        val revurderingId = revurdering.id

        taBehanding(tac, sak.id, revurderingId, saksbehandler)

        oppdaterBehandling(
            tac = tac,
            sakId = sak.id,
            behandlingId = revurdering.id,
            oppdaterBehandlingDTO = OppdaterRevurderingDTO.Stans(
                begrunnelseVilkårsvurdering = null,
                fritekstTilVedtaksbrev = null,
                valgteHjemler = nonEmptyListOf(ValgtHjemmelForStansDTO.Alder),
                stansFraOgMed = søknadsbehandling.virkningsperiode!!.fraOgMed,
            ),
        )

        return Tuple4(
            sak,
            søknad,
            revurderingId,
            sendRevurderingStansTilBeslutningForBehandlingId(
                tac,
                sakId,
                revurderingId,
                saksbehandler,
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

        val tiltaksdeltagelse = revurdering.saksopplysninger.tiltaksdeltagelser.single()

        val antallDager = SammenhengendePeriodisering(
            AntallDagerForMeldeperiode(MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE),
            revurderingVirkningsperiode,
        )

        oppdaterBehandling(
            tac = tac,
            sakId = sak.id,
            behandlingId = revurdering.id,
            oppdaterBehandlingDTO = OppdaterRevurderingDTO.Innvilgelse(
                begrunnelseVilkårsvurdering = null,
                fritekstTilVedtaksbrev = null,
                innvilgelsesperiode = revurderingVirkningsperiode.toDTO(),
                valgteTiltaksdeltakelser = listOf(
                    TiltaksdeltakelsePeriodeDTO(
                        eksternDeltagelseId = tiltaksdeltagelse.eksternDeltagelseId,
                        periode = tiltaksdeltagelse.periode!!.toDTO(),
                    ),
                ),
                antallDagerPerMeldeperiodeForPerioder = antallDager.toDTO(),
                barnetillegg = null,
            ),
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
//                innvilgelsesperiode = revurderingVirkningsperiode,
//                eksternDeltagelseId = søknad.tiltak.id,
            ),
        )
    }

    /** Forventer at det allerede finnes en behandling med status `UNDER_BEHANDLING` */
    suspend fun ApplicationTestBuilder.sendRevurderingStansTilBeslutningForBehandlingId(
        tac: TestApplicationContext,
        sakId: SakId,
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        forventetStatus: HttpStatusCode = HttpStatusCode.OK,
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
        ).apply {
            val bodyAsText = this.bodyAsText()
            withClue(
                """
                    Response details:
                    Status: ${this.status}
                    Content-Type: ${this.contentType()}
                    Body: $bodyAsText
                """.trimMargin(),
            ) {
                status shouldBe forventetStatus
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
        forventetStatus: HttpStatusCode = HttpStatusCode.OK,
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
        ).apply {
            val bodyAsText = this.bodyAsText()
            withClue(
                """
                    Response details:
                    Status: ${this.status}
                    Content-Type: ${this.contentType()}
                    Body: $bodyAsText
                """.trimMargin(),
            ) {
                status shouldBe forventetStatus
            }
            return bodyAsText
        }
    }
}
