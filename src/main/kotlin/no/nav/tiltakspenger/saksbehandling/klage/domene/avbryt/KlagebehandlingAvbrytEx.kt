package no.nav.tiltakspenger.saksbehandling.klage.domene.avbryt

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.AVBRUTT
import no.nav.tiltakspenger.saksbehandling.statistikk.Statistikkhendelser
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkhendelseType
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.klagebehandling.genererSaksstatistikk
import java.time.Clock

fun Klagebehandling.avbryt(
    kommando: AvbrytKlagebehandlingKommando,
    clock: Clock,
): Either<KanIkkeAvbryteKlagebehandling, Pair<Klagebehandling, Statistikkhendelser>> {
    if (erAvsluttet) {
        return KanIkkeAvbryteKlagebehandling.AlleredeAvsluttet(this.status).left()
    }
    if (ventestatus.erSattPåVent) {
        return KanIkkeAvbryteKlagebehandling.BehandlingenErSattPåVent.left()
    }
    // det går fint å bare sjekke om det finnes behandling på klagen fordi klagen fortsatt er aktiv, og da vil det ikke være mulig å avbryte klagen uten å avbryte behandlingen først
    if (erKnyttetTilBehandling) {
        return KanIkkeAvbryteKlagebehandling.KnyttetTilIkkeAvbruttBehandling(behandlingId).left()
    }
    if (!erSaksbehandlerPåBehandlingen(kommando.saksbehandler)) {
        return KanIkkeAvbryteKlagebehandling.SaksbehandlerMismatch(
            forventetSaksbehandler = this.saksbehandler!!,
            faktiskSaksbehandler = kommando.saksbehandler.navIdent,
        ).left()
    }
    val oppdatertKlagebehandling = this.copy(
        sistEndret = nå(clock),
        status = AVBRUTT,
        avbrutt = KlagebehandlingAvbrutt(
            begrunnelse = kommando.begrunnelse,
            saksbehandler = kommando.saksbehandler.navIdent,
            tidspunkt = nå(clock),
            status = kommando.status,
        ),
    )
    val statistikkhendelser = Statistikkhendelser(
        oppdatertKlagebehandling.genererSaksstatistikk(StatistikkhendelseType.AVSLUTTET_BEHANDLING),
    )
    return (oppdatertKlagebehandling to statistikkhendelser).right()
}
