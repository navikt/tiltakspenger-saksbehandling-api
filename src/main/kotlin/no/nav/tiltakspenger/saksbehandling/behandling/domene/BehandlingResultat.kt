package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.ValgteTiltaksdeltakelser

sealed interface BehandlingResultat {

    /** Kan være null ved sære tilfeller av avslag, og når behandlingen er uferdig */
    val virkningsperiode: Periode?

    /** Vil være null ved stans og når behandlingen er uferdig */
    val barnetillegg: Barnetillegg?

    /** Vil være null ved stans og når behandlingen er uferdig */
    val valgteTiltaksdeltakelser: ValgteTiltaksdeltakelser?

    /** Vil være null ved stans og når behandlingen er uferdig */
    val antallDagerPerMeldeperiode: SammenhengendePeriodisering<AntallDagerForMeldeperiode>?

    /** Sier noe om tilstanden til resultatet. Om det er klart for å sendes til beslutter og/eller iverksettes. */
    val erFerdigutfylt: Boolean

    /** Denne benyttes både i søknadsbehandlinger og revurderinger */
    sealed interface Innvilgelse : BehandlingResultat {
        val innvilgelsesperiode: Periode?

        /**
         * True dersom disse ikke er null: [innvilgelsesperiode], [valgteTiltaksdeltakelser], [barnetillegg] og [antallDagerPerMeldeperiode]
         * Sjekker også at periodene til [valgteTiltaksdeltakelser], [barnetillegg] og [antallDagerPerMeldeperiode] er lik [innvilgelsesperiode].
         */
        override val erFerdigutfylt: Boolean
            get() = innvilgelsesperiode != null &&
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
}
