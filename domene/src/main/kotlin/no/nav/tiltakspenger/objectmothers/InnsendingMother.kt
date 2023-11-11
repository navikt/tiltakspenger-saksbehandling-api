@file:Suppress("LongParameterList")

package no.nav.tiltakspenger.objectmothers

import no.nav.tiltakspenger.felles.Periode
import no.nav.tiltakspenger.felles.SøkerId
import no.nav.tiltakspenger.felles.januar
import no.nav.tiltakspenger.felles.januarDateTime
import no.nav.tiltakspenger.objectmothers.ObjectMother.foreldrepengerVedtak
import no.nav.tiltakspenger.objectmothers.ObjectMother.nyForeldrepengerHendelse
import no.nav.tiltakspenger.objectmothers.ObjectMother.nyOvergangsstønadHendelse
import no.nav.tiltakspenger.objectmothers.ObjectMother.nyPersonopplysningHendelse
import no.nav.tiltakspenger.objectmothers.ObjectMother.nySkjermingHendelse
import no.nav.tiltakspenger.objectmothers.ObjectMother.nySøknadMedTiltak
import no.nav.tiltakspenger.objectmothers.ObjectMother.nySøknadMottattHendelse
import no.nav.tiltakspenger.objectmothers.ObjectMother.nyTiltakHendelse
import no.nav.tiltakspenger.objectmothers.ObjectMother.nyUføreHendelse
import no.nav.tiltakspenger.objectmothers.ObjectMother.nyYtelseHendelse
import no.nav.tiltakspenger.objectmothers.ObjectMother.overgangsstønadVedtak
import no.nav.tiltakspenger.objectmothers.ObjectMother.uføreVedtak
import no.nav.tiltakspenger.objectmothers.ObjectMother.ytelseSak
import no.nav.tiltakspenger.vedtak.ForeldrepengerVedtak
import no.nav.tiltakspenger.vedtak.Innsending
import no.nav.tiltakspenger.vedtak.OvergangsstønadVedtak
import no.nav.tiltakspenger.vedtak.Personopplysninger
import no.nav.tiltakspenger.vedtak.Skjerming
import no.nav.tiltakspenger.vedtak.SkjermingPerson
import no.nav.tiltakspenger.vedtak.Søker
import no.nav.tiltakspenger.vedtak.Søknad
import no.nav.tiltakspenger.vedtak.Tiltak
import no.nav.tiltakspenger.vedtak.Tiltak.DeltakerStatus
import no.nav.tiltakspenger.vedtak.UføreVedtak
import no.nav.tiltakspenger.vedtak.YtelseSak
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Random

interface InnsendingMother {
    fun innsendingRegistrert(
        journalpostId: String = Random().nextInt().toString(),
        ident: String = Random().nextInt().toString(),
    ): Innsending {
        return Innsending(
            journalpostId = journalpostId,
            ident = ident,
        )
    }

    fun nySøker(
        søkerId: SøkerId = SøkerId.random(),
        ident: String = Random().nextInt().toString(),
        personopplysninger: Personopplysninger.Søker = personopplysningKjedeligFyr(ident = ident),
    ): Søker {
        return Søker.fromDb(
            søkerId = søkerId,
            ident = ident,
            personopplysninger = personopplysninger,
        )
    }

    fun innsendingMedSøknad(
        journalpostId: String = Random().nextInt().toString(),
        ident: String = Random().nextInt().toString(),
        søknad: Søknad = nySøknadMedTiltak(
            personopplysninger = Søknad.Personopplysninger(
                ident = ident,
                fornavn = "Fornavn",
                etternavn = "Etternavn",
            ),
            journalpostId = journalpostId,
        ),
    ): Innsending {
        val innsending = innsendingRegistrert(journalpostId, ident)
        val hendelse = nySøknadMottattHendelse(
            journalpostId = journalpostId,
            søknad = søknad,
        )
        innsending.håndter(hendelse)
        return innsending
    }

    fun innsendingMedPersonopplysninger(
        journalpostId: String = Random().nextInt().toString(),
        ident: String = Random().nextInt().toString(),
        søknad: Søknad = nySøknadMedTiltak(
            personopplysninger = Søknad.Personopplysninger(
                ident = ident,
                fornavn = "Fornavn",
                etternavn = "Etternavn",
            ),
            journalpostId = journalpostId,
        ),
        personopplysninger: List<Personopplysninger> = listOf(
            personopplysningKjedeligFyr(
                ident = ident,
                strengtFortroligUtland = false,
            ),
        ),
    ): Innsending {
        val innsending = innsendingMedSøknad(
            journalpostId = journalpostId,
            ident = ident,
            søknad = søknad,
        )
        innsending.håndter(
            nyPersonopplysningHendelse(
                journalpostId = journalpostId,
                personopplysninger = personopplysninger,
            ),
        )
        return innsending
    }

