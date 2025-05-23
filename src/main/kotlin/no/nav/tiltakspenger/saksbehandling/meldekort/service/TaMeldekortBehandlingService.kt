package no.nav.tiltakspenger.saksbehandling.meldekort.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.personklient.pdl.TilgangsstyringService
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.krevSaksbehandlerEllerBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.krevTilgangTilPerson
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
    ): MeldekortBehandling {
        val meldekortBehandling = meldekortBehandlingRepo.hent(meldekortId)!!
        tilgangsstyringService.krevTilgangTilPerson(saksbehandler, meldekortBehandling.fnr, correlationId)

        krevSaksbehandlerEllerBeslutterRolle(saksbehandler)

        return meldekortBehandling.taMeldekortBehandling(saksbehandler).also {
            when (it.status) {
                MeldekortBehandlingStatus.UNDER_BEHANDLING -> meldekortBehandlingRepo.taBehandlingSaksbehandler(
                    it.id,
                    saksbehandler,
                    it.status,
                )

                MeldekortBehandlingStatus.UNDER_BESLUTNING -> meldekortBehandlingRepo.taBehandlingBeslutter(
                    it.id,
                    saksbehandler,
                    it.status,
                )

                MeldekortBehandlingStatus.KLAR_TIL_BESLUTNING,
                MeldekortBehandlingStatus.GODKJENT,
                MeldekortBehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER,
                MeldekortBehandlingStatus.AUTOMATISK_BEHANDLET,
                MeldekortBehandlingStatus.AVBRUTT,
                -> throw IllegalStateException("Meldekortbehandlingen er i en ugyldig status for å kunne tildele seg selv")
            }
        }
    }
}
