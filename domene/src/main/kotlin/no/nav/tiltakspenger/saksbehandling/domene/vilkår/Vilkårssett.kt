package no.nav.tiltakspenger.saksbehandling.domene.vilkår

import arrow.core.Either
import no.nav.tiltakspenger.felles.exceptions.StøtterIkkeUtfallException
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Søknad
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.Tiltaksdeltagelse
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.alder.AlderSaksopplysning.Register
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.alder.AlderVilkår
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.alder.LeggTilAlderSaksopplysningCommand
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.institusjonsopphold.InstitusjonsoppholdVilkår
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.institusjonsopphold.institusjonsoppholdSaksopplysning
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.introduksjonsprogrammet.IntroVilkår
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.introduksjonsprogrammet.LeggTilIntroSaksopplysningCommand
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.introduksjonsprogrammet.introSaksopplysning
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.kravfrist.KravfristVilkår
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.kravfrist.LeggTilKravfristSaksopplysningCommand
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.kravfrist.kravfristSaksopplysning
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.kvp.KVPVilkår
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.kvp.LeggTilKvpSaksopplysningCommand
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.kvp.kvpSaksopplysning
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.livsopphold.LeggTilLivsoppholdSaksopplysningCommand
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.livsopphold.LivsoppholdVilkår
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.livsopphold.livsoppholdSaksopplysning
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.tiltaksdeltagelse.LeggTilTiltaksdeltagelseKommando
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.tiltaksdeltagelse.TiltaksdeltagelseVilkår
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.tiltaksdeltagelse.tilRegisterSaksopplysning
import java.time.LocalDate

/**
 * Ref til begrepskatalogen.
 * Vil både være inngangsvilkår og andre vilkår.
 * Det totale settet vilkår.
 * TODO John + Anders: Denne skal slettes når vi har laget den enkle vilkårsvurdering og vedtaksflyten. Husk migrering av databasen.
 */
