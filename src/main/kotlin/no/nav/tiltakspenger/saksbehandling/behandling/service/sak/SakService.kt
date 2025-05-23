package no.nav.tiltakspenger.saksbehandling.behandling.service.sak

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.personklient.pdl.TilgangsstyringService
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlinger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.KanIkkeOppretteBehandling
import no.nav.tiltakspenger.saksbehandling.behandling.ports.PoaoTilgangKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.person.KunneIkkeHenteEnkelPerson
import no.nav.tiltakspenger.saksbehandling.behandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.felles.Systembruker
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.IkkeFunnetException
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.TilgangException
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.krevLageHendelserRollen
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.krevSaksbehandlerEllerBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.krevTilgangTilPerson
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlinger
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.person.EnkelPersonMedSkjerming
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalinger
import no.nav.tiltakspenger.saksbehandling.vedtak.Vedtaksliste

class SakService(
    private val sakRepo: SakRepo,
    private val personService: PersonService,
    private val tilgangsstyringService: TilgangsstyringService,
    private val poaoTilgangKlient: PoaoTilgangKlient,
) {
    val logger = KotlinLogging.logger { }

    /**
     * Validerer at systembruker har rollen LAGE_HENDELSER.
     * @throws TilgangException dersom systembruker mangler tilgangen LAGE_HENDELSER
     */
    fun hentEllerOpprettSak(
        fnr: Fnr,
        systembruker: Systembruker,
        correlationId: CorrelationId,
    ): Sak {
        krevLageHendelserRollen(systembruker)

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

    /**
     * Validerer at saksbehandler har tilgang til person og at saksbehandler har SAKSBEHANDLER eller BESLUTTER-rollen.
     * @throws TilgangException dersom saksbehandler ikke har tilgang til saken.
     * @throws IkkeFunnetException dersom vi ikke fant saken.
     */
    suspend fun hentForSaksnummer(
        saksnummer: Saksnummer,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Sak {
        krevSaksbehandlerEllerBeslutterRolle(saksbehandler)
        val sak = sakRepo.hentForSaksnummer(saksnummer)
            ?: throw IkkeFunnetException("Fant ikke sak med saksnummer $saksnummer")
        tilgangsstyringService.krevTilgangTilPerson(saksbehandler, sak.fnr, correlationId)
        return sak
    }

    /**
     * Validerer at systembruker har rollen LAGE_HENDELSER.
     * @throws IkkeFunnetException dersom vi ikke fant saken.
     * @throws TilgangException dersom systembruker mangler tilgangen LAGE_HENDELSER
     */
    fun hentForSaksnummer(
        saksnummer: Saksnummer,
        systembruker: Systembruker,
    ): Sak {
        // TODO jah: Bør vi heller sjekke HENTE_DATA rollen her?
        krevLageHendelserRollen(systembruker)

        return sakRepo.hentForSaksnummer(saksnummer)
            ?: throw IkkeFunnetException("Fant ikke sak med saksnummer $saksnummer")
    }

    /**
     * Validerer at saksbehandler har tilgang til person og at saksbehandler har SAKSBEHANDLER eller BESLUTTER-rollen.
     * @throws TilgangException dersom saksbehandler ikke har tilgang til saken.
     * @return null dersom vi ikke fant saken.
     */
    suspend fun hentForFnr(
        fnr: Fnr,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Sak? {
        krevSaksbehandlerEllerBeslutterRolle(saksbehandler)

        val saker = sakRepo.hentForFnr(fnr)
        if (saker.saker.isEmpty()) return null

        val sak = saker.single()
        tilgangsstyringService.krevTilgangTilPerson(saksbehandler, sak.fnr, correlationId)

        return sak
    }

    suspend fun hentForSakId(
        sakId: SakId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Sak {
        val sak = sakRepo.hentForSakId(sakId) ?: throw IkkeFunnetException("Fant ikke sak med sakId $sakId")
        tilgangsstyringService.krevTilgangTilPerson(saksbehandler, sak.fnr, correlationId)
        return sak
    }

    /**
     * Sjekker tilgang til person og at saksbehandler har SAKSBEHANDLER eller BESLUTTER-rollen.
     * @throws IkkeFunnetException dersom vi ikke fant saken.
     * @throws TilgangException dersom saksbehandler ikke har tilgang til saken.
     * */
    suspend fun hentForSakIdEllerKast(
        sakId: SakId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Sak {
        krevSaksbehandlerEllerBeslutterRolle(saksbehandler)
        return hentForSakId(sakId, saksbehandler, correlationId)
    }

    /**
     * Merk at denne ikke skal sjekke om saksbehandler har tilgang til personen.
     */
    suspend fun hentEnkelPersonForSakId(
        sakId: SakId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Either<KunneIkkeHenteEnkelPerson, EnkelPersonMedSkjerming> {
        krevSaksbehandlerEllerBeslutterRolle(saksbehandler)
        // Merk at denne IKKE skal sjekke tilgang til person, siden informasjonen skal vise til saksbehandleren, slik at hen skjønner at hen ikke kan behandle denne saken uten de riktige rollene.
        val fnr = sakRepo.hentFnrForSakId(sakId)!!
        val erSkjermet = poaoTilgangKlient.erSkjermet(fnr, correlationId)
        val person = personService.hentEnkelPersonFnr(fnr)
            .getOrElse { return KunneIkkeHenteEnkelPerson.FeilVedKallMotPdl.left() }
        val personMedSkjerming =
            EnkelPersonMedSkjerming(
                person,
                erSkjermet,
            )
        return personMedSkjerming.right()
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
}

sealed interface KanIkkeStarteSøknadsbehandling {
    data class OppretteBehandling(
        val underliggende: KanIkkeOppretteBehandling,
    ) : KanIkkeStarteSøknadsbehandling
}

sealed interface KanIkkeBehandleSøknadPåNytt {
    data class OppretteBehandling(
        val underliggende: KanIkkeOppretteBehandling,
    ) : KanIkkeBehandleSøknadPåNytt
}
