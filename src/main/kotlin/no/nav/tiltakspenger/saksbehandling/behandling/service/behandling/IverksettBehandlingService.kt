package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingstype
import no.nav.tiltakspenger.saksbehandling.behandling.domene.KanIkkeIverksetteBehandling
import no.nav.tiltakspenger.saksbehandling.behandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.OppgaveGateway
import no.nav.tiltakspenger.saksbehandling.behandling.ports.RammevedtakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkStønadRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.KunneIkkeHenteSakForSakId
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.felles.Attestering
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringsstatus
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortBehandlingRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldeperiodeRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakDTO
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakService
import no.nav.tiltakspenger.saksbehandling.statistikk.vedtak.StatistikkStønadDTO
import no.nav.tiltakspenger.saksbehandling.statistikk.vedtak.genererStønadsstatistikkForRammevedtak
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import no.nav.tiltakspenger.saksbehandling.vedtak.Vedtakstype
import no.nav.tiltakspenger.saksbehandling.vedtak.opprettVedtak
import java.time.Clock

class IverksettBehandlingService(
    private val behandlingRepo: BehandlingRepo,
    private val rammevedtakRepo: RammevedtakRepo,
    private val meldekortBehandlingRepo: MeldekortBehandlingRepo,
    private val meldeperiodeRepo: MeldeperiodeRepo,
    private val sessionFactory: SessionFactory,
    private val statistikkSakRepo: StatistikkSakRepo,
    private val statistikkStønadRepo: StatistikkStønadRepo,
    private val sakService: SakService,
    private val oppgaveGateway: OppgaveGateway,
    private val clock: Clock,
    private val statistikkSakService: StatistikkSakService,
) {

    private val logger = KotlinLogging.logger {}

    suspend fun iverksett(
        behandlingId: BehandlingId,
        beslutter: Saksbehandler,
        correlationId: CorrelationId,
        sakId: SakId,
    ): Either<KanIkkeIverksetteBehandling, Behandling> {
        if (!beslutter.erBeslutter()) {
            logger.warn { "Navident ${beslutter.navIdent} med rollene ${beslutter.roller} har ikke tilgang til å iverksette behandlingen" }
            return KanIkkeIverksetteBehandling.MåVæreBeslutter.left()
        }
        val sak = sakService.hentForSakId(sakId, beslutter, correlationId).getOrElse {
            @Suppress("USELESS_IS_CHECK")
            when (it) {
                is KunneIkkeHenteSakForSakId -> return KanIkkeIverksetteBehandling.MåVæreBeslutter.left()
            }
        }
        val behandling = sak.hentBehandling(behandlingId)!!

        if (behandling.beslutter != beslutter.navIdent) {
            return KanIkkeIverksetteBehandling.BehandlingenEiesAvAnnenBeslutter(eiesAvBeslutter = behandling.beslutter)
                .left()
        }

        val attestering = Attestering(
            status = Attesteringsstatus.GODKJENT,
            begrunnelse = null,
            beslutter = beslutter.navIdent,
            tidspunkt = nå(clock),
        )
        val iverksattBehandling = behandling.iverksett(beslutter, attestering, clock)

        val (oppdatertSak, vedtak) = sak.opprettVedtak(iverksattBehandling, clock)

        val sakStatistikk = statistikkSakService.genererStatistikkForRammevedtak(
            rammevedtak = vedtak,
            behandlingId = behandlingId,
        )
        val stønadStatistikk = genererStønadsstatistikkForRammevedtak(vedtak)

        when (behandling.behandlingstype) {
            Behandlingstype.FØRSTEGANGSBEHANDLING -> oppdatertSak.iverksettFørstegangsbehandling(
                vedtak = vedtak,
                sakStatistikk = sakStatistikk,
                stønadStatistikk = stønadStatistikk,
            )

            Behandlingstype.REVURDERING -> oppdatertSak.iverksettRevurdering(
                vedtak = vedtak,
                sakStatistikk = sakStatistikk,
                stønadStatistikk = stønadStatistikk,
            )
        }

        Either.catch {
            behandling.oppgaveId?.let { id ->
                logger.info { "Ferdigstiller oppgave med id $id for behandling med behandlingsId $behandlingId" }
                oppgaveGateway.ferdigstillOppgave(id)
            }
        }.onLeft {
            return KanIkkeIverksetteBehandling.KunneIkkeOppretteOppgave.left()
        }

        return iverksattBehandling.right()
    }

    private fun Sak.iverksettFørstegangsbehandling(
        vedtak: Rammevedtak,
        sakStatistikk: StatistikkSakDTO,
        stønadStatistikk: StatistikkStønadDTO,
    ): Sak {
        when (vedtak.vedtaksType) {
            Vedtakstype.INNVILGELSE,
            Vedtakstype.STANS,
            -> {
                val (oppdatertSak, meldeperioder) = this.genererMeldeperioder(clock)
                // Denne har vi behov for å gjøre ved påfølgende førstegangsbehandligner (altså ikke den første)
                val (oppdaterteMeldekortbehandlinger, oppdaterteMeldekort) =
                    this.meldekortBehandlinger.oppdaterMedNyeKjeder(oppdatertSak.meldeperiodeKjeder, tiltakstypeperioder, clock)

                // journalføring og dokumentdistribusjon skjer i egen jobb
                // Dersom denne endres til søknadsbehandling og vi kan ha mer enn 1 for en sak og den kan overlappe den eksistrende saksperioden, må den legge til nye versjoner av meldeperiodene her.
                sessionFactory.withTransactionContext { tx ->
                    behandlingRepo.lagre(vedtak.behandling, tx)
                    sakService.oppdaterSisteDagSomGirRett(
                        sakId = oppdatertSak.id,
                        førsteDagSomGirRett = oppdatertSak.førsteDagSomGirRett,
                        sisteDagSomGirRett = oppdatertSak.sisteDagSomGirRett,
                        sessionContext = tx,
                    )
                    sakService.oppdaterSkalSendesTilMeldekortApi(
                        sakId = oppdatertSak.id,
                        skalSendesTilMeldekortApi = true,
                        sessionContext = tx,
                    )
                    rammevedtakRepo.lagre(vedtak, tx)
                    statistikkSakRepo.lagre(sakStatistikk, tx)
                    statistikkStønadRepo.lagre(stønadStatistikk, tx)
                    // Merk at simuleringen vil nulles ut her. Gjelder kun åpne meldekortbehandlinger.
                    oppdaterteMeldekort.forEach { meldekortBehandlingRepo.oppdater(it, null, tx) }
                    meldeperiodeRepo.lagre(meldeperioder, tx)
                }
                return oppdatertSak.copy(meldekortBehandlinger = oppdaterteMeldekortbehandlinger)
            }

            Vedtakstype.AVSLAG -> {
                // journalføring og dokumentdistribusjon skjer i egen jobb
                sessionFactory.withTransactionContext { tx ->
                    behandlingRepo.lagre(vedtak.behandling, tx)
                    rammevedtakRepo.lagre(vedtak, tx)
                    statistikkSakRepo.lagre(sakStatistikk, tx)
                    statistikkStønadRepo.lagre(stønadStatistikk, tx)
                }
                return this
            }
        }
    }

    private fun Sak.iverksettRevurdering(
        vedtak: Rammevedtak,
        sakStatistikk: StatistikkSakDTO,
        stønadStatistikk: StatistikkStønadDTO,
    ): Sak {
        val (oppdatertSak, oppdaterteMeldeperioder) = this.genererMeldeperioder(clock)
        val (oppdaterteMeldekortbehandlinger, oppdaterteMeldekort) =
            this.meldekortBehandlinger.oppdaterMedNyeKjeder(oppdatertSak.meldeperiodeKjeder, tiltakstypeperioder, clock)
        // journalføring og dokumentdistribusjon skjer i egen jobb
        sessionFactory.withTransactionContext { tx ->
            behandlingRepo.lagre(vedtak.behandling, tx)
            sakService.oppdaterSisteDagSomGirRett(
                sakId = oppdatertSak.id,
                førsteDagSomGirRett = oppdatertSak.førsteDagSomGirRett,
                sisteDagSomGirRett = oppdatertSak.sisteDagSomGirRett,
                sessionContext = tx,
            )
            sakService.oppdaterSkalSendesTilMeldekortApi(
                sakId = oppdatertSak.id,
                skalSendesTilMeldekortApi = true,
                sessionContext = tx,
            )
            rammevedtakRepo.lagre(vedtak, tx)
            statistikkSakRepo.lagre(sakStatistikk, tx)
            statistikkStønadRepo.lagre(stønadStatistikk, tx)
            // Merk at simuleringen vil nulles ut her. Gjelder kun åpne meldekortbehandlinger.
            oppdaterteMeldekort.forEach { meldekortBehandlingRepo.oppdater(it, null, tx) }
            meldeperiodeRepo.lagre(oppdaterteMeldeperioder, tx)
        }
        return oppdatertSak.copy(meldekortBehandlinger = oppdaterteMeldekortbehandlinger)
    }
}
