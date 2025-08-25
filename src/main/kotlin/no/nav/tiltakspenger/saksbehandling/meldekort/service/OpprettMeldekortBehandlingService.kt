package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
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
    ): Either<KanIkkeOppretteMeldekortBehandling, Sak> {
        val sak = sakService.hentForSakId(sakId)

        val navkontor = Either.catch {
            navkontorService.hentOppfolgingsenhet(sak.fnr)
        }.getOrElse {
            with("Kunne ikke hente navkontor for sak $sakId") {
                logger.error(it) { this }
                Sikkerlogg.error(it) { "$this - fnr ${sak.fnr.verdi}" }
            }
            return KanIkkeOppretteMeldekortBehandling.HenteNavkontorFeilet.left()
        }

        val (oppdatertSak, meldekortBehandling) = Either.catch {
            sak.opprettManuellMeldekortBehandling(
                kjedeId = kjedeId,
                navkontor = navkontor,
                saksbehandler = saksbehandler,
                clock = clock,
            )
        }.getOrElse {
            // TODO jah: Bør ikke styre flyt med throw - catch. Bytt til Either hvis det trengs.
            logger.error(it) { "Kunne ikke opprette meldekort behandling på kjede $kjedeId for sak $sakId" }
            return KanIkkeOppretteMeldekortBehandling.KanIkkeOpprettePåKjede.left()
        }

        sessionFactory.withTransactionContext { tx ->
            meldekortBehandlingRepo.lagre(meldekortBehandling, null, tx)
        }

        logger.info { "Opprettet behandling ${meldekortBehandling.id} på meldeperiode kjede $kjedeId for sak $sakId" }

        return oppdatertSak.right()
    }
}

sealed interface KanIkkeOppretteMeldekortBehandling {
    data object HenteNavkontorFeilet : KanIkkeOppretteMeldekortBehandling
    data object KanIkkeOpprettePåKjede : KanIkkeOppretteMeldekortBehandling
}