    fun innsendingMedSkjerming(
        journalpostId: String = Random().nextInt().toString(),
        ident: String = Random().nextInt().toString(),
        søknad: Søknad = nySøknadMedTiltak(
            personopplysninger = Søknad.Personopplysninger(
                ident = ident,
                fornavn = "Fornavn",
                etternavn = "Etternavn",
            ),
            journalpostId = journalpostId,
        ),
        personopplysninger: List<Personopplysninger> = listOf(
            personopplysningKjedeligFyr(
                ident = ident,
                strengtFortroligUtland = false,
            ),
        ),
        skjerming: Skjerming = skjermingFalse(ident = ident),
    ): Innsending {
        val innsending = innsendingMedPersonopplysninger(
            journalpostId = journalpostId,
            ident = ident,
            søknad = søknad,
            personopplysninger = personopplysninger,
        )
        innsending.håndter(
            nySkjermingHendelse(
                journalpostId = journalpostId,
                skjerming = skjerming,
            ),
        )
        return innsending
    }

    fun innsendingMedTiltak(
        journalpostId: String = Random().nextInt().toString(),
        ident: String = Random().nextInt().toString(),
        søknad: Søknad = nySøknadMedTiltak(
            personopplysninger = Søknad.Personopplysninger(
                ident = ident,
                fornavn = "Fornavn",
                etternavn = "Etternavn",
            ),
        ),
        personopplysninger: List<Personopplysninger> = listOf(
            personopplysningKjedeligFyr(
                ident = ident,
                strengtFortroligUtland = false,
            ),
        ),
        skjerming: Skjerming = skjermingFalse(ident = ident),
//        tiltak: InnhentedeTiltak = InnhentedeTiltak(
//            tiltaksliste = listOf(tiltaksaktivitet()),
//            tidsstempelInnhentet = 1.januarDateTime(2022),
//        ),
    ): Innsending {
        val innsending = innsendingMedSkjerming(
            journalpostId = journalpostId,
            ident = ident,
            søknad = søknad,
            personopplysninger = personopplysninger,
            skjerming = skjerming,
        )
        innsending.håndter(
            nyTiltakHendelse(
                journalpostId = journalpostId,
                tiltak = emptyList(),
                tidsstempelTiltakInnhentet = LocalDateTime.now(),
            ),
        )
        return innsending
    }

    fun innsendingMedYtelse(
        periode: Periode = Periode(1.januar(2022), 31.januar(2022)),
        journalpostId: String = Random().nextInt().toString(),
        ident: String = Random().nextInt().toString(),
        søknad: Søknad = nySøknadMedTiltak(
            periode = periode,
            personopplysninger = Søknad.Personopplysninger(
                ident = ident,
                fornavn = "Fornavn",
                etternavn = "Etternavn",
            ),
        ),
        personopplysninger: List<Personopplysninger> = listOf(
            personopplysningKjedeligFyr(
                ident = ident,
                strengtFortroligUtland = false,
            ),
        ),
        skjerming: Skjerming = skjermingFalse(ident = ident),
//        tiltak: InnhentedeTiltak = InnhentedeTiltak(
//            tiltaksliste = listOf(tiltaksaktivitet()),
//            tidsstempelInnhentet = 1.januarDateTime(2022),
//        ),
        ytelseSak: List<YtelseSak> = listOf(ytelseSak()),
    ): Innsending {
        val innsending = innsendingMedTiltak(
            journalpostId = journalpostId,
            ident = ident,
            søknad = søknad,
            personopplysninger = personopplysninger,
            skjerming = skjerming,
//            tiltak = tiltak,
        )
        innsending.håndter(
            nyYtelseHendelse(
                journalpostId = journalpostId,
                ytelseSak = ytelseSak,
            ),
        )
        return innsending
    }

    fun innsendingMedForeldrepenger(
        journalpostId: String = Random().nextInt().toString(),
        ident: String = Random().nextInt().toString(),
        søknad: Søknad = nySøknadMedTiltak(
            personopplysninger = Søknad.Personopplysninger(
                ident = ident,
                fornavn = "Fornavn",
                etternavn = "Etternavn",
            ),
        ),
        personopplysninger: List<Personopplysninger> = listOf(
            personopplysningKjedeligFyr(
                ident = ident,
                strengtFortroligUtland = false,
            ),
        ),
        skjerming: Skjerming = skjermingFalse(ident = ident),
//        tiltak: InnhentedeTiltak = InnhentedeTiltak(
//            tiltaksliste = listOf(tiltaksaktivitet()),
//            tidsstempelInnhentet = 1.januarDateTime(2022),
//        ),
        ytelseSak: List<YtelseSak> = listOf(ytelseSak()),
        foreldrepengerVedtakListe: List<ForeldrepengerVedtak> = listOf(foreldrepengerVedtak()),
    ): Innsending {
        val innsending = innsendingMedYtelse(
            journalpostId = journalpostId,
            ident = ident,
            søknad = søknad,
            personopplysninger = personopplysninger,
            skjerming = skjerming,
//            tiltak = tiltak,
            ytelseSak = ytelseSak,
        )

        innsending.håndter(
            nyForeldrepengerHendelse(
                ident = ident,
                journalpostId = journalpostId,
                foreldrepengerVedtakListe = foreldrepengerVedtakListe,
            ),
        )

        return innsending
    }

