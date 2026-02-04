package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.start

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.libs.periodisering.tilIkkeTomPeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingstype
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Omgjøringsresultat.OmgjøringInnvilgelse
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Søknadsbehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgStartRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgStartRevurderingOmgjøring
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgStartRevurderingStans
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

            val (sak, _, rammevedtakSøknadsbehandling, revurdering) = iverksettSøknadsbehandlingOgStartRevurderingOmgjøring(
                tac,
                søknadsbehandlingInnvilgelsesperioder = innvilgelsesperioder(innvilgelsesperiode),
            )!!
            val søknadsvedtakResultat =
                rammevedtakSøknadsbehandling.rammebehandling.resultat as `Søknadsbehandlingsresultat`.Innvilgelse
            val søknadsbehandling = rammevedtakSøknadsbehandling.rammebehandling as Søknadsbehandling
            revurdering.shouldBeInstanceOf<Revurdering>()
            revurdering.behandlingstype shouldBe Behandlingstype.REVURDERING
            revurdering.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
            revurdering.resultat.shouldBeInstanceOf<OmgjøringInnvilgelse>()
            revurdering.sakId shouldBe sak.id
            revurdering.fritekstTilVedtaksbrev shouldBe null
            revurdering.begrunnelseVilkårsvurdering shouldBe null
            revurdering.saksbehandler shouldBe "Z12345"
            revurdering.saksnummer shouldBe sak.saksnummer
            revurdering.vedtaksperiode shouldBe rammevedtakSøknadsbehandling.periode
            revurdering.resultat.vedtaksperiode shouldBe søknadsvedtakResultat.vedtaksperiode
            revurdering.resultat.vedtaksperiode shouldBe søknadsvedtakResultat.innvilgelsesperioder.totalPeriode
            revurdering.resultat.innvilgelsesperioder!!.totalPeriode shouldBe (innvilgelsesperiode)
            revurdering.barnetillegg shouldBe Barnetillegg(
                periodisering = SammenhengendePeriodisering(
                    søknadsbehandling.barnetillegg!!.periodisering.verdier.single(),
                    innvilgelsesperiode,
                ),
                begrunnelse = søknadsbehandling.barnetillegg.begrunnelse,
            )
            revurdering.antallDagerPerMeldeperiode shouldBe listOf(
                PeriodeMedVerdi(
                    søknadsbehandling.antallDagerPerMeldeperiode!!.verdier.single(),
                    innvilgelsesperiode,
                ),
            ).tilIkkeTomPeriodisering()
            revurdering.valgteTiltaksdeltakelser shouldBe listOf(
                PeriodeMedVerdi(
                    revurdering.saksopplysninger.tiltaksdeltakelser.single(),
                    innvilgelsesperiode,
                ),
            ).tilIkkeTomPeriodisering()
            revurdering.attesteringer shouldBe emptyList()
            revurdering.saksopplysninger.shouldNotBeNull()
            revurdering.erFerdigutfylt() shouldBe true
        }
    }

    @Test
    fun `revurdering til omgjøring - tiltaksdeltakelse har krympet før start`() {
        withTestApplicationContext { tac ->
            val (_, _, rammevedtakSøknadsbehandling, omgjøring) = iverksettSøknadsbehandlingOgStartRevurderingOmgjøring(
                tac = tac,
                søknadsbehandlingInnvilgelsesperioder = innvilgelsesperioder(1 til 10.april(2025)),
                oppdatertTiltaksdeltakelse = tiltaksdeltakelse(2 til 9.april(2025)),
            )!!
            rammevedtakSøknadsbehandling.rammebehandling.vedtaksperiode shouldBe (1 til 10.april(2025))
            rammevedtakSøknadsbehandling.rammebehandling.innvilgelsesperioder!!.totalPeriode shouldBe (1 til 10.april(2025))
            rammevedtakSøknadsbehandling.rammebehandling.saksopplysninger.tiltaksdeltakelser.single().periode shouldBe (
                1 til 10.april(
                    2025,
                )
                )
            omgjøring.saksopplysninger.tiltaksdeltakelser.single().periode shouldBe (2 til 9.april(2025))
            omgjøring.vedtaksperiode shouldBe (1 til 10.april(2025))
            omgjøring.innvilgelsesperioder!!.totalPeriode shouldBe (2 til 9.april(2025))
            omgjøring.barnetillegg shouldBe Barnetillegg(
                periodisering = SammenhengendePeriodisering(
                    rammevedtakSøknadsbehandling.barnetillegg!!.periodisering.verdier.single(),
                    (2 til 9.april(2025)),
                ),
                begrunnelse = rammevedtakSøknadsbehandling.rammebehandling.barnetillegg!!.begrunnelse,
            )
            omgjøring.valgteTiltaksdeltakelser shouldBe listOf(
                PeriodeMedVerdi(
                    omgjøring.saksopplysninger.tiltaksdeltakelser.single(),
                    (2 til 9.april(2025)),
                ),
            ).tilIkkeTomPeriodisering()
            omgjøring.antallDagerPerMeldeperiode shouldBe SammenhengendePeriodisering(
                rammevedtakSøknadsbehandling.antallDagerPerMeldeperiode!!.verdier.single(),
                (2 til 9.april(2025)),
            )
            omgjøring.erFerdigutfylt() shouldBe true

            // Forsikrer oss om at vi ikke har brutt noen init-regler i Sak.kt.
            tac.sakContext.sakService.hentForSakId(sakId = omgjøring.sakId).rammebehandlinger[1] shouldBe omgjøring
        }
    }

    @Test
    fun `revurdering til omgjøring - tiltaksdeltakelse har økt før start`() {
        withTestApplicationContext { tac ->
            val (_, _, rammevedtakSøknadsbehandling, omgjøring) = iverksettSøknadsbehandlingOgStartRevurderingOmgjøring(
                tac = tac,
                søknadsbehandlingInnvilgelsesperioder = innvilgelsesperioder(2 til 9.april(2025)),
                oppdatertTiltaksdeltakelse = tiltaksdeltakelse(1 til 10.april(2025)),
            )!!
            rammevedtakSøknadsbehandling.rammebehandling.vedtaksperiode shouldBe (2 til 9.april(2025))
            rammevedtakSøknadsbehandling.rammebehandling.innvilgelsesperioder!!.totalPeriode shouldBe (2 til 9.april(2025))
            rammevedtakSøknadsbehandling.rammebehandling.saksopplysninger.tiltaksdeltakelser.single().periode shouldBe (
                2 til 9.april(
                    2025,
                )
                )
            omgjøring.saksopplysninger.tiltaksdeltakelser.single().periode shouldBe (1 til 10.april(2025))
            omgjøring.vedtaksperiode shouldBe (2 til 9.april(2025))
            omgjøring.innvilgelsesperioder!!.totalPeriode shouldBe (2 til 9.april(2025))
            omgjøring.barnetillegg shouldBe Barnetillegg(
                periodisering = SammenhengendePeriodisering(
                    rammevedtakSøknadsbehandling.barnetillegg!!.periodisering.verdier.single(),
                    (2 til 9.april(2025)),
                ),
                begrunnelse = rammevedtakSøknadsbehandling.rammebehandling.barnetillegg!!.begrunnelse,
            )
            omgjøring.valgteTiltaksdeltakelser shouldBe listOf(
                PeriodeMedVerdi(
                    omgjøring.saksopplysninger.tiltaksdeltakelser.single(),
                    (2 til 9.april(2025)),
                ),
            ).tilIkkeTomPeriodisering()
            omgjøring.antallDagerPerMeldeperiode shouldBe SammenhengendePeriodisering(
                rammevedtakSøknadsbehandling.antallDagerPerMeldeperiode!!.verdier.single(),
                (2 til 9.april(2025)),
            )
            omgjøring.erFerdigutfylt() shouldBe true

            // Forsikrer oss om at vi ikke har brutt noen init-regler i Sak.kt.
            tac.sakContext.sakService.hentForSakId(sakId = omgjøring.sakId).rammebehandlinger[1] shouldBe omgjøring
        }
    }

    @Test
    fun `revurdering til omgjøring - tiltaksdeltakelse finnes ikke lenger`() {
        withTestApplicationContext { tac ->
            iverksettSøknadsbehandlingOgStartRevurderingOmgjøring(
                tac = tac,
                søknadsbehandlingInnvilgelsesperioder = innvilgelsesperioder(2 til 9.april(2025)),
                oppdatertTiltaksdeltakelse = null,
                forventetStatusForStartRevurdering = HttpStatusCode.BadRequest,
            ) shouldBe null
        }
    }
}
