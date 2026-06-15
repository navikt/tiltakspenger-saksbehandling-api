package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.settPåVent.KanIkkeSetteMeldekortbehandlingPåVent
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.settPåVent.SettMeldekortbehandlingPåVentKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.settPåVent.settPåVent
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortbehandlingRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock

class SettMeldekortbehandlingPåVentService(
    private val meldekortbehandlingRepo: MeldekortbehandlingRepo,
    private val sakService: SakService,
    private val clock: Clock,
) {
    private val logger = KotlinLogging.logger {}

    fun settPåVent(
        kommando: SettMeldekortbehandlingPåVentKommando,
    ): Either<KanIkkeSetteMeldekortbehandlingPåVent, Pair<Sak, Meldekortbehandling>> {
        val sak = sakService.hentForSakId(kommando.sakId)
        val meldekortbehandling = sak.hentMeldekortbehandling(kommando.meldekortId)!!

        return meldekortbehandling.settPåVent(kommando, clock).map { oppdatertMeldekortbehandling ->
            meldekortbehandlingRepo.oppdater(oppdatertMeldekortbehandling)
            logger.info { "Meldekortbehandling med id ${oppdatertMeldekortbehandling.id} satt på vent. Saksbehandler: ${kommando.saksbehandler.navIdent}" }
            sak.oppdaterMeldekortbehandling(oppdatertMeldekortbehandling) to oppdatertMeldekortbehandling
        }
    }
}
