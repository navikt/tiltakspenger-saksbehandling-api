@file:Suppress("LongParameterList", "UnusedPrivateMember")

package no.nav.tiltakspenger.vedtak.saksbehandling.domene.behandling

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.vedtak.felles.OppgaveId
import no.nav.tiltakspenger.vedtak.saksbehandling.domene.sak.Saksnummer
import java.time.LocalDate
import java.time.LocalDateTime

data class Søknad(
    val versjon: String = "1",
    val id: SøknadId,
    val journalpostId: String,
    val personopplysninger: Personopplysninger,
    val tiltak: Søknadstiltak,
    val barnetillegg: List<BarnetilleggFraSøknad>,
    val opprettet: LocalDateTime,
    val tidsstempelHosOss: LocalDateTime,
    val kvp: PeriodeSpm,
    val intro: PeriodeSpm,
    val institusjon: PeriodeSpm,
    val etterlønn: JaNeiSpm,
    val gjenlevendepensjon: PeriodeSpm,
    val alderspensjon: FraOgMedDatoSpm,
    val sykepenger: PeriodeSpm,
    val supplerendeStønadAlder: PeriodeSpm,
    val supplerendeStønadFlyktning: PeriodeSpm,
    val jobbsjansen: PeriodeSpm,
    val trygdOgPensjon: PeriodeSpm,
    val vedlegg: Int,
    val sakId: SakId,
    val saksnummer: Saksnummer,
    val oppgaveId: OppgaveId?,
    val avbrutt: Avbrutt?,
) {
    val kravdato: LocalDate = tidsstempelHosOss.toLocalDate()
    val erAvbrutt: Boolean by lazy { avbrutt != null }
    val fnr: Fnr = personopplysninger.fnr

    companion object {
        fun randomId() = SøknadId.random()
    }

    fun avbryt(avbruttAv: Saksbehandler, begrunnelse: String, tidspunkt: LocalDateTime): Søknad {
        if (this.avbrutt != null) {
            throw IllegalStateException("Søknad er allerede avbrutt")
        }
        return this.copy(
            avbrutt = Avbrutt(
                tidspunkt = tidspunkt,
                saksbehandler = avbruttAv.navIdent,
                begrunnelse = begrunnelse,
            ),
        )
    }

    fun vurderingsperiode(): Periode = Periode(tiltak.deltakelseFom, tiltak.deltakelseTom)

    fun harLivsoppholdYtelser(): Boolean =
        sykepenger.erJa() ||
            etterlønn.erJa() ||
            trygdOgPensjon.erJa() ||
            gjenlevendepensjon.erJa() ||
            supplerendeStønadAlder.erJa() ||
            supplerendeStønadFlyktning.erJa() ||
            alderspensjon.erJa() ||
            jobbsjansen.erJa() ||
            trygdOgPensjon.erJa()

    data class Personopplysninger(
        val fnr: Fnr,
        val fornavn: String,
        val etternavn: String,
    )

    sealed interface PeriodeSpm {
        data object Nei : PeriodeSpm

        data class Ja(
            val periode: Periode,
        ) : PeriodeSpm

        /** ignorerer perioden */
        fun erJa(): Boolean =
            when (this) {
                is Ja -> true
                is Nei -> false
            }
    }

    sealed interface JaNeiSpm {
        data object Ja : JaNeiSpm

        data object Nei : JaNeiSpm

        /** ignorerer perioden */
        fun erJa(): Boolean =
            when (this) {
                is Ja -> true
                Nei -> false
            }
    }

    sealed interface FraOgMedDatoSpm {
        data object Nei : FraOgMedDatoSpm

        data class Ja(
            val fra: LocalDate,
        ) : FraOgMedDatoSpm

        fun erJa(): Boolean =
            when (this) {
                is Ja -> true
                is Nei -> false
            }
    }
}

/**
 * @param id mappes fra aktivitetId som vi mottar fra søknadsfrontenden (via søknad-api). Dette er tiltaksdeltagelseIDen og vil kun være forskjellig avhengig om den kommer fra Arena (TA1234567), Komet (UUID) eller team Tiltak (?). Kalles ekstern_id i databasen.
 * @param typeKode f.eks. JOBBK, GRUPPEAMO, INDOPPFAG, ARBTREN
 * @param typeNavn f.eks. Jobbklubb, Arbeidsmarkedsopplæring (gruppe), Oppfølging, Arbeidstrening
 */
data class Søknadstiltak(
    val id: String,
    val deltakelseFom: LocalDate,
    val deltakelseTom: LocalDate,
    val typeKode: String,
    val typeNavn: String,
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
