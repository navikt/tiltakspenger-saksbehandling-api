package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.route

import no.nav.tiltakspenger.saksbehandling.infra.route.SladdetVerdi

/**
 * Sladding av [TiltaksdeltakelseSomFratarRettDTO].
 * Tittelen inneholder arrangørnavnet, som er stedsinformasjon om personen.
 * Tiltakstypen i seg selv er ikke stedlokaliserende og beholdes.
 */
fun TiltaksdeltakelseSomFratarRettDTO.sladdet(): TiltaksdeltakelseSomFratarRettDTO = this.copy(
    tittel = SladdetVerdi,
)
