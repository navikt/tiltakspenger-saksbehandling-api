package no.nav.tiltakspenger.vedtak.service.søker

import mu.KotlinLogging
import no.nav.tiltakspenger.domene.Periode
import no.nav.tiltakspenger.vedtak.Barnetillegg
import no.nav.tiltakspenger.vedtak.Innsending
import no.nav.tiltakspenger.vedtak.Personopplysninger
import no.nav.tiltakspenger.vedtak.Søker
import no.nav.tiltakspenger.vedtak.Søknad
import no.nav.tiltakspenger.vedtak.Tiltak
import no.nav.tiltakspenger.vedtak.Tiltaksaktivitet
import no.nav.tiltakspenger.vedtak.Vedlegg
import no.nav.tiltakspenger.vilkårsvurdering.Inngangsvilkårsvurderinger
import no.nav.tiltakspenger.vilkårsvurdering.Utfall
import no.nav.tiltakspenger.vilkårsvurdering.Vilkår
import no.nav.tiltakspenger.vilkårsvurdering.Vurdering
import no.nav.tiltakspenger.vilkårsvurdering.kategori.AlderVilkårsvurderingKategori
import no.nav.tiltakspenger.vilkårsvurdering.kategori.InstitusjonVilkårsvurderingKategori
import no.nav.tiltakspenger.vilkårsvurdering.kategori.KommunaleYtelserVilkårsvurderingKategori
import no.nav.tiltakspenger.vilkårsvurdering.kategori.LønnsinntektVilkårsvurderingKategori
import no.nav.tiltakspenger.vilkårsvurdering.kategori.PensjonsinntektVilkårsvurderingKategori
import no.nav.tiltakspenger.vilkårsvurdering.kategori.StatligeYtelserVilkårsvurderingKategori
import no.nav.tiltakspenger.vilkårsvurdering.kategori.TiltakspengerVilkårsvurderingKategori
import no.nav.tiltakspenger.vilkårsvurdering.kategori.VilkårsvurderingKategori
import no.nav.tiltakspenger.vilkårsvurdering.vurdering.AAPVilkårsvurdering
import no.nav.tiltakspenger.vilkårsvurdering.vurdering.AlderVilkårsvurdering
import no.nav.tiltakspenger.vilkårsvurdering.vurdering.DagpengerVilkårsvurdering
import no.nav.tiltakspenger.vilkårsvurdering.vurdering.ForeldrepengerVilkårsvurdering
import no.nav.tiltakspenger.vilkårsvurdering.vurdering.InstitusjonsoppholdVilkårsvurdering
import no.nav.tiltakspenger.vilkårsvurdering.vurdering.IntroProgrammetVilkårsvurdering
import no.nav.tiltakspenger.vilkårsvurdering.vurdering.KVPVilkårsvurdering
import no.nav.tiltakspenger.vilkårsvurdering.vurdering.LønnsinntektVilkårsvurdering
import no.nav.tiltakspenger.vilkårsvurdering.vurdering.OmsorgspengerVilkårsvurdering
import no.nav.tiltakspenger.vilkårsvurdering.vurdering.OpplæringspengerVilkårsvurdering
import no.nav.tiltakspenger.vilkårsvurdering.vurdering.PensjonsinntektVilkårsvurdering
import no.nav.tiltakspenger.vilkårsvurdering.vurdering.PleiepengerVilkårsvurdering
import no.nav.tiltakspenger.vilkårsvurdering.vurdering.SvangerskapspengerVilkårsvurdering
import no.nav.tiltakspenger.vilkårsvurdering.vurdering.TiltakspengerVilkårsvurdering
import no.nav.tiltakspenger.vilkårsvurdering.vurdering.UføreVilkarsvurdering

private val LOG = KotlinLogging.logger {}

class BehandlingMapper {

    fun mapSøkerOgInnsendinger(søker: Søker, innsendinger: List<Innsending>): SøkerDTO {
        return SøkerDTO(
            ident = søker.ident,
            personopplysninger = søker.personopplysninger?.let { mapPersonopplysninger(it) },
            behandlinger = innsendinger.mapNotNull { mapInnsendingMedSøknad(it) },
        )
    }

