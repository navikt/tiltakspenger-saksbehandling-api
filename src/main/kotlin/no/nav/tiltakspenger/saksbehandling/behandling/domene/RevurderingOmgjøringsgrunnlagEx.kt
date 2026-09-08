package no.nav.tiltakspenger.saksbehandling.behandling.domene

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Omgjøringsresultat
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjørRammevedtak

/**
 * Omgjøringsgrunnlaget som ble lagret på behandlingen, mot det grunnlaget saken gir nå.
 * Følger med til logg og feilrespons slik at avviket kan leses uten å slå opp saken.
 */
data class OmgjøringsgrunnlagEndret(
    val lagretGrunnlag: OmgjørRammevedtak,
    val nyttGrunnlag: OmgjørRammevedtak,
) {
    override fun toString(): String =
        "Omgjøringsgrunnlaget er endret. Lagret på behandlingen: $lagretGrunnlag, men saken gir nå: $nyttGrunnlag"
}

/**
 * Beregner omgjøringsgrunnlaget på nytt fra saken og sammenligner det med grunnlaget behandlingen ble oppdatert med.
 * Et annet vedtak kan ha omgjort de samme periodene etter at omgjøringen ble opprettet eller sist oppdatert.
 * Da må saksbehandler oppdatere vedtaksperioden før behandlingen kan gå videre.
 *
 * Kun omgjøringer med valgt vedtaksperiode har et grunnlag å sammenligne med; de andre slipper gjennom.
 *
 * @param finnRammevedtakSomOmgjøres samme oppslag som ved oppdatering av omgjøringen, jf. `Vedtaksliste.finnRammevedtakSomOmgjøres`.
 */
fun Revurdering.validerOmgjøringsgrunnlag(
    finnRammevedtakSomOmgjøres: (vedtaksperiode: Periode) -> OmgjørRammevedtak,
): Either<OmgjøringsgrunnlagEndret, Unit> {
    val resultat = this.resultat
    if (resultat !is Omgjøringsresultat) return Unit.right()
    val vedtaksperiode = resultat.vedtaksperiode ?: return Unit.right()

    val nyttGrunnlag = finnRammevedtakSomOmgjøres(vedtaksperiode)
    if (nyttGrunnlag != resultat.omgjørRammevedtak) {
        return OmgjøringsgrunnlagEndret(
            lagretGrunnlag = resultat.omgjørRammevedtak,
            nyttGrunnlag = nyttGrunnlag,
        ).left()
    }
    return Unit.right()
}
