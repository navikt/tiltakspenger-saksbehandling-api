package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.infra.route.SladdetVerdi
import no.nav.tiltakspenger.saksbehandling.infra.route.skalSladdeFor
import no.nav.tiltakspenger.saksbehandling.infra.route.sladdet

/**
 * Sladding av [MeldekortbehandlingDTO].
 * Navkontoret er stedsinformasjon om personen, og begrunnelsene er saksbehandlers fritekst.
 * Meldeperiodene, beregningene og beløpene er ikke identifiserende og beholdes.
 */

fun MeldekortbehandlingDTO.sladdet(): MeldekortbehandlingDTO = this.copy(
    navkontor = SladdetVerdi,
    navkontorNavn = SladdetVerdi,
    begrunnelse = SladdetVerdi,
    tekstTilVedtaksbrev = SladdetVerdi,
    attesteringer = attesteringer.map { it.sladdet() },
    avbrutt = avbrutt?.sladdet(),
    ventestatus = ventestatus.map { it.sladdet() },
)

fun MeldekortbehandlingDTO.sladdetFor(saksbehandler: Saksbehandler): MeldekortbehandlingDTO =
    if (skalSladdeFor(saksbehandler)) sladdet() else this
