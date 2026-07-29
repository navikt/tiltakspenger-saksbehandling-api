package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.start

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.libs.periodisering.tilIkkeTomPeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingstype
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Omgjøringsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgStartRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgStartRevurderingOmgjøring
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgStartRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterOmgjøringInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterOmgjøringOpphør
import org.junit.jupiter.api.Test

internal class StartRevurderingTest {
    @Test
    fun `kan starte revurdering stans`() {
        withTestApplicationContext { tac ->
            val (sak, _, _, revurdering) = iverksettSøknadsbehandlingOgStartRevurderingStans(tac)
            revurdering.shouldBeInstanceOf<Revurdering>()
            revurdering.behandlingstype shouldBe Behandlingstype.REVURDERING
            revurdering.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
            revurdering.resultat.shouldBeInstanceOf<Revurderingsresultat.Stans>()
            revurdering.sakId shouldBe sak.id
            revurdering.fritekstTilVedtaksbrev shouldBe null
            revurdering.begrunnelseVilkårsvurdering shouldBe null
            revurdering.saksbehandler shouldBe "Z12345"
            revurdering.saksnummer shouldBe sak.saksnummer
            revurdering.vedtaksperiode shouldBe null
            revurdering.attesteringer shouldBe emptyList()
            revurdering.saksopplysninger.shouldNotBeNull()
        }
    }

    @Test
    fun `kan starte revurdering innvilgelse`() {
        withTestApplicationContext { tac ->
            val (sak, _, _, revurdering) = iverksettSøknadsbehandlingOgStartRevurderingInnvilgelse(tac)
            revurdering.shouldBeInstanceOf<Revurdering>()
            revurdering.behandlingstype shouldBe Behandlingstype.REVURDERING
            revurdering.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
            revurdering.resultat.shouldBeInstanceOf<Revurderingsresultat.Innvilgelse>()
            revurdering.sakId shouldBe sak.id
            revurdering.fritekstTilVedtaksbrev shouldBe null
            revurdering.begrunnelseVilkårsvurdering shouldBe null
            revurdering.saksbehandler shouldBe "Z12345"
            revurdering.saksnummer shouldBe sak.saksnummer
            revurdering.vedtaksperiode shouldBe null
            revurdering.attesteringer shouldBe emptyList()
            revurdering.saksopplysninger.shouldNotBeNull()
        }
    }

    @Test
    fun `kan starte revurdering omgjøring`() {
        withTestApplicationContext { tac ->
            val innvilgelsesperiode = 3 til 10.april(2025)

            val (sak, _, _, omgjøring) = iverksettSøknadsbehandlingOgStartRevurderingOmgjøring(
                tac,
                søknadsbehandlingInnvilgelsesperioder = innvilgelsesperioder(innvilgelsesperiode),
            )!!

            omgjøring.shouldBeInstanceOf<Revurdering>()
            omgjøring.behandlingstype shouldBe Behandlingstype.REVURDERING
            omgjøring.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
            omgjøring.resultat.shouldBeInstanceOf<Omgjøringsresultat.OmgjøringIkkeValgt>()
            omgjøring.sakId shouldBe sak.id
            omgjøring.fritekstTilVedtaksbrev shouldBe null
            omgjøring.begrunnelseVilkårsvurdering shouldBe null
            omgjøring.saksbehandler shouldBe "Z12345"
            omgjøring.saksnummer shouldBe sak.saksnummer
            omgjøring.vedtaksperiode shouldBe null
            omgjøring.resultat.vedtaksperiode shouldBe null
        }
    }

    @Test
    fun `revurdering til omgjøring - tiltaksdeltakelse har krympet før start`() {
        withTestApplicationContext { tac ->
            val tiltaksdeltakelse = tiltaksdeltakelse(1 til 10.april(2025))
            val (sak, _, rammevedtakSøknadsbehandling, omgjøring) = iverksettSøknadsbehandlingOgStartRevurderingOmgjøring(
                tac = tac,
                søknadsbehandlingInnvilgelsesperioder = innvilgelsesperioder(periode = 1 til 10.april(2025), valgtTiltaksdeltakelse = tiltaksdeltakelse),
                oppdatertTiltaksdeltakelse = tiltaksdeltakelse.copy(deltakelseFraOgMed = 2.april(2025), deltakelseTilOgMed = 9.april(2025)),
            )!!
            rammevedtakSøknadsbehandling.rammebehandling.vedtaksperiode shouldBe (1 til 10.april(2025))
            rammevedtakSøknadsbehandling.rammebehandling.innvilgelsesperioder!!.totalPeriode shouldBe (
                1 til 10.april(
                    2025,
                )
                )
            rammevedtakSøknadsbehandling.rammebehandling.saksopplysninger.tiltaksdeltakelser.single().periode shouldBe (
                1 til 10.april(
                    2025,
                )
                )

            // Skal feile med innvilgelseperiode over utdatert tiltaksperiode
            oppdaterOmgjøringInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = omgjøring.id,
                vedtaksperiode = rammevedtakSøknadsbehandling.periode,
                innvilgelsesperioder = innvilgelsesperioder(rammevedtakSøknadsbehandling.periode),
                forventet = ForventetRespons(500, contentType = "application/json; charset=UTF-8"),
            )

            val (_, oppdatertOmgjøring) = oppdaterOmgjøringInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = omgjøring.id,
                vedtaksperiode = rammevedtakSøknadsbehandling.periode,
                innvilgelsesperioder = innvilgelsesperioder(2 til 9.april(2025)),
            )

