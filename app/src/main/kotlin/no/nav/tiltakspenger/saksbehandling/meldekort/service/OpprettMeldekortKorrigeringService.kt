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
import no.nav.tiltakspenger.saksbehandling.felles.sikkerlogg
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.opprettMeldekortKorrigering
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortBehandlingRepo
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.utbetaling.service.NavkontorService

class OpprettMeldekortKorrigeringService(
    val sakService: SakService,
    val meldekortBehandlingRepo: MeldekortBehandlingRepo,
    val navkontorService: NavkontorService,
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

        val navkontor = Either.catch {
            navkontorService.hentOppfolgingsenhet(sak.fnr)
        }.getOrElse {
            with("Kunne ikke hente navkontor for sak $sakId") {
                logger.error { this }
                sikkerlogg.error(it) { "$this - fnr ${sak.fnr.verdi}" }
            }
            return KanIkkeOppretteMeldekortKorrigering.HenteNavkontorFeilet.left()
        }

        val meldekortKorrigering = Either.catch {
            sak.opprettMeldekortKorrigering(
                saksbehandler = saksbehandler,
                navkontor = navkontor,
                kjedeId = kjedeId,
            )
        }.getOrElse {
            return KanIkkeOppretteMeldekortKorrigering.KanIkkeKorrigerePåKjede.left()
        }

        sessionFactory.withTransactionContext { tx ->
            meldekortBehandlingRepo.lagre(meldekortKorrigering, tx)
        }

        logger.info { "Opprettet korrigering av meldekort: ${meldekortKorrigering.id} for kjede $kjedeId på sak $sakId" }

        return meldekortKorrigering.right()
    }
}

sealed interface KanIkkeOppretteMeldekortKorrigering {
    data object IkkeTilgangTilSak : KanIkkeOppretteMeldekortKorrigering
    data object HenteNavkontorFeilet : KanIkkeOppretteMeldekortKorrigering
    data object KanIkkeKorrigerePåKjede : KanIkkeOppretteMeldekortKorrigering
}
