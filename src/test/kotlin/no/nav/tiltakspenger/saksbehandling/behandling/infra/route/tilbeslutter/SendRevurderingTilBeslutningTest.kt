package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.tilbeslutter

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForStans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Omgjøringsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.RammebehandlingResultatTypeDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.RammebehandlingsstatusDTO
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.harKode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.IverksettMeldekortKommando
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.beslutter
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.objectmothers.førsteMeldekortIverksatt
import no.nav.tiltakspenger.saksbehandling.objectmothers.medTillattFeilutbetaling
import no.nav.tiltakspenger.saksbehandling.objectmothers.meldekortTilBeslutter
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjørRammevedtak
import no.nav.tiltakspenger.saksbehandling.omgjøring.Omgjøringsgrad
import no.nav.tiltakspenger.saksbehandling.omgjøring.Omgjøringsperiode
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettOmgjøringOpphør
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgStartRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterOmgjøringOpphør
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendRevurderingInnvilgelseTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendRevurderingTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingOmgjøring
import org.json.JSONObject
import org.junit.jupiter.api.Test

class SendRevurderingTilBeslutningTest {
    @Test
    fun `kan sende revurdering stans til beslutning`() {
        withTestApplicationContext { tac ->
            val (sak, _, _, revurdering) = iverksettSøknadsbehandlingOgStartRevurderingStans(tac)

            val stansFraOgMed = sak.førsteDagSomGirRett!!.plusDays(1)

            oppdaterRevurderingStans(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                begrunnelseVilkårsvurdering = null,
                fritekstTilVedtaksbrev = null,
                valgteHjemler = setOf(HjemmelForStans.Alder),
                stansFraOgMed = stansFraOgMed,
                harValgtStansFraFørsteDagSomGirRett = false,
            )

            val responseBody = sendRevurderingTilBeslutningForBehandlingId(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                forventetStatus = HttpStatusCode.OK,
            )

            JSONObject(responseBody).getString("status") shouldBe RammebehandlingsstatusDTO.KLAR_TIL_BESLUTNING.name
        }
    }

    @Test
    fun `kan sende revurdering med forlenget innvilgelse til beslutning`() {
        withTestApplicationContext { tac ->
            val søknadsbehandlingInnvilgelsesperiode = 1 til 10.april(2025)
            val revurderingInnvilgelsesperiode = søknadsbehandlingInnvilgelsesperiode.plusTilOgMed(14L)

            val (sak, _, rammevedtakSøknadsbehandling, jsonResponse) = sendRevurderingInnvilgelseTilBeslutning(
                tac,
                søknadsbehandlingInnvilgelsesperioder = innvilgelsesperioder(søknadsbehandlingInnvilgelsesperiode),
                revurderingInnvilgelsesperioder = innvilgelsesperioder(revurderingInnvilgelsesperiode),
            )
            val søknadsbehandling = rammevedtakSøknadsbehandling.rammebehandling as Søknadsbehandling

            JSONObject(jsonResponse).getString("status") shouldBe RammebehandlingsstatusDTO.KLAR_TIL_BESLUTNING.name
            JSONObject(jsonResponse).getString("resultat") shouldBe RammebehandlingResultatTypeDTO.REVURDERING_INNVILGELSE.name

            val revurdering =
                tac.behandlingContext.rammebehandlingRepo.hent(
                    BehandlingId.fromString(
                        JSONObject(jsonResponse).getString(
                            "id",
                        ),
                    ),
                )

            revurdering.shouldBeInstanceOf<Revurdering>()
            val søknadsbehandlingsvedtak = sak.rammevedtaksliste.single()

            revurdering.resultat shouldBe Revurderingsresultat.Innvilgelse(
                barnetillegg = Barnetillegg(
                    periodisering = søknadsbehandling.barnetillegg!!.periodisering.nyPeriode(
                        revurderingInnvilgelsesperiode,
                        defaultVerdiDersomDenMangler = søknadsbehandling.barnetillegg.periodisering.verdier.first(),
                    ),
                    begrunnelse = søknadsbehandling.barnetillegg.begrunnelse,
                ),
                innvilgelsesperioder = innvilgelsesperioder(
                    periode = revurderingInnvilgelsesperiode,
                    valgtTiltaksdeltakelse = revurdering.valgteTiltaksdeltakelser!!.single().verdi,
                    antallDagerPerMeldeperiode = AntallDagerForMeldeperiode.default,
                ),
                omgjørRammevedtak = OmgjørRammevedtak(
                    Omgjøringsperiode(
                        rammevedtakId = søknadsbehandlingsvedtak.id,
                        periode = 1 til 10.april(2025),
                        omgjøringsgrad = Omgjøringsgrad.HELT,
                    ),
                ),
            )

            revurdering.vedtaksperiode shouldBe revurderingInnvilgelsesperiode
        }
    }

    @Test
    fun `kan ikke sende omgjøring uten resultat til beslutning`() {
        withTestApplicationContext { tac ->
            val (sak, _, søknadsvedtak) = iverksettSøknadsbehandling(
                tac,
            )

            val (_, omgjøring) = startRevurderingOmgjøring(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = søknadsvedtak.id,
            )!!

            sendRevurderingTilBeslutningForBehandlingId(
                tac = tac,
                sakId = sak.id,
                behandlingId = omgjøring.id,
                forventetStatus = HttpStatusCode.InternalServerError,
            )

            val behandling = tac.behandlingContext.rammebehandlingRepo.hent(omgjøring.id)

            behandling.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
            behandling.resultat.shouldBeInstanceOf<Omgjøringsresultat.OmgjøringIkkeValgt>()
        }
    }

    @Test
    fun `kan ikke sende til beslutning dersom beregning av utbetaling er endret`() {
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

                iverksettRevurderingStans(
                    tac = tac,
                    sakId = sak.id,
                    harValgtStansFraFørsteDagSomGirRett = true,
                )

                val response = sendRevurderingTilBeslutningForBehandlingId(
                    tac = tac,
                    sakId = sak.id,
                    behandlingId = omgjøring.id,
                    forventetStatus = HttpStatusCode.Conflict,
                )

                response harKode "simulering_endret"
            }
        }
    }

    @Test
    fun `kan ikke sende til beslutning dersom beregning av utbetaling er endret fra null`() {
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

                val response = sendRevurderingTilBeslutningForBehandlingId(
                    tac = tac,
                    sakId = sak.id,
                    behandlingId = omgjøring.id,
                    forventetStatus = HttpStatusCode.Conflict,
                )

                response harKode "simulering_endret"
            }
        }
    }

    @Test
    fun `kan ikke sende til beslutning dersom beregning av utbetaling er endret til null`() {
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

                // En annen omgjøring iverksettes i mellomtiden, som opphører samme periode som den første.
                // Beregningen av første omgjøring vil da være endret til null, og vi skal ikke kunne sende den til beslutning.
                iverksettOmgjøringOpphør(
                    tac = tac,
                    sakId = sak.id,
                    vedtaksperiode = søknadvedtak.periode,
                    rammevedtakIdSomOmgjøres = søknadvedtak.id,
                )

                val response = sendRevurderingTilBeslutningForBehandlingId(
                    tac = tac,
                    sakId = sak.id,
                    behandlingId = omgjøring.id,
                    forventetStatus = HttpStatusCode.Conflict,
                )

                response harKode "simulering_endret"
            }
        }
    }
}
