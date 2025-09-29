@file:Suppress("LongParameterList", "UnusedPrivateMember")

package no.nav.tiltakspenger.saksbehandling.søknad.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.felles.Avbrutt
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import java.time.LocalDate
import java.time.LocalDateTime

data class InnvilgbarSøknad(
    override val versjon: String = "1",
    override val id: SøknadId,
    override val journalpostId: String,
    override val personopplysninger: Søknad.Personopplysninger,
    override val tiltak: Søknadstiltak,
    override val barnetillegg: List<BarnetilleggFraSøknad>,
    override val opprettet: LocalDateTime,
    override val tidsstempelHosOss: LocalDateTime,
    override val sakId: SakId,
    override val saksnummer: Saksnummer,
    override val avbrutt: Avbrutt?,
    override val kvp: Søknad.PeriodeSpm,
    override val intro: Søknad.PeriodeSpm,
    override val institusjon: Søknad.PeriodeSpm,
    override val etterlønn: Søknad.JaNeiSpm,
    override val gjenlevendepensjon: Søknad.PeriodeSpm,
    override val alderspensjon: Søknad.FraOgMedDatoSpm,
    override val sykepenger: Søknad.PeriodeSpm,
    override val supplerendeStønadAlder: Søknad.PeriodeSpm,
    override val supplerendeStønadFlyktning: Søknad.PeriodeSpm,
    override val jobbsjansen: Søknad.PeriodeSpm,
    override val trygdOgPensjon: Søknad.PeriodeSpm,
    override val vedlegg: Int,
) : Søknad {
    val kravdato: LocalDate = tidsstempelHosOss.toLocalDate()
    override val fnr: Fnr = personopplysninger.fnr
    override val erAvbrutt: Boolean by lazy { avbrutt != null }

    /**
     * Merk at dette er sånn tiltaksdeltagelsen så ut i søknadsøyeblikket og kan ha endret seg i etterkant.
     * Man kan bare søke om tiltakspenger for en tiltaksdeltagelse per søknad (aug 2025).
     */
    override fun tiltaksdeltagelseperiodeDetErSøktOm(): Periode = Periode(tiltak.deltakelseFom, tiltak.deltakelseTom)

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

    fun harKvp(): Boolean =
        kvp.erJa()

    fun harIntro(): Boolean =
        intro.erJa()

    fun harInstitusjonsopphold(): Boolean =
        institusjon.erJa()

    fun harLagtTilBarnManuelt(): Boolean =
        barnetillegg.any { it is BarnetilleggFraSøknad.Manuell }

    fun harBarnUtenforEOS(): Boolean =
        barnetillegg.any { it.oppholderSegIEØS == Søknad.JaNeiSpm.Nei }

    fun harBarnSomFyller16FørDato(dato: LocalDate): Boolean =
        barnetillegg.any { !it.under16ForDato(dato) }

    fun harBarnSomBleFødtEtterDato(dato: LocalDate): Boolean =
        barnetillegg.any { it.fødselsdato.isAfter(dato) }

    fun harSoktMerEnn3ManederEtterOppstart(): Boolean =
        kravdato.withDayOfMonth(1).minusMonths(3).isAfter(tiltak.deltakelseFom)

    fun erUnder18ISoknadsperioden(fodselsdato: LocalDate): Boolean =
        fodselsdato.plusYears(18).isAfter(tiltaksdeltagelseperiodeDetErSøktOm().fraOgMed)
}
