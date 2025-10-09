package no.nav.tiltakspenger.saksbehandling.behandling.service.sak

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import arrow.core.toNonEmptyListOrNull
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.personklient.pdl.dto.ForelderBarnRelasjonRolle
import no.nav.tiltakspenger.libs.personklient.skjerming.FellesSkjermingsklient
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlinger
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.person.KunneIkkeHenteEnkelPerson
import no.nav.tiltakspenger.saksbehandling.behandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.IkkeFunnetException
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.person.BarnMedSkjerming
import no.nav.tiltakspenger.saksbehandling.person.EnkelPersonMedSkjerming
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.vedtak.Vedtaksliste

class SakService(
    private val sakRepo: SakRepo,
    private val personService: PersonService,
    private val fellesSkjermingsklient: FellesSkjermingsklient,
) {
    val logger = KotlinLogging.logger { }

    fun hentEllerOpprettSak(
        fnr: Fnr,
        correlationId: CorrelationId,
    ): Sak {
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
            behandlinger = Behandlinger.empty(),
            vedtaksliste = Vedtaksliste.empty(),
            meldeperiodeKjeder = MeldeperiodeKjeder(emptyList()),
            brukersMeldekort = emptyList(),
            søknader = emptyList(),
        )
        sakRepo.opprettSak(sak)
        logger.info { "Opprettet ny sak med saksnummer ${sak.saksnummer}, correlationId $correlationId" }
        return sak
    }

    fun hentForSaksnummer(
        saksnummer: Saksnummer,
    ): Sak {
        return sakRepo.hentForSaksnummer(saksnummer)
            ?: throw IkkeFunnetException("Fant ikke sak med saksnummer $saksnummer")
    }

    fun hentForFnr(
        fnr: Fnr,
    ): Sak? {
        val saker = sakRepo.hentForFnr(fnr)
        if (saker.saker.isEmpty()) return null

        val sak = saker.single()
        return sak
    }

    /** Gjør ingen tilgangskontroll */
    fun hentForSakId(
        sakId: SakId,
    ): Sak {
        return sakRepo.hentForSakId(sakId) ?: throw IkkeFunnetException("Fant ikke sak med sakId $sakId")
    }

    /**
     * Merk at denne ikke skal sjekke om saksbehandler har tilgang til personen.
     */
    suspend fun hentEnkelPersonForSakId(
        sakId: SakId,
        correlationId: CorrelationId,
    ): Either<KunneIkkeHenteEnkelPerson, EnkelPersonMedSkjerming> {
        // Merk at denne IKKE skal sjekke tilgang til person, siden informasjonen skal vise til saksbehandleren, slik at hen skjønner at hen ikke kan behandle denne saken uten de riktige rollene.
        val fnr = sakRepo.hentFnrForSakId(sakId)!!
        val erSkjermet = fellesSkjermingsklient.erSkjermetPerson(fnr, correlationId)
            .getOrElse { return KunneIkkeHenteEnkelPerson.FeilVedKallMotSkjerming.left() }
        val person = personService.hentEnkelPersonFnr(fnr)
            .getOrElse { return KunneIkkeHenteEnkelPerson.FeilVedKallMotPdl.left() }
        val personMedSkjerming =
            EnkelPersonMedSkjerming(
                person,
                erSkjermet,
            )
        return personMedSkjerming.right()
    }

    suspend fun hentBarnForSakId(
        sakId: SakId,
        correlationId: CorrelationId,
    ): Either<KunneIkkeHenteEnkelPerson, List<BarnMedSkjerming>> {
        val fnr = hentFnrForSakId(sakId)
        val forelderBarnRelasjoner = personService.hentForeldrerBarnRelasjoner(fnr)
            .getOrElse { return KunneIkkeHenteEnkelPerson.FeilVedKallMotPdl.left() }

        val barnasFnrs = forelderBarnRelasjoner
            .filter { it.relatertPersonsRolle == ForelderBarnRelasjonRolle.BARN }
            .mapNotNull { it.relatertPersonsIdent?.let { ident -> Fnr.fromString(ident) } }
            .distinct()
            .toNonEmptyListOrNull()

        if (barnasFnrs.isNullOrEmpty()) {
            return emptyList<BarnMedSkjerming>().right()
        }

        val erBarnSkjermet = fellesSkjermingsklient.erSkjermetPersoner(barnasFnrs, correlationId)
            .getOrElse { return KunneIkkeHenteEnkelPerson.FeilVedKallMotSkjerming.left() }

        val barn = personService.hentPersoner(fnr)
            .getOrElse { return KunneIkkeHenteEnkelPerson.FeilVedKallMotPdl.left() }

        val barnMedSkjerming = barn.map { barn ->
            val erSkjermet = erBarnSkjermet[barn.fnr] ?: false
            BarnMedSkjerming(
                barn = barn,
                erSkjermet = erSkjermet,
            )
        }

        return barnMedSkjerming.right()
    }

    fun hentFnrForSakId(sakId: SakId): Fnr {
        return sakRepo.hentFnrForSakId(sakId) ?: throw IkkeFunnetException("Fant ikke sak med sakId $sakId")
    }

    fun hentFnrForSaksnummer(saksnummer: Saksnummer): Fnr {
        return sakRepo.hentFnrForSaksnummer(saksnummer) ?: throw IkkeFunnetException("Fant ikke sak med saksnummer $saksnummer")
    }

    fun oppdaterSkalSendesTilMeldekortApi(
        sakId: SakId,
        skalSendesTilMeldekortApi: Boolean,
        sessionContext: SessionContext?,
    ) {
        sakRepo.oppdaterSkalSendesTilMeldekortApi(
            sakId = sakId,
            skalSendesTilMeldekortApi = skalSendesTilMeldekortApi,
            sessionContext = sessionContext,
        )
    }

    fun oppdaterSkalSendeMeldeperioderTilDatadelingOgSkalSendesTilMeldekortApi(
        sakId: SakId,
        skalSendesTilMeldekortApi: Boolean,
        skalSendeMeldeperioderTilDatadeling: Boolean,
        sessionContext: SessionContext?,
    ) {
        sakRepo.oppdaterSkalSendeMeldeperioderTilDatadelingOgSkalSendesTilMeldekortApi(
            sakId = sakId,
            skalSendesTilMeldekortApi = skalSendesTilMeldekortApi,
            skalSendeMeldeperioderTilDatadeling = skalSendeMeldeperioderTilDatadeling,
            sessionContext = sessionContext,
        )
    }
}
