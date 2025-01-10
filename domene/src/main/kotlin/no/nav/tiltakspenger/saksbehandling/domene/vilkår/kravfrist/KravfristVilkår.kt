package no.nav.tiltakspenger.saksbehandling.domene.vilkår.kravfrist

import no.nav.tiltakspenger.felles.exceptions.StøtterIkkeUtfallException
import no.nav.tiltakspenger.felles.nå
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.Lovreferanse
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.UtfallForPeriode
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.Vilkår

/**
 * Kravfrist
 *
 * @param søknadSaksopplysning Saksopplysninger som kan være avgjørende for vurderingen. Kan ikke ha hull. [avklartSaksopplysning]/faktumet er den avgjørende saksopplysningen.
 * @param avklartSaksopplysning Faktumet som avgjør om vilkåret er oppfylt eller ikke. Null implisiserer uavklart.
 */
data class KravfristVilkår private constructor(
    override val vurderingsperiode: Periode,
    val søknadSaksopplysning: KravfristSaksopplysning.Søknad,
    val saksbehandlerSaksopplysning: KravfristSaksopplysning.Saksbehandler?,
    val avklartSaksopplysning: KravfristSaksopplysning,
) : Vilkår {
    override val lovreferanse = Lovreferanse.FRIST_FOR_FRAMSETTING_AV_KRAV

    override val utfall: Periodisering<UtfallForPeriode> = run {
        val datoDetKanInnvilgesFra =
            avklartSaksopplysning.kravdato
                .withDayOfMonth(1)
                .minusMonths(3)
                .toLocalDate()

        when {
            datoDetKanInnvilgesFra <= vurderingsperiode.fraOgMed -> Periodisering(
                UtfallForPeriode.OPPFYLT,
                vurderingsperiode,
            )

            datoDetKanInnvilgesFra > vurderingsperiode.tilOgMed -> throw StøtterIkkeUtfallException("Kravdatoen vil føre til avslag")
            else -> throw StøtterIkkeUtfallException(
                "Tidligste dato det kan innvilges fra er $datoDetKanInnvilgesFra, ettersom kravdato er (${avklartSaksopplysning.kravdato}). Tiltaksperioden det er søkt for er ($vurderingsperiode). Kravdatoen vil føre til delvis innvilgelse.",
            )
        }
    }

    override fun krymp(nyPeriode: Periode): KravfristVilkår {
        if (vurderingsperiode == nyPeriode) return this
        require(vurderingsperiode.inneholderHele(nyPeriode)) { "Ny periode ($nyPeriode) må være innenfor vurderingsperioden ($vurderingsperiode)" }
        val nySøknadSaksopplysning = søknadSaksopplysning.oppdaterPeriode(nyPeriode)
        val nySaksbehandlerSaksopplysning = saksbehandlerSaksopplysning?.oppdaterPeriode(nyPeriode)
        return this.copy(
            vurderingsperiode = nyPeriode,
            søknadSaksopplysning = nySøknadSaksopplysning,
            saksbehandlerSaksopplysning = nySaksbehandlerSaksopplysning,
            avklartSaksopplysning = nySaksbehandlerSaksopplysning ?: nySøknadSaksopplysning,
        )
    }

    fun leggTilSaksbehandlerSaksopplysning(command: LeggTilKravfristSaksopplysningCommand): KravfristVilkår {
        val kravfristSaksopplysning =
            KravfristSaksopplysning.Saksbehandler(
                kravdato = command.kravdato,
                navIdent = command.saksbehandler.navIdent,
                tidsstempel = nå(),
            )
        return this.copy(
            saksbehandlerSaksopplysning = kravfristSaksopplysning,
            avklartSaksopplysning = kravfristSaksopplysning,
        )
    }

    companion object {
        fun opprett(
            søknadSaksopplysning: KravfristSaksopplysning.Søknad,
            vurderingsperiode: Periode,
        ): KravfristVilkår =
            KravfristVilkår(
                søknadSaksopplysning = søknadSaksopplysning,
                saksbehandlerSaksopplysning = null,
                avklartSaksopplysning = søknadSaksopplysning,
                vurderingsperiode = vurderingsperiode,
            )

        /**
         * Skal kun kalles fra database-laget og for assert av tester (expected).
         */
        fun fromDb(
            søknadSaksopplysning: KravfristSaksopplysning.Søknad,
            saksbehandlerSaksopplysning: KravfristSaksopplysning.Saksbehandler?,
            avklartSaksopplysning: KravfristSaksopplysning,
            vurderingsperiode: Periode,
            utfall: Periodisering<UtfallForPeriode>,
        ): KravfristVilkår =
            KravfristVilkår(
                søknadSaksopplysning = søknadSaksopplysning,
                saksbehandlerSaksopplysning = saksbehandlerSaksopplysning,
                avklartSaksopplysning = avklartSaksopplysning,
                vurderingsperiode = vurderingsperiode,
            ).also {
                check(utfall == it.utfall) {
                    "Mismatch mellom utfallet som er lagret i KravfristVilkår ($utfall), og utfallet som har blitt utledet (${it.utfall})"
                }
            }
    }
}
