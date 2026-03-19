package no.nav.tiltakspenger.saksbehandling.klage.domene.iverksett

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat.Avvist
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat.Omgjør
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat.Opprettholdt
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.OMGJØRING_ETTER_KLAGEINSTANS
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.UNDER_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.VEDTATT
import no.nav.tiltakspenger.saksbehandling.statistikk.Statistikkhendelser
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkhendelseType
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.klagebehandling.genererSaksstatistikk

fun Klagebehandling.iverksettOmgjøring(
    kommando: IverksettOmgjøringKommando,
): Either<KanIkkeIverksetteKlagebehandling, Pair<Klagebehandling, Statistikkhendelser>> {
    if (resultat !is Omgjør) {
        return KanIkkeIverksetteKlagebehandling.FeilResultat(
            Omgjør::class.simpleName!!,
            resultat?.javaClass?.simpleName,
        ).left()
    }
    if (!erUnderBehandling) {
        return KanIkkeIverksetteKlagebehandling.MåHaStatus(
            UNDER_BEHANDLING.toString(),
            status.toString(),
        ).left()
    }
    require(resultat.rammebehandlingId != null) { "RammebehandlingId skal ikke være null ved iverksettelse av omgjøring. Hvis dette skjer er det en bug som må fikses, eller så må det håndteres som en left." }
    // Vi aksepterer at den er null, siden denne funksjonen kun skal kalles fra Rammebehandling.
    require(kanIverksetteVedtak != false) { "Dette skal være håndtert over. Hvis dette skjer er det en bug som må fikses, eller så må det håndteres som en left." }

    val oppdatertKlagebehandling = this.copy(
        sistEndret = kommando.iverksattTidspunkt,
        iverksattTidspunkt = kommando.iverksattTidspunkt,
        status = VEDTATT,
    )
    val statistikkhendelser = Statistikkhendelser(oppdatertKlagebehandling.genererSaksstatistikk(StatistikkhendelseType.AVSLUTTET_BEHANDLING))
    return (oppdatertKlagebehandling to statistikkhendelser).right()
}

fun Klagebehandling.iverksettAvvisning(
    kommando: IverksettAvvisningKommando,
): Either<KanIkkeIverksetteKlagebehandling, Klagebehandling> {
    if (resultat !is Avvist) {
        return KanIkkeIverksetteKlagebehandling.FeilResultat(
            Avvist::class.simpleName!!,
            resultat?.javaClass?.simpleName,
        ).left()
    }
    if (!erUnderBehandling) {
        return KanIkkeIverksetteKlagebehandling.MåHaStatus(
            UNDER_BEHANDLING.toString(),
            status.toString(),
        ).left()
    }
    if (!erSaksbehandlerPåBehandlingen(kommando.saksbehandler)) {
        return KanIkkeIverksetteKlagebehandling.SaksbehandlerMismatch(
            forventetSaksbehandler = this.saksbehandler!!,
            faktiskSaksbehandler = kommando.saksbehandler.navIdent,
        ).left()
    }
    if (resultat.brevtekst.isNullOrEmpty()) {
        return KanIkkeIverksetteKlagebehandling.ManglerBrevtekst.left()
    }
    // Vi aksepterer at den er null, siden denne funksjonen kun skal kalles fra Rammebehandling.
    require(kanIverksetteVedtak != false) { "Dette skal være håndtert over. Hvis dette skjer er det en bug som må fikses, eller så må det håndteres som en left." }

    val oppdatertKlagebehandling = this.copy(
        sistEndret = kommando.iverksattTidspunkt,
        iverksattTidspunkt = kommando.iverksattTidspunkt,
        status = VEDTATT,
    )
    return oppdatertKlagebehandling.right()
}

fun Klagebehandling.iverksettOpprettholdelse(
    kommando: IverksettOpprettholdelseKommando,
): Either<KanIkkeIverksetteKlagebehandling, Pair<Klagebehandling, Statistikkhendelser>> {
    if (resultat !is Opprettholdt) {
        return KanIkkeIverksetteKlagebehandling.FeilResultat(
            Opprettholdt::class.simpleName!!,
            resultat?.javaClass?.simpleName,
        ).left()
    }

    if (!resultat.skalOmgjøresEtterKA) {
        return KanIkkeIverksetteKlagebehandling.SkalIkkeOmgjøresEtterKA.left()
    }

    if (!omgjørEtterKA) {
        return KanIkkeIverksetteKlagebehandling.MåHaStatus(
            forventetStats = OMGJØRING_ETTER_KLAGEINSTANS.toString(),
            actualStatus = status.toString(),
        ).left()
    }
    require(resultat.rammebehandlingId != null) { "RammebehandlingId skal ikke være null ved iverksettelse av opprettholdelse. Hvis dette skjer er det en bug som må fikses, eller så må det håndteres som en left." }
    // Vi aksepterer at den er null, siden denne funksjonen kun skal kalles fra Rammebehandling.
    require(kanIverksetteVedtak != false) { "Dette skal være håndtert over. Hvis dette skjer er det en bug som må fikses, eller så må det håndteres som en left." }
    val oppdatertKlagebehandling = this.copy(
        sistEndret = kommando.iverksattTidspunkt,
        iverksattTidspunkt = kommando.iverksattTidspunkt,
        status = VEDTATT,
    )
    val statistikkhendelser = Statistikkhendelser(oppdatertKlagebehandling.genererSaksstatistikk(StatistikkhendelseType.AVSLUTTET_BEHANDLING))
    return (oppdatertKlagebehandling to statistikkhendelser).right()
}