    fun mapInnsendingMedSøknad(innsending: Innsending): KlarEllerIkkeKlarForBehandlingDTO? {
        val søknaden = innsending.søknad ?: return null
        return søknaden.let { søknad ->
            if (!innsending.erFerdigstilt()) {
                IkkeKlarForBehandlingDTO(søknad = mapSøknad(søknad))
            } else {
                val vurderingsperiode =
                    innsending.vurderingsperiodeForSøknad()
                        ?: return IkkeKlarForBehandlingDTO(søknad = mapSøknad(søknad))
                            .also { LOG.warn("Fant ikke vurderingsperiode for innsending ${innsending.id}") }

                val vilkårsvurderinger = vilkårsvurderinger(innsending, vurderingsperiode, søknad)
                KlarForBehandlingDTO(
                    søknad = mapSøknad(søknad),
                    registrerteTiltak = innsending.tiltak!!.tiltaksliste.map { mapTiltak(it) },
                    vurderingsperiode = mapVurderingsperiode(vurderingsperiode),
                    tiltakspengerYtelser = mapTiltakspenger(vilkårsvurderinger.tiltakspengerYtelser),
                    statligeYtelser = mapStatligeYtelser(vilkårsvurderinger.statligeYtelser),
                    kommunaleYtelser = mapKommunaleYtelser(vilkårsvurderinger.kommunaleYtelser),
                    pensjonsordninger = mapPensjonsordninger(vilkårsvurderinger.pensjonsordninger),
                    lønnsinntekt = mapLønnsinntekt(vilkårsvurderinger.lønnsinntekt),
                    institusjonsopphold = mapInstitusjonsopphold(vilkårsvurderinger.institusjonopphold),
                    barnetillegg = mapBarnetillegg(søknad.barnetillegg, innsending.personopplysningerBarnMedIdent()),
                    alderVilkårsvurdering = mapAlderVilkårsvurdering(vilkårsvurderinger.alder),
                )
            }
        }
    }

    private fun mapPersonopplysninger(it: Personopplysninger.Søker) = PersonopplysningerDTO(
        fornavn = it.fornavn,
        etternavn = it.etternavn,
        ident = it.ident,
        fødselsdato = it.fødselsdato,
        barn = listOf(),
        fortrolig = it.fortrolig,
        strengtFortrolig = it.strengtFortrolig,
        skjermet = it.skjermet ?: false,
    )

    private fun mapVurderingsperiode(vurderingsperiode: Periode) = PeriodeDTO(
        fra = vurderingsperiode.fra,
        til = vurderingsperiode.til,
    )

    private fun mapTiltak(it: Tiltaksaktivitet) = TiltakDTO(
        arrangør = it.arrangør,
        navn = it.tiltak.navn,
        periode = it.deltakelsePeriode.fom?.let { fom ->
            PeriodeDTO(
                fra = fom,
                til = it.deltakelsePeriode.tom,
            )
        },
        prosent = it.deltakelseProsent,
        dagerIUken = it.antallDagerPerUke,
        status = it.deltakerStatus.tekst,
    )

    private fun mapSøknad(søknad: Søknad) = SøknadDTO(
        id = søknad.id.toString(),
        søknadId = søknad.søknadId,
        søknadsdato = (søknad.opprettet ?: søknad.tidsstempelHosOss).toLocalDate(),
        arrangoernavn = søknad.tiltak?.arrangoernavn,
        tiltakskode = if (søknad.tiltak == null) "Ukjent" else (søknad.tiltak as Tiltak).tiltakskode?.navn
            ?: "Annet",
        beskrivelse = when (søknad.tiltak) {
            is Tiltak.ArenaTiltak -> null
            is Tiltak.BrukerregistrertTiltak -> (søknad.tiltak as Tiltak.BrukerregistrertTiltak).beskrivelse
            else -> null
        },
        startdato = søknad.tiltak?.startdato,
        sluttdato = søknad.tiltak?.sluttdato,
        antallDager = if (søknad.tiltak is Tiltak.BrukerregistrertTiltak) {
            (søknad.tiltak as Tiltak.BrukerregistrertTiltak).antallDager
        } else null,
        fritekst = søknad.fritekst,
        vedlegg = mapVedlegg(søknad.vedlegg),
    )

    private fun mapVedlegg(
        vedlegg: List<Vedlegg>,
    ): List<VedleggDTO> {
        return vedlegg.map {
            VedleggDTO(
                journalpostId = it.journalpostId,
                dokumentInfoId = it.dokumentInfoId,
                filnavn = it.filnavn,
            )
        }
    }

    private fun mapBarnetillegg(
        barnetillegg: List<Barnetillegg>,
        barnMedIdent: List<Personopplysninger.BarnMedIdent>,
    ): List<BarnetilleggDTO> {
        return barnetillegg.map {
            BarnetilleggDTO(
                navn = if (it.fornavn != null) it.fornavn + " " + it.etternavn else null,
                alder = it.alder,
                fødselsdato = if (it is Barnetillegg.UtenIdent) it.fødselsdato
                else barnMedIdent.firstOrNull { b -> b.ident == (it as Barnetillegg.MedIdent).ident }?.fødselsdato,
                bosatt = it.oppholdsland,
                kilde = "Søknad",
                utfall = UtfallDTO.Oppfylt,
                søktBarnetillegg = it.søktBarnetillegg,
            )
        }
    }

