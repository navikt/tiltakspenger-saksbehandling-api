package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.ValgteTiltaksdeltakelser

sealed interface BehandlingResultat {

    /** Kan være null ved sære tilfeller av avslag, og når behandlingen er uferdig */
    val virkningsperiode: Periode?

    /** Vil kun være satt for søknadsbehandling, forlengelse, revurdering (inkl. omgjøring) til innvilgelse. */
    val innvilgelsesperiode: Periode?

    /** Vil være null ved stans og når behandlingen er uferdig */
    val barnetillegg: Barnetillegg?

    /** Vil være null ved stans og når behandlingen er uferdig */
    val valgteTiltaksdeltakelser: ValgteTiltaksdeltakelser?

    /** Vil være null ved stans og når behandlingen er uferdig */
    val antallDagerPerMeldeperiode: SammenhengendePeriodisering<AntallDagerForMeldeperiode>?

    /**
     * Sier noe om tilstanden til resultatet. Om det er klart for å sendes til beslutter og/eller iverksettes.
     *
     * @param saksopplysninger Kan være null dersom søker ikke har søkt på et tiltak [no.nav.tiltakspenger.saksbehandling.søknad.domene.IkkeInnvilgbarSøknad], gir kun mening ved avslag.
     */
    fun erFerdigutfylt(saksopplysninger: Saksopplysninger?): Boolean

    /** Denne benyttes både i søknadsbehandlinger og revurderinger */
    sealed interface Innvilgelse : BehandlingResultat {
        /** Kan være null fram til resultatet er ferdigutfylt. */
        override val innvilgelsesperiode: Periode?

        /**
         * True dersom disse ikke er null: [innvilgelsesperiode], [valgteTiltaksdeltakelser], [barnetillegg] og [antallDagerPerMeldeperiode]
         * Sjekker også at periodene til [valgteTiltaksdeltakelser], [barnetillegg] og [antallDagerPerMeldeperiode] er lik [innvilgelsesperiode].
         * TODO jah: Ikke direkte relatert til omgjøring, men vi bør utvide denne til og ta høyde for saksopplysninger.tiltaksdeltagelser
         */
        override fun erFerdigutfylt(saksopplysninger: Saksopplysninger?) = saksopplysninger != null && innvilgelsesperiode != null &&
            valgteTiltaksdeltakelser != null &&
            barnetillegg != null &&
            antallDagerPerMeldeperiode != null &&
            antallDagerPerMeldeperiode!!.totalPeriode == innvilgelsesperiode &&
            valgteTiltaksdeltakelser!!.periodisering.totalPeriode == innvilgelsesperiode &&
            barnetillegg!!.periodisering.totalPeriode == innvilgelsesperiode
    }
}

sealed interface BehandlingResultatType

enum class SøknadsbehandlingType : BehandlingResultatType {
    INNVILGELSE,
    AVSLAG,
}

enum class RevurderingType : BehandlingResultatType {
    STANS,
    INNVILGELSE,
    OMGJØRING,
}
