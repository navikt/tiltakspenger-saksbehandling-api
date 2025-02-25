package no.nav.tiltakspenger.saksbehandling.service.sak

import arrow.core.Either
import no.nav.tiltakspenger.felles.Systembruker
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.Saksbehandlerrolle
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandling
import no.nav.tiltakspenger.saksbehandling.domene.behandling.KanIkkeOppretteBehandling
import no.nav.tiltakspenger.saksbehandling.domene.benk.Saksoversikt
import no.nav.tiltakspenger.saksbehandling.domene.personopplysninger.EnkelPersonMedSkjerming
import no.nav.tiltakspenger.saksbehandling.domene.sak.Sak
import no.nav.tiltakspenger.saksbehandling.domene.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.service.person.KunneIkkeHenteEnkelPerson

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
    ): Either<KunneIkkeHenteEnkelPerson, EnkelPersonMedSkjerming>
}

sealed interface KanIkkeStarteSøknadsbehandling {
    data class HarAlleredeStartetBehandlingen(
        val behandling: Behandling,
    ) : KanIkkeStarteSøknadsbehandling

    data class OppretteBehandling(
        val underliggende: KanIkkeOppretteBehandling,
    ) : KanIkkeStarteSøknadsbehandling

    data class HarIkkeTilgang(
        val kreverEnAvRollene: Set<Saksbehandlerrolle>,
        val harRollene: Set<Saksbehandlerrolle>,
    ) : KanIkkeStarteSøknadsbehandling
}
