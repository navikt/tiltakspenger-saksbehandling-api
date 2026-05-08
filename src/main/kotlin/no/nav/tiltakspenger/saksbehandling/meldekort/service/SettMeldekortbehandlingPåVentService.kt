package no.nav.tiltakspenger.saksbehandling.meldekort.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
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
    ): Pair<Sak, Meldekortbehandling> {
        val sak = sakService.hentForSakId(kommando.sakId)
        val meldekortbehandling = sak.hentMeldekortbehandling(kommando.meldekortId)!!

        val oppdatertMeldekortbehandling = meldekortbehandling.settPåVent(kommando, clock)
            .also {
                meldekortbehandlingRepo.oppdater(it)
                logger.info { "Meldekortbehandling med id ${it.id} satt på vent. Saksbehandler: ${kommando.saksbehandler.navIdent}" }
            }

        return sak.oppdaterMeldekortbehandling(oppdatertMeldekortbehandling) to oppdatertMeldekortbehandling
    }
}
