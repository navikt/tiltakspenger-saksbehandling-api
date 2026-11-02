package no.nav.tiltakspenger.saksbehandling.statistikk.kodeverk

import no.nav.tiltakspenger.libs.tiltaksdeltakelse.infra.http.tiltakshistorikk.felles.EksternTiltakskodeSomGirRett
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.infra.http.tiltakshistorikk.felles.eksterneTiltakskoderSomGirRett
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http.tilLokalKilde
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.toDb
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.mapStønadstype

/**
 * En gjeldende rad i `statistikk_kodeverk_tiltakstype`: en ekstern tiltakskode og stønadstypen vi sender til helved for den.
 * Kildesystem og tiltakskode sammen identifiserer raden.
 */
data class Tiltakstypekodeverkrad(
    val eksternKildesystem: String,
    val eksternTiltakskode: String,
    val eksternStønadstype: String,
)

/** Kodeverket slik koden mapper i dag, bygd fra de samme tabellene og den samme mappingen som klassifiseringen og utbetalingen bruker. */
fun gjeldendeTiltakstypekodeverk(
    koder: List<EksternTiltakskodeSomGirRett> = eksterneTiltakskoderSomGirRett,
): Set<Tiltakstypekodeverkrad> = koder.map {
    Tiltakstypekodeverkrad(
        eksternKildesystem = it.kilde.tilLokalKilde().toDb(),
        eksternTiltakskode = it.eksternTiltakskode,
        eksternStønadstype = it.tiltakstype.mapStønadstype().name,
    )
}.toSet()
