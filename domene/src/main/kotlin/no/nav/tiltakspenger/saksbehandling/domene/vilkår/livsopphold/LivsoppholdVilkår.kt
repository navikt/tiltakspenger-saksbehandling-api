package no.nav.tiltakspenger.saksbehandling.domene.vilkår.livsopphold

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.felles.exceptions.StøtterIkkeUtfallException
import no.nav.tiltakspenger.felles.nå
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.KanIkkeLeggeTilSaksopplysning
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.Lovreferanse
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.SamletUtfall
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.UtfallForPeriode
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.Vilkår

/**
 * Livsoppholdytelser skal ha minimal med støtte ved lansering for én bruker.
 *
 *
 * @param søknadssaksopplysning Sier noe om bruker har svart at hen mottar livsoppholdytelser i søknaden.
 * @param saksbehandlerSaksopplysning Faktumet som avgjør om vilkåret er oppfylt eller ikke. Null implisiserer uavklart.
 * @param avklartSaksopplysning Sier noe om hvilken saksopplysning som er gjeldende; subsumsjonen. For livsoppholdytelser er dette til og begynne med alltid saksbehandler.
 * @param vurderingsperiode Vurderingsperioden faktumene må si noe om.
 *
 */
data class LivsoppholdVilkår private constructor(
    override val vurderingsperiode: Periode,
    val søknadssaksopplysning: LivsoppholdSaksopplysning.Søknad,
    val saksbehandlerSaksopplysning: LivsoppholdSaksopplysning.Saksbehandler?,
    val avklartSaksopplysning: LivsoppholdSaksopplysning?,
) : Vilkår {
    override val lovreferanse = Lovreferanse.LIVSOPPHOLDYTELSER

    override val utfall: Periodisering<UtfallForPeriode> =
        when {
            avklartSaksopplysning == null -> {
                Periodisering(
                    UtfallForPeriode.UAVKLART,
                    vurderingsperiode,
                )
            }

            !avklartSaksopplysning.harLivsoppholdYtelser -> {
                Periodisering(
                    UtfallForPeriode.OPPFYLT,
                    vurderingsperiode,
                )
            }

            avklartSaksopplysning.harLivsoppholdYtelser -> throw StøtterIkkeUtfallException("Andre ytelser til livsopphold fører til avslag eller delvis innvilgelse.")
            else -> throw IllegalStateException("Andre ytelser til livsopphold har ugyldig utfall")
        }

    override fun krymp(nyPeriode: Periode): LivsoppholdVilkår {
        if (vurderingsperiode == nyPeriode) return this
        require(vurderingsperiode.inneholderHele(nyPeriode)) { "Ny periode ($nyPeriode) må være innenfor vurderingsperioden ($vurderingsperiode)" }
        val nySøknadSaksopplysning = søknadssaksopplysning.oppdaterPeriode(periode = nyPeriode)
        val nySaksbehandlerSaksopplysning = saksbehandlerSaksopplysning?.oppdaterPeriode(periode = nyPeriode)
        return this.copy(
            vurderingsperiode = nyPeriode,
            søknadssaksopplysning = nySøknadSaksopplysning,
            saksbehandlerSaksopplysning = nySaksbehandlerSaksopplysning,
            avklartSaksopplysning = nySaksbehandlerSaksopplysning ?: nySøknadSaksopplysning,
        )
    }

    init {
        if (avklartSaksopplysning !=
            null
        ) {
            require(
                avklartSaksopplysning.periode.inneholderHele(vurderingsperiode),
            ) { "Saksopplysningnen må dekke hele vurderingsperioden" }
        }
        require(søknadssaksopplysning.periode.inneholderHele(vurderingsperiode)) { "Saksopplysningnen må dekke hele vurderingsperioden" }
        if (saksbehandlerSaksopplysning != null) {
            require(
                saksbehandlerSaksopplysning.periode.inneholderHele(vurderingsperiode),
            ) { "Saksopplysningnen må dekke hele vurderingsperioden" }

            require(avklartSaksopplysning == saksbehandlerSaksopplysning) {
                "Om vi har saksopplysning fra saksbehandler må den avklarte saksopplysningen være fra saksbehandler"
            }
        } else {
            require(
                avklartSaksopplysning == null,
            ) { "Dersom vi ikke har saksbehandlerSaksopplysning, skal avklartSaksopplysning være null" }
        }
    }

    val samletUtfall: SamletUtfall =
        when {
            avklartSaksopplysning == null -> SamletUtfall.UAVKLART
            avklartSaksopplysning.harLivsoppholdYtelser -> SamletUtfall.IKKE_OPPFYLT
            !avklartSaksopplysning.harLivsoppholdYtelser -> SamletUtfall.OPPFYLT
            else -> throw IllegalStateException(
                "Livoppholdvilkår: Ugyldig utfall. harLivsoppholdYtelser: ${avklartSaksopplysning.harLivsoppholdYtelser}",
            )
        }

    fun leggTilSaksbehandlerSaksopplysning(
        command: LeggTilLivsoppholdSaksopplysningCommand,
    ): Either<KanIkkeLeggeTilSaksopplysning, LivsoppholdVilkår> {
        if (!vurderingsperiode.inneholderHele(command.harYtelseForPeriode.periode)) {
            return KanIkkeLeggeTilSaksopplysning.PeriodenMåVæreLikVurderingsperioden.left()
        }
        val livsoppholdSaksopplysning =
            LivsoppholdSaksopplysning.Saksbehandler(
                harLivsoppholdYtelser = command.harYtelseForPeriode.harYtelse,
                tidsstempel = nå(),
                navIdent = command.saksbehandler.navIdent,
                periode = vurderingsperiode,
            )
        return this
            .copy(
                saksbehandlerSaksopplysning = livsoppholdSaksopplysning,
                avklartSaksopplysning = livsoppholdSaksopplysning,
            ).right()
    }

    companion object {
        fun opprett(
            søknadSaksopplysning: LivsoppholdSaksopplysning.Søknad,
            vurderingsperiode: Periode,
        ): LivsoppholdVilkår =
            LivsoppholdVilkår(
                søknadssaksopplysning = søknadSaksopplysning,
                saksbehandlerSaksopplysning = null,
                avklartSaksopplysning = null,
                vurderingsperiode = vurderingsperiode,
            )

        /**
         * Skal kun kalles fra database-laget og for assert av tester (expected).
         */
        fun fromDb(
            søknadSaksopplysning: LivsoppholdSaksopplysning.Søknad,
            saksbehandlerSaksopplysning: LivsoppholdSaksopplysning.Saksbehandler?,
            avklartSaksopplysning: LivsoppholdSaksopplysning?,
            vurderingsperiode: Periode,
            utfall: Periodisering<UtfallForPeriode>,
        ): LivsoppholdVilkår =
            LivsoppholdVilkår(
                søknadssaksopplysning = søknadSaksopplysning,
                saksbehandlerSaksopplysning = saksbehandlerSaksopplysning,
                avklartSaksopplysning = avklartSaksopplysning,
                vurderingsperiode = vurderingsperiode,
            ).also {
                check(utfall == it.utfall) {
                    "Mismatch mellom utfallet som er lagret i LivsoppholdVilkår ($utfall), og utfallet som har blitt utledet (${it.utfall})"
                }
            }
    }
}
