package no.nav.tiltakspenger.saksbehandling.klage.service

import arrow.core.Either
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.ferdigstill.FerdigstillKlagebehandlingCommand
import no.nav.tiltakspenger.saksbehandling.klage.domene.ferdigstill.KunneIkkeFerdigstilleKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.ferdigstill.ferdigstill
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagebehandlingRepo
import java.time.Clock

class FerdigstillKlagebehandlingService(
    private val klagebehandlingRepo: KlagebehandlingRepo,
    private val clock: Clock,
) {
    fun ferdigstill(command: FerdigstillKlagebehandlingCommand): Either<KunneIkkeFerdigstilleKlagebehandling, Klagebehandling> {
        val klagebehandling = klagebehandlingRepo.hentForKlagebehandlingId(command.klagebehandlingId)
            ?: throw IllegalStateException("Finner ikke klagebehandling med id ${command.klagebehandlingId}")

        return klagebehandling.ferdigstill(command, clock).onRight {
            klagebehandlingRepo.lagreKlagebehandling(it)
        }
    }
}
