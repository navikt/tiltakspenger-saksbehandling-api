package no.nav.tiltakspenger.saksbehandling.klage.domene.vurder

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.klage.domene.KanIkkeOppdatereKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.KLAR_TIL_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.UNDER_BEHANDLING
import java.time.Clock
import java.time.LocalDateTime

fun Klagebehandling.vurder(
    kommando: VurderKlagebehandlingKommando,
    rammebehandlingsstatus: Rammebehandlingsstatus?,
    clock: Clock,
): Either<KanIkkeVurdereKlagebehandling, Klagebehandling> {
    require(kommando is OmgjørKlagebehandlingKommando)
    kanOppdatereIDenneStatusen(rammebehandlingsstatus).onLeft {
        return KanIkkeVurdereKlagebehandling.KanIkkeOppdateres(
            it,
        ).left()
    }
    // TODO jah: Denne må nok gjøres om litt når vi legger til opprettholdelse
    if (resultat != null && !erOmgjøring) {
        return KanIkkeVurdereKlagebehandling.KanIkkeOppdateres(
            KanIkkeOppdatereKlagebehandling.FeilResultat(
                forventetResultat = Klagebehandlingsresultat.Omgjør::class.simpleName!!,
                faktiskResultat = resultat::class.simpleName!!,
            ),
        ).left()
    }
    if (!erSaksbehandlerPåBehandlingen(kommando.saksbehandler)) {
        return KanIkkeVurdereKlagebehandling.SaksbehandlerMismatch(
            forventetSaksbehandler = this.saksbehandler!!,
            faktiskSaksbehandler = kommando.saksbehandler.navIdent,
        ).left()
    }
    return this.copy(
        sistEndret = nå(clock),
        resultat = resultat?.let {
            it as Klagebehandlingsresultat.Omgjør
            it.oppdater(kommando)
        } ?: kommando.tilResultatUtenRammebehandlingId(),
    ).right()
}

fun Klagebehandling.oppdaterRammebehandlingId(
    rammebehandlingId: BehandlingId,
    saksbehandler: Saksbehandler,
    sistEndret: LocalDateTime,
): Klagebehandling {
    require(resultat is Klagebehandlingsresultat.Omgjør) {
        "Resultatet må være Omgjør, men var $resultat. sakId=$sakId, saksnummer:$saksnummer, klagebehandlingId=$id"
    }
    require(status == UNDER_BEHANDLING) {
        "Klagebehandling må være i status UNDER_BEHANDLING for at man kan knytte den til en rammebehandling.sakId=$sakId, saksnummer:$saksnummer, klagebehandlingId=$id"
    }
    require(erSaksbehandlerPåBehandlingen(saksbehandler))
    return this.copy(
        resultat = resultat.oppdaterRammebehandlingId(rammebehandlingId),
        sistEndret = sistEndret,
    )
}

fun Klagebehandling.fjernRammebehandlingId(
    rammebehandlingId: BehandlingId,
    saksbehandler: Saksbehandler,
    sistEndret: LocalDateTime,
): Klagebehandling {
    require(erKnyttetTilRammebehandling) {
        "Klagebehandling er ikke knyttet til en rammebehandling.sakId=$sakId, saksnummer:$saksnummer, klagebehandlingId=$id"
    }
    require(this.status == KLAR_TIL_BEHANDLING || this.status == UNDER_BEHANDLING) {
        "Klagebehandling må være i status KLAR_TIL_BEHANDLING eller UNDER_BEHANDLING for at man kan disassosiere rammebehandling.sakId=$sakId, saksnummer:$saksnummer, klagebehandlingId=$id"
    }
    require(erSaksbehandlerPåBehandlingen(saksbehandler))
    return when (val res = resultat) {
        is Klagebehandlingsresultat.Omgjør -> this.copy(resultat = res.fjernRammebehandlingId(rammebehandlingId), sistEndret = sistEndret)

        is Klagebehandlingsresultat.Avvist, null -> throw IllegalStateException(
            "Klagebehandling er ikke knyttet til en rammebehandling. sakId=$sakId, saksnummer:$saksnummer, klagebehandlingId=$id",
        )
    }
}
