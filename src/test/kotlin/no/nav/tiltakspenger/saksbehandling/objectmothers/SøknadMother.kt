package no.nav.tiltakspenger.saksbehandling.objectmothers

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.juni
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO
import no.nav.tiltakspenger.saksbehandling.common.januarDateTime
import no.nav.tiltakspenger.saksbehandling.felles.Avbrutt
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.søknad.domene.BarnetilleggFraSøknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Behandlingsarsak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.IkkeInnvilgbarSøknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.InnvilgbarSøknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadstiltak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadstype
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

interface SøknadMother {
    fun søknadstiltak(
        id: String = UUID.randomUUID().toString(),
        deltakelseFom: LocalDate = 1.januar(2022),
        deltakelseTom: LocalDate = 31.januar(2022),
        typeKode: TiltakResponsDTO.TiltakType = TiltakResponsDTO.TiltakType.GRUPPEAMO,
        typeNavn: String = "Gruppe AMO",
        tiltaksdeltakerId: TiltaksdeltakerId = TiltaksdeltakerId.random(),
    ): Søknadstiltak =
        Søknadstiltak(
            id = id,
            deltakelseFom = deltakelseFom,
            deltakelseTom = deltakelseTom,
            typeKode = typeKode,
            typeNavn = typeNavn,
            tiltaksdeltakerId = tiltaksdeltakerId,
        )

    fun barnetilleggMedIdent(
        oppholderSegIEØS: Søknad.JaNeiSpm = Søknad.JaNeiSpm.Ja,
        fornavn: String = "Fornavn Barn",
        mellomnavn: String? = "Mellomnavn Barn",
        etternavn: String = "Etternavn Barn",
        fødselsdato: LocalDate = 14.juni(2012),
        søktBarnetillegg: Boolean = true,
        fnr: Fnr? = null,
    ): BarnetilleggFraSøknad =
        BarnetilleggFraSøknad.FraPdl(
            oppholderSegIEØS = oppholderSegIEØS,
            fornavn = fornavn,
            mellomnavn = mellomnavn,
            etternavn = etternavn,
            fødselsdato = fødselsdato,
            fnr = fnr,
        )

    fun barnetilleggUtenIdent(
        oppholderSegIEØS: Søknad.JaNeiSpm = Søknad.JaNeiSpm.Ja,
        fornavn: String = "Fornavn Barn",
        mellomnavn: String? = "Mellomnavn Barn",
        etternavn: String = "Etternavn Barn",
        fødselsdato: LocalDate = 14.juni(2012),
    ): BarnetilleggFraSøknad =
        BarnetilleggFraSøknad.Manuell(
            oppholderSegIEØS = oppholderSegIEØS,
            fornavn = fornavn,
            mellomnavn = mellomnavn,
            etternavn = etternavn,
            fødselsdato = fødselsdato,
        )

    fun personopplysningFødselsdato() = 1.januar(2000)