            oppdatertOmgjøring.saksopplysninger.tiltaksdeltakelser.single().periode shouldBe (2 til 9.april(2025))
            oppdatertOmgjøring.vedtaksperiode shouldBe (1 til 10.april(2025))
            oppdatertOmgjøring.innvilgelsesperioder!!.totalPeriode shouldBe (2 til 9.april(2025))
            oppdatertOmgjøring.barnetillegg shouldBe Barnetillegg(
                periodisering = SammenhengendePeriodisering(
                    rammevedtakSøknadsbehandling.barnetillegg!!.periodisering.verdier.single(),
                    (2 til 9.april(2025)),
                ),
                begrunnelse = rammevedtakSøknadsbehandling.rammebehandling.barnetillegg!!.begrunnelse,
            )
            oppdatertOmgjøring.valgteTiltaksdeltakelser shouldBe listOf(
                PeriodeMedVerdi(
                    oppdatertOmgjøring.saksopplysninger.tiltaksdeltakelser.single(),
                    (2 til 9.april(2025)),
                ),
            ).tilIkkeTomPeriodisering()
            oppdatertOmgjøring.antallDagerPerMeldeperiode shouldBe SammenhengendePeriodisering(
                rammevedtakSøknadsbehandling.antallDagerPerMeldeperiode!!.verdier.single(),
                (2 til 9.april(2025)),
            )
            oppdatertOmgjøring.erFerdigutfylt() shouldBe true

            // Forsikrer oss om at vi ikke har brutt noen init-regler i Sak.kt.
            tac.sakContext.sakService.hentForSakId(sakId = oppdatertOmgjøring.sakId).rammebehandlinger[1] shouldBe oppdatertOmgjøring
        }
    }

    @Test
    fun `revurdering til omgjøring - tiltaksdeltakelse har økt før start`() {
        withTestApplicationContext { tac ->
            val tiltaksdeltakelse = tiltaksdeltakelse(2 til 9.april(2025))
            val (sak, _, rammevedtakSøknadsbehandling, omgjøring) = iverksettSøknadsbehandlingOgStartRevurderingOmgjøring(
                tac = tac,
                søknadsbehandlingInnvilgelsesperioder = innvilgelsesperioder(periode = 2 til 9.april(2025), valgtTiltaksdeltakelse = tiltaksdeltakelse),
                oppdatertTiltaksdeltakelse = tiltaksdeltakelse.copy(deltakelseFraOgMed = 1.april(2025), deltakelseTilOgMed = 10.april(2025)),
            )!!
            rammevedtakSøknadsbehandling.rammebehandling.vedtaksperiode shouldBe (2 til 9.april(2025))
            rammevedtakSøknadsbehandling.rammebehandling.innvilgelsesperioder!!.totalPeriode shouldBe (
                2 til 9.april(
                    2025,
                )
                )
            rammevedtakSøknadsbehandling.rammebehandling.saksopplysninger.tiltaksdeltakelser.single().periode shouldBe (
                2 til 9.april(
                    2025,
                )
                )

            val (_, oppdatertOmgjøring) = oppdaterOmgjøringInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = omgjøring.id,
                vedtaksperiode = 2 til 9.april(2025),
                innvilgelsesperioder = innvilgelsesperioder(2 til 9.april(2025)),
            )

            oppdatertOmgjøring.saksopplysninger.tiltaksdeltakelser.single().periode shouldBe (1 til 10.april(2025))
            oppdatertOmgjøring.vedtaksperiode shouldBe (2 til 9.april(2025))
            oppdatertOmgjøring.innvilgelsesperioder!!.totalPeriode shouldBe (2 til 9.april(2025))
            oppdatertOmgjøring.barnetillegg shouldBe Barnetillegg(
                periodisering = SammenhengendePeriodisering(
                    rammevedtakSøknadsbehandling.barnetillegg!!.periodisering.verdier.single(),
                    (2 til 9.april(2025)),
                ),
                begrunnelse = rammevedtakSøknadsbehandling.rammebehandling.barnetillegg!!.begrunnelse,
            )
            oppdatertOmgjøring.valgteTiltaksdeltakelser shouldBe listOf(
                PeriodeMedVerdi(
                    oppdatertOmgjøring.saksopplysninger.tiltaksdeltakelser.single(),
                    (2 til 9.april(2025)),
                ),
            ).tilIkkeTomPeriodisering()
            oppdatertOmgjøring.antallDagerPerMeldeperiode shouldBe SammenhengendePeriodisering(
                rammevedtakSøknadsbehandling.antallDagerPerMeldeperiode!!.verdier.single(),
                (2 til 9.april(2025)),
            )
            oppdatertOmgjøring.erFerdigutfylt() shouldBe true

            // Forsikrer oss om at vi ikke har brutt noen init-regler i Sak.kt.
            tac.sakContext.sakService.hentForSakId(sakId = oppdatertOmgjøring.sakId).rammebehandlinger[1] shouldBe oppdatertOmgjøring
        }
    }

    @Test
    fun `revurdering til omgjøring - tiltaksdeltakelse finnes ikke lenger`() {
        withTestApplicationContext { tac ->
            val (sak, _, _, omgjøring) = iverksettSøknadsbehandlingOgStartRevurderingOmgjøring(
                tac = tac,
                søknadsbehandlingInnvilgelsesperioder = innvilgelsesperioder(2 til 9.april(2025)),
                oppdatertTiltaksdeltakelse = null,
            )!!

            // Kan ikke innvilge uten tiltaksdeltakelse
            oppdaterOmgjøringInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = omgjøring.id,
                vedtaksperiode = 2 til 9.april(2025),
                forventet = ForventetRespons(500, contentType = "application/json; charset=UTF-8"),
            )

            // Men kan opphøre opprinnelig vedtaksperiode
            oppdaterOmgjøringOpphør(
                tac = tac,
                sakId = sak.id,
                behandlingId = omgjøring.id,
                vedtaksperiode = 2 til 9.april(2025),
                forventet = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
            )
        }
    }
}
