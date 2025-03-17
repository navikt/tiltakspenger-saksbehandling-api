package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.MeldeperiodeKjedeId
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
        kjedeId: MeldeperiodeKjedeId,
        sakId: SakId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Either<KanIkkeOppretteMeldekortKorrigering, MeldekortBehandling> {
        val sak = sakService.hentForSakId(sakId, saksbehandler, correlationId).getOrElse {
            logger.error { "Kunne ikke hente sak med id $sakId" }
            return KanIkkeOppretteMeldekortKorrigering.IkkeTilgangTilSak.left()
        }

        val meldekortBehandling = sak.hentSisteMeldekortBehandlingForKjede(kjedeId) ?: run {
            logger.error { "Fant ingen meldekortbehandlinger (kjede $kjedeId på sak $sakId)" }
            return KanIkkeOppretteMeldekortKorrigering.IngenBehandlinger.left()
        }

        if (meldekortBehandling.status != MeldekortBehandlingStatus.GODKJENT) {
            logger.error { "Siste behandling i kjeden må være godkjent for å opprette en ny korrigering (kjede $kjedeId på sak $sakId)" }
            return KanIkkeOppretteMeldekortKorrigering.SisteBehandlingIkkeGodkjent.left()
        }

        val meldekortKorrigering = sak.opprettMeldekortKorrigering(
            saksbehandler = saksbehandler,
            forrigeBehandling = meldekortBehandling,
        )

        sessionFactory.withTransactionContext { tx ->
            meldekortBehandlingRepo.lagre(meldekortKorrigering, tx)
        }

        logger.info { "Opprettet korrigering av meldekort ${meldekortKorrigering.id} for kjede ${meldekortBehandling.kjedeId} på sak $sakId" }

        return meldekortKorrigering.right()
    }
}

sealed interface KanIkkeOppretteMeldekortKorrigering {
    data object IkkeTilgangTilSak : KanIkkeOppretteMeldekortKorrigering
    data object IngenBehandlinger : KanIkkeOppretteMeldekortKorrigering
    data object SisteBehandlingIkkeGodkjent : KanIkkeOppretteMeldekortKorrigering
}
