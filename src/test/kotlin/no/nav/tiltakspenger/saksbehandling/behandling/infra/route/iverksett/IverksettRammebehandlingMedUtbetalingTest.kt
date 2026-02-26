package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.iverksett

import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.harKode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.IverksettMeldekortKommando
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.beslutter
import no.nav.tiltakspenger.saksbehandling.objectmothers.førsteMeldekortIverksatt
import no.nav.tiltakspenger.saksbehandling.objectmothers.medTillattFeilutbetaling
import no.nav.tiltakspenger.saksbehandling.objectmothers.meldekortTilBeslutter
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettOmgjøringOpphør
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterOmgjøringOpphør
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendRevurderingTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingOmgjøring
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taBehandling
import org.junit.jupiter.api.Test

class IverksettRammebehandlingMedUtbetalingTest {

    @Test
    fun `kan ikke iverksette dersom beregning av utbetaling er endret`() {
        withTestApplicationContext { tac ->
            medTillattFeilutbetaling {
                val sak = tac.førsteMeldekortIverksatt()

                val søknadvedtak = sak.rammevedtaksliste.first()

                val (_, omgjøring) = startRevurderingOmgjøring(
                    tac = tac,
                    sakId = sak.id,
                    rammevedtakIdSomOmgjøres = søknadvedtak.id,
                )!!

                oppdaterOmgjøringOpphør(
                    tac = tac,
                    sakId = sak.id,
                    behandlingId = omgjøring.id,
                    vedtaksperiode = søknadvedtak.periode,
                )

                sendRevurderingTilBeslutningForBehandlingId(
                    tac = tac,
                    sakId = sak.id,
                    behandlingId = omgjøring.id,
                )

                taBehandling(
                    tac = tac,
                    sakId = sak.id,
                    behandlingId = omgjøring.id,
                    saksbehandler = beslutter(),
                )

                iverksettRevurderingStans(
                    tac = tac,
                    sakId = sak.id,
                    harValgtStansFraFørsteDagSomGirRett = true,
                )

                iverksettForBehandlingId(
                    tac = tac,
                    sakId = sak.id,
                    behandlingId = omgjøring.id,
                    forventetStatus = HttpStatusCode.Conflict,
                ) {
                    it harKode "simulering_endret"
                }

                // Skal ikke fungere ved gjentatte forsøk heller!
                iverksettForBehandlingId(
                    tac = tac,
                    sakId = sak.id,
                    behandlingId = omgjøring.id,
                    forventetStatus = HttpStatusCode.Conflict,
                ) {
                    it harKode "simulering_endret"
                }
            }
        }
    }

    @Test
    fun `kan ikke iverksette dersom beregning av utbetaling er endret fra null`() {
        withTestApplicationContext { tac ->
            medTillattFeilutbetaling {
                val sak = tac.meldekortTilBeslutter()

                val søknadvedtak = sak.rammevedtaksliste.first()

                val (_, omgjøring) = startRevurderingOmgjøring(
                    tac = tac,
                    sakId = sak.id,
                    rammevedtakIdSomOmgjøres = søknadvedtak.id,
                )!!

                oppdaterOmgjøringOpphør(
                    tac = tac,
                    sakId = sak.id,
                    behandlingId = omgjøring.id,
                    vedtaksperiode = søknadvedtak.periode,
                )

                sendRevurderingTilBeslutningForBehandlingId(
                    tac = tac,
                    sakId = sak.id,
                    behandlingId = omgjøring.id,
                )

                taBehandling(
                    tac = tac,
                    sakId = sak.id,
                    behandlingId = omgjøring.id,
                    saksbehandler = beslutter(),
                )

                val meldekortId = sak.meldekortbehandlinger.first().id
                tac.meldekortContext.taMeldekortBehandlingService.taMeldekortBehandling(
                    sakId = sak.id,
                    meldekortId = meldekortId,
                    saksbehandler = beslutter(),
                )
                tac.meldekortContext.iverksettMeldekortService.iverksettMeldekort(
                    IverksettMeldekortKommando(
                        meldekortId = meldekortId,
                        sakId = sak.id,
                        beslutter = beslutter(),
                        correlationId = CorrelationId.generate(),
                    ),
                )

                iverksettForBehandlingId(
                    tac = tac,
                    sakId = sak.id,
                    behandlingId = omgjøring.id,
                    forventetStatus = HttpStatusCode.Conflict,
                ) {
                    it harKode "simulering_endret"
                }
            }
        }
    }

    @Test
    fun `kan ikke iverksette dersom beregning av utbetaling er endret til null`() {
        withTestApplicationContext { tac ->
            medTillattFeilutbetaling {
                val sak = tac.førsteMeldekortIverksatt()

                val søknadvedtak = sak.rammevedtaksliste.first()

                val (_, omgjøring) = startRevurderingOmgjøring(
                    tac = tac,
                    sakId = sak.id,
                    rammevedtakIdSomOmgjøres = søknadvedtak.id,
                )!!

                oppdaterOmgjøringOpphør(
                    tac = tac,
                    sakId = sak.id,
                    behandlingId = omgjøring.id,
                    vedtaksperiode = søknadvedtak.periode,
                )

                sendRevurderingTilBeslutningForBehandlingId(
                    tac = tac,
                    sakId = sak.id,
                    behandlingId = omgjøring.id,
                )

                taBehandling(
                    tac = tac,
                    sakId = sak.id,
                    behandlingId = omgjøring.id,
                    saksbehandler = beslutter(),
                )

                // En annen omgjøring iverksettes i mellomtiden, som opphører samme periode som den første.
                // Beregningen av første omgjøring vil da endres til null/ingen endring, og vi skal ikke kunne sende den til beslutning.
                // (den første omgjøringen vil forøvrig i dette tilfellet feile ved gjentatte forsøk også, ettersom perioden nå allerede er opphørt)
                iverksettOmgjøringOpphør(
                    tac = tac,
                    sakId = sak.id,
                    vedtaksperiode = søknadvedtak.periode,
                    rammevedtakIdSomOmgjøres = søknadvedtak.id,
                )

                iverksettForBehandlingId(
                    tac = tac,
                    sakId = sak.id,
                    behandlingId = omgjøring.id,
                    forventetStatus = HttpStatusCode.Conflict,
                ) {
                    it harKode "simulering_endret"
                }
            }
        }
    }
}
