package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.start

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.libs.periodisering.til
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingstype
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingResultat
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingOmgjøring
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingStans
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.ValgteTiltaksdeltakelser
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import org.junit.jupiter.api.Test

internal class StartRevurderingTest {
    @Test
    fun `kan starte revurdering stans`() {
        withTestApplicationContext { tac ->
            val (sak, _, _, revurdering) = startRevurderingStans(tac)
            revurdering.shouldBeInstanceOf<Revurdering>()
            revurdering.behandlingstype shouldBe Behandlingstype.REVURDERING
            revurdering.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
            revurdering.resultat.shouldBeInstanceOf<RevurderingResultat.Stans>()
            revurdering.sakId shouldBe sak.id
            revurdering.fritekstTilVedtaksbrev shouldBe null
            revurdering.begrunnelseVilkårsvurdering shouldBe null
            revurdering.saksbehandler shouldBe "Z12345"
            revurdering.saksnummer shouldBe sak.saksnummer
            revurdering.virkningsperiode shouldBe null
            revurdering.attesteringer shouldBe emptyList()
            revurdering.saksopplysninger.shouldNotBeNull()
        }
    }

    @Test
    fun `kan starte revurdering innvilgelse`() {
        withTestApplicationContext { tac ->
            val (sak, _, _, revurdering) = startRevurderingInnvilgelse(tac)
            revurdering.shouldBeInstanceOf<Revurdering>()
            revurdering.behandlingstype shouldBe Behandlingstype.REVURDERING
            revurdering.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
            revurdering.resultat.shouldBeInstanceOf<RevurderingResultat.Innvilgelse>()
            revurdering.sakId shouldBe sak.id
            revurdering.fritekstTilVedtaksbrev shouldBe null
            revurdering.begrunnelseVilkårsvurdering shouldBe null
            revurdering.saksbehandler shouldBe "Z12345"
            revurdering.saksnummer shouldBe sak.saksnummer
            revurdering.virkningsperiode shouldBe null
            revurdering.attesteringer shouldBe emptyList()
            revurdering.saksopplysninger.shouldNotBeNull()
        }
    }

    @Test
    fun `kan starte revurdering omgjøring`() {
        withTestApplicationContext { tac ->
            val (sak, _, søknadsbehandling, revurdering) = startRevurderingOmgjøring(tac)
            val søknadsvedtak: Rammevedtak = sak.vedtaksliste.single() as Rammevedtak
            val søknadsvedtakResultat = søknadsvedtak.behandling.resultat as SøknadsbehandlingResultat.Innvilgelse
            revurdering.shouldBeInstanceOf<Revurdering>()
            revurdering.behandlingstype shouldBe Behandlingstype.REVURDERING
            revurdering.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
            revurdering.resultat.shouldBeInstanceOf<RevurderingResultat.Omgjøring>()
            revurdering.sakId shouldBe sak.id
            revurdering.fritekstTilVedtaksbrev shouldBe null
            revurdering.begrunnelseVilkårsvurdering shouldBe null
            revurdering.saksbehandler shouldBe "Z12345"
            revurdering.saksnummer shouldBe sak.saksnummer
            revurdering.virkningsperiode shouldBe søknadsvedtak.periode
            revurdering.resultat.virkningsperiode shouldBe søknadsvedtakResultat.virkningsperiode
            revurdering.resultat.virkningsperiode shouldBe søknadsvedtakResultat.innvilgelsesperiode
            revurdering.resultat.innvilgelsesperiode shouldBe (3 til 10.april(2025))
            revurdering.barnetillegg shouldBe Barnetillegg(
                periodisering = SammenhengendePeriodisering(
                    søknadsbehandling.barnetillegg!!.periodisering.verdier.single(),
                    (3 til 10.april(2025)),
                ),
                begrunnelse = søknadsbehandling.barnetillegg.begrunnelse,
            )
            revurdering.antallDagerPerMeldeperiode shouldBe SammenhengendePeriodisering(
                søknadsbehandling.antallDagerPerMeldeperiode!!.verdier.single(),
                (3 til 10.april(2025)),
            )
            revurdering.valgteTiltaksdeltakelser shouldBe ValgteTiltaksdeltakelser(
                periodisering = SammenhengendePeriodisering(
                    søknadsbehandling.valgteTiltaksdeltakelser!!.periodisering.verdier.single(),
                    (3 til 10.april(2025)),
                ),
            )
            revurdering.attesteringer shouldBe emptyList()
            revurdering.saksopplysninger.shouldNotBeNull()
            revurdering.erFerdigutfylt() shouldBe true
        }
    }

