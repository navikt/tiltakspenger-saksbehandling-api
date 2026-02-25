package no.nav.tiltakspenger.saksbehandling.klage.domene.ferdigstill

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus
import java.time.Clock

fun Klagebehandling.ferdigstill(
    command: FerdigstillKlagebehandlingCommand,
    clock: Clock,
): Either<KunneIkkeFerdigstilleKlagebehandling, Klagebehandling> {
    if (!erSaksbehandlerPåBehandlingen(command.saksbehandler)) {
        return KunneIkkeFerdigstilleKlagebehandling.SaksbehandlerMismatch(
            forventetSaksbehandler = this.saksbehandler,
            faktiskSaksbehandler = command.saksbehandler.navIdent,
        ).left()
    }

    if (this.resultat !is Klagebehandlingsresultat.Opprettholdt) {
        return KunneIkkeFerdigstilleKlagebehandling.ResultatMåVæreOpprettholdt.left()
    }
    if (this.resultat.klageinstanshendelser.isEmpty()) {
        return KunneIkkeFerdigstilleKlagebehandling.KreverUtfallFraKlageinstans.left()
    }

    if (!kanFerdigstilleUtenNyRammebehandling) {
        return KunneIkkeFerdigstilleKlagebehandling.UtfallFraKlageinstansSkalFøreTilNyRammebehandling.left()
    }

    return this.copy(
        status = Klagebehandlingsstatus.FERDIGSTILT,
        sistEndret = nå(clock),
    ).right()
}

sealed interface KunneIkkeFerdigstilleKlagebehandling {
    data class SaksbehandlerMismatch(val forventetSaksbehandler: String?, val faktiskSaksbehandler: String) : KunneIkkeFerdigstilleKlagebehandling

    data object ResultatMåVæreOpprettholdt : KunneIkkeFerdigstilleKlagebehandling
    data object KreverUtfallFraKlageinstans : KunneIkkeFerdigstilleKlagebehandling
    data object UtfallFraKlageinstansSkalFøreTilNyRammebehandling : KunneIkkeFerdigstilleKlagebehandling
}
