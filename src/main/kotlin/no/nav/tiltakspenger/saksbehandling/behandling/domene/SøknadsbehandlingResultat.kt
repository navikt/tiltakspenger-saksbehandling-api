package no.nav.tiltakspenger.saksbehandling.behandling.domene

import arrow.core.Either
import arrow.core.NonEmptySet
import arrow.core.right
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjørRammevedtak

sealed interface SøknadsbehandlingResultat : BehandlingResultat {

    override fun oppdaterSaksopplysninger(oppdaterteSaksopplysninger: Saksopplysninger): Either<KunneIkkeOppdatereSaksopplysninger, SøknadsbehandlingResultat?>

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
        override val innvilgelsesperioder = null
        override val barnetillegg = null
        override val valgteTiltaksdeltakelser = null
        override val antallDagerPerMeldeperiode = null
        override val omgjørRammevedtak: OmgjørRammevedtak = OmgjørRammevedtak.empty

        /**
         * True dersom [avslagsgrunner] ikke er tom. Vi må støtte at [avslagsperiode] er null for særdeles mangelfulle søknader.
         * Må kunne avslå en søknad selv om det ikke er søkt på et tiltak.
         */
        override fun erFerdigutfylt(saksopplysninger: Saksopplysninger): Boolean = avslagsgrunner.isNotEmpty()

        /** Avslag påvirkes ikke av oppdaterte saksopplysninger */
        override fun oppdaterSaksopplysninger(oppdaterteSaksopplysninger: Saksopplysninger): Either<KunneIkkeOppdatereSaksopplysninger, Avslag> = this.right()
    }

    /**
     * Virkningsperioden/vedtaksperioden og avslagsperioden vil være 1-1 ved denne revurderingstypen.
     *
     * Når saksbehandler velger at en søknadsbehandling skal innvilges, får de ikke lagret før de har valgt [innvilgelsesperioder]
     */
    data class Innvilgelse(
        override val innvilgelsesperioder: Innvilgelsesperioder,
        override val barnetillegg: Barnetillegg?,
        override val omgjørRammevedtak: OmgjørRammevedtak,
    ) : BehandlingResultat.Innvilgelse,
        SøknadsbehandlingResultat {
        override val virkningsperiode = innvilgelsesperioder.totalPeriode
        override val valgteTiltaksdeltakelser = innvilgelsesperioder.valgteTiltaksdeltagelser
        override val antallDagerPerMeldeperiode = innvilgelsesperioder.antallDagerPerMeldeperiode

        override fun oppdaterSaksopplysninger(oppdaterteSaksopplysninger: Saksopplysninger): Either<KunneIkkeOppdatereSaksopplysninger, Innvilgelse?> {
            return if (skalNullstilleResultatVedNyeSaksopplysninger(
                    valgteTiltaksdeltakelser.verdier,
                    oppdaterteSaksopplysninger,
                )
            ) {
                null
            } else {
                this
            }.right()
        }
    }
}
