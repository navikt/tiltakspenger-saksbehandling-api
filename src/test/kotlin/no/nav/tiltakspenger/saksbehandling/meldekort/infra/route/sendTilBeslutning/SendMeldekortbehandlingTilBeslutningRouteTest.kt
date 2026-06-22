package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.sendTilBeslutning

import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.MeldekortDagStatusDTO
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.saksbehandling.objectmothers.tilOppdatertMeldeperiodeDTO
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOmgjøringInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgSendMeldekortbehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettMeldekortbehandlingForSakId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettOgOppdaterMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendMeldekortbehandlingTilBeslutning
import org.junit.jupiter.api.Test

internal class SendMeldekortbehandlingTilBeslutningRouteTest {
    @Test
    fun `kan sende meldekortbehandling til beslutter`() {
        runTest {
            withTestApplicationContext { tac ->
                this.iverksettSøknadsbehandlingOgSendMeldekortbehandlingTilBeslutning(
                    tac = tac,
                    saksbehandler = saksbehandler("saksbehandler"),
                )!!
            }
        }
    }

    @Test
    fun `kan ikke sende meldekortbehandling til beslutter hvis ingen dager gir rett`() {
        runTest {
            withTestApplicationContext { tac ->
                val førstePeriode = 6.januar(2025) til 19.januar(2025)
                val andrePeriode = førstePeriode.plus14Dager()
                val totalPeriode = førstePeriode.fraOgMed til andrePeriode.tilOgMed

                val (sak) = iverksettSøknadsbehandlingOgOmgjøringInnvilgelse(
                    tac = tac,
                    søknadsbehandlingInnvilgelsesperioder = innvilgelsesperioder(totalPeriode),
                    omgjøringInnvilgelsesperioder = innvilgelsesperioder(andrePeriode),
                )!!

                val (_, meldekortbehandling) = this.opprettMeldekortbehandlingForSakId(
                    tac = tac,
                    sakId = sak.id,
                    kjedeId = sak.meldeperiodeKjeder.first().kjedeId,
                )!!

                meldekortbehandling.ingenDagerGirRett shouldBe true

                this.sendMeldekortbehandlingTilBeslutning(
                    tac = tac,
                    sakId = sak.id,
                    meldekortId = meldekortbehandling.id,
                    forventetStatus = HttpStatusCode.InternalServerError,
                )
            }
        }
    }

    @Test
    fun `kan ikke sende meldekortbehandling til beslutter hvis den har dager som ikke er besvart`() {
        runTest {
            withTestApplicationContext { tac ->
                val periode = 6.januar(2025) til 19.januar(2025)

                val (sak) = iverksettSøknadsbehandling(
                    tac = tac,
                    innvilgelsesperioder = innvilgelsesperioder(periode),
                )

                val (_, meldekortbehandling) = opprettOgOppdaterMeldekortbehandling(
                    tac = tac,
                    sakId = sak.id,
                    kjedeId = sak.meldeperiodeKjeder.first().kjedeId,
                    meldeperioder = listOf(
                        sak.meldeperiodeKjeder.first()
                            .hentSisteMeldeperiode()
                            .tilOppdatertMeldeperiodeDTO()
                            .let { meldeperiode ->
                                meldeperiode.copy(
                                    dager = meldeperiode.dager.mapIndexed { index, dag ->
                                        if (index == 0) {
                                            dag.copy(status = MeldekortDagStatusDTO.IKKE_BESVART)
                                        } else {
                                            dag
                                        }
                                    },
                                )
                            },
                    ),
                )!!

                this.sendMeldekortbehandlingTilBeslutning(
                    tac = tac,
                    sakId = sak.id,
                    meldekortId = meldekortbehandling.id,
                    forventetStatus = HttpStatusCode.BadRequest,
                    forventetJsonBody = """
                        {                                                
                          "kode": "meldeperiodene_er_ikke_utfylt",
                          "melding": "Meldeperiodene må være fullstendig utfylt for å kunne sende meldekortet til beslutter."
                         }
                    """.trimIndent(),
                )
            }
        }
    }
}
