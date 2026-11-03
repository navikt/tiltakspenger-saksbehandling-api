package no.nav.tiltakspenger.saksbehandling.sak

import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.leggSammen
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.TiltaksdeltakelserDetErSøktTiltakspengerFor
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtaksliste

/**
 * Periodene vi har behandlingsgrunnlag for å hente inn saksopplysninger om, både tiltakspenger og andre ytelser som kan påvirke retten til tiltakspenger.
 * Unionen av tiltaksdeltakelsene det er søkt tiltakspenger for, og vedtaksperioden og tiltaksdeltakelsene til hvert rammevedtak.
 * Ingen søknad eller vedtak krymper periodene, så også avbrutte søknader, avslag, stans, opphør og omgjorte vedtak teller med.
 * Unntaket er avslag på en søknad uten tiltak, der vedtaksperioden ikke teller med selv om saksbehandler har satt søknadsperioden manuelt.
 * Periodene er sortert og slått sammen, og kan ha hull.
 */
data class Behandlingsgrunnlagsperioder(
    val perioder: NonEmptyList<Periode>,
) {
    init {
        require(perioder.zipWithNext().all { (a, b) -> a.tilOgMed < b.fraOgMed }) {
            "Periodene må være sortert og uten overlapp, men var $perioder"
        }
    }

    companion object {
        /** Gir null når verken søknadene eller rammevedtakene har en periode. */
        fun fra(
            tiltaksdeltakelserDetErSøktTiltakspengerFor: TiltaksdeltakelserDetErSøktTiltakspengerFor,
            rammevedtaksliste: Rammevedtaksliste,
        ): Behandlingsgrunnlagsperioder? {
            val vedtaksperioderOgTiltaksdeltakelser = rammevedtaksliste.flatMap { vedtak ->
                val erAvslagPåSøknadUtenTiltak = vedtak.erAvslag && (vedtak.rammebehandling as Søknadsbehandling).søknad.tiltak == null
                listOfNotNull(vedtak.periode.takeUnless { erAvslagPåSøknadUtenTiltak }) + vedtak.rammebehandling.saksopplysninger.tiltaksdeltakelser.perioder
            }
            return (tiltaksdeltakelserDetErSøktTiltakspengerFor.perioder + vedtaksperioderOgTiltaksdeltakelser)
                .leggSammen()
                .toNonEmptyListOrNull()
                ?.let { Behandlingsgrunnlagsperioder(it) }
        }
    }
}
