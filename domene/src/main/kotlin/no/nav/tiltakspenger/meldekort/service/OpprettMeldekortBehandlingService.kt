package no.nav.tiltakspenger.meldekort.service

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import mu.KotlinLogging
import no.nav.tiltakspenger.felles.sikkerlogg
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.meldekort.domene.BrukersMeldekortRepo
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.meldekort.domene.opprettMeldekortBehandling
import no.nav.tiltakspenger.meldekort.ports.MeldekortBehandlingRepo
import no.nav.tiltakspenger.saksbehandling.service.sak.SakService
import no.nav.tiltakspenger.utbetaling.service.NavkontorService

class OpprettMeldekortBehandlingService(
    val sakService: SakService,
    val meldekortBehandlingRepo: MeldekortBehandlingRepo,
    val brukersMeldekortRepo: BrukersMeldekortRepo,
    val navkontorService: NavkontorService,
    val sessionFactory: SessionFactory,
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
        }.also {
            if (it.hentMeldekortBehandlingForMeldeperiodeKjedeId(kjedeId).isNotEmpty()) {
                logger.error { "Det finnes allerede en behandling av $kjedeId: ${it.id}" }
                return KanIkkeOppretteMeldekortBehandling.BehandlingFinnes.left()
            }
        }

        val meldeperiode: Meldeperiode = sak.hentMeldeperiodeForKjedeId(kjedeId = kjedeId)

        val navkontor = Either.catch {
            navkontorService.hentOppfolgingsenhet(sak.fnr)
        }.getOrElse {
            with("Kunne ikke hente navkontor for sak $sakId") {
                logger.error { this }
                sikkerlogg.error(it) { "$this - fnr ${sak.fnr.verdi}" }
            }
            return KanIkkeOppretteMeldekortBehandling.HenteNavkontorFeilet.left()
        }

        val brukersMeldekort = brukersMeldekortRepo.hentForMeldeperiodeId(meldeperiode.id)

        val meldekortBehandling = sak.opprettMeldekortBehandling(
            meldeperiode = meldeperiode,
            navkontor = navkontor,
            saksbehandler = saksbehandler,
            brukersMeldekort = brukersMeldekort,
        )

        sessionFactory.withTransactionContext { tx ->
            meldekortBehandlingRepo.lagre(meldekortBehandling, tx)
        }

        logger.info { "Opprettet behandling ${meldekortBehandling.id} på meldeperiode kjede $kjedeId for sak $sakId" }

        return meldekortBehandling.right()
    }
}

sealed interface KanIkkeOppretteMeldekortBehandling {
    data object IkkeTilgangTilSak : KanIkkeOppretteMeldekortBehandling
    data object BehandlingFinnes : KanIkkeOppretteMeldekortBehandling
    data object IngenMeldeperiode : KanIkkeOppretteMeldekortBehandling
    data object HenteNavkontorFeilet : KanIkkeOppretteMeldekortBehandling
}
