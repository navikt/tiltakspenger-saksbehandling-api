package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.KanIkkeBehandleSøknadPåNytt
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.infra.metrikker.MetricRegister
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakService
import no.nav.tiltakspenger.saksbehandling.vedtak.Vedtakstype
import java.time.Clock

class BehandleSøknadPåNyttService(
    private val clock: Clock,
    private val sakService: SakService,
    private val behandlingRepo: BehandlingRepo,
    private val oppdaterSaksopplysningerService: OppdaterSaksopplysningerService,
    private val statistikkSakService: StatistikkSakService,
    private val statistikkSakRepo: StatistikkSakRepo,
    private val sessionFactory: SessionFactory,
) {
    val logger = KotlinLogging.logger { }

    suspend fun startSøknadsbehandlingPåNytt(
        søknadId: SøknadId,
        sakId: SakId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Either<KanIkkeBehandleSøknadPåNytt, Søknadsbehandling> {
        val sak = sakService.hentForSakId(sakId, saksbehandler, correlationId)
        val avslåtteSøknadsbehandlinger = sak.vedtaksliste.value
            .filter { it.vedtaksType == Vedtakstype.AVSLAG }
            .map { it.behandling }
            .filterIsInstance<Søknadsbehandling>()
            .filter { it.søknad.id == søknadId }

        if (avslåtteSøknadsbehandlinger.isEmpty()) {
            throw IllegalStateException("Kan ikke behandle søknad på nytt fordi det finnes ikke vedtatte avslag på søknaden: $søknadId")
        }

        val søknad = avslåtteSøknadsbehandlinger.first().søknad
        val perioderMedUtbetalinger = sak.utbetalinger.hentUtbetalingerFraPeriode(søknad.vurderingsperiode())

        if (perioderMedUtbetalinger.isNotEmpty()) {
            throw IllegalStateException("Det finnes utbetalinger i vurderingsperioden til søknaden: ${søknad.id}")
        }

        val hentSaksopplysninger: suspend (Periode) -> Saksopplysninger = { saksopplysningsperiode: Periode ->
            oppdaterSaksopplysningerService.hentSaksopplysningerFraRegistre(
                fnr = sak.fnr,
                correlationId = correlationId,
                saksopplysningsperiode = saksopplysningsperiode,
            )
        }

        val søknadsbehandling = Søknadsbehandling.opprett(
            sakId = sakId,
            saksnummer = sak.saksnummer,
            fnr = sak.fnr,
            søknad = søknad,
            saksbehandler = saksbehandler,
            hentSaksopplysninger = hentSaksopplysninger,
            clock = clock,
        ).getOrElse { return KanIkkeBehandleSøknadPåNytt.OppretteBehandling(it).left() }

        val opprettetBehandlingStatistikk = statistikkSakService.genererStatistikkForSøknadsbehandling(
            behandling = søknadsbehandling,
            søknadId = søknadId,
        )

        val opprettetBehandlingPåNyttStatistikk = statistikkSakService.genererStatistikkForSøknadSomBehandlesPåNytt(
            behandling = søknadsbehandling,
            søknadId = søknadId,
        )

        sessionFactory.withTransactionContext { tx ->
            behandlingRepo.lagre(søknadsbehandling, tx)
            statistikkSakRepo.lagre(opprettetBehandlingStatistikk, tx)
            statistikkSakRepo.lagre(opprettetBehandlingPåNyttStatistikk, tx)
            sakService.oppdaterSkalSendesTilMeldekortApi(
                sakId = sakId,
                skalSendesTilMeldekortApi = true,
                sessionContext = tx,
            )
        }

        MetricRegister.STARTET_BEHANDLING.inc()
        MetricRegister.SØKNAD_BEHANDLET_PÅ_NYTT.inc()
        return søknadsbehandling.right()
    }
}
