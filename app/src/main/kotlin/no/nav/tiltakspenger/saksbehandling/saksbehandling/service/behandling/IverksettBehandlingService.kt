package no.nav.tiltakspenger.saksbehandling.saksbehandling.service.behandling

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.person.AdressebeskyttelseGradering
import no.nav.tiltakspenger.libs.person.harStrengtFortroligAdresse
import no.nav.tiltakspenger.libs.personklient.pdl.TilgangsstyringService
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortBehandlingRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldeperiodeRepo
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.Attestering
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.Attesteringsstatus
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.Behandling
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.Behandlingstype
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.KanIkkeIverksetteBehandling
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.sak.Sak
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.vedtak.Rammevedtak
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.vedtak.opprettVedtak
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.OppgaveGateway
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.RammevedtakRepo
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.StatistikkStønadRepo
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.sak.KunneIkkeHenteSakForSakId
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.statistikk.sak.StatistikkSakDTO
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.statistikk.sak.genererSaksstatistikkForRammevedtak
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.statistikk.stønad.StatistikkStønadDTO
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.statistikk.stønad.genererStønadsstatistikkForRammevedtak

class IverksettBehandlingService(
    private val behandlingRepo: BehandlingRepo,
    private val rammevedtakRepo: RammevedtakRepo,
    private val meldekortBehandlingRepo: MeldekortBehandlingRepo,
    private val meldeperiodeRepo: MeldeperiodeRepo,
    private val sessionFactory: SessionFactory,
    private val statistikkSakRepo: StatistikkSakRepo,
    private val statistikkStønadRepo: StatistikkStønadRepo,
    private val tilgangsstyringService: TilgangsstyringService,
    private val personService: PersonService,
    private val sakService: SakService,
    private val gitHash: String,
    private val oppgaveGateway: OppgaveGateway,
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
        val attestering = Attestering(
            status = Attesteringsstatus.GODKJENT,
            begrunnelse = null,
            beslutter = beslutter.navIdent,
        )
        val iverksattBehandling = behandling.iverksett(beslutter, attestering)

        val (oppdatertSak, vedtak) = sak.opprettVedtak(iverksattBehandling)

        val fnr = personService.hentFnrForBehandlingId(behandlingId)
        val adressebeskyttelseGradering: List<AdressebeskyttelseGradering>? =
            tilgangsstyringService.adressebeskyttelseEnkel(fnr).getOrElse {
                throw IllegalArgumentException(
                    "Kunne ikke hente adressebeskyttelsegradering for person. BehandlingId: $behandlingId",
                )
            }

        require(adressebeskyttelseGradering != null) { "Fant ikke adressebeskyttelse for person. BehandlingId: $behandlingId" }

        val sakStatistikk = genererSaksstatistikkForRammevedtak(
            vedtak = vedtak,
            gjelderKode6 = adressebeskyttelseGradering.harStrengtFortroligAdresse(),
            versjon = gitHash,
        )
        val stønadStatistikk = genererStønadsstatistikkForRammevedtak(vedtak)

        Either.catch {
            behandling.oppgaveId?.let { id ->
                logger.info { "Ferdigstiller oppgave med id $id for behandling med behandlingsId $behandlingId" }
                oppgaveGateway.ferdigstillOppgave(id)
            }
        }.onLeft {
            return KanIkkeIverksetteBehandling.KunneIkkeOppretteOppgave.left()
        }

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

        return iverksattBehandling.right()
    }

    private fun Sak.iverksettFørstegangsbehandling(
        vedtak: Rammevedtak,
        sakStatistikk: StatistikkSakDTO,
        stønadStatistikk: StatistikkStønadDTO,
    ) {
        val (_, meldeperioder) = this.meldeperiodeKjeder.genererMeldeperioder(this.vedtaksliste)

        // journalføring og dokumentdistribusjon skjer i egen jobb
        // Dersom denne endres til søknadsbehandling og vi kan ha mer enn 1 for en sak og den kan overlappe den eksistrende saksperioden, må den legge til nye versjoner av meldeperiodene her.
        sessionFactory.withTransactionContext { tx ->
            behandlingRepo.lagre(vedtak.behandling, tx)
            rammevedtakRepo.lagre(vedtak, tx)
            statistikkSakRepo.lagre(sakStatistikk, tx)
            statistikkStønadRepo.lagre(stønadStatistikk, tx)
            meldeperioder.forEach {
                meldeperiodeRepo.lagre(it, tx)
            }
        }
    }

    private fun Sak.iverksettRevurdering(
        vedtak: Rammevedtak,
        sakStatistikk: StatistikkSakDTO,
        stønadStatistikk: StatistikkStønadDTO,
    ): Sak {
        val (oppdaterteKjeder, oppdaterteMeldeperioder) =
            this.meldeperiodeKjeder.oppdaterMedNyStansperiode(vedtak.periode)
        val (oppdaterteMeldekortbehandlinger, OppdaterteMeldekort) =
            this.meldekortBehandlinger.oppdaterMedNyeKjeder(oppdaterteKjeder, tiltakstypeperioder)
        // journalføring og dokumentdistribusjon skjer i egen jobb
        sessionFactory.withTransactionContext { tx ->
            behandlingRepo.lagre(vedtak.behandling, tx)
            rammevedtakRepo.lagre(vedtak, tx)
            statistikkSakRepo.lagre(sakStatistikk, tx)
            statistikkStønadRepo.lagre(stønadStatistikk, tx)
            OppdaterteMeldekort.forEach { meldekortBehandlingRepo.oppdater(it, tx) }
            oppdaterteMeldeperioder.forEach { meldeperiodeRepo.lagre(it, tx) }
        }
        return this.copy(meldeperiodeKjeder = oppdaterteKjeder, meldekortBehandlinger = oppdaterteMeldekortbehandlinger)
    }
}
