package no.nav.tiltakspenger.saksbehandling.saksbehandling.service.sak

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import arrow.core.toNonEmptyListOrNull
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.Saksbehandlerrolle
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.personklient.pdl.TilgangsstyringService
import no.nav.tiltakspenger.saksbehandling.felles.Systembruker
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.IkkeFunnetException
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.TilgangException
import no.nav.tiltakspenger.saksbehandling.felles.sikkerlogg
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlinger
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.Behandlinger
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.benk.Saksoversikt
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.personopplysninger.EnkelPersonMedSkjerming
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.sak.Sak
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.vedtak.Vedtaksliste
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.PoaoTilgangGateway
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.SakRepo
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.SaksoversiktRepo
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.person.KunneIkkeHenteEnkelPerson
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalinger

class SakServiceImpl(
    private val sakRepo: SakRepo,
    private val saksoversiktRepo: SaksoversiktRepo,
    private val personService: PersonService,
    private val tilgangsstyringService: TilgangsstyringService,
    private val poaoTilgangGateway: PoaoTilgangGateway,
) : SakService {
    val logger = KotlinLogging.logger { }

    override suspend fun hentEllerOpprettSak(
        fnr: Fnr,
        systembruker: Systembruker,
        correlationId: CorrelationId,
    ): Sak {
        require(systembruker.roller.harLageHendelser()) { "Systembruker mangler rollen LAGE_HENDELSER. Systembrukers roller: ${systembruker.roller}" }

        val saker = sakRepo.hentForFnr(fnr)
        if (saker.size > 1) {
            throw IllegalStateException("Vi støtter ikke flere saker per søker i piloten. correlationId: $correlationId")
        }
        if (saker.isNotEmpty()) {
            return saker.single()
        }

        val sak = Sak(
            id = SakId.random(),
            fnr = fnr,
            saksnummer = sakRepo.hentNesteSaksnummer(),
            behandlinger = Behandlinger(emptyList()),
            vedtaksliste = Vedtaksliste.empty(),
            meldekortBehandlinger = MeldekortBehandlinger.empty(),
            utbetalinger = Utbetalinger(emptyList()),
            meldeperiodeKjeder = MeldeperiodeKjeder(emptyList()),
            brukersMeldekort = emptyList(),
            soknader = emptyList(),
        )
        sakRepo.opprettSak(sak)
        logger.info { "Opprettet ny sak med saksnummer ${sak.saksnummer}, correlationId $correlationId" }
        return sak
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
        sjekkTilgangTilPerson(sak.id, saksbehandler, correlationId)

        return sak.right()
    }

    override suspend fun hentForSaksnummer(
        saksnummer: Saksnummer,
        systembruker: Systembruker,
    ): Sak {
        require(systembruker.roller.harLageHendelser()) { "Systembruker mangler rollen LAGE_HENDELSER. Systembrukers roller: ${systembruker.roller}" }
        val sak = sakRepo.hentForSaksnummer(saksnummer)
            ?: throw IkkeFunnetException("Fant ikke sak med saksnummer $saksnummer")
        return sak
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
        sjekkTilgangTilPerson(sak.id, saksbehandler, correlationId)

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
        sjekkTilgangTilPerson(sakId, saksbehandler, correlationId)
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
        val behandlinger = saksoversiktRepo.hentÅpneBehandlinger()
        val søknader = saksoversiktRepo.hentÅpneSøknader()
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

    private suspend fun sjekkTilgangTilPerson(sakId: SakId, saksbehandler: Saksbehandler, correlationId: CorrelationId) {
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
