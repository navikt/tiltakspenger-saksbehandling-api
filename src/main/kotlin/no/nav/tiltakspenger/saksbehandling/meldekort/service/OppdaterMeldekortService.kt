package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.personklient.pdl.TilgangsstyringService
import no.nav.tiltakspenger.saksbehandling.behandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.IkkeFunnetException
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.TilgangException
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.KanIkkeOppdatereMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.OppdaterMeldekortKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.oppdaterMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortBehandlingRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.utbetaling.service.SimulerService
import java.lang.IllegalStateException

/**
 * Har ansvar for å ta imot et utfylt meldekort og lagre det.
 */
class OppdaterMeldekortService(
    private val tilgangsstyringService: TilgangsstyringService,
    private val personService: PersonService,
    private val meldekortBehandlingRepo: MeldekortBehandlingRepo,
    private val sakService: SakService,
    private val simulerService: SimulerService,
) {
    private val logger = KotlinLogging.logger {}

    suspend fun oppdaterMeldekort(
        kommando: OppdaterMeldekortKommando,
    ): Either<KanIkkeOppdatereMeldekort, Pair<Sak, MeldekortUnderBehandling>> {
        val sak = hentSak(kommando).getOrElse { return it.left() }
        return sak.oppdaterMeldekort(
            kommando = kommando,
            simuler = { behandling ->
                simulerService.simulerMeldekort(
                    behandling = behandling,
                    forrigeUtbetaling = sak.utbetalinger.lastOrNull(),
                    brukersNavkontor = { behandling.navkontor },
                )
            },
        ).map { (sak, meldekort, simulering) ->
            meldekortBehandlingRepo.oppdater(meldekort, simulering)
            logger.info { "Meldekort under behandling med id ${meldekort.id} oppdatert. Saksbehandler: ${kommando.saksbehandler.navIdent}" }
            Pair(sak, meldekort)
        }
    }

    // TODO jah: Kopiert til [SendMeldekortTilBeslutterService] - lage noe felles?
    private suspend fun hentSak(
        kommando: OppdaterMeldekortKommando,
    ): Either<KanIkkeOppdatereMeldekort, Sak> {
        if (!kommando.saksbehandler.erSaksbehandler()) {
            return KanIkkeOppdatereMeldekort.MåVæreSaksbehandler(
                kommando.saksbehandler.roller,
            ).left()
        }

        kastHvisIkkeTilgangTilPerson(kommando.saksbehandler, kommando.meldekortId, kommando.correlationId)
        val sak = sakService.hentForSakId(kommando.sakId, kommando.saksbehandler, kommando.correlationId)
            .getOrElse { return KanIkkeOppdatereMeldekort.KunneIkkeHenteSak(it).left() }

        val meldekortbehandling = sak.hentMeldekortBehandling(kommando.meldekortId)!!
        val meldeperiode = meldekortbehandling.meldeperiode
        if (!sak.erSisteVersjonAvMeldeperiode(meldeperiode)) {
            throw IllegalStateException("Kan ikke iverksette meldekortbehandling hvor meldeperioden (${meldeperiode.versjon}) ikke er siste versjon av meldeperioden i saken. sakId: ${sak.id}, meldekortId: ${meldekortbehandling.id}")
        }

        return sak.right()
    }

    // TODO jah: Kopiert til [SendMeldekortTilBeslutterService] - lage noe felles?
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
