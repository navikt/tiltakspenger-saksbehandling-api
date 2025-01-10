package no.nav.tiltakspenger.saksbehandling.domene.vilkår.kvp

import no.nav.tiltakspenger.felles.exceptions.StøtterIkkeUtfallException
import no.nav.tiltakspenger.felles.nå
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.Lovreferanse
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.UtfallForPeriode
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.Vilkår
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.felles.Deltagelse

/**
 * Kvalifiseringsprogrammet (KVP): https://www.nav.no/kvalifiseringsprogrammet
 *
 * @param søknadSaksopplysning Saksopplysninger som kan være avgjørende for vurderingen. Kan ikke ha hull. [avklartSaksopplysning]/faktumet er den avgjørende saksopplysningen.
 * @param avklartSaksopplysning Faktumet som avgjør om vilkåret er oppfylt eller ikke. Null implisiserer uavklart.
 */
data class KVPVilkår private constructor(
    override val vurderingsperiode: Periode,
    val søknadSaksopplysning: KvpSaksopplysning,
    val saksbehandlerSaksopplysning: KvpSaksopplysning?,
    val avklartSaksopplysning: KvpSaksopplysning,
) : Vilkår {
    override val lovreferanse = Lovreferanse.KVP

    init {
        require(vurderingsperiode == søknadSaksopplysning.totalePeriode) {
            "søknadSaksopplysning (${søknadSaksopplysning.totalePeriode}) og vurderingsperiode($vurderingsperiode) må ha samme totale periode."
        }
        require(vurderingsperiode == avklartSaksopplysning.totalePeriode) {
            "avklartSaksopplysning (${avklartSaksopplysning.totalePeriode}) og vurderingsperiode($vurderingsperiode) må ha samme totale periode."
        }
        if (saksbehandlerSaksopplysning != null) {
            require(vurderingsperiode == saksbehandlerSaksopplysning.totalePeriode) {
                "saksbehandlerSaksopplysning (${saksbehandlerSaksopplysning.totalePeriode}) og vurderingsperiode($vurderingsperiode) må ha samme totale periode."
            }
        }
    }

    override val utfall: Periodisering<UtfallForPeriode> =
        avklartSaksopplysning.deltar.map {
            when (it) {
                Deltagelse.DELTAR -> throw StøtterIkkeUtfallException("Deltagelse på kvalifikasjonsprogram fører til avslag eller delvis innvilgelse.")
                Deltagelse.DELTAR_IKKE -> UtfallForPeriode.OPPFYLT
            }
        }

    override fun krymp(nyPeriode: Periode): KVPVilkår {
        if (vurderingsperiode == nyPeriode) return this
        require(vurderingsperiode.inneholderHele(nyPeriode)) { "Ny periode ($nyPeriode) må være innenfor vurderingsperioden ($vurderingsperiode)" }
        val nySøknadSaksopplsyning = søknadSaksopplysning.oppdaterPeriode(nyPeriode)
        val nySaksbehandlerSaksopplysning = saksbehandlerSaksopplysning?.oppdaterPeriode(nyPeriode)
        return this.copy(
            vurderingsperiode = nyPeriode,
            søknadSaksopplysning = nySøknadSaksopplsyning,
            saksbehandlerSaksopplysning = nySaksbehandlerSaksopplysning,
            avklartSaksopplysning = nySaksbehandlerSaksopplysning ?: nySøknadSaksopplsyning,
        )
    }

    fun leggTilSaksbehandlerSaksopplysning(command: LeggTilKvpSaksopplysningCommand): KVPVilkår {
        val kvpSaksopplysning =
            KvpSaksopplysning.Saksbehandler(
                deltar =
                Periodisering(
                    command.deltakelseForPeriode.map { PeriodeMedVerdi(it.tilDeltagelse(), it.periode) },
                ).utvid(Deltagelse.DELTAR_IKKE, vurderingsperiode),
                navIdent = command.saksbehandler.navIdent,
                tidsstempel = nå(),
            )
        return this.copy(
            saksbehandlerSaksopplysning = kvpSaksopplysning,
            avklartSaksopplysning = kvpSaksopplysning,
        )
    }

    init {
        if (saksbehandlerSaksopplysning != null) {
            require(søknadSaksopplysning.totalePeriode == saksbehandlerSaksopplysning.totalePeriode) {
                "søknadSaksopplysning (${søknadSaksopplysning.totalePeriode}) og saksbehandlerSaksopplysning(${saksbehandlerSaksopplysning.totalePeriode}) må ha samme totale periode."
            }
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
            søknadSaksopplysning: KvpSaksopplysning,
        ): KVPVilkår =
            KVPVilkår(
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
            søknadSaksopplysning: KvpSaksopplysning,
            saksbehandlerSaksopplysning: KvpSaksopplysning?,
            avklartSaksopplysning: KvpSaksopplysning,
            utfall: Periodisering<UtfallForPeriode>,
        ): KVPVilkår =
            KVPVilkår(
                vurderingsperiode = vurderingsperiode,
                søknadSaksopplysning = søknadSaksopplysning,
                saksbehandlerSaksopplysning = saksbehandlerSaksopplysning,
                avklartSaksopplysning = avklartSaksopplysning,
            ).also {
                check(utfall == it.utfall) {
                    "Mismatch mellom utfallet som er lagret i KVPVilkår ($utfall), og utfallet som har blitt utledet (${it.utfall})"
                }
            }
    }
}
