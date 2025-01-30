package no.nav.tiltakspenger.meldekort.service

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import mu.KotlinLogging
import no.nav.tiltakspenger.felles.sikkerlogg
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.HendelseId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.meldekort.domene.opprettMeldekortBehandling
import no.nav.tiltakspenger.meldekort.ports.MeldekortRepo
import no.nav.tiltakspenger.saksbehandling.service.sak.SakService
import no.nav.tiltakspenger.utbetaling.service.NavkontorService

class OpprettMeldekortBehandlingService(
    val sakService: SakService,
    val meldekortRepo: MeldekortRepo,
    val navkontorService: NavkontorService,
    val sessionFactory: SessionFactory,
) {
    private val logger = KotlinLogging.logger {}

    suspend fun opprettBehandling(
        id: HendelseId,
        sakId: SakId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Either<KanIkkeOppretteMeldekortBehandling, Unit> {
        val sak = sakService.hentForSakId(sakId, saksbehandler, correlationId).getOrElse {
            logger.error { "Kunne ikke hente sak med id $sakId" }
            return KanIkkeOppretteMeldekortBehandling.IkkeTilgangTilSak.left()
        }.also {
            if (it.hentMeldekortBehandling(id) != null) {
                logger.error { "Det finnes allerede en behandling av $id: ${it.id}" }
                return KanIkkeOppretteMeldekortBehandling.BehandlingFinnes.left()
            }
        }

        val meldeperiode = sak.hentMeldeperiode(id = id)
        if (meldeperiode == null) {
            logger.error { "Fant ingen meldeperiode med id $id for sak $sakId" }
            return KanIkkeOppretteMeldekortBehandling.IngenMeldeperiode.left()
        }

        val navkontor = Either.catch {
            navkontorService.hentOppfolgingsenhet(sak.fnr)
        }.getOrElse {
            with("Kunne ikke hente navkontor for sak $sakId") {
                logger.error { this }
                sikkerlogg.error(it) { "$this - fnr ${sak.fnr.verdi}" }
            }
            return KanIkkeOppretteMeldekortBehandling.HenteNavkontorFeilet.left()
        }

        val meldekortBehandling = sak.opprettMeldekortBehandling(
            meldeperiode = meldeperiode,
            navkontor = navkontor,
            saksbehandler = saksbehandler,
        )

        sessionFactory.withTransactionContext { tx ->
            meldekortRepo.lagre(meldekortBehandling, tx)
        }

        logger.info { "Opprettet behandling ${meldekortBehandling.id} av meldeperiode hendelse $id for sak $sakId" }

        return Unit.right()
    }
}

sealed interface KanIkkeOppretteMeldekortBehandling {
    data object IkkeTilgangTilSak : KanIkkeOppretteMeldekortBehandling
    data object BehandlingFinnes : KanIkkeOppretteMeldekortBehandling
    data object IngenMeldeperiode : KanIkkeOppretteMeldekortBehandling
    data object HenteNavkontorFeilet : KanIkkeOppretteMeldekortBehandling
}
