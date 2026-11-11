package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.oppdater

import arrow.core.nonEmptyListOf
import arrow.core.nonEmptySetOf
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetBody
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.barnetillegg.BarnMedBarnetillegg
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.barnetillegg.BarnetilleggV1
import no.nav.tiltakspenger.saksbehandling.barnetillegg.BarnetilleggV2
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
import no.nav.tiltakspenger.saksbehandling.infra.route.harKode
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.barnMedBarnetillegg
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.barnetilleggV2
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterRevurderingInnvilgelse
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

            oppdatertBehandling.resultat shouldBe Søknadsbehandlingsresultat.IkkeValgt
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

            val barnetillegg = barnetilleggV2(
                begrunnelse = Begrunnelse.create("barnetillegg begrunnelse"),
                innvilgelsesperioder = nonEmptyListOf(nyInnvilgelsesperiode),
                barn = listOf(barnMedBarnetillegg(fornavn = "Ola"), barnMedBarnetillegg(fornavn = "Kari", perioder = listOf(nyInnvilgelsesperiode.minusTilOgMed(7)))),
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
    fun `barnetillegg for et barn fra en søknad som ikke finnes i saken avvises`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)
            val tiltaksdeltakelse = behandling.saksopplysninger.tiltaksdeltakelser.first()
            val innvilgelsesperiode = tiltaksdeltakelse.lukketPeriode!!

            val (_, _, responseJson) = oppdaterSøknadsbehandlingInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = behandling.id,
                innvilgelsesperioder = innvilgelsesperioder(innvilgelsesperiode, tiltaksdeltakelse),
                barnetillegg = barnetilleggV2(
                    innvilgelsesperioder = nonEmptyListOf(innvilgelsesperiode),
                    barn = listOf(barnMedBarnetillegg(kilde = BarnMedBarnetillegg.Kilde.Søknad(SøknadId.random()))),
                ),
                forventet = ForventetRespons(400, contentType = "application/json; charset=UTF-8"),
            )

            JSONObject(responseJson).getString("kode") shouldBe "ugyldig_barnetillegg"
            tac.behandlingContext.rammebehandlingRepo.hent(behandling.id).barnetillegg.shouldBeNull()
        }
    }

    @Test
    fun `barn med valgt periode uten perioder avvises`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)
            val tiltaksdeltakelse = behandling.saksopplysninger.tiltaksdeltakelser.first()
            val innvilgelsesperiode = tiltaksdeltakelse.lukketPeriode!!

            val (_, _, responseJson) = oppdaterSøknadsbehandlingInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = behandling.id,
                innvilgelsesperioder = innvilgelsesperioder(innvilgelsesperiode, tiltaksdeltakelse),
                barnetilleggJson = """
                    {
                      "begrunnelse": null,
                      "barn": [{"fnr": null, "fornavn": "Ola", "mellomnavn": null, "etternavn": "Nordmann", "fødselsdato": "2020-01-01", "kilde": "SAKSBEHANDLER", "søknadId": null, "perioder": []}]
                    }
                """.trimIndent(),
                forventet = ForventetRespons(400, contentType = "application/json; charset=UTF-8"),
            )

            JSONObject(responseJson).getString("kode") shouldBe "ugyldig_barnetillegg"
            JSONObject(responseJson).getString("melding") shouldBe "Det må velges minst én periode for barnet født 1. januar 2020."
            tac.behandlingContext.rammebehandlingRepo.hent(behandling.id).barnetillegg.shouldBeNull()
        }
    }

    @Test
    fun `barn med søknad som ikke passer med kilden avvises`() {
        withTestApplicationContext { tac ->
            val (sak, søknad, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)
            val tiltaksdeltakelse = behandling.saksopplysninger.tiltaksdeltakelser.first()
            val innvilgelsesperiode = tiltaksdeltakelse.lukketPeriode!!

            listOf(
                "SØKNAD" to null,
                "SØKNAD" to "\"ugyldig\"",
                "SAKSBEHANDLER" to "\"${søknad.id}\"",
            ).forEach { (kilde, søknadId) ->
                val (_, _, responseJson) = oppdaterSøknadsbehandlingInnvilgelse(
                    tac = tac,
                    sakId = sak.id,
                    behandlingId = behandling.id,
                    innvilgelsesperioder = innvilgelsesperioder(innvilgelsesperiode, tiltaksdeltakelse),
                    barnetilleggJson = """
                        {
                          "begrunnelse": null,
                          "barn": [{"fnr": null, "fornavn": "Ola", "mellomnavn": null, "etternavn": "Nordmann", "fødselsdato": "2020-01-01", "kilde": "$kilde", "søknadId": $søknadId, "perioder": null}]
                        }
                    """.trimIndent(),
                    forventet = ForventetRespons(400, contentType = "application/json; charset=UTF-8"),
                )

                JSONObject(responseJson).getString("kode") shouldBe "ugyldig_barnetillegg"
                JSONObject(responseJson).getString("melding") shouldBe "Barnet født 1. januar 2020 har ugyldig kilde eller søknad."
            }
            tac.behandlingContext.rammebehandlingRepo.hent(behandling.id).barnetillegg.shouldBeNull()
        }
    }

    @Test
    fun `valgte perioder som tilstøter hverandre lagres sammenslått`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)
            val tiltaksdeltakelse = behandling.saksopplysninger.tiltaksdeltakelser.first()
            val innvilgelsesperiode = tiltaksdeltakelse.lukketPeriode!!
            val første = innvilgelsesperiode.fraOgMed til innvilgelsesperiode.fraOgMed.plusDays(4)
            val andre = første.tilOgMed.plusDays(1) til første.tilOgMed.plusDays(5)

            oppdaterSøknadsbehandlingInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = behandling.id,
                innvilgelsesperioder = innvilgelsesperioder(innvilgelsesperiode, tiltaksdeltakelse),
                barnetilleggJson = """
                    {
                      "begrunnelse": null,
                      "barn": [{"fnr": null, "fornavn": "Ola", "mellomnavn": null, "etternavn": "Nordmann", "fødselsdato": "2020-01-01", "kilde": "SAKSBEHANDLER", "søknadId": null, "perioder": [
                        {"fraOgMed": "${andre.fraOgMed}", "tilOgMed": "${andre.tilOgMed}"},
                        {"fraOgMed": "${første.fraOgMed}", "tilOgMed": "${første.tilOgMed}"}
                      ]}]
                    }
                """.trimIndent(),
            )

            val barnetillegg = tac.behandlingContext.rammebehandlingRepo.hent(behandling.id).barnetillegg
                .shouldBeInstanceOf<BarnetilleggV2>()
            barnetillegg.barn.single().perioder shouldBe nonEmptyListOf(første.fraOgMed til andre.tilOgMed)
        }
    }

    @Test
    fun `eldre frontend kan lagre barnetillegg som antall barn per periode`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)
            val tiltaksdeltakelse = behandling.saksopplysninger.tiltaksdeltakelser.first()
            val innvilgelsesperiode = tiltaksdeltakelse.lukketPeriode!!
            val medBarn = innvilgelsesperiode.fraOgMed til innvilgelsesperiode.fraOgMed.plusDays(9)

            oppdaterSøknadsbehandlingInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = behandling.id,
                innvilgelsesperioder = innvilgelsesperioder(innvilgelsesperiode, tiltaksdeltakelse),
                barnetilleggJson = """
                    {
                      "begrunnelse": "To barn",
                      "perioder": [{"antallBarn": 2, "periode": {"fraOgMed": "${medBarn.fraOgMed}", "tilOgMed": "${medBarn.tilOgMed}"}}]
                    }
                """.trimIndent(),
            )

            val barnetillegg = tac.behandlingContext.rammebehandlingRepo.hent(behandling.id).barnetillegg
            barnetillegg.shouldBeInstanceOf<BarnetilleggV1>()
            barnetillegg.begrunnelse shouldBe Begrunnelse.create("To barn")
            barnetillegg.periodisering.perioderMedVerdi.toList() shouldBe listOf(
                PeriodeMedVerdi(AntallBarn(2), medBarn),
                PeriodeMedVerdi(AntallBarn.ZERO, medBarn.tilOgMed.plusDays(1) til innvilgelsesperiode.tilOgMed),
            )
        }
    }

    @Test
    fun `eldre frontend uten barnetillegg lagrer barnetillegg uten barn`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)
            val tiltaksdeltakelse = behandling.saksopplysninger.tiltaksdeltakelser.first()
            val innvilgelsesperiode = tiltaksdeltakelse.lukketPeriode!!

            oppdaterSøknadsbehandlingInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = behandling.id,
                innvilgelsesperioder = innvilgelsesperioder(innvilgelsesperiode, tiltaksdeltakelse),
                barnetilleggJson = """{"begrunnelse": null, "perioder": []}""",
            )

            tac.behandlingContext.rammebehandlingRepo.hent(behandling.id).barnetillegg shouldBe
                Barnetillegg.utenBarnetillegg(nonEmptyListOf(innvilgelsesperiode))
        }
    }

    @Test
    fun `barnetillegg fra eldre frontend utenfor innvilgelsesperioden avvises`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)
            val tiltaksdeltakelse = behandling.saksopplysninger.tiltaksdeltakelser.first()
            val innvilgelsesperiode = tiltaksdeltakelse.lukketPeriode!!

            val (_, _, responseJson) = oppdaterSøknadsbehandlingInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = behandling.id,
                innvilgelsesperioder = innvilgelsesperioder(innvilgelsesperiode, tiltaksdeltakelse),
                barnetilleggJson = """
                    {
                      "begrunnelse": null,
                      "perioder": [{"antallBarn": 1, "periode": {"fraOgMed": "${innvilgelsesperiode.fraOgMed}", "tilOgMed": "${innvilgelsesperiode.tilOgMed.plusDays(1)}"}}]
                    }
                """.trimIndent(),
                forventet = ForventetRespons(400, contentType = "application/json; charset=UTF-8"),
            )

            JSONObject(responseJson).getString("kode") shouldBe "ugyldig_barnetillegg"
            tac.behandlingContext.rammebehandlingRepo.hent(behandling.id).barnetillegg.shouldBeNull()
        }
    }

    @Test
    fun `kan oppdatere innvilget søknadsbehandling uten brev`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)

            val tiltaksdeltakelse = behandling.saksopplysninger.tiltaksdeltakelser.first()
            val nyInnvilgelsesperiode = tiltaksdeltakelse.lukketPeriode!!.minusTilOgMed(1)

            val barnetillegg = barnetilleggV2(
                begrunnelse = Begrunnelse.create("barnetillegg begrunnelse"),
                innvilgelsesperioder = nonEmptyListOf(nyInnvilgelsesperiode),
                barn = listOf(barnMedBarnetillegg(fornavn = "Ola"), barnMedBarnetillegg(fornavn = "Kari", perioder = listOf(nyInnvilgelsesperiode.minusTilOgMed(7)))),
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

            val (_, _, body) = oppdaterSøknadsbehandlingInnvilgelse(
                tac,
                sak.id,
                behandlingId,
                fritekstTilVedtaksbrev = "asdf",
                begrunnelseVilkårsvurdering = null,
                innvilgelsesperioder = innvilgelsesperioder(oppdatertTiltaksdeltakelsesPeriode),
                barnetillegg = Barnetillegg.utenBarnetillegg(oppdatertTiltaksdeltakelsesPeriode),
                forventet = ForventetRespons(400, contentType = "application/json; charset=UTF-8"),
            )
            body.shouldContainJsonKeyValue("$.kode", "innvilgelsen_passer_ikke_med_tiltaksdeltakelsene")

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
                    getString("melding") shouldContain "mangler rollen saksbehandler"
                }
            }

            tac.behandlingContext.rammebehandlingRepo.hent(behandling.id).also {
                it.fritekstTilVedtaksbrev.shouldBeNull()
            }
        }
    }

    @Test
    fun `kan ikke oppdatere søknadsbehandling som revurdering`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)

            oppdaterRevurderingInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = behandling.id,
                forventet = ForventetRespons(400, contentType = "application/json; charset=UTF-8"),
            ).also { (_, oppdatertBehandling, response) ->
                response harKode "feil_resultattype"
                oppdatertBehandling shouldBe behandling
            }
        }
    }
}
