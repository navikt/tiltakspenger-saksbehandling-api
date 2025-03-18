package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.personklient.pdl.TilgangsstyringService
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.IkkeFunnetException
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.TilgangException
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.KanIkkeSendeMeldekortTilBeslutning
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.SendMeldekortTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortBehandlingRepo
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.sak.SakService
import java.lang.IllegalStateException

/**
 * Har ansvar for å ta imot et utfylt meldekort og sende det til beslutter.
 */
class SendMeldekortTilBeslutningService(
    private val tilgangsstyringService: TilgangsstyringService,
    private val personService: PersonService,
    private val meldekortBehandlingRepo: MeldekortBehandlingRepo,
    private val sakService: SakService,
) {
    private val logger = KotlinLogging.logger {}

    /**
     * @throws IllegalStateException Dersom vi ikke fant saken.
     */
    suspend fun sendMeldekortTilBeslutter(
        kommando: SendMeldekortTilBeslutningKommando,
    ): Either<KanIkkeSendeMeldekortTilBeslutning, MeldekortBehandling.MeldekortBehandlet> {
        if (!kommando.saksbehandler.erSaksbehandler()) {
            return KanIkkeSendeMeldekortTilBeslutning.MåVæreSaksbehandler(
                kommando.saksbehandler.roller,
            ).left()
        }

        kastHvisIkkeTilgangTilPerson(kommando.saksbehandler, kommando.meldekortId, kommando.correlationId)
        val sak = sakService.hentForSakId(kommando.sakId, kommando.saksbehandler, kommando.correlationId)
            .getOrElse { return KanIkkeSendeMeldekortTilBeslutning.KunneIkkeHenteSak(it).left() }

        val meldekortbehandling = sak.hentMeldekortBehandling(kommando.meldekortId)!!
        val meldeperiode = meldekortbehandling.meldeperiode
        if (!sak.erSisteVersjonAvMeldeperiode(meldeperiode)) {
            throw IllegalStateException("Kan ikke iverksette meldekortbehandling hvor meldeperioden (${meldeperiode.versjon}) ikke er siste versjon av meldeperioden i saken. sakId: ${sak.id}, meldekortId: ${meldekortbehandling.id}")
        }
        return sak.meldekortBehandlinger
            .sendTilBeslutter(kommando, sak.barnetilleggsperioder, sak.tiltakstypeperioder)
            .map { it.second }
            .onRight {
                meldekortBehandlingRepo.oppdater(it)
                logger.info { "Meldekort med id ${it.id} sendt til beslutter. Saksbehandler: ${kommando.saksbehandler.navIdent}" }
            }
    }

    private suspend fun kastHvisIkkeTilgangTilPerson(
        saksbehandler: Saksbehandler,
        meldekortId: MeldekortId,
        correlationId: CorrelationId,
    ) {
        val fnr = personService.hentFnrForMeldekortId(meldekortId)
        tilgangsstyringService.harTilgangTilPerson(
            fnr = fnr,
            roller = saksbehandler.roller,
            correlationId = correlationId,
        ).onLeft {
            throw IkkeFunnetException("Feil ved sjekk av tilgang til person. meldekortId: $meldekortId. CorrelationId: $correlationId")
        }.onRight {
            if (!it) throw TilgangException("Saksbehandler ${saksbehandler.navIdent} har ikke tilgang til person")
        }
    }
}
