package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
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
import no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.KanIkkeStarteSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.behandling.service.statistikk.sak.genererStatistikkForNyFørstegangsbehandling
import java.time.Clock

class StartSøknadsbehandlingService(
    private val sakService: no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService,
    private val sessionFactory: SessionFactory,
    private val tilgangsstyringService: TilgangsstyringService,
    private val gitHash: String,
    private val behandlingRepo: BehandlingRepo,
    private val statistikkSakRepo: StatistikkSakRepo,
    private val oppdaterSaksopplysningerService: no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.OppdaterSaksopplysningerService,
    private val clock: Clock,
) {

    val logger = KotlinLogging.logger {}

    suspend fun startSøknadsbehandling(
        søknadId: SøknadId,
        sakId: SakId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Either<no.nav.tiltakspenger.saksbehandling.behandling.service.sak.KanIkkeStarteSøknadsbehandling, Behandling> {
        if (!saksbehandler.erSaksbehandler()) {
            logger.warn { "Navident ${saksbehandler.navIdent} med rollene ${saksbehandler.roller} har ikke tilgang til å opprette behandling for fnr" }
            return no.nav.tiltakspenger.saksbehandling.behandling.service.sak.KanIkkeStarteSøknadsbehandling.HarIkkeTilgang(
                kreverEnAvRollene = setOf(Saksbehandlerrolle.SAKSBEHANDLER),
                harRollene = saksbehandler.roller,
            ).left()
        }
        // hentForSakId gjør en sjekk på tilgang til sak og til person
        val sak = sakService.hentForSakId(sakId, saksbehandler, correlationId)
            .getOrElse { throw IllegalStateException("Fant ikke sak") }
        val fnr = sak.fnr

        val soknad = sak.soknader.single { it.id == søknadId }
        val adressebeskyttelseGradering: List<AdressebeskyttelseGradering>? =
            tilgangsstyringService.adressebeskyttelseEnkel(fnr)
                .getOrElse {
                    throw IllegalArgumentException(
                        "Kunne ikke hente adressebeskyttelsegradering for person. SøknadId: $søknadId",
                    )
                }
        require(adressebeskyttelseGradering != null) { "Fant ikke adressebeskyttelse for person. SøknadId: $søknadId" }
        val hentSaksopplysninger: suspend (Periode) -> no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger = { saksopplysningsperiode: Periode ->
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
            clock = clock,
        ).getOrElse { return no.nav.tiltakspenger.saksbehandling.behandling.service.sak.KanIkkeStarteSøknadsbehandling.OppretteBehandling(it).left() }

        val statistikk =
            no.nav.tiltakspenger.saksbehandling.behandling.service.statistikk.sak.genererStatistikkForNyFørstegangsbehandling(
                behandling = førstegangsbehandling,
                gjelderKode6 = adressebeskyttelseGradering.harStrengtFortroligAdresse(),
                versjon = gitHash,
                clock = clock,
            )

        sessionFactory.withTransactionContext { tx ->
            behandlingRepo.lagre(førstegangsbehandling, tx)
            statistikkSakRepo.lagre(statistikk, tx)
        }

        return førstegangsbehandling.right()
    }
}
