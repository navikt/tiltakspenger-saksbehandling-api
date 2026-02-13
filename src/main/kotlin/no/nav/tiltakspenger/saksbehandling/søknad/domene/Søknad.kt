package no.nav.tiltakspenger.saksbehandling.søknad.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.NonBlankString
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO
import no.nav.tiltakspenger.saksbehandling.felles.Avbrutt
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import java.time.LocalDate
import java.time.LocalDateTime

sealed interface Søknad {
    val versjon: String
    val id: SøknadId
    val personopplysninger: Personopplysninger
    val sakId: SakId
    val saksnummer: Saksnummer
    val journalpostId: String
    val opprettet: LocalDateTime
    val tidsstempelHosOss: LocalDateTime
    val avbrutt: Avbrutt?
    val erAvbrutt: Boolean
    val fnr: Fnr
    val harSøktPåTiltak: JaNeiSpm
    val tiltak: Søknadstiltak?
    val harSøktOmBarnetillegg: JaNeiSpm
    val barnetillegg: List<BarnetilleggFraSøknad>
    val kvp: PeriodeSpm
    val intro: PeriodeSpm
    val institusjon: PeriodeSpm
    val etterlønn: JaNeiSpm
    val gjenlevendepensjon: PeriodeSpm
    val alderspensjon: FraOgMedDatoSpm
    val sykepenger: PeriodeSpm
    val supplerendeStønadAlder: PeriodeSpm
    val supplerendeStønadFlyktning: PeriodeSpm
    val jobbsjansen: PeriodeSpm
    val trygdOgPensjon: PeriodeSpm
    val vedlegg: Int
    val søknadstype: Søknadstype
    val manueltRegistrert: Boolean

    // Blir ikke satt for digitale søknader
    val manueltSattSøknadsperiode: Periode?

    // Brukes hvis behandlingen er overført fra Arena
    val behandlingsarsak: Behandlingsarsak?

    /**
     * Fritekstfelt for at saksbehandler kan skrive inn hvilket tiltak søknaden gjelder for ved manuelt registrerte søknader,
     * hvor man ikke har kunne velge et relevant tiltak. Enten fordi man ikke fikk treff på tiltaket når søknaden
     * ble registrert eller fordi tiltaket ikke gir rett til tiltakspenger.
     */
    val manueltSattTiltak: String?

    companion object {
        fun randomId() = SøknadId.random()
        fun opprett(
            sak: Sak,
            journalpostId: String,
            opprettet: LocalDateTime,
            tidsstempelHosOss: LocalDateTime,
            personopplysninger: Personopplysninger,
            søknadstiltak: Søknadstiltak?,
            barnetillegg: List<BarnetilleggFraSøknad>,
            harSøktPåTiltak: JaNeiSpm,
            harSøktOmBarnetillegg: JaNeiSpm,
            kvp: PeriodeSpm,
            intro: PeriodeSpm,
            institusjon: PeriodeSpm,
            etterlønn: JaNeiSpm,
            gjenlevendepensjon: PeriodeSpm,
            alderspensjon: FraOgMedDatoSpm,
            sykepenger: PeriodeSpm,
            supplerendeStønadAlder: PeriodeSpm,
            supplerendeStønadFlyktning: PeriodeSpm,
            jobbsjansen: PeriodeSpm,
            trygdOgPensjon: PeriodeSpm,
            antallVedlegg: Int,
            manueltSattSøknadsperiode: Periode?,
            manueltSattTiltak: String?,
            søknadstype: Søknadstype,
            behandlingsarsak: Behandlingsarsak?,
            manueltRegistrert: Boolean,
        ): Søknad =
            if (søknadstiltak != null) {
                InnvilgbarSøknad(
                    id = SøknadId.random(),
                    journalpostId = journalpostId,
                    personopplysninger = personopplysninger,
                    tiltak = søknadstiltak,
                    barnetillegg = barnetillegg,
                    opprettet = opprettet,
                    tidsstempelHosOss = tidsstempelHosOss,
                    sakId = sak.id,
                    saksnummer = sak.saksnummer,
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
                    vedlegg = antallVedlegg,
                    manueltSattSøknadsperiode = manueltSattSøknadsperiode,
                    manueltSattTiltak = manueltSattTiltak,
                    søknadstype = søknadstype,
                    behandlingsarsak = behandlingsarsak,
                    manueltRegistrert = manueltRegistrert,
                )
            } else {
                IkkeInnvilgbarSøknad(
                    id = SøknadId.random(),
                    journalpostId = journalpostId,
                    personopplysninger = personopplysninger,
                    tiltak = søknadstiltak,
                    barnetillegg = barnetillegg,
                    opprettet = opprettet,
                    tidsstempelHosOss = tidsstempelHosOss,
                    sakId = sak.id,
                    saksnummer = sak.saksnummer,
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
                    vedlegg = antallVedlegg,
                    manueltSattSøknadsperiode = manueltSattSøknadsperiode,
                    manueltSattTiltak = manueltSattTiltak,
                    søknadstype = søknadstype,
                    behandlingsarsak = behandlingsarsak,
                    manueltRegistrert = manueltRegistrert,
                )
            }
    }

