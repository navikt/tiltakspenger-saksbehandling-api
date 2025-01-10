package no.nav.tiltakspenger.saksbehandling.domene.vilkår.introduksjonsprogrammet

import no.nav.tiltakspenger.felles.exceptions.StøtterIkkeUtfallException
import no.nav.tiltakspenger.felles.nå
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.Lovreferanse
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.UtfallForPeriode
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.Vilkår
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.felles.Deltagelse.DELTAR
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.felles.Deltagelse.DELTAR_IKKE

/**
 * Introduksjonsprogrammet: https://www.regjeringen.no/no/tema/innvandring-og-integrering/asd/Verkemiddel-i-integreringsarbeidet/introduksjonsprogram/id2343472/
 *
 * @param søknadSaksopplysning Saksopplysninger som kan være avgjørende for vurderingen. Kan ikke ha hull. [avklartSaksopplysning]/faktumet er den avgjørende saksopplysningen.
 * @param avklartSaksopplysning Faktumet som avgjør om vilkåret er oppfylt eller ikke. Null implisiserer uavklart.
 */
data class IntroVilkår private constructor(
    override val vurderingsperiode: Periode,
    val søknadSaksopplysning: IntroSaksopplysning.Søknad,
    val saksbehandlerSaksopplysning: IntroSaksopplysning.Saksbehandler?,
    val avklartSaksopplysning: IntroSaksopplysning,
) : Vilkår {
    override val lovreferanse = Lovreferanse.INTROPROGRAMMET

    override val utfall: Periodisering<UtfallForPeriode> =
        avklartSaksopplysning.deltar.map {
            when (it) {
                DELTAR -> throw StøtterIkkeUtfallException("Deltagelse på introduksjonsprogrammet fører til avslag eller delvis innvilgelse")
                DELTAR_IKKE -> UtfallForPeriode.OPPFYLT
            }
        }

    override fun krymp(nyPeriode: Periode): IntroVilkår {
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

    fun leggTilSaksbehandlerSaksopplysning(command: LeggTilIntroSaksopplysningCommand): IntroVilkår {
        val introSaksopplysning =
            IntroSaksopplysning.Saksbehandler(
                deltar =
                Periodisering(
                    command.deltakelseForPeriode.map { PeriodeMedVerdi(it.tilDeltagelse(), it.periode) },
                ).utvid(DELTAR_IKKE, vurderingsperiode),
                navIdent = command.saksbehandler.navIdent,
                tidsstempel = nå(),
            )
        return this.copy(
            saksbehandlerSaksopplysning = introSaksopplysning,
            avklartSaksopplysning = introSaksopplysning,
        )
    }

    init {
        if (saksbehandlerSaksopplysning != null) {
            require(søknadSaksopplysning.totalePeriode == saksbehandlerSaksopplysning.totalePeriode) {
                "søknadSaksopplysning (${søknadSaksopplysning.totalePeriode}) og saksbehandlerSaksopplysning(${saksbehandlerSaksopplysning.totalePeriode}) må ha samme totale periode."
            }
            // TODO jah: Her burde vi kanskje heller sjekke at de er det samme objektet?
            require(saksbehandlerSaksopplysning.totalePeriode == avklartSaksopplysning.totalePeriode) {
                "saksbehandlerSaksopplysning (${saksbehandlerSaksopplysning.totalePeriode}) og avklartSaksopplysning(${avklartSaksopplysning.totalePeriode}) må ha samme totale periode."
            }
        }
        require(søknadSaksopplysning.totalePeriode == avklartSaksopplysning.totalePeriode) {
            "søknadSaksopplysning (${søknadSaksopplysning.totalePeriode}) og avklartSaksopplysning(${avklartSaksopplysning.totalePeriode}) må ha samme totale periode."
        }
    }

    companion object {
        fun opprett(
            vurderingsperiode: Periode,
            søknadSaksopplysning: IntroSaksopplysning.Søknad,
        ): IntroVilkår =
            IntroVilkår(
                vurderingsperiode = vurderingsperiode,
                søknadSaksopplysning = søknadSaksopplysning,
                saksbehandlerSaksopplysning = null,
                avklartSaksopplysning = søknadSaksopplysning,
            )

        /**
         * Skal kun kalles fra database-laget og for assert av tester (expected).
         */
        fun fromDb(
            vurderingsperiode: Periode,
            søknadSaksopplysning: IntroSaksopplysning.Søknad,
            saksbehandlerSaksopplysning: IntroSaksopplysning.Saksbehandler?,
            avklartSaksopplysning: IntroSaksopplysning,
            utfall: Periodisering<UtfallForPeriode>,
        ): IntroVilkår =
            IntroVilkår(
                vurderingsperiode = vurderingsperiode,
                søknadSaksopplysning = søknadSaksopplysning,
                saksbehandlerSaksopplysning = saksbehandlerSaksopplysning,
                avklartSaksopplysning = avklartSaksopplysning,
            ).also {
                check(utfall == it.utfall) {
                    "Mismatch mellom utfallet som er lagret i IntroVilkår ($utfall), og utfallet som har blitt utledet (${it.utfall})"
                }
            }
    }
}
