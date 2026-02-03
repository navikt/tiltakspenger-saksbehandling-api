package no.nav.tiltakspenger.saksbehandling.klage.service

import arrow.core.Either
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.settPåVent.KanIkkeSetteKlagebehandlingPåVent
import no.nav.tiltakspenger.saksbehandling.klage.domene.settPåVent.SettKlagebehandlingPåVentKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.settPåVent.settKlagebehandlingPåVent
import no.nav.tiltakspenger.saksbehandling.klage.domene.vurder.KanIkkeVurdereKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.vurder.VurderKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.vurderKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock

class SettKlagebehandlingPåVentService(
    private val sakService: SakService,
    private val klagebehandlingRepo: KlagebehandlingRepo,
    private val clock: Clock,
) {
    fun settPåVent(
        kommando: SettKlagebehandlingPåVentKommando,
    ): Either<KanIkkeSetteKlagebehandlingPåVent, Pair<Sak, Klagebehandling>> {
        val sak: Sak = sakService.hentForSakId(kommando.sakId)

        return sak.settKlagebehandlingPåVent(kommando, clock).onRight {
            klagebehandlingRepo.lagreKlagebehandling(it.second)
        }
    }
}
