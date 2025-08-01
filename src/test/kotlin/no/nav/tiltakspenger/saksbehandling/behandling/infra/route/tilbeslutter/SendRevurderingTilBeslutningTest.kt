package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.tilbeslutter

import arrow.core.nonEmptyListOf
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.BehandlingResultatDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.BehandlingsstatusDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.RevurderingDTO
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.routes
import no.nav.tiltakspenger.saksbehandling.infra.setup.jacksonSerialization
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendRevurderingInnvilgelseTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendRevurderingStansTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingStans
import org.junit.jupiter.api.Test

class SendRevurderingTilBeslutningTest {
    @Test
    fun `kan sende revurdering stans til beslutning`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }
                val (sak, _, _, revurdering) = startRevurderingStans(tac)

                val stansdato = sak.vedtaksliste.førsteDagSomGirRett!!.plusDays(1)

                val responseBody = sendRevurderingStansTilBeslutningForBehandlingId(
                    tac = tac,
                    sakId = sak.id,
                    behandlingId = revurdering.id,
                    stansperiode = Periode(stansdato, stansdato.plusDays(10)),
                    valgteHjemler = nonEmptyListOf("Alder"),
                    forventetStatus = HttpStatusCode.OK,
                )

                val oppdatertRevurdering = objectMapper.readValue<RevurderingDTO>(responseBody)
                oppdatertRevurdering.status shouldBe BehandlingsstatusDTO.KLAR_TIL_BESLUTNING
            }
        }
    }

    @Test
    fun `send revurdering stans til beslutning feiler hvis stansdato er før innvilgelsesperioden`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }
                val (sak, _, _, revurdering) = startRevurderingStans(tac)

                val stansdato = sak.vedtaksliste.førsteDagSomGirRett!!.minusDays(2)

                sendRevurderingStansTilBeslutningForBehandlingId(
                    tac = tac,
                    sakId = sak.id,
                    behandlingId = revurdering.id,
                    stansperiode = Periode(stansdato, stansdato.plusDays(10)),
                    valgteHjemler = nonEmptyListOf("Alder"),
                    forventetStatus = HttpStatusCode.InternalServerError,
                )
            }
        }
    }

    @Test
    fun `send revurdering stans til beslutning feiler hvis stansdato er etter innvilgelsesperioden`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }
                val (sak, _, _, revurdering) = startRevurderingStans(tac)
                val stansdato = sak.sisteDagSomGirRett!!.plusDays(2)

                sendRevurderingStansTilBeslutningForBehandlingId(
                    tac = tac,
                    sakId = sak.id,
                    behandlingId = revurdering.id,
                    stansperiode = Periode(stansdato, stansdato.plusDays(10)),
                    valgteHjemler = nonEmptyListOf("Alder"),
                    forventetStatus = HttpStatusCode.InternalServerError,
                )
            }
        }
    }

    @Test
    fun `kan sende revurdering med forlenget innvilgelse til beslutning`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }

                val søknadsbehandlingVirkningsperiode = Periode(1.april(2025), 10.april(2025))
                val revurderingInnvilgelsesperiode = søknadsbehandlingVirkningsperiode.plusTilOgMed(14L)

                val (_, _, søknadsbehandling, jsonResponse) = sendRevurderingInnvilgelseTilBeslutning(
                    tac,
                    søknadsbehandlingVirkningsperiode = søknadsbehandlingVirkningsperiode,
                    revurderingVirkningsperiode = revurderingInnvilgelsesperiode,
                )

                val behandlingDTO = objectMapper.readValue<RevurderingDTO>(jsonResponse)

                behandlingDTO.status shouldBe BehandlingsstatusDTO.KLAR_TIL_BESLUTNING
                behandlingDTO.resultat shouldBe BehandlingResultatDTO.REVURDERING_INNVILGELSE

                val revurdering = tac.behandlingContext.behandlingRepo.hent(BehandlingId.fromString(behandlingDTO.id))

                revurdering.shouldBeInstanceOf<Revurdering>()

                revurdering.resultat shouldBe RevurderingResultat.Innvilgelse(
                    valgteTiltaksdeltakelser = revurdering.valgteTiltaksdeltakelser!!,
                    barnetillegg = søknadsbehandling.barnetillegg,
                    antallDagerPerMeldeperiode = SammenhengendePeriodisering(
                        AntallDagerForMeldeperiode.default,
                        revurderingInnvilgelsesperiode,
                    ),
                    utbetaling = null,
                )

                revurdering.virkningsperiode shouldBe revurderingInnvilgelsesperiode
            }
        }
    }
}
