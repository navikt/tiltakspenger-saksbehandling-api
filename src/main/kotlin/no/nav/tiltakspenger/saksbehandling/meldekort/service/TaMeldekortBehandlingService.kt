package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.personklient.pdl.TilgangsstyringService
import no.nav.tiltakspenger.saksbehandling.behandling.domene.KanIkkeTaBehandling
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.TilgangException
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortBehandlingRepo

class TaMeldekortBehandlingService(
    private val tilgangsstyringService: TilgangsstyringService,
    private val meldekortBehandlingRepo: MeldekortBehandlingRepo,
) {
    val logger = KotlinLogging.logger { }

    suspend fun taMeldekortBehandling(
        meldekortId: MeldekortId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Either<KanIkkeTaBehandling, MeldekortBehandling> {
        val meldekortBehandling = meldekortBehandlingRepo.hent(meldekortId)
            ?: throw IllegalStateException("Fant ikke meldekortBehandling for id $meldekortId")
        tilgangsstyringService.harTilgangTilPerson(meldekortBehandling.fnr, saksbehandler.roller, correlationId)
            .onLeft {
                throw TilgangException("Feil ved tilgangssjekk til person når beslutter tar behandling. Feilen var $it")
            }.onRight {
                if (!it) throw TilgangException("Saksbehandler ${saksbehandler.navIdent} har ikke tilgang til person")
            }

        if (!saksbehandler.erBeslutter()) {
            logger.warn { "Navident ${saksbehandler.navIdent} med rollene ${saksbehandler.roller} har ikke tilgang til å ta behandling" }
            return KanIkkeTaBehandling.MåVæreSaksbehandlerEllerBeslutter.left()
        }

        return meldekortBehandling.taMeldekortBehandling(saksbehandler).also {
            require(it.status == MeldekortBehandlingStatus.UNDER_BESLUTNING)
            meldekortBehandlingRepo.taBehandlingBeslutter(
                it.id,
                saksbehandler,
                it.status,
            )
        }.right()
    }
}
