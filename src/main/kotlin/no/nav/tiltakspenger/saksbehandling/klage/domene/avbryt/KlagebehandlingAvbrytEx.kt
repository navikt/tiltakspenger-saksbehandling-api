package no.nav.tiltakspenger.saksbehandling.klage.domene.avbryt

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.felles.Avbrutt
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
    if (erKnyttetTilRammebehandling) {
        return KanIkkeAvbryteKlagebehandling.KnyttetTilIkkeAvbruttRammebehandling(rammebehandlingId!!).left()
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
        avbrutt = Avbrutt(
            begrunnelse = kommando.begrunnelse,
            saksbehandler = kommando.saksbehandler.navIdent,
            tidspunkt = nå(clock),
        ),
    )
    val statistikkhendelser = Statistikkhendelser(
        oppdatertKlagebehandling.genererSaksstatistikk(StatistikkhendelseType.AVSLUTTET_BEHANDLING),
    )
    return (oppdatertKlagebehandling to statistikkhendelser).right()
}
