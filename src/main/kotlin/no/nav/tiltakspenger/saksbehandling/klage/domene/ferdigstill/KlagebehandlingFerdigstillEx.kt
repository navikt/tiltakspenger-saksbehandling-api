package no.nav.tiltakspenger.saksbehandling.klage.domene.ferdigstill

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.statistikk.Statistikkhendelser
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkhendelseType
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.klagebehandling.genererSaksstatistikk
import java.time.Clock

/**
 * Gjelder kun for opprettholdt klage etter vi har mottatt svar fra klageinstansen.
 */
fun Klagebehandling.ferdigstill(
    kommando: FerdigstillKlagebehandlingKommando,
    clock: Clock,
): Either<KunneIkkeFerdigstilleKlagebehandling, Pair<Klagebehandling, Statistikkhendelser>> {
    if (!erSaksbehandlerPåBehandlingen(kommando.saksbehandler)) {
        return KunneIkkeFerdigstilleKlagebehandling.SaksbehandlerMismatch(
            forventetSaksbehandler = this.saksbehandler,
            faktiskSaksbehandler = kommando.saksbehandler.navIdent,
        ).left()
    }
    if (this.resultat !is Klagebehandlingsresultat.Opprettholdt) {
        return KunneIkkeFerdigstilleKlagebehandling.ResultatMåVæreOpprettholdt.left()
    }
    if (this.resultat.klageinstanshendelser.isEmpty()) {
        return KunneIkkeFerdigstilleKlagebehandling.KreverUtfallFraKlageinstans.left()
    }

    // TODO jah: Ref. møte på tirsdag. Vi må støtte at denne ferdigstilles selvom den "egentlig" skal være knyttet til en rammebehandling. Saksbehandler kan ha løst det på andre måter.
    if (this.resultat.skalVæreKnyttetTilRammebehandling) {
        throw IllegalStateException("Klagebehandling med id ${this.id} sak ${this.sakId} skal ferdigstilles ved å opprette ny rammbehandling")
    }
    val ferdigstiltTidspunkt = nå(clock)

    val oppdatertKlagebehandling = this.copy(
        status = Klagebehandlingsstatus.FERDIGSTILT,
        sistEndret = ferdigstiltTidspunkt,
        resultat = this.resultat.oppdaterFerdigstiltTidspunkt(ferdigstiltTidspunkt),
    )
    val statistikkhendelser = Statistikkhendelser(
        oppdatertKlagebehandling.genererSaksstatistikk(StatistikkhendelseType.AVSLUTTET_BEHANDLING),
    )
    return (oppdatertKlagebehandling to statistikkhendelser).right()
}

sealed interface KunneIkkeFerdigstilleKlagebehandling {
    data class SaksbehandlerMismatch(val forventetSaksbehandler: String?, val faktiskSaksbehandler: String) : KunneIkkeFerdigstilleKlagebehandling

    data object ResultatMåVæreOpprettholdt : KunneIkkeFerdigstilleKlagebehandling
    data object KreverUtfallFraKlageinstans : KunneIkkeFerdigstilleKlagebehandling
}