    private fun mapTiltakspenger(vilkårsvurdering: VilkårsvurderingKategori): TiltakspengerDTO {
        val perioderMedTiltakspenger =
            vilkårsvurdering.vurderinger()
                .filter { it.vilkår is Vilkår.TILTAKSPENGER }
        return TiltakspengerDTO(
            samletUtfall = vilkårsvurdering.samletUtfall().mapToUtfallDTO(),
            perioder = perioderMedTiltakspenger.map { mapVurderingToVilkårsvurderingDTO(it) },
        )
    }

    private fun mapPensjonsordninger(vilkårsvurdering: VilkårsvurderingKategori): PensjonsordningerDTO {
        val perioderMedPensjonsordning =
            vilkårsvurdering.vurderinger()
                .filter { it.vilkår is Vilkår.PENSJONSINNTEKT }
        return PensjonsordningerDTO(
            samletUtfall = vilkårsvurdering.samletUtfall().mapToUtfallDTO(),
            perioder = perioderMedPensjonsordning.map { mapVurderingToVilkårsvurderingDTO(it) },
        )
    }

    private fun mapLønnsinntekt(vilkårsvurdering: VilkårsvurderingKategori): LønnsinntekterDTO {
        val perioderMedLønnsinntekter =
            vilkårsvurdering.vurderinger()
                .filter { it.vilkår is Vilkår.LØNNSINNTEKT }
        return LønnsinntekterDTO(
            samletUtfall = vilkårsvurdering.samletUtfall().mapToUtfallDTO(),
            perioder = perioderMedLønnsinntekter.map { mapVurderingToVilkårsvurderingDTO(it) },
        )
    }

    private fun mapInstitusjonsopphold(vilkårsvurdering: VilkårsvurderingKategori): InstitusjonsoppholdDTO {
        val perioderMedInstitusjonsopphold =
            vilkårsvurdering.vurderinger()
                .filter { it.vilkår is Vilkår.INSTITUSJONSOPPHOLD }
        return InstitusjonsoppholdDTO(
            samletUtfall = vilkårsvurdering.samletUtfall().mapToUtfallDTO(),
            perioder = perioderMedInstitusjonsopphold.map { mapVurderingToVilkårsvurderingDTO(it) },
        )
    }

    private fun mapAlderVilkårsvurdering(vilkårsvurdering: VilkårsvurderingKategori): AlderVilkårsvurderingDTO {
        val perioder =
            vilkårsvurdering.vurderinger()
                .filter { it.vilkår is Vilkår.ALDER }
        return AlderVilkårsvurderingDTO(
            samletUtfall = vilkårsvurdering.samletUtfall().mapToUtfallDTO(),
            perioder = perioder.map { mapVurderingToVilkårsvurderingDTO(it) },
        )
    }

    private fun mapStatligeYtelser(v: VilkårsvurderingKategori): StatligeYtelserDTO {
        val perioderMedDagpenger =
            v.vurderinger().filter { it.vilkår is Vilkår.DAGPENGER }
        val perioderMedAAP =
            v.vurderinger().filter { it.vilkår is Vilkår.AAP }
        return StatligeYtelserDTO(
            samletUtfall = v.samletUtfall().mapToUtfallDTO(),
            aap = perioderMedAAP.map { mapVurderingToVilkårsvurderingDTO(it) },
            dagpenger = perioderMedDagpenger.map { mapVurderingToVilkårsvurderingDTO(it) },
        )
    }

    private fun mapKommunaleYtelser(v: VilkårsvurderingKategori): KommunaleYtelserDTO {
        val perioderMedKVP = v.vurderinger().filter { it.vilkår is Vilkår.KVP }
        val perioderMedIntroprogrammet =
            v.vurderinger().filter { it.vilkår is Vilkår.INTROPROGRAMMET }
        return KommunaleYtelserDTO(
            samletUtfall = v.samletUtfall().mapToUtfallDTO(),
            kvp = perioderMedKVP.map { mapVurderingToVilkårsvurderingDTO(it) },
            introProgrammet = perioderMedIntroprogrammet.map { mapVurderingToVilkårsvurderingDTO(it) },
        )
    }

    private fun mapVurderingToVilkårsvurderingDTO(vurdering: Vurdering) =
        VilkårsvurderingDTO(
            periode = vurdering.fom?.let { fom ->
                PeriodeDTO(
                    fra = fom,
                    til = vurdering.tom,
                )
            },
            kilde = vurdering.kilde,
            detaljer = vurdering.detaljer,
            kreverManuellVurdering = vurdering.utfall === Utfall.KREVER_MANUELL_VURDERING,
            utfall = vurdering.utfall.mapToUtfallDTO(),
        )

//    private fun mapBarn(innsending: Innsending) = listOf<BarnDTO>()
    /*
    søker.personopplysningerBarnMedIdent().map {
        BarnDTO(
            fornavn = it.fornavn,
            etternavn = it.etternavn,
            ident = it.ident,
            bosted = it.oppholdsland,
        )
    } + søker.personopplysningerBarnUtenIdent().map {
        BarnDTO(
            fornavn = it.fornavn!!,
            etternavn = it.etternavn!!,
            ident = null, // TODO
            bosted = null, // TODO
        )
    }
     */

