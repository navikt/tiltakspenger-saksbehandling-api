package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.ValiderOpprettMeldekortbehandlingFeil
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.opprettManuellMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortbehandlingRepo
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.NavkontorService
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock

class OpprettMeldekortbehandlingService(
    val sakService: SakService,
    val meldekortbehandlingRepo: MeldekortbehandlingRepo,
    val navkontorService: NavkontorService,
    val sessionFactory: SessionFactory,
    private val clock: Clock,
) {
    private val logger = KotlinLogging.logger {}
    data class OpprettMeldekortbehandlingKommando(
        val sakId: SakId,
        val kjedeId: MeldeperiodeKjedeId,
        val saksbehandler: Saksbehandler,
        val klagebehandlingId: KlagebehandlingId? = null,
        val correlationId: CorrelationId = CorrelationId.generate(),
    )

    suspend fun opprettBehandling(
        kommando: OpprettMeldekortbehandlingKommando,
    ): Either<KanIkkeOppretteMeldekortbehandling, Pair<Sak, Meldekortbehandling>> {
        val sak = sakService.hentForSakId(kommando.sakId)

        val navkontor = Either.catch {
            navkontorService.hentOppfolgingsenhet(
                fnr = sak.fnr,
                sakId = sak.id.toString(),
                saksnummer = sak.saksnummer.verdi,
            )
        }.getOrElse {
            with("Kunne ikke hente navkontor for sak ${kommando.sakId}") {
                logger.error(it) { this }
                Sikkerlogg.error(it) { "$this - fnr ${sak.fnr.verdi}" }
            }
            return KanIkkeOppretteMeldekortbehandling.HenteNavKontorFeilet.left()
        }

        val (oppdatertSak, meldekortbehandling) = sak.opprettManuellMeldekortbehandling(
            kjedeId = kommando.kjedeId,
            navkontor = navkontor,
            saksbehandler = kommando.saksbehandler,
            clock = clock,
            klagebehandlingId = kommando.klagebehandlingId,
        ).getOrElse {
            return it.left()
        }

        sessionFactory.withTransactionContext { tx ->
            meldekortbehandlingRepo.lagre(meldekortbehandling, null, tx)
        }

        logger.info { "Opprettet behandling ${meldekortbehandling.id} på meldeperiode kjede ${kommando.kjedeId} for sak ${kommando.sakId}" }

        return (oppdatertSak to meldekortbehandling).right()
    }
}

sealed interface KanIkkeOppretteMeldekortbehandling {
    data object HenteNavKontorFeilet : KanIkkeOppretteMeldekortbehandling

    data class ValiderOpprettFeil(
        val feil: ValiderOpprettMeldekortbehandlingFeil,
    ) : KanIkkeOppretteMeldekortbehandling

    data class SaksbehandlerMismatch(
        val forventetSaksbehandler: String?,
        val faktiskSaksbehandler: String,
    ) : KanIkkeOppretteMeldekortbehandling

    data class FinnesÅpenBehandling(
        val behandlingId: BehandlingId,
    ) : KanIkkeOppretteMeldekortbehandling
}
