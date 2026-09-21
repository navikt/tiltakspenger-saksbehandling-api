package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import arrow.core.left
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortbehandlingRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.angre.KanIkkeAngreMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.angre.angreMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock

class AngreMeldekortbehandlingService(
    private val sakService: SakService,
    private val clock: Clock,
    private val meldekortbehandlingRepo: MeldekortbehandlingRepo,
) {

    val logger = KotlinLogging.logger { }

    fun angreMeldekortbehandling(
        sakId: SakId,
        meldekortId: MeldekortId,
        saksbehandler: Saksbehandler,
    ): Either<KanIkkeAngreMeldekortbehandling, Pair<Sak, Meldekortbehandling>> {
        val sak: Sak = sakService.hentForSakId(sakId)
        
        val meldekortbehandlingResonse: Meldekortbehandling = sak.hentMeldekortbehandling(meldekortId)
            ?: return KanIkkeAngreMeldekortbehandling.MeldekortbehandlingFinnesIkke.left()

        return meldekortbehandlingResonse.angreMeldekortbehandling(
            saksbehandler,
            clock,
        ).map { meldekortbehandling ->
            meldekortbehandlingRepo.angreMeldekortbehandlingSendtTilBeslutning(meldekortbehandling)
            sak.oppdaterMeldekortbehandling(meldekortbehandling) to meldekortbehandling
        }
    }
}
