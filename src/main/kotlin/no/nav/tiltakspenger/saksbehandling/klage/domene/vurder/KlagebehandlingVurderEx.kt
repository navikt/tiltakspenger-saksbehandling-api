package no.nav.tiltakspenger.saksbehandling.klage.domene.vurder

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.klage.domene.KanIkkeOppdatereKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat.Omgjør
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat.Opprettholdt
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.FERDIGSTILT
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.KLAR_TIL_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.OMGJØRING_ETTER_KLAGEINSTANS
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.UNDER_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.klage.domene.TilknyttetBehandlingsstatus
import java.time.Clock
import java.time.LocalDateTime

fun Klagebehandling.vurder(
    kommando: VurderKlagebehandlingKommando,
    tilknyttetBehandlingsstatus: TilknyttetBehandlingsstatus?,
    clock: Clock,
): Either<KanIkkeVurdereKlagebehandling, Klagebehandling> {
    kanOppdatereIDenneStatusen(tilknyttetBehandlingsstatus).onLeft {
        return KanIkkeVurdereKlagebehandling.KanIkkeOppdateres(
            it,
        ).left()
    }
    if (!erSaksbehandlerPåBehandlingen(kommando.saksbehandler)) {
        return KanIkkeVurdereKlagebehandling.SaksbehandlerMismatch(
            forventetSaksbehandler = this.saksbehandler!!,
            faktiskSaksbehandler = kommando.saksbehandler.navIdent,
        ).left()
    }

    return when (kommando) {
        is VurderOmgjørKlagebehandlingKommando -> this.vurderOmgjøring(kommando, clock).right()
        is VurderOpprettholdKlagebehandlingKommando -> this.vurderOpprettholdelse(kommando, clock)
    }
}

private fun Klagebehandling.vurderOmgjøring(
    kommando: VurderOmgjørKlagebehandlingKommando,
    clock: Clock,
): Klagebehandling = this.copy(
    sistEndret = nå(clock),
    resultat = (resultat as? Omgjør)?.oppdater(kommando) ?: kommando.tilResultatUtenBehandlingId(),
)

private fun Klagebehandling.vurderOpprettholdelse(
    kommando: VurderOpprettholdKlagebehandlingKommando,
    clock: Clock,
): Either<KanIkkeVurdereKlagebehandling, Klagebehandling> {
    if (this.behandlingId.isNotEmpty()) {
        return KanIkkeVurdereKlagebehandling.KanIkkeOppdateres(
            KanIkkeOppdatereKlagebehandling.KlageErKnyttetTilBehandling(behandlingId = this.behandlingId),
        ).left()
    }

    return this.copy(
        sistEndret = nå(clock),
        resultat = (resultat as? Opprettholdt)?.oppdaterHjemler(kommando.hjemler) ?: kommando.tilResultat(),
    ).right()
}

fun Klagebehandling.oppdaterBehandlingId(
    behandlingId: BehandlingId,
    saksbehandler: Saksbehandler,
    sistEndret: LocalDateTime,
): Klagebehandling {
    require(resultat != null && resultat !is Klagebehandlingsresultat.Avvist) {
        "Resultatet var null men forventet at den var definert. Dette skjedde for sakId=$sakId, saksnummer:$saksnummer, klagebehandlingId=$id"
    }
    require(erUnderBehandling || erMottattFraKlageinstans || erFerdigstilt) {
        "Klagebehandling må være i status UNDER_BEHANDLING / MOTTATT_FRA_KLAGEINSTANS / FERDIGSTILT for at man kan knytte den til en behandling. sakId=$sakId, saksnummer:$saksnummer, klagebehandlingId=$id"
    }
    if (!erFerdigstilt) {
        require(erSaksbehandlerPåBehandlingen(saksbehandler))
    }
    return this.copy(
        resultat = resultat.leggTilNyÅpenBehandling(behandlingId, this.id),
        sistEndret = if (erFerdigstilt) this.sistEndret else sistEndret,
        status = when (resultat) {
            is Omgjør -> status
            is Opprettholdt -> if (erFerdigstilt) status else OMGJØRING_ETTER_KLAGEINSTANS
        },
    )
}

fun Klagebehandling.fjernBehandlingId(
    behandlingId: BehandlingId,
    saksbehandler: Saksbehandler,
    sistEndret: LocalDateTime,
): Klagebehandling {
    require(erKnyttetTilBehandling) {
        "Klagebehandling er ikke knyttet til en behandling. sakId=$sakId, saksnummer:$saksnummer, klagebehandlingId=$id"
    }
    require(this.status == KLAR_TIL_BEHANDLING || this.status == UNDER_BEHANDLING || this.status == OMGJØRING_ETTER_KLAGEINSTANS || this.status == FERDIGSTILT) {
        "Klagebehandling må være i status KLAR_TIL_BEHANDLING, UNDER_BEHANDLING, OMGJØRING_ETTER_KLAGEINSTANS, FERDIGSTILT for at man kan disassosiere behandling. status was ${this.status}, sakId=$sakId, saksnummer:$saksnummer, klagebehandlingId=$id"
    }
    if (!erFerdigstilt) {
        require(erSaksbehandlerPåBehandlingen(saksbehandler))
    }
    return when (val res = resultat) {
        is Omgjør -> this.copy(
            resultat = res.fjernBehandlingId(behandlingId),
            sistEndret = sistEndret,
        )

        is Opprettholdt -> this.copy(
            resultat = res.fjernBehandlingId(behandlingId),
            sistEndret = sistEndret,
            status = if (status == OMGJØRING_ETTER_KLAGEINSTANS) Klagebehandlingsstatus.MOTTATT_FRA_KLAGEINSTANS else status,
        )

        is Klagebehandlingsresultat.Avvist, null -> throw IllegalStateException(
            "Klagebehandling er ikke knyttet til en behandling. sakId=$sakId, saksnummer:$saksnummer, klagebehandlingId=$id",
        )
    }
}

private fun Omgjør.fjernBehandlingId(behandlingId: BehandlingId): Omgjør {
    require(this.behandlingId.contains(behandlingId)) {
        "Kan kun fjerne behandlingId hvis den matcher eksisterende verdi"
    }
    require(åpenBehandlingId == behandlingId) {
        "Kan kun fjerne behandlingId hvis åpenBehandlingId matcher behandlingId som skal fjernes"
    }
    return this.copy(behandlingId = this.behandlingId.minus(behandlingId), åpenBehandlingId = null)
}

private fun Opprettholdt.fjernBehandlingId(behandlingId: BehandlingId): Opprettholdt {
    require(this.behandlingId.contains(behandlingId)) {
        "Kan kun fjerne behandlingId hvis den matcher eksisterende verdi"
    }
    require(åpenBehandlingId == behandlingId) {
        "Kan kun fjerne behandlingId hvis åpenBehandlingId matcher behandlingId som skal fjernes"
    }
    return this.copy(behandlingId = this.behandlingId.minus(behandlingId), åpenBehandlingId = null)
}