    /**
     * Merk at dette er sånn tiltaksdeltakelsen så ut i søknadsøyeblikket og kan ha endret seg i etterkant.
     * Man kan bare søke om tiltakspenger for en tiltaksdeltakelse per søknad (aug 2025).
     */
    fun tiltaksdeltakelseperiodeDetErSøktOm(): Periode?
    fun erManueltRegistrertSøknad() = manueltRegistrert

    fun erDigitalSøknad() = !manueltRegistrert
    fun kanInnvilges() =
        (erDigitalSøknad() && tiltak != null) || (erManueltRegistrertSøknad() && tiltak != null && manueltSattSøknadsperiode != null)

    fun avbryt(avbruttAv: Saksbehandler, begrunnelse: NonBlankString, tidspunkt: LocalDateTime): Søknad {
        if (this.avbrutt != null) {
            throw IllegalStateException("Søknad er allerede avbrutt")
        }

        val avbrutt = Avbrutt(
            tidspunkt = tidspunkt,
            saksbehandler = avbruttAv.navIdent,
            begrunnelse = begrunnelse,
        )

        return when (this) {
            is InnvilgbarSøknad -> this.copy(avbrutt = avbrutt)
            is IkkeInnvilgbarSøknad -> this.copy(avbrutt = avbrutt)
        }
    }

    data class Personopplysninger(
        val fnr: Fnr,
        val fornavn: String,
        val etternavn: String,
    )

    sealed interface PeriodeSpm {
        data object IkkeBesvart : PeriodeSpm
        data object Nei : PeriodeSpm

        data class Ja(
            val fraOgMed: LocalDate?,
            val tilOgMed: LocalDate?,
        ) : PeriodeSpm

        /** ignorerer perioden */
        fun erJa(): Boolean =
            when (this) {
                is Ja -> true
                is Nei, IkkeBesvart -> false
            }
    }

    sealed interface JaNeiSpm {
        data object Ja : JaNeiSpm

        data object Nei : JaNeiSpm

        data object IkkeBesvart : JaNeiSpm

        /** ignorerer perioden */
        fun erJa(): Boolean =
            when (this) {
                is Ja -> true
                is Nei, IkkeBesvart -> false
            }
    }

    sealed interface FraOgMedDatoSpm {
        data object Nei : FraOgMedDatoSpm

        data object IkkeBesvart : FraOgMedDatoSpm

        data class Ja(
            val fra: LocalDate?,
        ) : FraOgMedDatoSpm

        fun erJa(): Boolean =
            when (this) {
                is Ja -> true
                is Nei, IkkeBesvart -> false
            }
    }
}

/**
 * @param id mappes fra aktivitetId som vi mottar fra søknadsfrontenden (via søknad-api). Dette er tiltaksdeltakelseIDen og vil kun være forskjellig avhengig om den kommer fra Arena (TA1234567), Komet (UUID) eller team Tiltak (?). Kalles ekstern_id i databasen.
 * @param typeKode f.eks. JOBBK, GRUPPEAMO, INDOPPFAG, ARBTREN ([TiltakResponsDTO.TiltakType])
 * @param typeNavn f.eks. Jobbklubb, Arbeidsmarkedsopplæring (gruppe), Oppfølging, Arbeidstrening
 */
data class Søknadstiltak(
    val id: String,
    val deltakelseFom: LocalDate,
    val deltakelseTom: LocalDate,
    val typeKode: TiltakResponsDTO.TiltakType,
    val typeNavn: String,
    val tiltaksdeltakerId: TiltaksdeltakerId,
)

sealed class BarnetilleggFraSøknad {
    abstract val oppholderSegIEØS: Søknad.JaNeiSpm
    abstract val fornavn: String?
    abstract val mellomnavn: String?
    abstract val etternavn: String?
    abstract val fødselsdato: LocalDate

    abstract fun under16ForDato(dato: LocalDate): Boolean

    data class FraPdl(
        override val oppholderSegIEØS: Søknad.JaNeiSpm,
        override val fornavn: String?,
        override val mellomnavn: String?,
        override val etternavn: String?,
        override val fødselsdato: LocalDate,
        val fnr: Fnr?,
    ) : BarnetilleggFraSøknad() {
        override fun under16ForDato(dato: LocalDate): Boolean = fødselsdato.plusYears(16) > dato
    }

    data class Manuell(
        override val oppholderSegIEØS: Søknad.JaNeiSpm,
        override val fornavn: String,
        override val mellomnavn: String?,
        override val etternavn: String,
        override val fødselsdato: LocalDate,
    ) : BarnetilleggFraSøknad() {
        override fun under16ForDato(dato: LocalDate): Boolean = fødselsdato.plusYears(16) > dato
    }
}
