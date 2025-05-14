package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.opprettManuellMeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortBehandlingRepo
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.NavkontorService
import no.nav.tiltakspenger.saksbehandling.sak.Sak
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
    ): Either<KanIkkeOppretteMeldekortBehandling, Sak> {
        val sak = sakService.hentForSakId(sakId, saksbehandler, correlationId).getOrElse {
            logger.error { "Kunne ikke hente sak med id $sakId" }
            return KanIkkeOppretteMeldekortBehandling.IkkeTilgangTilSak.left()
        }

        val navkontor = Either.catch {
            navkontorService.hentOppfolgingsenhet(sak.fnr)
        }.getOrElse {
            with("Kunne ikke hente navkontor for sak $sakId") {
                logger.error(it) { this }
                Sikkerlogg.error(it) { "$this - fnr ${sak.fnr.verdi}" }
            }
            return KanIkkeOppretteMeldekortBehandling.HenteNavkontorFeilet.left()
        }

        val meldekortBehandling = Either.catch {
            sak.opprettManuellMeldekortBehandling(
                kjedeId = kjedeId,
                navkontor = navkontor,
                saksbehandler = saksbehandler,
                clock = clock,
            )
        }.getOrElse {
            logger.error(it) { "Kunne ikke opprette meldekort behandling på kjede $kjedeId for sak $sakId" }
            return KanIkkeOppretteMeldekortBehandling.KanIkkeOpprettePåKjede.left()
        }
        // Ikke fjern denne, vi må verifisere at vi kan legge til den nye behandlingen før vi persisterer
        val oppdatertSak = sak.leggTilMeldekortbehandling(meldekortBehandling)

        sessionFactory.withTransactionContext { tx ->
            meldekortBehandlingRepo.lagre(meldekortBehandling, null, tx)
        }

        logger.info { "Opprettet behandling ${meldekortBehandling.id} på meldeperiode kjede $kjedeId for sak $sakId" }

        return oppdatertSak.right()
    }
}

sealed interface KanIkkeOppretteMeldekortBehandling {
    data object IkkeTilgangTilSak : KanIkkeOppretteMeldekortBehandling
    data object HenteNavkontorFeilet : KanIkkeOppretteMeldekortBehandling
    data object KanIkkeOpprettePåKjede : KanIkkeOppretteMeldekortBehandling
}
