package no.nav.tiltakspenger.saksbehandling.behandling.service.sak

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.Saksbehandlerrolle
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.behandling.domene.KanIkkeOppretteBehandling
import no.nav.tiltakspenger.saksbehandling.benk.Saksoversikt
import no.nav.tiltakspenger.saksbehandling.felles.Systembruker
import no.nav.tiltakspenger.saksbehandling.person.EnkelPersonMedSkjerming
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import java.time.LocalDate

interface SakService {
    suspend fun hentEllerOpprettSak(
        fnr: Fnr,
        systembruker: Systembruker,
        correlationId: CorrelationId,
    ): Sak

    suspend fun hentForSaksnummer(
        saksnummer: Saksnummer,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Either<KunneIkkeHenteSakForSaksnummer, Sak>

    suspend fun hentForSaksnummer(
        saksnummer: Saksnummer,
        systembruker: Systembruker,
    ): Sak

    suspend fun hentForFnr(
        fnr: Fnr,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Either<KunneIkkeHenteSakForFnr, Sak>

    /**
     * Sjekker tilgang til person og at saksbehandler har SAKSBEHANDLER-rollen.
     */
    suspend fun hentForSakId(
        sakId: SakId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Either<KunneIkkeHenteSakForSakId, Sak>

    suspend fun hentBenkOversikt(
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Either<KanIkkeHenteSaksoversikt, Saksoversikt>

    suspend fun hentEnkelPersonForSakId(
        sakId: SakId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Either<no.nav.tiltakspenger.saksbehandling.behandling.service.person.KunneIkkeHenteEnkelPerson, EnkelPersonMedSkjerming>

    /**
     * @param sisteDagSomGirRett er ikke masterdata - bruk vedtak & tidslinje på sak for å finne sisteDagSomGirRett.
     *
     * Ment for å optimalisere db-spørringer (generering av meldeperioder)
     */
    fun oppdaterSisteDagSomGirRett(
        sakId: SakId,
        førsteDagSomGirRett: LocalDate?,
        sisteDagSomGirRett: LocalDate?,
        sessionContext: SessionContext,
    )

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

    data class HarIkkeTilgang(
        val kreverEnAvRollene: Set<Saksbehandlerrolle>,
        val harRollene: Set<Saksbehandlerrolle>,
    ) : KanIkkeStarteSøknadsbehandling
}
