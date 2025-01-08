package no.nav.tiltakspenger.saksbehandling.service.sak

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import arrow.core.toNonEmptyListOrNull
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.tiltakspenger.felles.exceptions.IkkeFunnetException
import no.nav.tiltakspenger.felles.exceptions.TilgangException
import no.nav.tiltakspenger.felles.sikkerlogg
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.Saksbehandlerrolle
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.person.AdressebeskyttelseGradering
import no.nav.tiltakspenger.libs.person.harStrengtFortroligAdresse
import no.nav.tiltakspenger.libs.personklient.pdl.TilgangsstyringService
import no.nav.tiltakspenger.saksbehandling.domene.behandling.KanIkkeOppretteBehandling
import no.nav.tiltakspenger.saksbehandling.domene.benk.Saksoversikt
import no.nav.tiltakspenger.saksbehandling.domene.personopplysninger.EnkelPersonMedSkjerming
import no.nav.tiltakspenger.saksbehandling.domene.sak.Sak
import no.nav.tiltakspenger.saksbehandling.domene.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.ports.PoaoTilgangGateway
import no.nav.tiltakspenger.saksbehandling.ports.SakRepo
import no.nav.tiltakspenger.saksbehandling.ports.SaksoversiktRepo
import no.nav.tiltakspenger.saksbehandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.ports.TiltakGateway
import no.nav.tiltakspenger.saksbehandling.service.SøknadService
import no.nav.tiltakspenger.saksbehandling.service.person.KunneIkkeHenteEnkelPerson
import no.nav.tiltakspenger.saksbehandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.service.statistikk.sak.genererStatistikkForNyFørstegangsbehandling

