package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.infra.route.SLADDET_TEKST
import no.nav.tiltakspenger.saksbehandling.infra.route.skalSladdeFor
import no.nav.tiltakspenger.saksbehandling.infra.route.sladdet

/**
 * Sladding av [MeldekortbehandlingDTO].
 * Navkontoret er stedsinformasjon om personen, og begrunnelsene er saksbehandlers fritekst.
 * Meldeperiodene, beregningene og beløpene er ikke identifiserende og beholdes.
 */

fun MeldekortbehandlingDTO.sladdet(): MeldekortbehandlingDTO = this.copy(
    navkontor = SLADDET_TEKST,
    navkontorNavn = navkontorNavn?.let { SLADDET_TEKST },
    begrunnelse = begrunnelse?.let { SLADDET_TEKST },
    tekstTilVedtaksbrev = tekstTilVedtaksbrev?.let { SLADDET_TEKST },
    attesteringer = attesteringer.map { it.sladdet() },
    avbrutt = avbrutt?.sladdet(),
    ventestatus = ventestatus.map { it.sladdet() },
)

fun MeldekortbehandlingDTO.sladdetFor(saksbehandler: Saksbehandler): MeldekortbehandlingDTO =
    if (skalSladdeFor(saksbehandler)) sladdet() else this
