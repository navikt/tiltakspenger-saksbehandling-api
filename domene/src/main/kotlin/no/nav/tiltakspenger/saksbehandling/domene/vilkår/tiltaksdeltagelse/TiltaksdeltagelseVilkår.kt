package no.nav.tiltakspenger.saksbehandling.domene.vilkår.tiltaksdeltagelse

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import mu.KotlinLogging
import no.nav.tiltakspenger.felles.nå
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.KanIkkeLeggeTilSaksopplysning
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.Lovreferanse
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.UtfallForPeriode
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.Vilkår

/**
 * @param registerSaksopplysning Saksopplysninger som er avgjørende for vurderingen. Kan ikke ha hull. Må gå til kildesystem for å oppdatere/endre dersom vi oppdager feil i datasettet.
 */
data class TiltaksdeltagelseVilkår private constructor(
    override val vurderingsperiode: Periode,
    val registerSaksopplysning: TiltaksdeltagelseSaksopplysning.Register,
    val saksbehandlerSaksopplysning: TiltaksdeltagelseSaksopplysning.Saksbehandler?,
    val avklartSaksopplysning: TiltaksdeltagelseSaksopplysning,
) : Vilkår {
    private val logger = KotlinLogging.logger { }

    init {
        if (saksbehandlerSaksopplysning != null) {
            require(avklartSaksopplysning == saksbehandlerSaksopplysning) { "Avklart saksopplysning må være lik saksbehandler saksopplysning" }
        }
        check(vurderingsperiode == registerSaksopplysning.deltagelsePeriode) { "Vurderingsperioden ($vurderingsperiode) må være lik deltagelsesperioden (${registerSaksopplysning.deltagelsePeriode})" }
    }

    override val utfall: Periodisering<UtfallForPeriode> = avklartSaksopplysning.utfallForPeriode.utvid(UtfallForPeriode.IKKE_OPPFYLT, vurderingsperiode)

    override fun krymp(nyPeriode: Periode): TiltaksdeltagelseVilkår {
        if (vurderingsperiode == nyPeriode) return this
        require(vurderingsperiode.inneholderHele(nyPeriode)) { "Ny periode ($nyPeriode) må være innenfor vurderingsperioden ($vurderingsperiode)" }
        val nyRegisterSaksopplysning = registerSaksopplysning.oppdaterPeriode(nyPeriode)
        val nySaksbehandlerSaksopplysning = saksbehandlerSaksopplysning?.oppdaterPeriode(nyPeriode)
        return this.copy(
            vurderingsperiode = nyPeriode,
            registerSaksopplysning = nyRegisterSaksopplysning,
            saksbehandlerSaksopplysning = nySaksbehandlerSaksopplysning,
            avklartSaksopplysning = nySaksbehandlerSaksopplysning ?: nyRegisterSaksopplysning,

        )
    }

    fun leggTilSaksbehandlerSaksopplysning(
        kommando: LeggTilTiltaksdeltagelseKommando,
    ): Either<KanIkkeLeggeTilSaksopplysning, TiltaksdeltagelseVilkår> {
        if (vurderingsperiode != kommando.totalPeriode) {
            return KanIkkeLeggeTilSaksopplysning.PeriodenMåVæreLikVurderingsperioden.left()
        }
        val tiltaksdeltagelseSaksopplysning =
            TiltaksdeltagelseSaksopplysning.Saksbehandler(
                tidsstempel = nå(),
                tiltaksnavn = avklartSaksopplysning.tiltaksnavn,
                eksternDeltagelseId = avklartSaksopplysning.eksternDeltagelseId,
                gjennomføringId = avklartSaksopplysning.gjennomføringId,
                deltagelsePeriode = avklartSaksopplysning.deltagelsePeriode,
                // Kommentar jah: Vi støtter kun 1 periode i førsteomgang. Vi må endre på hele datamodellen når vi skal støtter periodisering.
                status = kommando.statusForPeriode.single().status,
                kilde = avklartSaksopplysning.kilde,
                tiltakstype = avklartSaksopplysning.tiltakstype,
                navIdent = kommando.saksbehandler.navIdent,
                årsakTilEndring = kommando.årsakTilEndring,
            )
        return this
            .copy(
                saksbehandlerSaksopplysning = tiltaksdeltagelseSaksopplysning,
                avklartSaksopplysning = tiltaksdeltagelseSaksopplysning,
            ).right()
    }

    override val lovreferanse = Lovreferanse.TILTAKSDELTAGELSE

    companion object {
        fun opprett(
            vurderingsperiode: Periode,
            registerSaksopplysning: TiltaksdeltagelseSaksopplysning.Register,
        ): TiltaksdeltagelseVilkår =
            TiltaksdeltagelseVilkår(
                vurderingsperiode = vurderingsperiode,
                registerSaksopplysning = registerSaksopplysning,
                saksbehandlerSaksopplysning = null,
                avklartSaksopplysning = registerSaksopplysning,
            )

        /**
         * Skal kun kalles fra database-laget og for assert av tester (expected).
         */
        fun fromDb(
            registerSaksopplysning: TiltaksdeltagelseSaksopplysning.Register,
            saksbehandlerSaksopplysning: TiltaksdeltagelseSaksopplysning.Saksbehandler?,
            avklartSaksopplysning: TiltaksdeltagelseSaksopplysning,
            vurderingsperiode: Periode,
            utfall: Periodisering<UtfallForPeriode>,
        ): TiltaksdeltagelseVilkår =
            TiltaksdeltagelseVilkår(
                registerSaksopplysning = registerSaksopplysning,
                vurderingsperiode = vurderingsperiode,
                saksbehandlerSaksopplysning = saksbehandlerSaksopplysning,
                avklartSaksopplysning = avklartSaksopplysning,
            ).also {
                check(utfall == it.utfall) {
                    "Mismatch mellom utfallet som er lagret i TiltakDeltagelseVilkår ($utfall), og utfallet som har blitt utledet (${it.utfall})"
                }
            }
    }
}
