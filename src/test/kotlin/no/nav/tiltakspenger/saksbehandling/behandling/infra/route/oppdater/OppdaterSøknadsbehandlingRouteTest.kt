package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.oppdater

import arrow.core.nonEmptyListOf
import arrow.core.nonEmptySetOf
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetBody
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.barnetillegg.utenBarnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Prioritering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Søknadsbehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Søknadsbehandlingsresultat.Avslag
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.barnetillegg
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterSøknadsbehandlingAvslag
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterSøknadsbehandlingIkkeValgt
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterSøknadsbehandlingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendSøknadsbehandlingTilBeslutning
import org.json.JSONObject
import org.junit.jupiter.api.Test

class OppdaterSøknadsbehandlingRouteTest {
    @Test
    fun `kan oppdatere uten valgt resultat`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)

            oppdaterSøknadsbehandlingIkkeValgt(
                tac = tac,
                sakId = sak.id,
                behandlingId = behandling.id,
                fritekstTilVedtaksbrev = "ny brevtekst",
                begrunnelseVilkårsvurdering = "ny begrunnelse",
            )

            val oppdatertBehandling = tac.behandlingContext.rammebehandlingRepo.hent(behandling.id)

            oppdatertBehandling.resultat.shouldBeNull()
            oppdatertBehandling.vedtaksperiode.shouldBeNull()
            oppdatertBehandling.fritekstTilVedtaksbrev!!.verdi shouldBe "ny brevtekst"
            oppdatertBehandling.begrunnelseVilkårsvurdering!!.verdi shouldBe "ny begrunnelse"
        }
    }

    @Test
    fun `kan oppdatere innvilget søknadsbehandling med brev`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)

            val tiltaksdeltakelse = behandling.saksopplysninger.tiltaksdeltakelser.first()
            val nyInnvilgelsesperiode = tiltaksdeltakelse.lukketPeriode!!.minusTilOgMed(1)

            val barnetillegg = barnetillegg(
                begrunnelse = Begrunnelse.create("barnetillegg begrunnelse"),
                periode = nyInnvilgelsesperiode,
                antallBarn = AntallBarn(1),
            )

            val antallDager = SammenhengendePeriodisering(
                AntallDagerForMeldeperiode(DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE),
                nyInnvilgelsesperiode,
            )

            val nyeInnvilgelsesperioder = innvilgelsesperioder(nyInnvilgelsesperiode, tiltaksdeltakelse)

            oppdaterSøknadsbehandlingInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = behandling.id,
                fritekstTilVedtaksbrev = "ny brevtekst",
                begrunnelseVilkårsvurdering = "ny begrunnelse",
                innvilgelsesperioder = nyeInnvilgelsesperioder,
                barnetillegg = barnetillegg,
                skalSendeVedtaksbrev = true,
            )

            val oppdatertBehandling = tac.behandlingContext.rammebehandlingRepo.hent(behandling.id)

            oppdatertBehandling.resultat.shouldBeInstanceOf<Søknadsbehandlingsresultat.Innvilgelse>()
            oppdatertBehandling.fritekstTilVedtaksbrev!!.verdi shouldBe "ny brevtekst"
            oppdatertBehandling.begrunnelseVilkårsvurdering!!.verdi shouldBe "ny begrunnelse"
            oppdatertBehandling.vedtaksperiode shouldBe nyInnvilgelsesperiode
            oppdatertBehandling.barnetillegg shouldBe barnetillegg
            oppdatertBehandling.antallDagerPerMeldeperiode shouldBe antallDager
            oppdatertBehandling.innvilgelsesperioder shouldBe nyeInnvilgelsesperioder
        }
    }

    @Test
    fun `kan oppdatere innvilget søknadsbehandling uten brev`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)

            val tiltaksdeltakelse = behandling.saksopplysninger.tiltaksdeltakelser.first()
            val nyInnvilgelsesperiode = tiltaksdeltakelse.lukketPeriode!!.minusTilOgMed(1)

            val barnetillegg = barnetillegg(
                begrunnelse = Begrunnelse.create("barnetillegg begrunnelse"),
                periode = nyInnvilgelsesperiode,
                antallBarn = AntallBarn(1),
            )

            val antallDager = SammenhengendePeriodisering(
                AntallDagerForMeldeperiode(DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE),
                nyInnvilgelsesperiode,
            )

            val nyeInnvilgelsesperioder = innvilgelsesperioder(nyInnvilgelsesperiode, tiltaksdeltakelse)

            oppdaterSøknadsbehandlingInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = behandling.id,
                fritekstTilVedtaksbrev = "ny brevtekst",
                begrunnelseVilkårsvurdering = "ny begrunnelse",
                innvilgelsesperioder = nyeInnvilgelsesperioder,
                barnetillegg = barnetillegg,
                skalSendeVedtaksbrev = false,
            )

            val oppdatertBehandling = tac.behandlingContext.rammebehandlingRepo.hent(behandling.id)

            oppdatertBehandling.resultat.shouldBeInstanceOf<Søknadsbehandlingsresultat.Innvilgelse>()
            oppdatertBehandling.fritekstTilVedtaksbrev!!.verdi shouldBe "ny brevtekst"
            oppdatertBehandling.begrunnelseVilkårsvurdering!!.verdi shouldBe "ny begrunnelse"
            oppdatertBehandling.vedtaksperiode shouldBe nyInnvilgelsesperiode
            oppdatertBehandling.barnetillegg shouldBe barnetillegg
            oppdatertBehandling.antallDagerPerMeldeperiode shouldBe antallDager
            oppdatertBehandling.innvilgelsesperioder shouldBe nyeInnvilgelsesperioder
            oppdatertBehandling.skalSendeVedtaksbrev shouldBe false
        }
    }

    @Test
    fun `kan oppdatere avslått søknadsbehandling med brev`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)

            oppdaterSøknadsbehandlingAvslag(
                tac = tac,
                sakId = sak.id,
                behandlingId = behandling.id,
                fritekstTilVedtaksbrev = "ny brevtekst",
                begrunnelseVilkårsvurdering = "ny begrunnelse",
                avslagsgrunner = setOf(Avslagsgrunnlag.DeltarIkkePåArbeidsmarkedstiltak),
                skalSendeVedtaksbrev = true,
            )

            val oppdatertBehandling = tac.behandlingContext.rammebehandlingRepo.hent(behandling.id)

            oppdatertBehandling.resultat.shouldBeInstanceOf<Avslag>()
            oppdatertBehandling.fritekstTilVedtaksbrev!!.verdi shouldBe "ny brevtekst"
            oppdatertBehandling.begrunnelseVilkårsvurdering!!.verdi shouldBe "ny begrunnelse"
            (oppdatertBehandling.resultat as Avslag).avslagsgrunner shouldBe nonEmptySetOf(Avslagsgrunnlag.DeltarIkkePåArbeidsmarkedstiltak)
        }
    }

    @Test
    fun `kan oppdatere avslått søknadsbehandling uten brev`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)

            oppdaterSøknadsbehandlingAvslag(
                tac = tac,
                sakId = sak.id,
                behandlingId = behandling.id,
                fritekstTilVedtaksbrev = "ny brevtekst",
                begrunnelseVilkårsvurdering = "ny begrunnelse",
                avslagsgrunner = setOf(Avslagsgrunnlag.DeltarIkkePåArbeidsmarkedstiltak),
                skalSendeVedtaksbrev = false,
            )

            val oppdatertBehandling = tac.behandlingContext.rammebehandlingRepo.hent(behandling.id)

            oppdatertBehandling.resultat.shouldBeInstanceOf<Avslag>()
            oppdatertBehandling.fritekstTilVedtaksbrev!!.verdi shouldBe "ny brevtekst"
            oppdatertBehandling.begrunnelseVilkårsvurdering!!.verdi shouldBe "ny begrunnelse"
            (oppdatertBehandling.resultat as Avslag).avslagsgrunner shouldBe nonEmptySetOf(Avslagsgrunnlag.DeltarIkkePåArbeidsmarkedstiltak)
            oppdatertBehandling.skalSendeVedtaksbrev shouldBe false
        }
    }

    @Test
    fun `oppdatering feiler hvis behandlingsperioden er utenfor deltakelsesperioden`() = runTest {
        withTestApplicationContext { tac ->
            val saksbehandler = saksbehandler()
            val (sak, _, behandling) = this.opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse(
                tac,
                saksbehandler = saksbehandler,
            )
            val behandlingId = behandling.id

            tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).also {
                it.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
                it.saksbehandler shouldBe saksbehandler.navIdent
                it.beslutter shouldBe null
            }

            val tiltaksdeltakelse = behandling.saksopplysninger.tiltaksdeltakelser.single()
            val tiltaksdeltakelsePeriode = tiltaksdeltakelse.lukketPeriode!!

            val oppdatertTiltaksdeltakelsesPeriode = tiltaksdeltakelsePeriode.minusFraOgMed(7)

            oppdaterSøknadsbehandlingInnvilgelse(
                tac,
                sak.id,
                behandlingId,
                fritekstTilVedtaksbrev = "asdf",
                begrunnelseVilkårsvurdering = null,
                innvilgelsesperioder = innvilgelsesperioder(oppdatertTiltaksdeltakelsesPeriode),
                barnetillegg = Barnetillegg.utenBarnetillegg(oppdatertTiltaksdeltakelsesPeriode),
                forventet = ForventetRespons(500, contentType = "application/json; charset=UTF-8"),
            )

            tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).also {
                it.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
                it.saksbehandler shouldBe saksbehandler.navIdent
                it.beslutter shouldBe null
                it.fritekstTilVedtaksbrev.shouldBeNull()
            }
        }
    }

    @Test
    fun `oppdatering feiler dersom søknadsbehandling opphører en tidligere innvilget periode`() = runTest {
        withTestApplicationContext { tac ->
            val saksbehandler = saksbehandler()
            val innvilgelsesperioder = innvilgelsesperioder(1.januar(2026) til 31.januar(2026))

            val (sak) = this.iverksettSøknadsbehandling(
                tac,
                innvilgelsesperioder = innvilgelsesperioder,
            )

            val (_, _, nesteSøknadsbehandling) = this.opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse(
                tac,
                saksbehandler = saksbehandler,
                sakId = sak.id,
                innvilgelsesperioder = innvilgelsesperioder,
            )

            val behandlingId = nesteSøknadsbehandling.id

            // Perioder med hull i innvilgelsen
            val nyeInnvilgelsesperioder = innvilgelsesperioder(
                (1.januar(2026) til 15.januar(2026)),
                (17.januar(2026) til 31.januar(2026)),
            )

            val (_, _, responseJson) = oppdaterSøknadsbehandlingInnvilgelse(
                tac,
                sakId = sak.id,
                behandlingId = nesteSøknadsbehandling.id,
                fritekstTilVedtaksbrev = "asdf",
                begrunnelseVilkårsvurdering = null,
                innvilgelsesperioder = nyeInnvilgelsesperioder,
                barnetillegg = Barnetillegg.utenBarnetillegg(
                    nonEmptyListOf(
                        1.januar(2026) til 15.januar(2026),
                        17.januar(2026) til 31.januar(2026),
                    ),
                ),
                forventet = ForventetRespons(400, contentType = "application/json; charset=UTF-8"),
            )

            JSONObject(responseJson).getString("kode") shouldBe "kan_ikke_opphøre"

            tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).also {
                it.innvilgelsesperioder shouldBe innvilgelsesperioder
            }
        }
    }

    /**
     * Prioriteringen er et felt i behandlingsskjemaet, så den settes og fjernes ved å lagre skjemaet.
     */
    @Test
    fun `kan prioritere behandlingen og fjerne prioriteringen igjen`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)

            val (_, prioritert, prioritertJson) = oppdaterSøknadsbehandlingInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = behandling.id,
                prioriteringBegrunnelse = "Bruker står uten inntekt",
            )

            prioritert.prioritering shouldBe Prioritering.create("Bruker står uten inntekt")
            JSONObject(prioritertJson).getJSONObject("prioritering").getJSONObject("begrunnelse").apply {
                getString("verdi") shouldBe "Bruker står uten inntekt"
                getBoolean("erSladdet") shouldBe false
            }

            val (_, ikkePrioritert, ikkePrioritertJson) = oppdaterSøknadsbehandlingAvslag(
                tac = tac,
                sakId = sak.id,
                behandlingId = behandling.id,
            )

            ikkePrioritert.prioritering.shouldBeNull()
            JSONObject(ikkePrioritertJson).isNull("prioritering") shouldBe true
        }
    }

    @Test
    fun `kan prioritere behandlingen uten valgt resultat og med avslag`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)

            oppdaterSøknadsbehandlingIkkeValgt(
                tac = tac,
                sakId = sak.id,
                behandlingId = behandling.id,
                prioriteringBegrunnelse = "Ikke valgt",
            ).second.prioritering shouldBe Prioritering.create("Ikke valgt")

            oppdaterSøknadsbehandlingAvslag(
                tac = tac,
                sakId = sak.id,
                behandlingId = behandling.id,
                prioriteringBegrunnelse = "Avslag",
            ).second.prioritering shouldBe Prioritering.create("Avslag")
        }
    }

    /**
     * En prioritering uten begrunnelse gir ingen mening, så klienten må sende null for å fjerne den.
     */
    @Test
    fun `prioritering med blank begrunnelse avvises`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)

            oppdaterSøknadsbehandlingIkkeValgt(
                tac = tac,
                sakId = sak.id,
                behandlingId = behandling.id,
                prioriteringBegrunnelse = "  ",
                forventet = ForventetRespons(
                    status = 400,
                    contentType = "application/json; charset=UTF-8",
                    body = ForventetBody.Json(
                        """{"melding":"Kunne ikke deserialisere request","kode":"ugyldig_request"}""",
                    ),
                ),
            ).second.prioritering.shouldBeNull()
        }
    }

    @Test
    fun `kan ikke oppdatere en behandling som er tildelt en annen saksbehandler`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)

            val (_, _, responseJson) = oppdaterSøknadsbehandlingIkkeValgt(
                tac = tac,
                sakId = sak.id,
                behandlingId = behandling.id,
                saksbehandler = ObjectMother.saksbehandler123(),
                forventet = ForventetRespons(400, contentType = "application/json; charset=UTF-8"),
            )

            JSONObject(responseJson).getString("kode") shouldBe "behandling_eies_av_annen_saksbehandler"
        }
    }

    @Test
    fun `kan ikke oppdatere en behandling som ikke er under behandling`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandlingId) = sendSøknadsbehandlingTilBeslutning(tac)

            val (_, _, responseJson) = oppdaterSøknadsbehandlingIkkeValgt(
                tac = tac,
                sakId = sak.id,
                behandlingId = behandlingId,
                forventet = ForventetRespons(400, contentType = "application/json; charset=UTF-8"),
            )

            JSONObject(responseJson).getString("kode") shouldBe "behandlingen_er_ikke_i_status_under_behandling"
        }
    }

    /**
     * Veileder og utvikler er leseroller og skal ikke kunne endre en behandling.
     * Rollesjekken skjer før tilgangskontroll og domenelogikk, så feilen er den samme uansett behandlingens tilstand.
     */
    @Test
    fun `veileder og utvikler får 403 ved oppdatering av behandling`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)

            listOf(ObjectMother.veileder(), ObjectMother.utvikler()).forEach { leserolle ->
                val (_, _, responseJson) = oppdaterSøknadsbehandlingIkkeValgt(
                    tac = tac,
                    sakId = sak.id,
                    behandlingId = behandling.id,
                    saksbehandler = leserolle,
                    forventet = ForventetRespons(403, contentType = "application/json; charset=UTF-8"),
                )

                JSONObject(responseJson).apply {
                    getString("kode") shouldBe "tilgang_nektet_krev_rolle"
                    getString("melding") shouldContain "mangler rollen SAKSBEHANDLER"
                }
            }

            tac.behandlingContext.rammebehandlingRepo.hent(behandling.id).also {
                it.fritekstTilVedtaksbrev.shouldBeNull()
            }
        }
    }
}
