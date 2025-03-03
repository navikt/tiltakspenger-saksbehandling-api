package no.nav.tiltakspenger.saksbehandling.service.behandling

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.Saksbehandlerrolle
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.person.AdressebeskyttelseGradering
import no.nav.tiltakspenger.libs.person.harStrengtFortroligAdresse
import no.nav.tiltakspenger.libs.personklient.pdl.TilgangsstyringService
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandling
import no.nav.tiltakspenger.saksbehandling.domene.sak.Sak
import no.nav.tiltakspenger.saksbehandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.service.sak.KanIkkeStarteSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.service.statistikk.sak.genererStatistikkForNyFørstegangsbehandling

class StartSøknadsbehandlingService(
    private val sakService: SakService,
    private val sessionFactory: SessionFactory,
    private val tilgangsstyringService: TilgangsstyringService,
    private val gitHash: String,
    private val behandlingRepo: BehandlingRepo,
    private val statistikkSakRepo: StatistikkSakRepo,
    private val oppdaterSaksopplysningerService: OppdaterSaksopplysningerService,
) {

    val logger = KotlinLogging.logger {}

    suspend fun startSøknadsbehandling(
        søknadId: SøknadId,
        sakId: SakId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Either<KanIkkeStarteSøknadsbehandling, Sak> {
        if (!saksbehandler.erSaksbehandler()) {
            logger.warn { "Navident ${saksbehandler.navIdent} med rollene ${saksbehandler.roller} har ikke tilgang til å opprette behandling for fnr" }
            return KanIkkeStarteSøknadsbehandling.HarIkkeTilgang(
                kreverEnAvRollene = setOf(Saksbehandlerrolle.SAKSBEHANDLER),
                harRollene = saksbehandler.roller,
            ).left()
        }
        // hentForSakId gjør en sjekk på tilgang til sak og til person
        val sak = sakService.hentForSakId(sakId, saksbehandler, correlationId)
            .getOrElse { throw IllegalStateException("Fant ikke sak") }
        val fnr = sak.fnr

        if (sak.førstegangsbehandling != null) {
            return KanIkkeStarteSøknadsbehandling.HarAlleredeStartetBehandlingen(sak.førstegangsbehandling).left()
        }

        val soknad = sak.soknader.single { it.id == søknadId }
        val adressebeskyttelseGradering: List<AdressebeskyttelseGradering>? =
            tilgangsstyringService.adressebeskyttelseEnkel(fnr)
                .getOrElse {
                    throw IllegalArgumentException(
                        "Kunne ikke hente adressebeskyttelsegradering for person. SøknadId: $søknadId",
                    )
                }
        require(adressebeskyttelseGradering != null) { "Fant ikke adressebeskyttelse for person. SøknadId: $søknadId" }
        val hentSaksopplysninger: suspend (Periode) -> Saksopplysninger = { saksopplysningsperiode: Periode ->
            oppdaterSaksopplysningerService.hentSaksopplysningerFraRegistre(
                fnr = fnr,
                correlationId = correlationId,
                saksopplysningsperiode = saksopplysningsperiode,
            )
        }
        val førstegangsbehandling = Behandling.opprettSøknadsbehandling(
            sakId = sakId,
            saksnummer = sak.saksnummer,
            fnr = fnr,
            søknad = soknad,
            saksbehandler = saksbehandler,
            hentSaksopplysninger = hentSaksopplysninger,
        ).getOrElse { return KanIkkeStarteSøknadsbehandling.OppretteBehandling(it).left() }

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
}
