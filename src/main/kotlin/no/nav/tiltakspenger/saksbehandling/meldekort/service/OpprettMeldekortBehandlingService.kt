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
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.SkalLagreEllerOppdatere
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.ValiderOpprettMeldekortbehandlingFeil
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.opprettManuellMeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortBehandlingRepo
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.NavkontorService
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock

// Brukes både for å opprette en ny meldekortbehandling, og for å ta opp en meldekortbehandling som har blitt lagt tilbake
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
    ): Either<KanIkkeOppretteMeldekortbehandling, Pair<Sak, MeldekortBehandling>> {
        val sak = sakService.hentForSakId(sakId)

        val navkontor = Either.catch {
            navkontorService.hentOppfolgingsenhet(sak.fnr)
        }.getOrElse {
            with("Kunne ikke hente navkontor for sak $sakId") {
                logger.error(it) { this }
                Sikkerlogg.error(it) { "$this - fnr ${sak.fnr.verdi}" }
            }
            return KanIkkeOppretteMeldekortbehandling.HenteNavKontorFeilet.left()
        }

        val (oppdatertSak, meldekortBehandling, skalLagreEllerOppdatere) = sak.opprettManuellMeldekortBehandling(
            kjedeId = kjedeId,
            navkontor = navkontor,
            saksbehandler = saksbehandler,
            clock = clock,
        ).getOrElse {
            return it.left()
        }

        when (skalLagreEllerOppdatere) {
            SkalLagreEllerOppdatere.Lagre -> sessionFactory.withTransactionContext { tx ->
                meldekortBehandlingRepo.lagre(meldekortBehandling, null, tx)
            }

            SkalLagreEllerOppdatere.Oppdatere -> sessionFactory.withTransactionContext { tx ->
                meldekortBehandlingRepo.oppdater(meldekortBehandling, null, tx)
            }
        }

        logger.info { "Opprettet behandling ${meldekortBehandling.id} på meldeperiode kjede $kjedeId for sak $sakId" }

        return (oppdatertSak to meldekortBehandling).right()
    }
}

sealed interface KanIkkeOppretteMeldekortbehandling {
    data object HenteNavKontorFeilet : KanIkkeOppretteMeldekortbehandling

    data class ValiderOpprettFeil(
        val feil: ValiderOpprettMeldekortbehandlingFeil,
    ) : KanIkkeOppretteMeldekortbehandling
}
