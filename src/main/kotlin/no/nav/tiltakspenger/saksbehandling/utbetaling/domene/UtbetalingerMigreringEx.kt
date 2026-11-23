package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import arrow.core.Either
import arrow.core.left
import arrow.core.right

/**
 * Hel Ved viderefører kjedene fra [Oppdragsversjon.V2] bare når alle utbetalingene er sendt og kvittert OK.
 * En usendt utbetaling blokkerer også, siden den ellers kunne blitt sendt på [Oppdragsversjon.V2] rett før byttet.
 */
fun Utbetalinger.kanMigreresTilOppdragsversjon3(): Either<KanIkkeMigrereTilOppdragsversjon3.HarUtbetalingSomIkkeErKvittert, Unit> {
    val ikkeKvittert = filter { it.status?.erOK() != true }.map { it.id }
    return if (ikkeKvittert.isEmpty()) {
        Unit.right()
    } else {
        KanIkkeMigrereTilOppdragsversjon3.HarUtbetalingSomIkkeErKvittert(ikkeKvittert).left()
    }
}
