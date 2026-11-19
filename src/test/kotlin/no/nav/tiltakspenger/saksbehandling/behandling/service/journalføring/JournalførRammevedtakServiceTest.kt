package no.nav.tiltakspenger.saksbehandling.behandling.service.journalføring

import arrow.core.nonEmptySetOf
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.routes.JobberEtterIverksettelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingAvslag
import org.junit.jupiter.api.Test

/**
 * GenererFakeVedtaksbrevKlient husker avslagsbrevene den genererer, og testene leser dem fra `tac.avslagsbrev`.
 * Journalføringsjobben tar alle vedtak uten journalpost på tvers av saker, så testene kjører isolert.
 */
class JournalførRammevedtakServiceTest {
    @Test
    @IsolatedDatabaseTest
    fun `avslag med grunn InnvilgetTiltakspenger får den tidligere innvilgelsen i brevet`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val innvilgelsesperiode = 1.januar(2025) til 31.mars(2025)
            val tiltaksdeltakelse = tac.tiltaksdeltakelse(innvilgelsesperiode)
            val (sak, _, innvilgelse, _) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(innvilgelsesperiode, tiltaksdeltakelse),
            )
            // Journalføringen er slått av ved iverksettelse.
            // Det er kallet til journalførRammevedtaksbrevService.journalfør() nedenfor som genererer brevet.
            val (_, avslag) = iverksettSøknadsbehandlingAvslag(
                tac = tac,
                sakId = sak.id,
                fnr = sak.fnr,
                tiltaksdeltakelse = tiltaksdeltakelse,
                avslagsgrunner = nonEmptySetOf(Avslagsgrunnlag.InnvilgetTiltakspenger),
                jobber = JobberEtterIverksettelse(journalførVedtaksbrev = false),
            )

            tac.behandlingContext.journalførRammevedtaksbrevService.journalfør()

            tac.avslagsbrev.single().also { avslagsbrev ->
                avslagsbrev.forhåndsvisning shouldBe false
                avslagsbrev.tidligereInnvilgelse shouldBe PeriodeMedVerdi(innvilgelse, innvilgelsesperiode)
            }
        }
    }

    /**
     * Journalføringsjobben kan henge etter, så det kan komme nye vedtak mellom avslaget og brevet.
     * Brevet skal vise innvilgelsen slik den var da avslaget ble fattet.
     */
    @Test
    @IsolatedDatabaseTest
    fun `revurdering etter avslaget påvirker ikke brevet`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val innvilgelsesperiode = 1.januar(2025) til 31.mars(2025)
            val tiltaksdeltakelse = tac.tiltaksdeltakelse(innvilgelsesperiode)
            val utenJournalføring = JobberEtterIverksettelse(journalførVedtaksbrev = false)
            val (sak, _, innvilgelse, _) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(innvilgelsesperiode, tiltaksdeltakelse),
            )
            val (_, avslag) = iverksettSøknadsbehandlingAvslag(
                tac = tac,
                sakId = sak.id,
                fnr = sak.fnr,
                tiltaksdeltakelse = tiltaksdeltakelse,
                avslagsgrunner = nonEmptySetOf(Avslagsgrunnlag.InnvilgetTiltakspenger),
                jobber = utenJournalføring,
            )
            val (sakEtterStans, _, _) = iverksettRevurderingStans(
                tac = tac,
                sakId = sak.id,
                stansFraOgMed = 1.februar(2025),
                jobber = utenJournalføring,
            )
            // Forutsetning: i nåtilstanden gjelder bare januar, så uten opprettetFør ville brevet fått feil periode.
            sakEtterStans.rammevedtaksliste.gjeldendeInnvilgelseForPeriode(avslag.periode)?.periode shouldBe
                (1.januar(2025) til 31.januar(2025))

            tac.behandlingContext.journalførRammevedtaksbrevService.journalfør()

            tac.avslagsbrev.single().tidligereInnvilgelse shouldBe PeriodeMedVerdi(innvilgelse, innvilgelsesperiode)
        }
    }
}
