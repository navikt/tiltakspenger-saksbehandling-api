package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.felles.sikkerlogg
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.opprettMeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortBehandlingRepo
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.utbetaling.service.NavkontorService
import java.time.Clock

class OpprettMeldekortBehandlingService(
    val sakService: SakService,
    val meldekortBehandlingRepo: MeldekortBehandlingRepo,
    val navkontorService: NavkontorService,
    val sessionFactory: SessionFactory,
    private val clock: Clock,
) {
    private val logger = KotlinLogging.logger {}

    suspend fun opprettBehandling(
        kjedeId: MeldeperiodeKjedeId,
        sakId: SakId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Either<KanIkkeOppretteMeldekortBehandling, MeldekortBehandling> {
        val sak = sakService.hentForSakId(sakId, saksbehandler, correlationId).getOrElse {
            logger.error { "Kunne ikke hente sak med id $sakId" }
            return KanIkkeOppretteMeldekortBehandling.IkkeTilgangTilSak.left()
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

        val meldekortBehandling = Either.catch {
            sak.opprettMeldekortBehandling(
                kjedeId = kjedeId,
                navkontor = navkontor,
                saksbehandler = saksbehandler,
                clock = clock,
            )
        }.getOrElse {
            return KanIkkeOppretteMeldekortBehandling.KanIkkeOpprettePåKjede.left()
        }

        sessionFactory.withTransactionContext { tx ->
            meldekortBehandlingRepo.lagre(meldekortBehandling, tx)
        }

        logger.info { "Opprettet behandling ${meldekortBehandling.id} på meldeperiode kjede $kjedeId for sak $sakId" }

        return meldekortBehandling.right()
    }
}

sealed interface KanIkkeOppretteMeldekortBehandling {
    data object IkkeTilgangTilSak : KanIkkeOppretteMeldekortBehandling
    data object HenteNavkontorFeilet : KanIkkeOppretteMeldekortBehandling
    data object KanIkkeOpprettePåKjede : KanIkkeOppretteMeldekortBehandling
}
