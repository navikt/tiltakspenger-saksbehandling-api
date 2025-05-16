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
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.KanIkkeSendeMeldekortTilBeslutter
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandletManuelt
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.SendMeldekortTilBeslutterKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.sendMeldekortTilBeslutter
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortBehandlingRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.utbetaling.service.SimulerService
import java.time.Clock

/**
 * Har ansvar for å sende et meldekort til beslutter og evt. lagre dager/begrunnelse dersom det sendes med.
 */
class SendMeldekortTilBeslutterService(
    private val tilgangsstyringService: TilgangsstyringService,
    private val personService: PersonService,
    private val meldekortBehandlingRepo: MeldekortBehandlingRepo,
    private val sakService: SakService,
    private val simulerService: SimulerService,
    private val clock: Clock,
) {
    private val logger = KotlinLogging.logger {}

    suspend fun sendMeldekortTilBeslutter(
        kommando: SendMeldekortTilBeslutterKommando,
    ): Either<KanIkkeSendeMeldekortTilBeslutter, Pair<Sak, MeldekortBehandletManuelt>> {
        val sak = hentSak(kommando).getOrElse { return it.left() }

        return sak.sendMeldekortTilBeslutter(
            kommando = kommando,
            simuler = { behandling ->
                simulerService.simulerMeldekort(
                    behandling = behandling,
                    forrigeUtbetaling = sak.utbetalinger.lastOrNull(),
                    meldeperiodeKjeder = sak.meldeperiodeKjeder,
                    brukersNavkontor = { behandling.navkontor },
                )
            },
            clock = clock,
        ).map { (sak, meldekort, simulering) ->
            meldekortBehandlingRepo.oppdater(meldekort, simulering)
            logger.info { "Meldekort med id ${meldekort.id} sendt til beslutter. Saksbehandler: ${kommando.saksbehandler.navIdent}" }
            Pair(sak, meldekort)
        }
    }

    // TODO jah: Kopiert fra [OppdaterMeldekortService] - lage noe felles?
    private suspend fun hentSak(
        kommando: SendMeldekortTilBeslutterKommando,
    ): Either<KanIkkeSendeMeldekortTilBeslutter, Sak> {
        if (!kommando.saksbehandler.erSaksbehandler()) {
            return KanIkkeSendeMeldekortTilBeslutter.MåVæreSaksbehandler(
                kommando.saksbehandler.roller,
            ).left()
        }

        kastHvisIkkeTilgangTilPerson(kommando.saksbehandler, kommando.meldekortId, kommando.correlationId)
        val sak = sakService.hentForSakId(kommando.sakId, kommando.saksbehandler, kommando.correlationId)
            .getOrElse { return KanIkkeSendeMeldekortTilBeslutter.KunneIkkeHenteSak(it).left() }

        val meldekortbehandling = sak.hentMeldekortBehandling(kommando.meldekortId)!!
        val meldeperiode = meldekortbehandling.meldeperiode
        if (!sak.erSisteVersjonAvMeldeperiode(meldeperiode)) {
            throw IllegalStateException("Kan ikke iverksette meldekortbehandling hvor meldeperioden (${meldeperiode.versjon}) ikke er siste versjon av meldeperioden i saken. sakId: ${sak.id}, meldekortId: ${meldekortbehandling.id}")
        }

        return sak.right()
    }

    // TODO jah: Kopiert fra [OppdaterMeldekortService] - lage noe felles?
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
