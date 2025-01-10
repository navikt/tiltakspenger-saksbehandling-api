package no.nav.tiltakspenger.saksbehandling.domene.vilkår.alder

import no.nav.tiltakspenger.felles.exceptions.StøtterIkkeUtfallException
import no.nav.tiltakspenger.felles.nå
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.Lovreferanse
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.UtfallForPeriode
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.Vilkår

/**
 * Alder
 *
 * @param registerSaksopplysning Saksopplysninger som kan være avgjørende for vurderingen. Kan ikke ha hull. [avklartSaksopplysning]/faktumet er den avgjørende saksopplysningen.
 * @param avklartSaksopplysning Faktumet som avgjør om vilkåret er oppfylt eller ikke. Null implisiserer uavklart.
 */
data class AlderVilkår private constructor(
    override val vurderingsperiode: Periode,
    val registerSaksopplysning: AlderSaksopplysning.Register,
    val saksbehandlerSaksopplysning: AlderSaksopplysning.Saksbehandler?,
    val avklartSaksopplysning: AlderSaksopplysning,
) : Vilkår {
    override val lovreferanse = Lovreferanse.ALDER
    override val utfall: Periodisering<UtfallForPeriode> = run {
        // Om noen har bursdag 29. mars (skuddår) og de akkurat har fylt 18 vil fødselsdagen bli satt til 28. mars, og de vil få krav på tiltakspenger én dag før de er 18.
        // Dette er så cornercase at vi per nå velger ikke å gjøre det pga. a) veldig lav forekomst/sannsynlighet og b) konsekvens; dette er i brukers favør.
        val dagenBrukerFyller18År = registerSaksopplysning.fødselsdato.plusYears(18)
        if (dagenBrukerFyller18År.isAfter(vurderingsperiode.fraOgMed)) throw StøtterIkkeUtfallException("Vi støtter ikke delvis innvilgelse eller avslag")
        Periodisering(
            UtfallForPeriode.OPPFYLT,
            vurderingsperiode,
        )
    }

    override fun krymp(nyPeriode: Periode): AlderVilkår {
        if (vurderingsperiode == nyPeriode) return this
        require(vurderingsperiode.inneholderHele(nyPeriode)) { "Ny periode ($nyPeriode) må være innenfor vurderingsperioden ($vurderingsperiode)" }
        val nyRegisterSaksopplysning = registerSaksopplysning.oppdaterPeriode(nyPeriode)
        val nySaksbehandlerSaksopplysning = saksbehandlerSaksopplysning?.oppdaterPeriode(nyPeriode)
        return this.copy(
            vurderingsperiode = nyPeriode,
            saksbehandlerSaksopplysning = nySaksbehandlerSaksopplysning,
            registerSaksopplysning = nyRegisterSaksopplysning,
            avklartSaksopplysning = nySaksbehandlerSaksopplysning ?: nyRegisterSaksopplysning,
        )
    }

    fun leggTilSaksbehandlerSaksopplysning(command: LeggTilAlderSaksopplysningCommand): AlderVilkår {
        val introSaksopplysning =
            AlderSaksopplysning.Saksbehandler(
                fødselsdato = command.fødselsdato,
                navIdent = command.saksbehandler.navIdent,
                tidsstempel = nå(),
            )
        return this.copy(
            saksbehandlerSaksopplysning = introSaksopplysning,
            avklartSaksopplysning = introSaksopplysning,
        )
    }

    companion object {
        fun opprett(
            registerSaksopplysning: AlderSaksopplysning.Register,
            vurderingsperiode: Periode,
        ): AlderVilkår =
            AlderVilkår(
                registerSaksopplysning = registerSaksopplysning,
                saksbehandlerSaksopplysning = null,
                avklartSaksopplysning = registerSaksopplysning,
                vurderingsperiode = vurderingsperiode,
            )

        /**
         * Skal kun kalles fra database-laget og for assert av tester (expected).
         */
        fun fromDb(
            registerSaksopplysning: AlderSaksopplysning.Register,
            saksbehandlerSaksopplysning: AlderSaksopplysning.Saksbehandler?,
            avklartSaksopplysning: AlderSaksopplysning,
            vurderingsperiode: Periode,
            utfall: Periodisering<UtfallForPeriode>,
        ): AlderVilkår =
            AlderVilkår(
                registerSaksopplysning = registerSaksopplysning,
                saksbehandlerSaksopplysning = saksbehandlerSaksopplysning,
                avklartSaksopplysning = avklartSaksopplysning,
                vurderingsperiode = vurderingsperiode,
            ).also {
                check(utfall == it.utfall) {
                    "Mismatch mellom utfallet som er lagret i AlderVilkår ($utfall), og utfallet som har blitt utledet (${it.utfall})"
                }
            }
    }
}