    @Test
    fun `revurdering til omgjøring - tiltaksdeltagelse har krympet før start`() {
        withTestApplicationContext { tac ->
            val (_, _, søknadsbehandling, omgjøring) = startRevurderingOmgjøring(
                tac = tac,
                søknadsbehandlingVirkningsperiode = 1 til 10.april(2025),
                oppdaterTiltaksdeltagelsesperiode = 2 til 9.april(2025),
            )
            søknadsbehandling.virkningsperiode shouldBe (1 til 10.april(2025))
            søknadsbehandling.innvilgelsesperiode shouldBe (1 til 10.april(2025))
            søknadsbehandling.saksopplysninger.tiltaksdeltagelser.single().periode shouldBe (1 til 10.april(2025))
            omgjøring!!.saksopplysninger.tiltaksdeltagelser.single().periode shouldBe (2 til 9.april(2025))
            omgjøring.virkningsperiode shouldBe (1 til 10.april(2025))
            omgjøring.innvilgelsesperiode shouldBe (2 til 9.april(2025))
            omgjøring.barnetillegg shouldBe Barnetillegg(
                periodisering = SammenhengendePeriodisering(
                    søknadsbehandling.barnetillegg!!.periodisering.verdier.single(),
                    (2 til 9.april(2025)),
                ),
                begrunnelse = søknadsbehandling.barnetillegg.begrunnelse,
            )
            omgjøring.valgteTiltaksdeltakelser shouldBe ValgteTiltaksdeltakelser(
                periodisering = SammenhengendePeriodisering(
                    søknadsbehandling.valgteTiltaksdeltakelser!!.periodisering.verdier.single(),
                    (2 til 9.april(2025)),
                ),
            )
            omgjøring.antallDagerPerMeldeperiode shouldBe SammenhengendePeriodisering(
                søknadsbehandling.antallDagerPerMeldeperiode!!.verdier.single(),
                (2 til 9.april(2025)),
            )
            omgjøring.erFerdigutfylt() shouldBe true

            // Forsikrer oss om at vi ikke har brutt noen init-regler i Sak.kt.
            tac.sakContext.sakService.hentForSakId(sakId = omgjøring.sakId).rammebehandlinger[1] shouldBe omgjøring
        }
    }

    @Test
    fun `revurdering til omgjøring - tiltaksdeltagelse har økt før start`() {
        withTestApplicationContext { tac ->
            val (_, _, søknadsbehandling, omgjøring) = startRevurderingOmgjøring(
                tac = tac,
                søknadsbehandlingVirkningsperiode = 2 til 9.april(2025),
                oppdaterTiltaksdeltagelsesperiode = 1 til 10.april(2025),
            )
            søknadsbehandling.virkningsperiode shouldBe (2 til 9.april(2025))
            søknadsbehandling.innvilgelsesperiode shouldBe (2 til 9.april(2025))
            søknadsbehandling.saksopplysninger.tiltaksdeltagelser.single().periode shouldBe (2 til 9.april(2025))
            omgjøring!!.saksopplysninger.tiltaksdeltagelser.single().periode shouldBe (1 til 10.april(2025))
            omgjøring.virkningsperiode shouldBe (2 til 9.april(2025))
            omgjøring.innvilgelsesperiode shouldBe (2 til 9.april(2025))
            omgjøring.barnetillegg shouldBe Barnetillegg(
                periodisering = SammenhengendePeriodisering(
                    søknadsbehandling.barnetillegg!!.periodisering.verdier.single(),
                    (2 til 9.april(2025)),
                ),
                begrunnelse = søknadsbehandling.barnetillegg.begrunnelse,
            )
            omgjøring.valgteTiltaksdeltakelser shouldBe ValgteTiltaksdeltakelser(
                periodisering = SammenhengendePeriodisering(
                    søknadsbehandling.valgteTiltaksdeltakelser!!.periodisering.verdier.single(),
                    (2 til 9.april(2025)),
                ),
            )
            omgjøring.antallDagerPerMeldeperiode shouldBe SammenhengendePeriodisering(
                søknadsbehandling.antallDagerPerMeldeperiode!!.verdier.single(),
                (2 til 9.april(2025)),
            )
            omgjøring.erFerdigutfylt() shouldBe true

            // Forsikrer oss om at vi ikke har brutt noen init-regler i Sak.kt.
            tac.sakContext.sakService.hentForSakId(sakId = omgjøring.sakId).rammebehandlinger[1] shouldBe omgjøring
        }
    }

    @Test
    fun `revurdering til omgjøring - tiltaksdeltagelse finnes ikke lenger`() {
        withTestApplicationContext { tac ->
            val (_, _, _, omgjøring) = startRevurderingOmgjøring(
                tac = tac,
                søknadsbehandlingVirkningsperiode = 2 til 9.april(2025),
                oppdaterTiltaksdeltagelsesperiode = null,
                forventetStatus = HttpStatusCode.InternalServerError,
            )
            omgjøring shouldBe null
        }
    }
}
