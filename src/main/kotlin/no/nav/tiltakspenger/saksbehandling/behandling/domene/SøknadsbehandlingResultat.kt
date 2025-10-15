package no.nav.tiltakspenger.saksbehandling.behandling.domene

import arrow.core.NonEmptySet
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.ValgteTiltaksdeltakelser

sealed interface SøknadsbehandlingResultat : BehandlingResultat {

    /**
     * Virkningsperioden/vedtaksperioden og avslagsperioden vil være 1-1 ved denne revurderingstypen.
     * Den vil ikke påvirke vedtakstidslinjen, beregninger eller utbetalinger.
     * @param avslagsperiode Kan være null ved papirsøknad, dersom det ikke er søkt for et spesifikt tiltak eller periode.
     */
    data class Avslag(
        val avslagsgrunner: NonEmptySet<Avslagsgrunnlag>,
        val avslagsperiode: Periode?,
    ) : SøknadsbehandlingResultat {
        override val virkningsperiode = avslagsperiode
    }

    /**
     * Virkningsperioden/vedtaksperioden og avslagsperioden vil være 1-1 ved denne revurderingstypen.
     *
     * Når saksbehandler velger at en søknadsbehandling skal innvilges, får de ikke lagret før de har valgt [innvilgelsesperiode] og [valgteTiltaksdeltakelser]
     */
    data class Innvilgelse(
        override val valgteTiltaksdeltakelser: ValgteTiltaksdeltakelser,
        override val barnetillegg: Barnetillegg?,
        override val antallDagerPerMeldeperiode: SammenhengendePeriodisering<AntallDagerForMeldeperiode>?,
        val innvilgelsesperiode: Periode,
    ) : BehandlingResultat.Innvilgelse,
        SøknadsbehandlingResultat {
        override val virkningsperiode = innvilgelsesperiode
    }
}
