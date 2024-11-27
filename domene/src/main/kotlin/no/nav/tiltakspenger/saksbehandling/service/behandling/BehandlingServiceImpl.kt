package no.nav.tiltakspenger.saksbehandling.service.behandling

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import mu.KotlinLogging
import no.nav.tiltakspenger.felles.Systembruker
import no.nav.tiltakspenger.felles.exceptions.IkkeFunnetException
import no.nav.tiltakspenger.felles.exceptions.TilgangException
import no.nav.tiltakspenger.felles.sikkerlogg
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.person.AdressebeskyttelseGradering
import no.nav.tiltakspenger.libs.person.harStrengtFortroligAdresse
import no.nav.tiltakspenger.libs.personklient.pdl.TilgangsstyringService
import no.nav.tiltakspenger.meldekort.domene.opprettFørsteMeldekortForEnSak
import no.nav.tiltakspenger.meldekort.ports.MeldekortRepo
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Attestering
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Attesteringsstatus
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandling
import no.nav.tiltakspenger.saksbehandling.domene.behandling.KanIkkeHenteBehandling
import no.nav.tiltakspenger.saksbehandling.domene.behandling.KanIkkeIverksetteBehandling
import no.nav.tiltakspenger.saksbehandling.domene.behandling.KanIkkeSendeTilBeslutter
import no.nav.tiltakspenger.saksbehandling.domene.behandling.KanIkkeTaBehandling
import no.nav.tiltakspenger.saksbehandling.domene.behandling.KanIkkeUnderkjenne
import no.nav.tiltakspenger.saksbehandling.domene.vedtak.opprettVedtak
import no.nav.tiltakspenger.saksbehandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.ports.RammevedtakRepo
import no.nav.tiltakspenger.saksbehandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.ports.StatistikkStønadRepo
import no.nav.tiltakspenger.saksbehandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.service.statistikk.sak.genererStatistikkForIverksattFørstegangsbehandling
import no.nav.tiltakspenger.saksbehandling.service.statistikk.stønad.genererStønadsstatistikkForRammevedtak

class BehandlingServiceImpl(
    private val førstegangsbehandlingRepo: BehandlingRepo,
    private val rammevedtakRepo: RammevedtakRepo,
    private val meldekortRepo: MeldekortRepo,
    private val sessionFactory: SessionFactory,
    private val statistikkSakRepo: StatistikkSakRepo,
    private val statistikkStønadRepo: StatistikkStønadRepo,
    private val tilgangsstyringService: TilgangsstyringService,
    private val personService: PersonService,
    private val gitHash: String,
) : BehandlingService {
    val logger = KotlinLogging.logger { }

    override fun hentBehandlingForSystem(
        behandlingId: BehandlingId,
        sessionContext: SessionContext?,
    ): Behandling = førstegangsbehandlingRepo.hent(behandlingId, sessionContext)

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
            førstegangsbehandlingRepo.lagre(it)
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

        val behandling = hentBehandling(behandlingId, beslutter, correlationId).sendTilbake(beslutter, attestering).also {
            sessionFactory.withTransactionContext { tx ->
                førstegangsbehandlingRepo.lagre(it, tx)
            }
        }
        return behandling.right()
    }

    override suspend fun iverksett(
        behandlingId: BehandlingId,
        beslutter: Saksbehandler,
        correlationId: CorrelationId,
    ): Either<KanIkkeIverksetteBehandling, Behandling> {
        if (!beslutter.erBeslutter()) {
            logger.warn { "Navident ${beslutter.navIdent} med rollene ${beslutter.roller} har ikke tilgang til å iverksette behandlingen" }
            return KanIkkeIverksetteBehandling.MåVæreBeslutter.left()
        }
        val behandling = hentBehandling(behandlingId, beslutter, correlationId)
        val attestering =
            Attestering(
                status = Attesteringsstatus.GODKJENT,
                begrunnelse = "",
                beslutter = beslutter.navIdent,
            )
        val iverksattBehandling = behandling.iverksett(beslutter, attestering)

        val vedtak = iverksattBehandling.opprettVedtak()

        val fnr = personService.hentFnrForBehandlingId(behandlingId)
        val adressebeskyttelseGradering: List<AdressebeskyttelseGradering>? =
            tilgangsstyringService.adressebeskyttelseEnkel(fnr)
                .getOrElse {
                    throw IllegalArgumentException(
                        "Kunne ikke hente adressebeskyttelsegradering for person. BehandlingId: $behandlingId",
                    )
                }

        require(adressebeskyttelseGradering != null) { "Fant ikke adressebeskyttelse for person. BehandlingId: $behandlingId" }

        val sakStatistikk = genererStatistikkForIverksattFørstegangsbehandling(
            behandling = iverksattBehandling,
            vedtak = vedtak,
            gjelderKode6 = adressebeskyttelseGradering.harStrengtFortroligAdresse(),
            versjon = gitHash,
        )
        val stønadStatistikk = genererStønadsstatistikkForRammevedtak(vedtak)
        val førsteMeldekort = vedtak.opprettFørsteMeldekortForEnSak()

        // journalføring og dokumentdistribusjon skjer i egen jobb
        sessionFactory.withTransactionContext { tx ->
            førstegangsbehandlingRepo.lagre(iverksattBehandling, tx)
            rammevedtakRepo.lagre(vedtak, tx)
            statistikkSakRepo.lagre(sakStatistikk, tx)
            statistikkStønadRepo.lagre(stønadStatistikk, tx)
            meldekortRepo.lagre(førsteMeldekort, tx)
        }
        return iverksattBehandling.right()
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
            førstegangsbehandlingRepo.lagre(it)
        }
        return behandling.right()
    }

    // er tenkt brukt fra datadeling og henter alle behandlinger som ikke er iverksatt for en ident
    override fun hentBehandlingerUnderBehandlingForIdent(
        fnr: Fnr,
        periode: Periode,
        systembruker: Systembruker,
    ): List<Behandling> {
        require(systembruker.roller.harHenteData()) { "Systembruker mangler rollen HENTE_DATA. Systembrukers roller: ${systembruker.roller}" }
        return førstegangsbehandlingRepo
            .hentAlleForIdent(fnr)
            .filter { behandling -> !behandling.erIverksatt }
            .filter { behandling ->
                behandling.vurderingsperiode.overlapperMed(periode)
            }
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