class SakServiceImpl(
    private val sakRepo: SakRepo,
    private val saksoversiktRepo: SaksoversiktRepo,
    private val søknadService: SøknadService,
    private val personService: PersonService,
    private val sessionFactory: SessionFactory,
    private val tiltakGateway: TiltakGateway,
    private val statistikkSakRepo: StatistikkSakRepo,
    private val tilgangsstyringService: TilgangsstyringService,
    private val poaoTilgangGateway: PoaoTilgangGateway,
    private val gitHash: String,
) : SakService {
    val logger = KotlinLogging.logger { }

    override suspend fun startFørstegangsbehandling(
        søknadId: SøknadId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Either<KanIkkeStarteFørstegangsbehandling, Sak> {
        if (!saksbehandler.erSaksbehandler()) {
            logger.warn { "Navident ${saksbehandler.navIdent} med rollene ${saksbehandler.roller} har ikke tilgang til å hente sak for fnr" }
            return KanIkkeStarteFørstegangsbehandling.HarIkkeTilgang(
                kreverEnAvRollene = setOf(Saksbehandlerrolle.SAKSBEHANDLER),
                harRollene = saksbehandler.roller,
            ).left()
        }
        val fnr = personService.hentFnrForSøknadId(søknadId)

        sjekkTilgangTilSøknad(fnr, søknadId, saksbehandler, correlationId)

        sakRepo.hentForSøknadId(søknadId)?.also {
            return KanIkkeStarteFørstegangsbehandling.HarAlleredeStartetBehandlingen(it.førstegangsbehandling.id).left()
        }
        val søknad = søknadService.hentSøknad(søknadId)

        if (sakRepo.hentForFnr(fnr).isNotEmpty()) {
            throw IllegalStateException("Vi støtter ikke flere saker per søker i piloten. søknadId: $søknadId")
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
            tiltakGateway.hentTiltak(
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
        val sak = Sak
            .lagSak(
                saksnummer = sakRepo.hentNesteSaksnummer(),
                fødselsdato = personopplysninger.fødselsdato,
                søknad = søknad,
                saksbehandler = saksbehandler,
                registrerteTiltak = registrerteTiltak,
            ).getOrElse { return KanIkkeStarteFørstegangsbehandling.OppretteBehandling(it).left() }
        val statistikk =
            genererStatistikkForNyFørstegangsbehandling(
                behandling = sak.førstegangsbehandling,
                gjelderKode6 = adressebeskyttelseGradering.harStrengtFortroligAdresse(),
                versjon = gitHash,
            )

        sessionFactory.withTransactionContext { tx ->
            sakRepo.opprettSakOgFørstegangsbehandling(sak, tx)
            statistikkSakRepo.lagre(statistikk, tx)
        }

        return sak.right()
    }

    override suspend fun hentForSaksnummer(
        saksnummer: Saksnummer,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Either<KunneIkkeHenteSakForSaksnummer, Sak> {
        if (!saksbehandler.erSaksbehandlerEllerBeslutter()) {
            logger.warn { "Navident ${saksbehandler.navIdent} med rollene ${saksbehandler.roller} har ikke tilgang til å hente sak for saksnummer" }
            return KunneIkkeHenteSakForSaksnummer.HarIkkeTilgang(
                kreverEnAvRollene = setOf(Saksbehandlerrolle.SAKSBEHANDLER, Saksbehandlerrolle.BESLUTTER),
                harRollene = saksbehandler.roller,
            ).left()
        }
        val sak = sakRepo.hentForSaksnummer(saksnummer)
            ?: throw IkkeFunnetException("Fant ikke sak med saksnummer $saksnummer")
        sjekkTilgangTilSak(sak.id, saksbehandler, correlationId)

        return sak.right()
    }

    override suspend fun hentForFnr(
        fnr: Fnr,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Either<KunneIkkeHenteSakForFnr, Sak> {
        if (!saksbehandler.erSaksbehandlerEllerBeslutter()) {
            logger.warn { "Navident ${saksbehandler.navIdent} med rollene ${saksbehandler.roller} har ikke tilgang til å hente sak for fnr" }
            return KunneIkkeHenteSakForFnr.HarIkkeTilgang(
                kreverEnAvRollene = setOf(Saksbehandlerrolle.SAKSBEHANDLER, Saksbehandlerrolle.BESLUTTER),
                harRollene = saksbehandler.roller,
            ).left()
        }
        val saker = sakRepo.hentForFnr(fnr)
        if (saker.saker.isEmpty()) return KunneIkkeHenteSakForFnr.FantIkkeSakForFnr.left()
        if (saker.size > 1) throw IllegalStateException("Vi støtter ikke flere saker per søker i piloten.")

        val sak = saker.single()
        sjekkTilgangTilSak(sak.id, saksbehandler, correlationId)

        return sak.right()
    }

    override suspend fun hentForSakId(
        sakId: SakId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Either<KunneIkkeHenteSakForSakId, Sak> {
        if (!saksbehandler.erSaksbehandlerEllerBeslutter()) {
            logger.warn { "Navident ${saksbehandler.navIdent} med rollene ${saksbehandler.roller} har ikke tilgang til å hente sak for fnr" }
            return KunneIkkeHenteSakForSakId.HarIkkeTilgang(
                kreverEnAvRollene = setOf(Saksbehandlerrolle.SAKSBEHANDLER, Saksbehandlerrolle.BESLUTTER),
                harRollene = saksbehandler.roller,
            ).left()
        }
        sjekkTilgangTilSak(sakId, saksbehandler, correlationId)
        return sakRepo.hentForSakId(sakId)!!.right()
    }

    override suspend fun hentBenkOversikt(
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Either<KanIkkeHenteSaksoversikt, Saksoversikt> {
        if (!saksbehandler.erSaksbehandlerEllerBeslutter()) {
            logger.warn { "Navident ${saksbehandler.navIdent} med rollene ${saksbehandler.roller} har ikke tilgang til å hente saksoversikt" }
            return KanIkkeHenteSaksoversikt.HarIkkeTilgang(
                kreverEnAvRollene = setOf(Saksbehandlerrolle.SAKSBEHANDLER, Saksbehandlerrolle.BESLUTTER),
                harRollene = saksbehandler.roller,
            ).left()
        }
        val behandlinger = saksoversiktRepo.hentAlleBehandlinger()
        val søknader = saksoversiktRepo.hentAlleSøknader()
        val benkOversikt = Saksoversikt(behandlinger + søknader)

        if (benkOversikt.isEmpty()) return benkOversikt.right()
        val tilganger = tilgangsstyringService.harTilgangTilPersoner(
            fnrListe = benkOversikt.map { it.fnr }.toNonEmptyListOrNull()!!,
            roller = saksbehandler.roller,
            correlationId = correlationId,
        ).getOrElse { throw IllegalStateException("Feil ved henting av tilganger") }
        return benkOversikt.filter {
            val harTilgang = tilganger[it.fnr]
            if (harTilgang == null) {
                logger.debug { "tilgangsstyring: Filtrerte vekk bruker fra benk for saksbehandler $saksbehandler. Kunne ikke avgjøre om hen har tilgang. Se sikkerlogg for mer kontekst." }
                sikkerlogg.debug { "tilgangsstyring: Filtrerte vekk bruker ${it.fnr.verdi} fra benk for saksbehandler $saksbehandler. Kunne ikke avgjøre om hen har tilgang." }
            }
            if (harTilgang == false) {
                logger.debug { "tilgangsstyring: Filtrerte vekk bruker fra benk for saksbehandler $saksbehandler. Saksbehandler har ikke tilgang. Se sikkerlogg for mer kontekst." }
                sikkerlogg.debug { "tilgangsstyring: Filtrerte vekk bruker ${it.fnr.verdi} fra benk for saksbehandler $saksbehandler. Saksbehandler har ikke tilgang." }
            }
            harTilgang == true
        }.right()
    }

    override suspend fun hentEnkelPersonForSakId(
        sakId: SakId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Either<KunneIkkeHenteEnkelPerson, EnkelPersonMedSkjerming> {
        if (!saksbehandler.erSaksbehandlerEllerBeslutter()) {
            logger.warn { "Navident ${saksbehandler.navIdent} med rollene ${saksbehandler.roller} har ikke tilgang til å hente sak for fnr" }
            return KunneIkkeHenteEnkelPerson.HarIkkeTilgang(
                kreverEnAvRollene = setOf(Saksbehandlerrolle.SAKSBEHANDLER, Saksbehandlerrolle.BESLUTTER),
                harRollene = saksbehandler.roller,
            ).left()
        }
        val fnr = sakRepo.hentFnrForSakId(sakId) ?: return KunneIkkeHenteEnkelPerson.FantIkkeSakId.left()
        val erSkjermet = poaoTilgangGateway.erSkjermet(fnr, correlationId)
        val person = personService.hentEnkelPersonFnr(fnr).getOrElse { return KunneIkkeHenteEnkelPerson.FeilVedKallMotPdl.left() }
        val personMedSkjerming = EnkelPersonMedSkjerming(person, erSkjermet)
        return personMedSkjerming.right()
    }

    private suspend fun sjekkTilgangTilSak(sakId: SakId, saksbehandler: Saksbehandler, correlationId: CorrelationId) {
        val fnr = personService.hentFnrForSakId(sakId)
        tilgangsstyringService
            .harTilgangTilPerson(
                fnr = fnr,
                roller = saksbehandler.roller,
                correlationId = correlationId,
            )
            .onLeft { throw IkkeFunnetException("Feil ved sjekk av tilgang til person. SakId: $sakId. CorrelationId: $correlationId") }
            .onRight { if (!it) throw TilgangException("Saksbehandler ${saksbehandler.navIdent} har ikke tilgang til person") }
    }

    private suspend fun sjekkTilgangTilSøknad(
        fnr: Fnr,
        søknadId: SøknadId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ) {
        tilgangsstyringService
            .harTilgangTilPerson(
                fnr = fnr,
                roller = saksbehandler.roller,
                correlationId = correlationId,
            )
            .onLeft { throw IkkeFunnetException("Feil ved sjekk av tilgang til person. SøknadId: $søknadId. CorrelationId: $correlationId") }
            .onRight { if (!it) throw TilgangException("Saksbehandler ${saksbehandler.navIdent} har ikke tilgang til person") }
    }
}