    fun innsendingMedOvergangsstønad(
        journalpostId: String = Random().nextInt().toString(),
        ident: String = Random().nextInt().toString(),
        søknad: Søknad = nySøknadMedTiltak(
            personopplysninger = Søknad.Personopplysninger(
                ident = ident,
                fornavn = "Fornavn",
                etternavn = "Etternavn",
            ),
        ),
        personopplysninger: List<Personopplysninger> = listOf(
            personopplysningKjedeligFyr(
                ident = ident,
                strengtFortroligUtland = false,
            ),
        ),
        skjerming: Skjerming = skjermingFalse(ident = ident),
//        tiltak: InnhentedeTiltak = InnhentedeTiltak(
//            tiltaksliste = listOf(tiltaksaktivitet()),
//            tidsstempelInnhentet = 1.januarDateTime(2022),
//        ),
        ytelseSak: List<YtelseSak> = listOf(ytelseSak()),
        foreldrepengerVedtakListe: List<ForeldrepengerVedtak> = listOf(foreldrepengerVedtak()),
        overgangsstønader: List<OvergangsstønadVedtak> = listOf(overgangsstønadVedtak()),
    ): Innsending {
        val innsending = innsendingMedForeldrepenger(
            journalpostId = journalpostId,
            ident = ident,
            søknad = søknad,
            personopplysninger = personopplysninger,
            skjerming = skjerming,
//            tiltak = tiltak,
            ytelseSak = ytelseSak,
            foreldrepengerVedtakListe = foreldrepengerVedtakListe,
        )

        innsending.håndter(
            nyOvergangsstønadHendelse(
                ident = ident,
                journalpostId = journalpostId,
                overgansstønader = overgangsstønader,
            ),
        )

        return innsending
    }

    fun innsendingMedUføre(
        journalpostId: String = Random().nextInt().toString(),
        ident: String = Random().nextInt().toString(),
        søknad: Søknad = nySøknadMedTiltak(
            personopplysninger = Søknad.Personopplysninger(
                ident = ident,
                fornavn = "Fornavn",
                etternavn = "Etternavn",
            ),
        ),
        personopplysninger: List<Personopplysninger> = listOf(
            personopplysningKjedeligFyr(
                ident = ident,
                strengtFortroligUtland = false,
            ),
        ),
        skjerming: Skjerming = skjermingFalse(ident = ident),
//        tiltak: InnhentedeTiltak = InnhentedeTiltak(
//            tiltaksliste = listOf(tiltaksaktivitet()),
//            tidsstempelInnhentet = 1.januarDateTime(2022),
//        ),
        ytelseSak: List<YtelseSak> = listOf(ytelseSak()),
        foreldrepengerVedtakListe: List<ForeldrepengerVedtak> = listOf(foreldrepengerVedtak()),
        uføreVedtak: UføreVedtak = uføreVedtak(),
    ): Innsending {
        val innsending = innsendingMedOvergangsstønad(
            journalpostId = journalpostId,
            ident = ident,
            søknad = søknad,
            personopplysninger = personopplysninger,
            skjerming = skjerming,
//            tiltak = tiltak,
            ytelseSak = ytelseSak,
            foreldrepengerVedtakListe = foreldrepengerVedtakListe,
        )

        innsending.håndter(
            nyUføreHendelse(
                ident = ident,
                journalpostId = journalpostId,
                uføreVedtak = uføreVedtak,
            ),
        )

        return innsending
    }

    fun personopplysningKjedeligFyr(
        ident: String = Random().nextInt().toString(),
        fødselsdato: LocalDate = 1.januar(2001),
        fornavn: String = "Fornavn",
        mellomnavn: String? = null,
        etternavn: String = "Etternavn",
        fortrolig: Boolean = false,
        strengtFortrolig: Boolean = false,
        strengtFortroligUtland: Boolean = false,
        kommune: String? = null,
        bydel: String? = null,
        skjermet: Boolean? = null,
        tidsstempelHosOss: LocalDateTime = 1.januarDateTime(2022),
    ): Personopplysninger.Søker = Personopplysninger.Søker(
        ident = ident,
        fødselsdato = fødselsdato,
        fornavn = fornavn,
        mellomnavn = mellomnavn,
        etternavn = etternavn,
        fortrolig = fortrolig,
        strengtFortrolig = strengtFortrolig,
        strengtFortroligUtland = strengtFortroligUtland,
        skjermet = skjermet,
        kommune = kommune,
        bydel = bydel,
        tidsstempelHosOss = tidsstempelHosOss,
    )

