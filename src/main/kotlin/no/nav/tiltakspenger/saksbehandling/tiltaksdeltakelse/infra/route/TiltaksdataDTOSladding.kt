package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.route

import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.infra.route.SladdetVerdi
import no.nav.tiltakspenger.saksbehandling.infra.route.skalSladdeFor

/**
 * Sladding av [TiltaksdataDTO].
 * Tittelen inneholder arrangøren, som er stedsinformasjon om personen.
 * Id-ene er ikke personopplysninger og beholdes, slik at klienten kan koble dataene til tiltakene.
 */

fun TiltaksdataDTO.sladdet(): TiltaksdataDTO = this.copy(
    tittel = SladdetVerdi,
)

fun List<TiltaksdataDTO>.sladdetFor(saksbehandler: Saksbehandler): List<TiltaksdataDTO> =
    if (skalSladdeFor(saksbehandler)) this.map { it.sladdet() } else this
