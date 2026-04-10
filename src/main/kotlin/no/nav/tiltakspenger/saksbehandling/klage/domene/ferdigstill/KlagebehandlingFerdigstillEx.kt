package no.nav.tiltakspenger.saksbehandling.klage.domene.ferdigstill

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentKlagebehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.Statistikkhendelser
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkhendelseType
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.klagebehandling.genererSaksstatistikk
import java.time.Clock

fun Sak.ferdigstillKlagebehandling(
    kommando: FerdigstillKlagebehandlingKommando,
    clock: Clock,
): Either<KunneIkkeFerdigstilleKlagebehandling, Pair<Klagebehandling, Statistikkhendelser>> {
    val klagebehandling = this.hentKlagebehandling(kommando.klagebehandlingId)

    val tilknyttedeRammebehandlinger = klagebehandling.rammebehandlingId.let { rammebehandlingId ->
        rammebehandlingId.mapNotNull { this.hentRammebehandling(it) }
    }

    return klagebehandling.ferdigstill(kommando, tilknyttedeRammebehandlinger, clock)
}

fun Klagebehandling.ferdigstill(
    kommando: FerdigstillKlagebehandlingKommando,
    rammebehandlinger: List<Rammebehandling>,
    clock: Clock,
): Either<KunneIkkeFerdigstilleKlagebehandling, Pair<Klagebehandling, Statistikkhendelser>> {
    if (!erSaksbehandlerPåBehandlingen(kommando.saksbehandler)) {
        return KunneIkkeFerdigstilleKlagebehandling.SaksbehandlerMismatch(
            forventetSaksbehandler = this.saksbehandler,
            faktiskSaksbehandler = kommando.saksbehandler.navIdent,
        ).left()
    }

    if (this.resultat !is Klagebehandlingsresultat.Opprettholdt && this.resultat !is Klagebehandlingsresultat.Omgjør) {
        return KunneIkkeFerdigstilleKlagebehandling.`ResultatMåVæreOpprettholdEllerOmgjør`.left()
    }

    rammebehandlinger.forEach { rammebehandling ->
        require(this.rammebehandlingId.contains(rammebehandling.id)) {
            "Klagebehandling $id på sak $sakId har rammebehandlingId ${this.rammebehandlingId} som ikke finnes i listen over rammebehandlinger"
        }
    }

    if (!rammebehandlinger.all { it.erAvsluttet }) {
        return KunneIkkeFerdigstilleKlagebehandling.BehandlingErKnyttetTilEnRammebehandling.left()
    }

    return when (this.resultat) {
        is Klagebehandlingsresultat.Omgjør -> feridgstillOmgjør(kommando, clock)
        is Klagebehandlingsresultat.Opprettholdt -> ferdigstillOpprettholdelse(kommando, clock)
    }
}

private fun Klagebehandling.ferdigstillOpprettholdelse(
    command: FerdigstillKlagebehandlingKommando,
    clock: Clock,
): Either<KunneIkkeFerdigstilleKlagebehandling, Pair<Klagebehandling, Statistikkhendelser>> {
    require(this.resultat is Klagebehandlingsresultat.Opprettholdt) {
        "Klagebehandling $id på sak $sakId må ha resultat Opprettholdt for å ferdigstilles ved opprettholdelse"
    }

    if (this.resultat.klageinstanshendelser.isEmpty()) {
        return KunneIkkeFerdigstilleKlagebehandling.KreverUtfallFraKlageinstans.left()
    }

    val ferdigstiltTidspunkt = nå(clock)

    val oppdatertKlagebehandling = this.copy(
        status = Klagebehandlingsstatus.FERDIGSTILT,
        sistEndret = ferdigstiltTidspunkt,
        resultat = this.resultat.ferdigstill(ferdigstiltTidspunkt, command.begrunnelse),
    )
    val statistikkhendelser = Statistikkhendelser(
        oppdatertKlagebehandling.genererSaksstatistikk(StatistikkhendelseType.AVSLUTTET_BEHANDLING),
    )
    return (oppdatertKlagebehandling to statistikkhendelser).right()
}

private fun Klagebehandling.feridgstillOmgjør(
    command: FerdigstillKlagebehandlingKommando,
    clock: Clock,
): Either<KunneIkkeFerdigstilleKlagebehandling, Pair<Klagebehandling, Statistikkhendelser>> {
    require(this.resultat is Klagebehandlingsresultat.Omgjør) {
        "Klagebehandling $id på sak $sakId må ha resultat Omgjør for å ferdigstilles ved omgjør"
    }

    val ferdigstiltTidspunkt = nå(clock)

    val oppdatertKlagebehandling = this.copy(
        status = Klagebehandlingsstatus.FERDIGSTILT,
        sistEndret = ferdigstiltTidspunkt,
        resultat = this.resultat.ferdigstill(ferdigstiltTidspunkt, command.begrunnelse),
    )
    val statistikkhendelser = Statistikkhendelser(
        oppdatertKlagebehandling.genererSaksstatistikk(StatistikkhendelseType.AVSLUTTET_BEHANDLING),
    )
    return (oppdatertKlagebehandling to statistikkhendelser).right()
}

sealed interface KunneIkkeFerdigstilleKlagebehandling {
    data class SaksbehandlerMismatch(val forventetSaksbehandler: String?, val faktiskSaksbehandler: String) : KunneIkkeFerdigstilleKlagebehandling

    data object ResultatMåVæreOpprettholdEllerOmgjør : KunneIkkeFerdigstilleKlagebehandling
    data object KreverUtfallFraKlageinstans : KunneIkkeFerdigstilleKlagebehandling
    data object BehandlingErKnyttetTilEnRammebehandling : KunneIkkeFerdigstilleKlagebehandling
}
