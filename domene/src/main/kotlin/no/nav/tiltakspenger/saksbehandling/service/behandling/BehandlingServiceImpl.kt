package no.nav.tiltakspenger.saksbehandling.service.behandling

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.tiltakspenger.felles.exceptions.IkkeFunnetException
import no.nav.tiltakspenger.felles.exceptions.TilgangException
import no.nav.tiltakspenger.felles.sikkerlogg
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.Saksbehandlerrolle
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.person.AdressebeskyttelseGradering
import no.nav.tiltakspenger.libs.person.harStrengtFortroligAdresse
import no.nav.tiltakspenger.libs.personklient.pdl.TilgangsstyringService
import no.nav.tiltakspenger.meldekort.domene.opprettFørsteMeldeperiode
import no.nav.tiltakspenger.meldekort.ports.MeldekortBehandlingRepo
import no.nav.tiltakspenger.meldekort.ports.MeldeperiodeRepo
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Attestering
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Attesteringsstatus
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandling
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandlingstype
import no.nav.tiltakspenger.saksbehandling.domene.behandling.KanIkkeHenteBehandling
import no.nav.tiltakspenger.saksbehandling.domene.behandling.KanIkkeIverksetteBehandling
import no.nav.tiltakspenger.saksbehandling.domene.behandling.KanIkkeOppretteBehandling
import no.nav.tiltakspenger.saksbehandling.domene.behandling.KanIkkeSendeTilBeslutter
import no.nav.tiltakspenger.saksbehandling.domene.behandling.KanIkkeTaBehandling
import no.nav.tiltakspenger.saksbehandling.domene.behandling.KanIkkeUnderkjenne
import no.nav.tiltakspenger.saksbehandling.domene.sak.Sak
import no.nav.tiltakspenger.saksbehandling.domene.vedtak.Rammevedtak
import no.nav.tiltakspenger.saksbehandling.domene.vedtak.opprettVedtak
import no.nav.tiltakspenger.saksbehandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.ports.OppgaveGateway
import no.nav.tiltakspenger.saksbehandling.ports.RammevedtakRepo
import no.nav.tiltakspenger.saksbehandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.ports.StatistikkStønadRepo
import no.nav.tiltakspenger.saksbehandling.ports.TiltakGateway
import no.nav.tiltakspenger.saksbehandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.service.sak.KanIkkeStarteFørstegangsbehandling
import no.nav.tiltakspenger.saksbehandling.service.sak.KunneIkkeHenteSakForSakId
import no.nav.tiltakspenger.saksbehandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.service.statistikk.sak.StatistikkSakDTO
import no.nav.tiltakspenger.saksbehandling.service.statistikk.sak.genererSaksstatistikkForRammevedtak
import no.nav.tiltakspenger.saksbehandling.service.statistikk.sak.genererStatistikkForNyFørstegangsbehandling
import no.nav.tiltakspenger.saksbehandling.service.statistikk.stønad.StatistikkStønadDTO
import no.nav.tiltakspenger.saksbehandling.service.statistikk.stønad.genererStønadsstatistikkForRammevedtak

