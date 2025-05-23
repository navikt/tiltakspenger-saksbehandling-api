package no.nav.tiltakspenger.saksbehandling.behandling.service.sak

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.behandling.domene.KanIkkeOppretteBehandling
import no.nav.tiltakspenger.saksbehandling.benk.Saksoversikt
import no.nav.tiltakspenger.saksbehandling.felles.Systembruker
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.IkkeFunnetException
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.TilgangException
import no.nav.tiltakspenger.saksbehandling.person.EnkelPersonMedSkjerming
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer

interface SakService {

    /**
     * Validerer at systembruker har rollen LAGE_HENDELSER.
     * @throws TilgangException dersom systembruker mangler tilgangen LAGE_HENDELSER
     */
    suspend fun hentEllerOpprettSak(
        fnr: Fnr,
        systembruker: Systembruker,
        correlationId: CorrelationId,
    ): Sak

    /**
     * Validerer at saksbehandler har tilgang til person og at saksbehandler har SAKSBEHANDLER eller BESLUTTER-rollen.
     * @throws TilgangException dersom saksbehandler ikke har tilgang til saken.
     * @throws IkkeFunnetException dersom vi ikke fant saken.
     */
    suspend fun hentForSaksnummer(
        saksnummer: Saksnummer,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Sak

    /**
     * Validerer at systembruker har rollen LAGE_HENDELSER.
     * @throws IkkeFunnetException dersom vi ikke fant saken.
     * @throws TilgangException dersom systembruker mangler tilgangen LAGE_HENDELSER
     */
    suspend fun hentForSaksnummer(
        saksnummer: Saksnummer,
        systembruker: Systembruker,
    ): Sak

    /**
     * Validerer at saksbehandler har tilgang til person og at saksbehandler har SAKSBEHANDLER eller BESLUTTER-rollen.
     * @throws TilgangException dersom saksbehandler ikke har tilgang til saken.
     * @return null dersom vi ikke fant saken.
     */
    suspend fun hentForFnr(
        fnr: Fnr,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Sak?

    /**
     * Sjekker tilgang til person og at saksbehandler har SAKSBEHANDLER eller BESLUTTER-rollen.
     * @throws IkkeFunnetException dersom vi ikke fant saken.
     * @throws TilgangException dersom saksbehandler ikke har tilgang til saken.
     * */
    suspend fun hentForSakIdEllerKast(sakId: SakId, saksbehandler: Saksbehandler, correlationId: CorrelationId): Sak

    suspend fun hentBenkOversikt(
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Saksoversikt

    /**
     * Merk at denne ikke skal sjekke om saksbehandler har tilgang til personen.
     */
    suspend fun hentEnkelPersonForSakId(
        sakId: SakId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Either<no.nav.tiltakspenger.saksbehandling.behandling.service.person.KunneIkkeHenteEnkelPerson, EnkelPersonMedSkjerming>

    fun oppdaterSkalSendesTilMeldekortApi(
        sakId: SakId,
        skalSendesTilMeldekortApi: Boolean,
        sessionContext: SessionContext?,
    )
}

sealed interface KanIkkeStarteSøknadsbehandling {
    data class OppretteBehandling(
        val underliggende: KanIkkeOppretteBehandling,
    ) : KanIkkeStarteSøknadsbehandling
}
