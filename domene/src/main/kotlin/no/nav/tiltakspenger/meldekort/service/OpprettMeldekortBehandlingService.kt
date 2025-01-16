package no.nav.tiltakspenger.meldekort.service

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.HendelseId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.meldekort.domene.opprettMeldekortBehandling
import no.nav.tiltakspenger.meldekort.ports.MeldekortRepo
import no.nav.tiltakspenger.saksbehandling.service.sak.SakService

class OpprettMeldekortBehandlingService(
    val sakService: SakService,
    val meldekortRepo: MeldekortRepo,
    val sessionFactory: SessionFactory,
) {
    private val logger = KotlinLogging.logger {}

    suspend fun opprettBehandling(
        hendelseId: HendelseId,
        sakId: SakId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Either<KanIkkeOppretteMeldekortBehandling, Unit> {
        val sak = sakService.hentForSakId(sakId, saksbehandler, correlationId).getOrElse {
            logger.error { "Kunne ikke hente sak med id $sakId" }
            return KanIkkeOppretteMeldekortBehandling.IkkeTilgangTilSak.left()
        }.also {
            if (it.hentMeldekortBehandling(hendelseId) != null) {
                logger.error { "Det finnes allerede en behandling av $hendelseId: ${it.id}" }
                return KanIkkeOppretteMeldekortBehandling.BehandlingFinnes.left()
            }
        }

        val meldeperiode = sak.hentMeldeperiode(hendelseId = hendelseId)
        if (meldeperiode == null) {
            logger.error { "Fant ingen meldeperiode med id $hendelseId for sak $sakId" }
            return KanIkkeOppretteMeldekortBehandling.IngenMeldeperiode.left()
        }

        // TODO: Hent denne fra pdl/norg2 når funksjonaliteten for det er på plass
        val navkontor = sak.meldekortBehandlinger.sisteGodkjenteMeldekort?.navkontor
        val meldekortBehandling = sak.opprettMeldekortBehandling(meldeperiode, navkontor)

        sessionFactory.withTransactionContext { tx ->
            meldekortRepo.lagre(meldekortBehandling, tx)
        }

        logger.info { "Opprettet behandling av meldeperiode $hendelseId for sak $sakId" }

        return Unit.right()
    }
}

sealed interface KanIkkeOppretteMeldekortBehandling {
    data object IkkeTilgangTilSak : KanIkkeOppretteMeldekortBehandling
    data object BehandlingFinnes : KanIkkeOppretteMeldekortBehandling
    data object IngenMeldeperiode : KanIkkeOppretteMeldekortBehandling
}