data class Vilkårssett(
    val vurderingsperiode: Periode,
    val institusjonsoppholdVilkår: InstitusjonsoppholdVilkår,
    val kvpVilkår: KVPVilkår,
    val tiltakDeltagelseVilkår: TiltaksdeltagelseVilkår,
    val introVilkår: IntroVilkår,
    val livsoppholdVilkår: LivsoppholdVilkår,
    val alderVilkår: AlderVilkår,
    val kravfristVilkår: KravfristVilkår,
) {
    private val vilkårliste: List<Vilkår> =
        listOf(
            institusjonsoppholdVilkår,
            kvpVilkår,
            tiltakDeltagelseVilkår,
            introVilkår,
            livsoppholdVilkår,
            alderVilkår,
            kravfristVilkår,
        )

    val samletUtfall: SamletUtfall =
        when {
            vilkårliste.any { it.samletUtfall() == SamletUtfall.UAVKLART } -> SamletUtfall.UAVKLART
            vilkårliste.all { it.samletUtfall() == SamletUtfall.OPPFYLT } -> SamletUtfall.OPPFYLT
            // Her har vi allerede sjekket at ingen vilkår er uavklart. Dersom ett er IKKE_OPPFYLT, så er det samlet sett IKKE_OPPFYLT.
            vilkårliste.any { it.samletUtfall() == SamletUtfall.IKKE_OPPFYLT } -> SamletUtfall.IKKE_OPPFYLT
            else -> throw StøtterIkkeUtfallException("Vi støtter ikke delvis oppfylt")
        }

    fun utfallsperioder(): Periodisering<UtfallForPeriode> =
        vilkårliste.fold(
            Periodisering(UtfallForPeriode.OPPFYLT, vurderingsperiode),
        ) { total, vilkår -> total.kombiner(vilkår.utfall, UtfallForPeriode::kombiner).slåSammenTilstøtendePerioder() }

    init {
        require(vurderingsperiode == institusjonsoppholdVilkår.vurderingsperiode) {
            "vurderingsperiode($vurderingsperiode) og institusjonsoppholdVilkår.vurderingsperiode(${institusjonsoppholdVilkår.vurderingsperiode}) må være like."
        }
        require(vurderingsperiode == kvpVilkår.vurderingsperiode) {
            "vurderingsperiode($vurderingsperiode) og kvpVilkår.vurderingsperiode(${kvpVilkår.vurderingsperiode}) må være like."
        }
        require(vurderingsperiode == tiltakDeltagelseVilkår.vurderingsperiode) {
            "vurderingsperiode($vurderingsperiode) og tiltakDeltagelseVilkår.vurderingsperiode(${tiltakDeltagelseVilkår.vurderingsperiode}) må være like."
        }
        require(vurderingsperiode == introVilkår.vurderingsperiode) {
            "vurderingsperiode($vurderingsperiode) og introVilkår.vurderingsperiode(${introVilkår.vurderingsperiode}) må være like."
        }
        require(vurderingsperiode == livsoppholdVilkår.vurderingsperiode) {
            "vurderingsperiode($vurderingsperiode) og livsoppholdVilkår.vurderingsperiode(${livsoppholdVilkår.vurderingsperiode}) må være like."
        }
        require(vurderingsperiode == alderVilkår.vurderingsperiode) {
            "vurderingsperiode($vurderingsperiode) og alderVilkår.vurderingsperiode(${alderVilkår.vurderingsperiode}) må være like."
        }
        require(vurderingsperiode == kravfristVilkår.vurderingsperiode) {
            "vurderingsperiode($vurderingsperiode) og kravfristVilkår.vurderingsperiode(${kravfristVilkår.vurderingsperiode}) må være like."
        }
    }

    fun oppdaterKVP(command: LeggTilKvpSaksopplysningCommand): Vilkårssett {
        return this.copy(
            kvpVilkår = kvpVilkår.leggTilSaksbehandlerSaksopplysning(command),
        )
    }

    fun oppdaterIntro(command: LeggTilIntroSaksopplysningCommand): Vilkårssett {
        return this.copy(
            introVilkår = introVilkår.leggTilSaksbehandlerSaksopplysning(command),
        )
    }

    fun oppdaterAlder(command: LeggTilAlderSaksopplysningCommand): Vilkårssett {
        return this.copy(
            alderVilkår = alderVilkår.leggTilSaksbehandlerSaksopplysning(command),
        )
    }

    fun oppdaterKravdato(command: LeggTilKravfristSaksopplysningCommand): Vilkårssett {
        return this.copy(
            kravfristVilkår = kravfristVilkår.leggTilSaksbehandlerSaksopplysning(command),
        )
    }

    fun oppdaterLivsopphold(command: LeggTilLivsoppholdSaksopplysningCommand): Either<KanIkkeLeggeTilSaksopplysning, Vilkårssett> {
        return livsoppholdVilkår.leggTilSaksbehandlerSaksopplysning(command).map {
            this.copy(livsoppholdVilkår = it)
        }
    }
    fun oppdaterTiltaksdeltagelse(kommando: LeggTilTiltaksdeltagelseKommando): Either<KanIkkeLeggeTilSaksopplysning, Vilkårssett> {
        return tiltakDeltagelseVilkår.leggTilSaksbehandlerSaksopplysning(kommando).map {
            this.copy(tiltakDeltagelseVilkår = it)
        }
    }

    /**
     * Støtter kun krymping av periode i første versjon.
     */
    fun krymp(nyPeriode: Periode): Vilkårssett {
        if (vurderingsperiode == nyPeriode) return this
        require(vurderingsperiode.inneholderHele(nyPeriode)) { "Ny periode ($nyPeriode) må være innenfor vedtakets periode ($vurderingsperiode)" }
        return this.copy(
            vurderingsperiode = nyPeriode,
            institusjonsoppholdVilkår = institusjonsoppholdVilkår.krymp(nyPeriode),
            kvpVilkår = kvpVilkår.krymp(nyPeriode),
            tiltakDeltagelseVilkår = tiltakDeltagelseVilkår.krymp(nyPeriode),
            introVilkår = introVilkår.krymp(nyPeriode),
            livsoppholdVilkår = livsoppholdVilkår.krymp(nyPeriode),
            alderVilkår = alderVilkår.krymp(nyPeriode),
            kravfristVilkår = kravfristVilkår.krymp(nyPeriode),
        )
    }

    companion object {
        fun opprett(
            søknad: Søknad,
            fødselsdato: LocalDate,
            tiltaksdeltagelse: Tiltaksdeltagelse,
            vurderingsperiode: Periode,
        ): Vilkårssett =
            Vilkårssett(
                vurderingsperiode = vurderingsperiode,
                institusjonsoppholdVilkår =
                InstitusjonsoppholdVilkår.opprett(
                    vurderingsperiode,
                    søknad.institusjonsoppholdSaksopplysning(
                        vurderingsperiode,
                    ),
                ),
                kvpVilkår = KVPVilkår.opprett(vurderingsperiode, søknad.kvpSaksopplysning(vurderingsperiode)),
                introVilkår = IntroVilkår.opprett(vurderingsperiode, søknad.introSaksopplysning(vurderingsperiode)),
                livsoppholdVilkår =
                LivsoppholdVilkår.opprett(
                    søknad.livsoppholdSaksopplysning(vurderingsperiode),
                    vurderingsperiode,
                ),
                alderVilkår =
                AlderVilkår.opprett(
                    Register.opprett(fødselsdato = fødselsdato),
                    vurderingsperiode,
                ),
                kravfristVilkår = KravfristVilkår.opprett(søknad.kravfristSaksopplysning(), vurderingsperiode),
                tiltakDeltagelseVilkår =
                TiltaksdeltagelseVilkår.opprett(
                    vurderingsperiode = vurderingsperiode,
                    registerSaksopplysning = tiltaksdeltagelse.tilRegisterSaksopplysning(),
                ),
            )
    }
}
