package no.nav.tiltakspenger.saksbehandling.vedtak.infra.route

import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.sladdet
import no.nav.tiltakspenger.saksbehandling.infra.route.skalSladdeFor

/**
 * Sladding av [RammevedtakDTO].
 * Begrunnelsen i barnetillegget er saksbehandlers fritekst.
 * Vedtaksperioder, beløp, identer og ID-er er ikke identifiserende og beholdes.
 */

fun RammevedtakDTO.sladdet(): RammevedtakDTO = this.copy(
    barnetillegg = barnetillegg?.sladdet(),
)

fun RammevedtakDTO.sladdetFor(saksbehandler: Saksbehandler): RammevedtakDTO =
    if (skalSladdeFor(saksbehandler)) sladdet() else this
