package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.route

import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.infra.route.SladdetVerdi
import no.nav.tiltakspenger.saksbehandling.infra.route.skalSladdeFor

/**
 * Sladding av [TiltaksdeltakelseMedArrangørnavnDTO].
 * Visningsnavnet inneholder arrangørnavnet, som er stedsinformasjon om personen.
 * Tiltakstypen i seg selv er ikke stedlokaliserende og beholdes.
 */

fun TiltaksdeltakelseMedArrangørnavnDTO.sladdet(): TiltaksdeltakelseMedArrangørnavnDTO = this.copy(
    visningsnavn = SladdetVerdi,
)

fun TiltaksdeltakelseMedArrangørnavnDTO.sladdetFor(saksbehandler: Saksbehandler): TiltaksdeltakelseMedArrangørnavnDTO =
    if (skalSladdeFor(saksbehandler)) sladdet() else this

fun List<TiltaksdeltakelseMedArrangørnavnDTO>.sladdetFor(
    saksbehandler: Saksbehandler,
): List<TiltaksdeltakelseMedArrangørnavnDTO> =
    if (skalSladdeFor(saksbehandler)) this.map { it.sladdet() } else this
