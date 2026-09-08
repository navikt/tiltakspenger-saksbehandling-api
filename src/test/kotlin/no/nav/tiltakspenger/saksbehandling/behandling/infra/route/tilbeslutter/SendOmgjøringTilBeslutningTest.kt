package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.tilbeslutter

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.juni
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.omgjøringsgrunnlagetErEndretForSaksbehandler
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterOmgjøringInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterOmgjøringOpphør
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendRevurderingTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingOmgjøring
import org.junit.jupiter.api.Test

/**
 * Omgjøringsgrunnlaget beregnes på nytt når behandlingen sendes til beslutning.
 * Et annet vedtak kan ha omgjort de samme periodene etter at omgjøringen ble oppdatert.
 */
class SendOmgjøringTilBeslutningTest {

    @Test
    fun `kan ikke sende omgjøring til beslutning når et annet vedtak har endret omgjøringsgrunnlaget`() {
        withTestApplicationContext { tac ->
            val innvilgelsesperiode = 1.januar(2025) til 31.mars(2025)

            val (sak, _, søknadVedtak) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(innvilgelsesperiode),
            )

            val (_, omgjøring) = startRevurderingOmgjøring(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = søknadVedtak.id,
            )!!

            oppdaterOmgjøringInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = omgjøring.id,
                vedtaksperiode = innvilgelsesperiode,
                innvilgelsesperioder = innvilgelsesperioder(innvilgelsesperiode),
            )

            iverksettRevurderingStans(
                tac = tac,
                sakId = sak.id,
                stansFraOgMed = 1.februar(2025),
            )

            sendRevurderingTilBeslutningForBehandlingId(
                tac = tac,
                sakId = sak.id,
                behandlingId = omgjøring.id,
                forventet = omgjøringsgrunnlagetErEndretForSaksbehandler,
            )

            tac.behandlingContext.rammebehandlingRepo.hent(omgjøring.id).status shouldBe
                Rammebehandlingsstatus.UNDER_BEHANDLING
        }
    }

    @Test
    fun `et nytt vedtak utenfor omgjøringens vedtaksperiode endrer ikke omgjøringsgrunnlaget`() {
        withTestApplicationContext { tac ->
            val innvilgelsesperiode = 1.januar(2025) til 30.juni(2025)
            val omgjøringsperiode = 1.januar(2025) til 28.februar(2025)

            val (sak, _, søknadVedtak) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(innvilgelsesperiode),
            )

            val (_, omgjøring) = startRevurderingOmgjøring(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = søknadVedtak.id,
            )!!

            oppdaterOmgjøringInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = omgjøring.id,
                vedtaksperiode = omgjøringsperiode,
                innvilgelsesperioder = innvilgelsesperioder(omgjøringsperiode),
            )

            iverksettRevurderingStans(
                tac = tac,
                sakId = sak.id,
                stansFraOgMed = 1.mai(2025),
            )

            sendRevurderingTilBeslutningForBehandlingId(
                tac = tac,
                sakId = sak.id,
                behandlingId = omgjøring.id,
            )

            tac.behandlingContext.rammebehandlingRepo.hent(omgjøring.id).status shouldBe
                Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
        }
    }

    @Test
    fun `kan ikke sende omgjøring til opphør til beslutning når et annet vedtak har endret omgjøringsgrunnlaget`() {
        withTestApplicationContext { tac ->
            val innvilgelsesperiode = 1.januar(2025) til 31.mars(2025)

            val (sak, _, søknadVedtak) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(innvilgelsesperiode),
            )

            val (_, omgjøring) = startRevurderingOmgjøring(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = søknadVedtak.id,
            )!!

            oppdaterOmgjøringOpphør(
                tac = tac,
                sakId = sak.id,
                behandlingId = omgjøring.id,
                vedtaksperiode = innvilgelsesperiode,
            )

            iverksettRevurderingStans(
                tac = tac,
                sakId = sak.id,
                stansFraOgMed = 1.februar(2025),
            )

            sendRevurderingTilBeslutningForBehandlingId(
                tac = tac,
                sakId = sak.id,
                behandlingId = omgjøring.id,
                forventet = omgjøringsgrunnlagetErEndretForSaksbehandler,
            )

            tac.behandlingContext.rammebehandlingRepo.hent(omgjøring.id).status shouldBe
                Rammebehandlingsstatus.UNDER_BEHANDLING
        }
    }

    @Test
    fun `kan sende til beslutning etter at vedtaksperioden er oppdatert til det nye grunnlaget`() {
        withTestApplicationContext { tac ->
            val innvilgelsesperiode = 1.januar(2025) til 31.mars(2025)
            val gjenværendePeriode = 1.januar(2025) til 31.januar(2025)

            val (sak, _, søknadVedtak) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(innvilgelsesperiode),
            )

            val (_, omgjøring) = startRevurderingOmgjøring(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = søknadVedtak.id,
            )!!

            oppdaterOmgjøringInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = omgjøring.id,
                vedtaksperiode = innvilgelsesperiode,
                innvilgelsesperioder = innvilgelsesperioder(innvilgelsesperiode),
            )

            iverksettRevurderingStans(
                tac = tac,
                sakId = sak.id,
                stansFraOgMed = 1.februar(2025),
            )

            sendRevurderingTilBeslutningForBehandlingId(
                tac = tac,
                sakId = sak.id,
                behandlingId = omgjøring.id,
                forventet = omgjøringsgrunnlagetErEndretForSaksbehandler,
            )

            // Handlingen feilmeldingen ber om: saksbehandler krymper vedtaksperioden til det som fortsatt er gjeldende.
            oppdaterOmgjøringInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = omgjøring.id,
                vedtaksperiode = gjenværendePeriode,
                innvilgelsesperioder = innvilgelsesperioder(gjenværendePeriode),
            )

            sendRevurderingTilBeslutningForBehandlingId(
                tac = tac,
                sakId = sak.id,
                behandlingId = omgjøring.id,
            )

            tac.behandlingContext.rammebehandlingRepo.hent(omgjøring.id).status shouldBe
                Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
        }
    }
}
