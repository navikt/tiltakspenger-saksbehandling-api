package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import arrow.core.Either
import arrow.core.left
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RammebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.konverter.KanIkkeKonvertereRevurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.konverter.KonverterRevurderingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.konverter.konverterRevurdering
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock

class KonverterRevurderingService(
    private val sakService: SakService,
    private val rammebehandlingRepo: RammebehandlingRepo,
    private val clock: Clock,
    private val forlengelseErPåslått: Boolean,
) {
    fun konverter(kommando: KonverterRevurderingKommando): Either<KanIkkeKonvertereRevurdering, Pair<Sak, Revurdering>> {
        if (kommando is KonverterRevurderingKommando.TilForlengelse && !forlengelseErPåslått) {
            return KanIkkeKonvertereRevurdering.ForlengelseErIkkePåslått.left()
        }

        val sak = sakService.hentForSakId(kommando.sakId)

        return sak.konverterRevurdering(kommando, clock).onRight { (_, revurdering) ->
            rammebehandlingRepo.lagre(revurdering)
        }
    }
}
