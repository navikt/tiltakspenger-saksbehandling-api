package no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat

import arrow.core.Either
import arrow.core.NonEmptySet
import arrow.core.right
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdater.KunneIkkeOppdatereSaksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjørRammevedtak

sealed interface Søknadsbehandlingsresultat : Rammebehandlingsresultat {

    override fun oppdaterSaksopplysninger(oppdaterteSaksopplysninger: Saksopplysninger): Either<KunneIkkeOppdatereSaksopplysninger, Søknadsbehandlingsresultat>

    /**
     * Saksbehandler har ikke valgt om søknaden skal innvilges eller avslås ennå.
     * Er også resultatet når et tidligere valg er nullstilt, for eksempel ved underkjenning av en automatisk saksbehandling eller når saksopplysningene endrer seg.
     */
    data object IkkeValgt :
        Søknadsbehandlingsresultat,
        Rammebehandlingsresultat.IkkeValgt {
        override val vedtaksperiode = null
        override val innvilgelsesperioder = null
        override val barnetillegg = null
        override val valgteTiltaksdeltakelser = null
        override val antallDagerPerMeldeperiode = null
        override val omgjørRammevedtak: OmgjørRammevedtak = OmgjørRammevedtak.empty

        /** IkkeValgt er aldri en ferdig utfylling */
        override fun erFerdigutfylt(saksopplysninger: Saksopplysninger): Boolean = false

        override fun oppdaterSaksopplysninger(oppdaterteSaksopplysninger: Saksopplysninger): Either<KunneIkkeOppdatereSaksopplysninger, IkkeValgt> = this.right()

        override fun vedtakError(): Nothing {
            throw IllegalStateException("Søknadsbehandling uten valgt resultat kan ikke vedtas")
        }
    }

    /**
     * Vedtaksperioden og avslagsperioden vil være 1-1 ved denne revurderingstypen.
     * Den vil ikke påvirke vedtakstidslinjen, beregninger eller utbetalinger.
     * @param avslagsperiode Kan være null ved manuelt registrert søknad, dersom det ikke er søkt for et spesifikt tiltak eller periode.
     */
    data class Avslag(
        val avslagsgrunner: NonEmptySet<Avslagsgrunnlag>,
        val avslagsperiode: Periode?,
    ) : Søknadsbehandlingsresultat {
        override val vedtaksperiode = avslagsperiode
        override val innvilgelsesperioder = null
        override val barnetillegg = null
        override val valgteTiltaksdeltakelser = null
        override val antallDagerPerMeldeperiode = null
        override val omgjørRammevedtak: OmgjørRammevedtak = OmgjørRammevedtak.empty

        /**
         * True dersom [avslagsgrunner] ikke er tom.
         * Vi må støtte at [avslagsperiode] er null for særdeles mangelfulle søknader.
         * Må kunne avslå en søknad selv om det ikke er søkt på et tiltak.
         */
        override fun erFerdigutfylt(saksopplysninger: Saksopplysninger): Boolean = avslagsgrunner.isNotEmpty()

        /** Avslag påvirkes ikke av oppdaterte saksopplysninger */
        override fun oppdaterSaksopplysninger(oppdaterteSaksopplysninger: Saksopplysninger): Either<KunneIkkeOppdatereSaksopplysninger, Avslag> = this.right()
    }

    /**
     * Vedtaksperioden og avslagsperioden vil være 1-1 ved denne revurderingstypen.
     *
     * Når saksbehandler velger at en søknadsbehandling skal innvilges, får de ikke lagret før de har valgt [innvilgelsesperioder]
     */
    data class Innvilgelse(
        override val innvilgelsesperioder: Innvilgelsesperioder,
        override val barnetillegg: Barnetillegg,
        override val omgjørRammevedtak: OmgjørRammevedtak,
    ) : Rammebehandlingsresultat.Innvilgelse,
        Søknadsbehandlingsresultat {
        override val vedtaksperiode = innvilgelsesperioder.totalPeriode
        override val valgteTiltaksdeltakelser = innvilgelsesperioder.valgteTiltaksdeltagelser
        override val antallDagerPerMeldeperiode = innvilgelsesperioder.antallDagerPerMeldeperiode

        init {
            require(!innvilgelsesperioder.harReversertOpphør) { "Reversert opphør kan bare innvilges ved omgjøring" }
        }

        /**
         * Innvilgelsen nullstilles ikke dersom tiltaksdeltakelsene er endret.
         * Se [Innvilgelsesperioder.medOppdaterteTiltaksdeltakelser].
         */
        override fun oppdaterSaksopplysninger(oppdaterteSaksopplysninger: Saksopplysninger): Either<KunneIkkeOppdatereSaksopplysninger, Søknadsbehandlingsresultat> {
            return this.copy(
                innvilgelsesperioder = innvilgelsesperioder.medOppdaterteTiltaksdeltakelser(oppdaterteSaksopplysninger.tiltaksdeltakelser),
            ).right()
        }
    }
}
