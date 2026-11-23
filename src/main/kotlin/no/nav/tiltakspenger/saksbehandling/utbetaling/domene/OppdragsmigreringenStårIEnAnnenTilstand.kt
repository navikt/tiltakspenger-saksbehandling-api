package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.saksbehandling.felles.Loggkontekst

/** Saken kunne ikke flyttes fra [fra] til [til] fordi oppdragsmigreringen allerede står i en annen tilstand, typisk fordi et samtidig kall kom først. */
data class OppdragsmigreringenStårIEnAnnenTilstand(
    val sakId: SakId,
    val fra: Oppdragsmigrering,
    val til: Oppdragsmigrering,
    val faktisk: Oppdragstilstand,
) : KanIkkeMigrereTilOppdragsversjon3,
    KanIkkeOppheveUtbetalingssperre {
    override val loggkontekst = Loggkontekst(
        "Vi prøvde å flytte sak $sakId fra $fra til $til, men fant oppdragsversjon ${faktisk.versjon} og tilstand ${faktisk.migrering}, så ingenting er endret",
    )
}