    fun nyInnvilgbarSøknad(
        clock: Clock = TikkendeKlokke(),
        periode: Periode = ObjectMother.vedtaksperiode(),
        versjon: String = "1",
        id: SøknadId = Søknad.randomId(),
        journalpostId: String = "journalpostId",
        dokumentInfoId: String = "dokumentInfoId",
        filnavn: String = "filnavn",
        fnr: Fnr = Fnr.random(),
        personopplysninger: Søknad.Personopplysninger = personSøknad(fnr = fnr),
        opprettet: LocalDateTime = 1.januarDateTime(2022),
        barnetillegg: List<BarnetilleggFraSøknad> = listOf(),
        tidsstempelHosOss: LocalDateTime = 1.januarDateTime(2022),
        søknadstiltak: Søknadstiltak = søknadstiltak(
            deltakelseFom = periode.fraOgMed,
            deltakelseTom = periode.tilOgMed,
        ),
        harSøktPåTiltak: Søknad.JaNeiSpm = Søknad.JaNeiSpm.Ja,
        harSøktOmBarnetillegg: Søknad.JaNeiSpm = if (barnetillegg.isEmpty()) Søknad.JaNeiSpm.Nei else Søknad.JaNeiSpm.Ja,
        kvp: Søknad.PeriodeSpm = periodeNei(),
        intro: Søknad.PeriodeSpm = periodeNei(),
        institusjon: Søknad.PeriodeSpm = periodeNei(),
        trygdOgPensjon: Søknad.PeriodeSpm = periodeNei(),
        etterlønn: Søknad.JaNeiSpm = nei(),
        gjenlevendepensjon: Søknad.PeriodeSpm = periodeNei(),
        alderspensjon: Søknad.FraOgMedDatoSpm = fraOgMedDatoNei(),
        sykepenger: Søknad.PeriodeSpm = periodeNei(),
        supplerendeStønadAlder: Søknad.PeriodeSpm = periodeNei(),
        supplerendeStønadFlyktning: Søknad.PeriodeSpm = periodeNei(),
        jobbsjansen: Søknad.PeriodeSpm = periodeNei(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(løpenr = "1001", clock = clock),
        vedlegg: Int = 0,
        avbrutt: Avbrutt? = null,
        søknadstype: Søknadstype = Søknadstype.DIGITAL,
        søknadsperiode: Periode? = null,
    ): InnvilgbarSøknad =
        InnvilgbarSøknad(
            versjon = versjon,
            id = id,
            journalpostId = journalpostId,
            personopplysninger = personopplysninger,
            tiltak = søknadstiltak,
            barnetillegg = barnetillegg,
            opprettet = opprettet,
            tidsstempelHosOss = tidsstempelHosOss,
            vedlegg = vedlegg,
            harSøktPåTiltak = harSøktPåTiltak,
            harSøktOmBarnetillegg = harSøktOmBarnetillegg,
            kvp = kvp,
            intro = intro,
            institusjon = institusjon,
            etterlønn = etterlønn,
            gjenlevendepensjon = gjenlevendepensjon,
            alderspensjon = alderspensjon,
            sykepenger = sykepenger,
            supplerendeStønadAlder = supplerendeStønadAlder,
            supplerendeStønadFlyktning = supplerendeStønadFlyktning,
            jobbsjansen = jobbsjansen,
            trygdOgPensjon = trygdOgPensjon,
            sakId = sakId,
            saksnummer = saksnummer,
            avbrutt = avbrutt,
            manueltSattSøknadsperiode = søknadsperiode,
            søknadstype = søknadstype,
            manueltRegistrert = false,
        )

    fun nyIkkeInnvilgbarSøknad(
        clock: Clock = TikkendeKlokke(),
        periode: Periode = ObjectMother.vedtaksperiode(),
        versjon: String = "1",
        id: SøknadId = Søknad.randomId(),
        journalpostId: String = "journalpostId",
        dokumentInfoId: String = "dokumentInfoId",
        filnavn: String = "filnavn",
        fnr: Fnr = Fnr.random(),
        personopplysninger: Søknad.Personopplysninger = personSøknad(fnr = fnr),
        opprettet: LocalDateTime = 1.januarDateTime(2022),
        barnetillegg: List<BarnetilleggFraSøknad> = listOf(),
        tidsstempelHosOss: LocalDateTime = 1.januarDateTime(2022),
        søknadstiltak: Søknadstiltak? = null,
        harSøktPåTiltak: Søknad.JaNeiSpm = if (søknadstiltak == null) Søknad.JaNeiSpm.Nei else Søknad.JaNeiSpm.Ja,
        harSøktOmBarnetillegg: Søknad.JaNeiSpm = if (barnetillegg.isEmpty()) Søknad.JaNeiSpm.Nei else Søknad.JaNeiSpm.Ja,
        kvp: Søknad.PeriodeSpm = periodeNei(),
        intro: Søknad.PeriodeSpm = periodeNei(),
        institusjon: Søknad.PeriodeSpm = periodeNei(),
        trygdOgPensjon: Søknad.PeriodeSpm = periodeNei(),
        etterlønn: Søknad.JaNeiSpm = nei(),
        gjenlevendepensjon: Søknad.PeriodeSpm = periodeNei(),
        alderspensjon: Søknad.FraOgMedDatoSpm = fraOgMedDatoNei(),
        sykepenger: Søknad.PeriodeSpm = periodeNei(),
        supplerendeStønadAlder: Søknad.PeriodeSpm = periodeNei(),
        supplerendeStønadFlyktning: Søknad.PeriodeSpm = periodeNei(),
        jobbsjansen: Søknad.PeriodeSpm = periodeNei(),
        sakId: SakId = SakId.random(),
        vedlegg: Int = 0,
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(løpenr = "1001", clock = clock),
        avbrutt: Avbrutt? = null,
        søknadsperiode: Periode? = ObjectMother.vedtaksperiode(),
        manueltSattTiltak: String? = null,
        søknadstype: Søknadstype = Søknadstype.PAPIR_SKJEMA,
        behandlingsarsak: Behandlingsarsak? = null,
    ): IkkeInnvilgbarSøknad =
        IkkeInnvilgbarSøknad(
            versjon = versjon,
            id = id,
            journalpostId = journalpostId,
            personopplysninger = personopplysninger,
            tiltak = søknadstiltak,
            barnetillegg = barnetillegg,
            opprettet = opprettet,
            tidsstempelHosOss = tidsstempelHosOss,
            vedlegg = vedlegg,
            harSøktPåTiltak = harSøktPåTiltak,
            harSøktOmBarnetillegg = harSøktOmBarnetillegg,
            kvp = kvp,
            intro = intro,
            institusjon = institusjon,
            etterlønn = etterlønn,
            gjenlevendepensjon = gjenlevendepensjon,
            alderspensjon = alderspensjon,
            sykepenger = sykepenger,
            supplerendeStønadAlder = supplerendeStønadAlder,
            supplerendeStønadFlyktning = supplerendeStønadFlyktning,
            jobbsjansen = jobbsjansen,
            trygdOgPensjon = trygdOgPensjon,
            sakId = sakId,
            saksnummer = saksnummer,
            avbrutt = avbrutt,
            manueltSattSøknadsperiode = søknadsperiode,
            manueltSattTiltak = manueltSattTiltak,
            søknadstype = søknadstype,
            behandlingsarsak = behandlingsarsak,
            manueltRegistrert = true,
        )

    fun personSøknad(
        fornavn: String = "Fornavn",
        etternavn: String = "Etternavn",
        fnr: Fnr = Fnr.random(),
    ) = Søknad.Personopplysninger(
        fornavn = fornavn,
        etternavn = etternavn,
        fnr = fnr,
    )

    fun ikkeBesvart() = Søknad.JaNeiSpm.IkkeBesvart
    fun fraOgMedDatoIkkeBesvart() = Søknad.FraOgMedDatoSpm.IkkeBesvart
    fun periodeIkkeBesvart() = Søknad.PeriodeSpm.IkkeBesvart

    fun nei() = Søknad.JaNeiSpm.Nei
    fun fraOgMedDatoNei() = Søknad.FraOgMedDatoSpm.Nei
    fun periodeNei() = Søknad.PeriodeSpm.Nei

    fun ja() = Søknad.JaNeiSpm.Ja
    fun fraOgMedDatoJa(fom: LocalDate? = 1.januar(2022)) =
        Søknad.FraOgMedDatoSpm.Ja(
            fra = fom,
        )

    fun periodeJa(
        fom: LocalDate? = 1.januar(2022),
        tom: LocalDate? = 31.januar(2022),
    ) = Søknad.PeriodeSpm.Ja(
        fraOgMed = fom,
        tilOgMed = tom,
    )
}