    private fun vilkårsvurderinger(
        innsending: Innsending,
        vurderingsperiode: Periode,
        søknad: Søknad,
    ) = Inngangsvilkårsvurderinger(
        tiltakspengerYtelser = TiltakspengerVilkårsvurderingKategori(
            tiltakspengerVilkårsvurdering = TiltakspengerVilkårsvurdering(
                ytelser = innsending.ytelser!!.ytelserliste,
                vurderingsperiode = vurderingsperiode,
            ),
        ),
        statligeYtelser = StatligeYtelserVilkårsvurderingKategori(
            aap = AAPVilkårsvurdering(
                ytelser = innsending.ytelser!!.ytelserliste,
                vurderingsperiode = vurderingsperiode,
            ),
            dagpenger = DagpengerVilkårsvurdering(
                ytelser = innsending.ytelser!!.ytelserliste,
                vurderingsperiode = vurderingsperiode,
            ),
            foreldrepenger = ForeldrepengerVilkårsvurdering(
                ytelser = innsending.foreldrepengerVedtak!!.foreldrepengerVedtakliste,
                vurderingsperiode = vurderingsperiode,
            ),
            pleiepenger = PleiepengerVilkårsvurdering(
                ytelser = innsending.foreldrepengerVedtak!!.foreldrepengerVedtakliste,
                vurderingsperiode = vurderingsperiode,
            ),
            omsorgspenger = OmsorgspengerVilkårsvurdering(
                ytelser = innsending.foreldrepengerVedtak!!.foreldrepengerVedtakliste,
                vurderingsperiode = vurderingsperiode,
            ),
            opplæringspenger = OpplæringspengerVilkårsvurdering(
                ytelser = innsending.foreldrepengerVedtak!!.foreldrepengerVedtakliste,
                vurderingsperiode = vurderingsperiode,
            ),
            svangerskapspenger = SvangerskapspengerVilkårsvurdering(
                ytelser = innsending.foreldrepengerVedtak!!.foreldrepengerVedtakliste,
                vurderingsperiode = vurderingsperiode,
            ),
            uføretrygd = UføreVilkarsvurdering(
                uføreVedtak = innsending.uføreVedtak?.uføreVedtak,
                vurderingsperiode = vurderingsperiode,
            ),
        ),
        kommunaleYtelser = KommunaleYtelserVilkårsvurderingKategori(
            intro = IntroProgrammetVilkårsvurdering(søknad = søknad, vurderingsperiode = vurderingsperiode),
            kvp = KVPVilkårsvurdering(søknad = søknad, vurderingsperiode = vurderingsperiode),
        ),
        pensjonsordninger = PensjonsinntektVilkårsvurderingKategori(
            pensjonsinntektVilkårsvurdering = PensjonsinntektVilkårsvurdering(
                søknad = søknad,
                vurderingsperiode = vurderingsperiode,
            ),
        ),
        lønnsinntekt = LønnsinntektVilkårsvurderingKategori(
            lønnsinntektVilkårsvurdering = LønnsinntektVilkårsvurdering(
                søknad = søknad,
                vurderingsperiode = vurderingsperiode,
            ),
        ),
        institusjonopphold = InstitusjonVilkårsvurderingKategori(
            institusjonsoppholdVilkårsvurdering = InstitusjonsoppholdVilkårsvurdering(
                søknad = søknad,
                vurderingsperiode = vurderingsperiode,
                // institusjonsopphold = emptyList(),
            ),
        ),
        alder = AlderVilkårsvurderingKategori(
            alderVilkårsvurdering = AlderVilkårsvurdering(
                vurderingsperiode = vurderingsperiode,
                søkersFødselsdato = innsending.personopplysningerSøker()!!.fødselsdato,
            ),
        ),
    )

    private fun Utfall.mapToUtfallDTO(): UtfallDTO {
        return when (this) {
            Utfall.OPPFYLT -> UtfallDTO.Oppfylt
            Utfall.IKKE_OPPFYLT -> UtfallDTO.IkkeOppfylt
            Utfall.KREVER_MANUELL_VURDERING -> UtfallDTO.KreverManuellVurdering
            Utfall.IKKE_IMPLEMENTERT -> UtfallDTO.IkkeImplementert
        }
    }
}