    fun personopplysningMaxFyr(
        ident: String = Random().nextInt().toString(),
        fødselsdato: LocalDate = 1.januar(2001),
        fornavn: String = "Kjell",
        mellomnavn: String? = "T.",
        etternavn: String = "Ring",
        fortrolig: Boolean = false,
        strengtFortrolig: Boolean = true,
        strengtFortroligUtland: Boolean = false,
        kommune: String? = "Oslo",
        bydel: String? = "3440",
        skjermet: Boolean? = true,
        tidsstempelHosOss: LocalDateTime = 1.januarDateTime(2022),
    ): Personopplysninger.Søker = Personopplysninger.Søker(
        ident = ident,
        fødselsdato = fødselsdato,
        fornavn = fornavn,
        mellomnavn = mellomnavn,
        etternavn = etternavn,
        fortrolig = fortrolig,
        strengtFortrolig = strengtFortrolig,
        strengtFortroligUtland = strengtFortroligUtland,
        skjermet = skjermet,
        kommune = kommune,
        bydel = bydel,
        tidsstempelHosOss = tidsstempelHosOss,
    )

    fun barn(
        ident: String = Random().nextInt().toString(),
        fødselsdato: LocalDate = 1.januar(2001),
        fornavn: String = "Fornavn",
        mellomnavn: String? = null,
        etternavn: String = "Etternavn",
        fortrolig: Boolean = false,
        strengtFortrolig: Boolean = false,
        strengtFortroligUtland: Boolean = false,
        skjermet: Boolean = false,
        oppholdsland: String? = null,
        tidsstempelHosOss: LocalDateTime = 1.januarDateTime(2022),
    ): Personopplysninger.BarnMedIdent {
        return Personopplysninger.BarnMedIdent(
            ident = ident,
            fødselsdato = fødselsdato,
            fornavn = fornavn,
            mellomnavn = mellomnavn,
            etternavn = etternavn,
            fortrolig = fortrolig,
            strengtFortrolig = strengtFortrolig,
            strengtFortroligUtland = strengtFortroligUtland,
            skjermet = skjermet,
            oppholdsland = oppholdsland,
            tidsstempelHosOss = tidsstempelHosOss,
        )
    }

    fun skjermingFalse(
        ident: String = Random().nextInt().toString(),
        barn: List<SkjermingPerson> = emptyList(),
    ): Skjerming {
        return Skjerming(
            søker = SkjermingPerson(
                ident = ident,
                skjerming = false,
            ),
            barn = barn,
            innhentet = 1.januarDateTime(2022),
        )
    }

    fun skjermingTrue(
        ident: String = Random().nextInt().toString(),
        barn: List<SkjermingPerson> = emptyList(),
    ): Skjerming {
        return Skjerming(
            søker = SkjermingPerson(
                ident = ident,
                skjerming = true,
            ),
            barn = barn,
            innhentet = 1.januarDateTime(2022),
        )
    }

    fun tiltak(
        id: String = "123,",
        gjennomføring: Tiltak.Gjennomføring = Tiltak.Gjennomføring(
            id = "123",
            arrangørnavn = "arrangør",
            typeNavn = "Arbeidsmarkedsopplæring (AMO)",
            typeKode = "AMO",
            rettPåTiltakspenger = true,
            fom = 1.januar(2022),
            tom = 31.januar(2022),
        ),
        deltakelseFom: LocalDate = 1.januar(2022),
        deltakelseTom: LocalDate = 31.januar(2022),
        kilde: String = "Komet",
        deltakelseProsent: Float? = 100F,
        deltakerStatus: DeltakerStatus = DeltakerStatus("DELTAR", true),
        antallDagerPerUke: Float? = 1F,
        registrertDato: LocalDateTime = 1.januarDateTime(2022),
        innhentet: LocalDateTime = 1.januarDateTime(2022),
    ): Tiltak {
        return Tiltak(
            id = id,
            gjennomføring = gjennomføring,
            deltakelseFom = deltakelseFom,
            deltakelseTom = deltakelseTom,
            deltakelseStatus = deltakerStatus,
            deltakelseDagerUke = antallDagerPerUke,
            deltakelseProsent = deltakelseProsent,
            kilde = kilde,
            registrertDato = registrertDato,
            innhentet = innhentet,
        )
    }
}