class BehandlingServiceImpl(
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
    private val tiltakGateway: TiltakGateway,
    private val oppgaveGateway: OppgaveGateway,
) : BehandlingService {
    val logger = KotlinLogging.logger { }

    override suspend fun startFørstegangsbehandling(
        søknadId: SøknadId,
        sakId: SakId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Either<KanIkkeStarteFørstegangsbehandling, Sak> {
        if (!saksbehandler.erSaksbehandler()) {
            logger.warn { "Navident ${saksbehandler.navIdent} med rollene ${saksbehandler.roller} har ikke tilgang til å opprette behandling for fnr" }
            return KanIkkeStarteFørstegangsbehandling.HarIkkeTilgang(
                kreverEnAvRollene = setOf(Saksbehandlerrolle.SAKSBEHANDLER),
                harRollene = saksbehandler.roller,
            ).left()
        }
        // hentForSakId gjør en sjekk på tilgang til sak og til person
        val sak = sakService.hentForSakId(sakId, saksbehandler, correlationId).getOrElse { throw IllegalStateException("Fant ikke sak") }
        val fnr = sak.fnr

        if (sak.førstegangsbehandling != null) {
            return KanIkkeStarteFørstegangsbehandling.HarAlleredeStartetBehandlingen(sak.førstegangsbehandling.id).left()
        }

        val personopplysninger = personService.hentPersonopplysninger(fnr)
        val adressebeskyttelseGradering: List<AdressebeskyttelseGradering>? =
            tilgangsstyringService.adressebeskyttelseEnkel(fnr)
                .getOrElse {
                    throw IllegalArgumentException(
                        "Kunne ikke hente adressebeskyttelsegradering for person. SøknadId: $søknadId",
                    )
                }
        require(adressebeskyttelseGradering != null) { "Fant ikke adressebeskyttelse for person. SøknadId: $søknadId" }
        val registrerteTiltak = runBlocking {
            tiltakGateway.hentTiltaksdeltagelse(
                fnr = fnr,
                maskerTiltaksnavn = adressebeskyttelseGradering.harStrengtFortroligAdresse(),
                correlationId = correlationId,
            )
        }
        if (registrerteTiltak.isEmpty()) {
            return KanIkkeStarteFørstegangsbehandling.OppretteBehandling(
                KanIkkeOppretteBehandling.FantIkkeTiltak,
            ).left()
        }
        val soknad = sak.soknader.single { it.id == søknadId }
        val førstegangsbehandling =
            Behandling.opprettDeprecatedFørstegangsbehandling(
                sakId = sakId,
                saksnummer = sak.saksnummer,
                fnr = fnr,
                søknad = soknad,
                fødselsdato = personopplysninger.fødselsdato,
                saksbehandler = saksbehandler,
                registrerteTiltak = registrerteTiltak,
            ).getOrElse { return KanIkkeStarteFørstegangsbehandling.OppretteBehandling(it).left() }

        val statistikk =
            genererStatistikkForNyFørstegangsbehandling(
                behandling = førstegangsbehandling,
                gjelderKode6 = adressebeskyttelseGradering.harStrengtFortroligAdresse(),
                versjon = gitHash,
            )

        sessionFactory.withTransactionContext { tx ->
            behandlingRepo.lagre(førstegangsbehandling, tx)
            statistikkSakRepo.lagre(statistikk, tx)
        }
        val oppdatertSak = sakService.hentForSakId(sakId, saksbehandler, correlationId)
            .getOrElse { throw IllegalStateException("Fant ikke sak med id $sakId som vi nettopp opprettet førstegangsbehandling for") }
        return oppdatertSak.right()
    }

    override fun hentBehandlingForSystem(
        behandlingId: BehandlingId,
        sessionContext: SessionContext?,
    ): Behandling = behandlingRepo.hent(behandlingId, sessionContext)

    override suspend fun hentBehandling(
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
        sessionContext: SessionContext?,
    ): Behandling {
        require(saksbehandler.erSaksbehandlerEllerBeslutter()) { "Saksbehandler må ha rollen SAKSBEHANDLER eller BESLUTTER" }
        sjekkTilgang(behandlingId, saksbehandler, correlationId)

        val behandling = hentBehandlingForSystem(behandlingId, sessionContext)
        return behandling
    }

    override suspend fun hentBehandlingForSaksbehandler(
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
        sessionContext: SessionContext?,
    ): Either<KanIkkeHenteBehandling, Behandling> {
        if (!saksbehandler.erSaksbehandler()) {
            logger.warn { "Navident ${saksbehandler.navIdent} med rollene ${saksbehandler.roller} har ikke tilgang til å hente behandling" }
            return KanIkkeHenteBehandling.MåVæreSaksbehandlerEllerBeslutter.left()
        }
        sjekkTilgang(behandlingId, saksbehandler, correlationId)

        val behandling = hentBehandlingForSystem(behandlingId, sessionContext)
        return behandling.right()
    }

    override suspend fun sendTilBeslutter(
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Either<KanIkkeSendeTilBeslutter, Behandling> {
        if (!saksbehandler.erSaksbehandler()) {
            logger.warn { "Navident ${saksbehandler.navIdent} med rollene ${saksbehandler.roller} har ikke tilgang til å sende behandling til beslutter" }
            return KanIkkeSendeTilBeslutter.MåVæreSaksbehandler.left()
        }
        return hentBehandling(behandlingId, saksbehandler, correlationId).tilBeslutning(saksbehandler).also {
            behandlingRepo.lagre(it)
        }.right()
    }

    override suspend fun sendTilbakeTilSaksbehandler(
        behandlingId: BehandlingId,
        beslutter: Saksbehandler,
        begrunnelse: String,
        correlationId: CorrelationId,
    ): Either<KanIkkeUnderkjenne, Behandling> {
        if (!beslutter.erBeslutter()) {
            logger.warn { "Navident ${beslutter.navIdent} med rollene ${beslutter.roller} har ikke tilgang til å underkjenne behandlingen" }
            return KanIkkeUnderkjenne.MåVæreBeslutter.left()
        }
        val attestering =
            Attestering(
                status = Attesteringsstatus.SENDT_TILBAKE,
                begrunnelse = begrunnelse,
                beslutter = beslutter.navIdent,
            )

        val behandling =
            hentBehandling(behandlingId, beslutter, correlationId).sendTilbake(beslutter, attestering).also {
                sessionFactory.withTransactionContext { tx ->
                    behandlingRepo.lagre(it, tx)
                }
            }
        return behandling.right()
    }

    override suspend fun iverksett(
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

        behandling.oppgaveId?.let { id ->
            logger.info { "Ferdigstiller oppgave med id $id for behandling med behandlingsId $behandlingId" }
            oppgaveGateway.ferdigstillOppgave(id)
        }

        return iverksattBehandling.right()
    }

    private fun Sak.iverksettFørstegangsbehandling(
        vedtak: Rammevedtak,
        sakStatistikk: StatistikkSakDTO,
        stønadStatistikk: StatistikkStønadDTO,
    ) {
        val førsteMeldeperiode = this.opprettFørsteMeldeperiode()

        // journalføring og dokumentdistribusjon skjer i egen jobb
        // Dersom denne endres til søknadsbehandling og vi kan ha mer enn 1 for en sak og den kan overlappe den eksistrende saksperioden, må den legge til nye versjoner av meldeperiodene her.
        sessionFactory.withTransactionContext { tx ->
            behandlingRepo.lagre(vedtak.behandling, tx)
            rammevedtakRepo.lagre(vedtak, tx)
            statistikkSakRepo.lagre(sakStatistikk, tx)
            statistikkStønadRepo.lagre(stønadStatistikk, tx)
            meldeperiodeRepo.lagre(førsteMeldeperiode, tx)
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
            this.meldekortBehandlinger.oppdaterMedNyeKjeder(oppdaterteKjeder)
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

    override suspend fun taBehandling(
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Either<KanIkkeTaBehandling, Behandling> {
        if (!saksbehandler.erSaksbehandlerEllerBeslutter()) {
            logger.warn { "Navident ${saksbehandler.navIdent} med rollene ${saksbehandler.roller} har ikke tilgang til å ta behandling" }
            return KanIkkeTaBehandling.MåVæreSaksbehandlerEllerBeslutter.left()
        }
        val behandling = hentBehandling(behandlingId, saksbehandler, correlationId)
        behandling.taBehandling(saksbehandler).also {
            behandlingRepo.lagre(it)
        }
        return behandling.right()
    }

    private suspend fun sjekkTilgang(
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ) {
        val fnr = personService.hentFnrForBehandlingId(behandlingId)
        tilgangsstyringService
            .harTilgangTilPerson(
                fnr = fnr,
                roller = saksbehandler.roller,
                correlationId = correlationId,
            )
            .onLeft { underliggendeFeil ->
                sikkerlogg.error(
                    underliggendeFeil.exception ?: IllegalArgumentException("Trigger en stacktrace for debugging"),
                ) { "Feil ved sjekk av tilgang til person. BehandlingId: $behandlingId. CorrelationId: $correlationId. body: ${underliggendeFeil.body}, status: ${underliggendeFeil.status}" }
                throw IkkeFunnetException("Feil ved sjekk av tilgang til person. BehandlingId: $behandlingId. CorrelationId: $correlationId. Feiltype: ${underliggendeFeil::class.simpleName} Se sikkerlogg for mer context")
            }
            .onRight { if (!it) throw TilgangException("Saksbehandler ${saksbehandler.navIdent} har ikke tilgang til person") }
    }
}
