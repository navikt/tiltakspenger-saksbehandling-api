package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.opprettMeldekortKorrigering
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortBehandlingRepo
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.sak.SakService

class OpprettMeldekortKorrigeringService(
    val sakService: SakService,
    val meldekortBehandlingRepo: MeldekortBehandlingRepo,
    val sessionFactory: SessionFactory,
) {
    private val logger = KotlinLogging.logger {}

    suspend fun opprettKorrigering(
        meldekortId: MeldekortId,
        sakId: SakId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Either<KanIkkeOppretteMeldekortKorrigering, MeldekortBehandling> {
        val sak = sakService.hentForSakId(sakId, saksbehandler, correlationId).getOrElse {
            logger.error { "Kunne ikke hente sak med id $sakId" }
            return KanIkkeOppretteMeldekortKorrigering.IkkeTilgangTilSak.left()
        }

        val meldekortBehandling = sak.hentMeldekortBehandling(meldekortId) ?: run {
            logger.error { "Fant ikke meldekortbehandlingen $meldekortId på sak $sakId" }
            return KanIkkeOppretteMeldekortKorrigering.BehandlingenFinnesIkke.left()
        }

        if (meldekortBehandling.status != MeldekortBehandlingStatus.GODKJENT) {
            logger.error { "Behandlingen for meldekortet $meldekortId må være godkjent for å opprette korrigering" }
            return KanIkkeOppretteMeldekortKorrigering.BehandlingenIkkeGodkjent.left()
        }

        val meldekortKorrigering = sak.opprettMeldekortKorrigering(
            saksbehandler = saksbehandler,
            meldekortBehandling = meldekortBehandling,
        )

//        sessionFactory.withTransactionContext { tx ->
//            meldekortBehandlingRepo.lagre(meldekortKorrigering, tx)
//        }

        logger.info { "Opprettet korrigering av meldekort ${meldekortBehandling.id} på kjede ${meldekortBehandling.kjedeId} for sak $sakId" }

        return meldekortKorrigering.right()
    }
}

sealed interface KanIkkeOppretteMeldekortKorrigering {
    data object IkkeTilgangTilSak : KanIkkeOppretteMeldekortKorrigering
    data object BehandlingenFinnesIkke : KanIkkeOppretteMeldekortKorrigering
    data object BehandlingenIkkeGodkjent : KanIkkeOppretteMeldekortKorrigering
}
