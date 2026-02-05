package no.nav.tiltakspenger.saksbehandling.søknad.infra.route

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.soknad.BarnetilleggDTO
import no.nav.tiltakspenger.libs.soknad.FraOgMedDatoSpmDTO
import no.nav.tiltakspenger.libs.soknad.JaNeiSpmDTO
import no.nav.tiltakspenger.libs.soknad.PeriodeSpmDTO
import no.nav.tiltakspenger.libs.soknad.SpmSvarDTO
import no.nav.tiltakspenger.libs.soknad.SøknadDTO
import no.nav.tiltakspenger.libs.soknad.SøknadsTiltakDTO
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.BarnetilleggFraSøknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.InnvilgbarSøknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadstiltak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadstype
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import java.time.LocalDateTime

object SøknadDTOMapper {
    fun mapDigitalsøknad(
        dto: SøknadDTO,
        innhentet: LocalDateTime,
        sak: Sak,
        internTiltaksdeltakelsesId: TiltaksdeltakerId,
    ): InnvilgbarSøknad =
        InnvilgbarSøknad(
            id = SøknadId.fromString(dto.søknadId),
            versjon = dto.versjon,
            journalpostId = dto.journalpostId,
            personopplysninger =
            Søknad.Personopplysninger(
                fornavn = dto.personopplysninger.fornavn,
                etternavn = dto.personopplysninger.etternavn,
                fnr = Fnr.fromString(dto.personopplysninger.ident),
            ),
            tiltak = dto.tiltak.tilDomene(internTiltaksdeltakelsesId),
            barnetillegg =
            dto.barnetilleggPdl.map { it.tilDomenePdl() } +
                dto.barnetilleggManuelle.map { it.tilDomeneManuell() },
            opprettet = dto.opprettet,
            tidsstempelHosOss = innhentet,
            vedlegg = dto.vedlegg,
            harSøktPåTiltak = Søknad.JaNeiSpm.Ja,
            harSøktOmBarnetillegg = if (dto.barnetilleggPdl.isNotEmpty() || dto.barnetilleggManuelle.isNotEmpty()) Søknad.JaNeiSpm.Ja else Søknad.JaNeiSpm.Nei,
            kvp = dto.kvp.tilDomene(),
            intro = dto.intro.tilDomene(),
            institusjon = dto.institusjon.tilDomene(),
            etterlønn = dto.etterlønn.tilDomene(),
            gjenlevendepensjon = dto.gjenlevendepensjon.tilDomene(),
            alderspensjon = dto.alderspensjon.tilDomene(),
            sykepenger = dto.sykepenger.tilDomene(),
            supplerendeStønadAlder = dto.supplerendeStønadAlder.tilDomene(),
            supplerendeStønadFlyktning = dto.supplerendeStønadFlyktning.tilDomene(),
            jobbsjansen = dto.jobbsjansen.tilDomene(),
            trygdOgPensjon = dto.trygdOgPensjon.tilDomene(),
            sakId = sak.id,
            saksnummer = sak.saksnummer,
            avbrutt = null,
            søknadstype = Søknadstype.DIGITAL,
        )

    fun PeriodeSpmDTO.tilDomene(): Søknad.PeriodeSpm =
        when (this.svar) {
            SpmSvarDTO.Nei -> Søknad.PeriodeSpm.Nei

            SpmSvarDTO.Ja -> {
                Søknad.PeriodeSpm.Ja(
                    fraOgMed = this.fom,
                    tilOgMed = this.tom,
                )
            }
        }

    fun FraOgMedDatoSpmDTO.tilDomene(): Søknad.FraOgMedDatoSpm {
        return when (this.svar) {
            SpmSvarDTO.Nei -> Søknad.FraOgMedDatoSpm.Nei

            SpmSvarDTO.Ja -> {
                Søknad.FraOgMedDatoSpm.Ja(
                    fra = this.fom,
                )
            }
        }
    }

    fun SøknadsTiltakDTO.tilDomene(internTiltaksdeltakelsesId: TiltaksdeltakerId): Søknadstiltak =
        Søknadstiltak(
            id = this.id,
            deltakelseFom = this.deltakelseFom,
            deltakelseTom = this.deltakelseTom,
            typeKode = TiltakResponsDTO.TiltakType.valueOf(this.typeKode),
            typeNavn = this.typeNavn,
            tiltaksdeltakerId = internTiltaksdeltakelsesId,
        )

    fun BarnetilleggDTO.tilDomeneManuell(): BarnetilleggFraSøknad.Manuell {
        checkNotNull(this.fornavn) { "Fornavn kan ikke være null for barnetillegg, manuelle barn " }
        checkNotNull(this.etternavn) { "Etternavn kan ikke være null for barnetillegg, manuelle barn " }
        checkNotNull(this.fødselsdato) { "Fødselsdato kan ikke være null for barnetillegg, manuelle barn " }

        return BarnetilleggFraSøknad.Manuell(
            oppholderSegIEØS = this.oppholderSegIEØS.tilDomene(),
            fornavn = this.fornavn!!,
            mellomnavn = this.mellomnavn,
            etternavn = this.etternavn!!,
            fødselsdato = this.fødselsdato!!,
        )
    }

    fun BarnetilleggDTO.tilDomenePdl(): BarnetilleggFraSøknad.FraPdl {
        checkNotNull(this.fødselsdato) { "Fødselsdato kan ikke være null for barnetillegg fra PDL" }
        return BarnetilleggFraSøknad.FraPdl(
            oppholderSegIEØS = this.oppholderSegIEØS.tilDomene(),
            fornavn = this.fornavn,
            mellomnavn = this.mellomnavn,
            etternavn = this.etternavn,
            fødselsdato = this.fødselsdato!!,
            fnr = this.fnr?.let { Fnr.fromString(it) },
        )
    }

    fun JaNeiSpmDTO.tilDomene(): Søknad.JaNeiSpm =
        when (this.svar) {
            SpmSvarDTO.Nei -> Søknad.JaNeiSpm.Nei
            SpmSvarDTO.Ja -> Søknad.JaNeiSpm.Ja
        }
}
