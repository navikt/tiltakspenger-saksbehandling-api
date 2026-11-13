package no.nav.tiltakspenger.saksbehandling.barnetillegg

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.left
import arrow.core.right
import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.dato.norskDatoFormatter
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.leggSammen
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.periodisering.IkkeTomPeriodisering
import no.nav.tiltakspenger.libs.periodisering.tilIkkeTomPeriodisering
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse
import java.time.LocalDate

/**
 * Barnetillegg for barna saksbehandler har valgt.
 * Antall barn per dag ([periodisering]) utledes fra barna og behandlingens innvilgelsesperioder, slik at det alltid er sporbart hvilke barn det er gitt barnetillegg for.
 * Innvilgelsesperiodene lagres ikke i barnetillegget, så de kan ikke komme ut av synk med behandlingens.
 *
 * Et barn gir barnetillegg de dagene i innvilgelsesperiodene som både er innenfor barnets valgte perioder
 * (hele innvilgelsesperioden når de ikke er valgt) og barnets rett, se [perioderMedBarnetillegg].
 * Slik avkortes barnetillegget automatisk for barn som blir født eller fyller 16 år i innvilgelsesperioden.
 *
 * Valideringen ligger i [create], og ikke i init, siden barnetillegget skal kunne leses fra databasen selv om et barn da ikke lenger har rett i perioden.
 *
 * @param barn Tom liste betyr at det ikke er gitt barnetillegg.
 */
data class BarnetilleggV2(
    val barn: List<BarnMedBarnetillegg>,
    override val begrunnelse: Begrunnelse?,
) : Barnetillegg {

    /** Dager mellom [innvilgelsesperioder] er ikke innvilget, og har ikke barnetillegg. */
    override fun periodisering(innvilgelsesperioder: NonEmptyList<Periode>): IkkeTomPeriodisering<AntallBarn> {
        val sammenslåtte = innvilgelsesperioder.slåSammenInnvilgelsesperioder()
        val perioderMedBarn = barn.flatMap { perioderMedBarnetillegg(it, sammenslåtte) }
        return sammenslåtte
            .flatMap { perioderMedBarn.periodiserAntall(it).perioderMedVerdi }
            .tilIkkeTomPeriodisering()
    }

    /** Dagene i [innvilgelsesperioder] der [barn] gir barnetillegg, sortert og med tilstøtende perioder slått sammen. */
    fun perioderMedBarnetillegg(barn: BarnMedBarnetillegg, innvilgelsesperioder: NonEmptyList<Periode>): List<Periode> {
        val sammenslåtte = innvilgelsesperioder.slåSammenInnvilgelsesperioder()
        return (barn.perioder ?: sammenslåtte)
            .mapNotNull { it.overlappendePeriode(barn.periodeMedRett) }
            .flatMap { it.overlappendePerioder(sammenslåtte) }
            .leggSammen()
    }

    /**
     * Klipper barnas valgte perioder til de nye innvilgelsesperiodene [perioder], slik at resultatet fortsatt er gyldig etter [create].
     * Barn som da ikke gir barnetillegg noen dag, fjernes.
     */
    override fun krympTilPerioder(perioder: List<Periode>): BarnetilleggV2? {
        val innvilgelsesperioder = perioder.toNonEmptyListOrNull()?.slåSammenInnvilgelsesperioder() ?: return null
        val totalPeriode = innvilgelsesperioder.totalPeriode()
        return copy(
            barn = barn.mapNotNull { barn ->
                val klippet = when (barn.perioder) {
                    null -> barn

                    else -> barn.copy(
                        perioder = barn.perioder
                            .mapNotNull { it.overlappendePeriode(totalPeriode) }
                            .toNonEmptyListOrNull()
                            ?: return@mapNotNull null,
                    )
                }
                klippet.takeIf { perioderMedBarnetillegg(it, innvilgelsesperioder).isNotEmpty() }
            },
        )
    }

    companion object {
        fun utenBarnetillegg(): BarnetilleggV2 = BarnetilleggV2(barn = emptyList(), begrunnelse = null)

        /**
         * Lager barnetillegget saksbehandler har valgt for behandlingens [innvilgelsesperioder], og validerer at:
         * - barn lagt til av saksbehandler har fornavn og etternavn
         * - samme barn ikke er valgt flere ganger
         * - valgte perioder ikke overlapper hverandre, og er innenfor innvilgelsesperioden og barnets rett
         * - hvert barn gir barnetillegg minst én dag
         *
         * Barnas valgte perioder sorteres, og perioder som tilstøter hverandre slås sammen.
         */
        fun create(
            barn: List<BarnMedBarnetillegg>,
            innvilgelsesperioder: NonEmptyList<Periode>,
            begrunnelse: Begrunnelse?,
        ): Either<UgyldigBarnetillegg, BarnetilleggV2> {
            val barn = barn.map { it.copy(perioder = it.perioder?.sortedBy { periode -> periode.fraOgMed }?.toNonEmptyListOrNull()) }
            val barnetillegg = BarnetilleggV2(barn = barn, begrunnelse = begrunnelse)
            val totalPeriode = innvilgelsesperioder.totalPeriode()

            barn.forEachIndexed { index, it ->
                if (it.kilde is BarnMedBarnetillegg.Kilde.Saksbehandler && (it.fornavn.isNullOrBlank() || it.etternavn.isNullOrBlank())) {
                    return UgyldigBarnetillegg.ManglerNavn(it).left()
                }
                if (barn.take(index).any { tidligere -> tidligere.erSammeBarn(it) }) {
                    return UgyldigBarnetillegg.SammeBarnFlereGanger(it).left()
                }
                val perioder = it.perioder.orEmpty()
                if (perioder.zipWithNext().any { (a, b) -> a.overlapperMed(b) }) {
                    return UgyldigBarnetillegg.OverlappendePerioder(it).left()
                }
                if (perioder.any { periode -> !totalPeriode.inneholderHele(periode) }) {
                    return UgyldigBarnetillegg.PeriodeUtenforInnvilgelsen(it, totalPeriode).left()
                }
                if (perioder.any { periode -> !it.periodeMedRett.inneholderHele(periode) }) {
                    return UgyldigBarnetillegg.PeriodeUtenforBarnetsRett(it).left()
                }
                if (barnetillegg.perioderMedBarnetillegg(it, innvilgelsesperioder).isEmpty()) {
                    return UgyldigBarnetillegg.IngenDagerMedBarnetillegg(it).left()
                }
            }
            return barnetillegg.copy(
                barn = barn.map { it.copy(perioder = it.perioder?.leggSammen()?.toNonEmptyListOrNull()) },
            ).right()
        }
    }
}

/**
 * Sorterer [this] og slår sammen tilstøtende perioder.
 * Behandlingens innvilgelsesperioder er ofte delt opp per tiltaksdeltakelse og antall dager, men det har ingen betydning for barnetillegget.
 */
private fun NonEmptyList<Periode>.slåSammenInnvilgelsesperioder(): NonEmptyList<Periode> =
    this.leggSammen(godtaOverlapp = false).toNonEmptyListOrNull()!!

private fun NonEmptyList<Periode>.totalPeriode(): Periode = minOf { it.fraOgMed } til maxOf { it.tilOgMed }

/**
 * Grunner til at barnetillegget saksbehandler har valgt, ikke kan lagres.
 * Meldingene vises til saksbehandler, og inneholder derfor ikke fnr.
 */
sealed interface UgyldigBarnetillegg {
    val melding: String

    data class UgyldigFnr(val fødselsdato: LocalDate) : UgyldigBarnetillegg {
        override val melding = "Barnet født ${fødselsdato.format(norskDatoFormatter)} har ugyldig fødselsnummer."
    }

    /** Barn fra søknaden må ha en gyldig søknadId, og barn lagt til av saksbehandler kan ikke ha det. */
    data class UgyldigKilde(val fødselsdato: LocalDate) : UgyldigBarnetillegg {
        override val melding = "Barnet født ${fødselsdato.format(norskDatoFormatter)} har ugyldig kilde eller søknad."
    }

    data class UkjentSøknad(val barn: BarnMedBarnetillegg) : UgyldigBarnetillegg {
        override val melding = "Barnet født ${barn.fødselsdato.format(norskDatoFormatter)} er fra en søknad som ikke finnes i saken."
    }

    data class ManglerNavn(val barn: BarnMedBarnetillegg) : UgyldigBarnetillegg {
        override val melding = "Barnet født ${barn.fødselsdato.format(norskDatoFormatter)} mangler fornavn eller etternavn."
    }

    data class SammeBarnFlereGanger(val barn: BarnMedBarnetillegg) : UgyldigBarnetillegg {
        override val melding = "Barnet født ${barn.fødselsdato.format(norskDatoFormatter)} er valgt flere ganger."
    }

    data class ManglerPerioder(val barn: BarnMedBarnetillegg) : UgyldigBarnetillegg {
        override val melding = "Det må velges minst én periode for barnet født ${barn.fødselsdato.format(norskDatoFormatter)}."
    }

    data class OverlappendePerioder(val barn: BarnMedBarnetillegg) : UgyldigBarnetillegg {
        override val melding = "Periodene for barnet født ${barn.fødselsdato.format(norskDatoFormatter)} overlapper hverandre."
    }

    data class PeriodeUtenforInnvilgelsen(val barn: BarnMedBarnetillegg, val innvilgelsesperiode: Periode) : UgyldigBarnetillegg {
        override val melding = "Periodene for barnet født ${barn.fødselsdato.format(norskDatoFormatter)} må være innenfor innvilgelsesperioden " +
            "${innvilgelsesperiode.fraOgMed.format(norskDatoFormatter)} - ${innvilgelsesperiode.tilOgMed.format(norskDatoFormatter)}."
    }

    data class PeriodeUtenforBarnetsRett(val barn: BarnMedBarnetillegg) : UgyldigBarnetillegg {
        override val melding = "Periodene for barnet født ${barn.fødselsdato.format(norskDatoFormatter)} må være fra fødselsdatoen " +
            "til og med dagen før barnet fyller 16 år (${barn.periodeMedRett.tilOgMed.format(norskDatoFormatter)})."
    }

    data class IngenDagerMedBarnetillegg(val barn: BarnMedBarnetillegg) : UgyldigBarnetillegg {
        override val melding = "Barnet født ${barn.fødselsdato.format(norskDatoFormatter)} gir ikke rett til barnetillegg i innvilgelsesperioden."
    }
}

/**
 * Barn fra søknaden må være fra en av [søknadIder], altså fra en søknad i saken.
 */
fun List<BarnMedBarnetillegg>.validerSøknader(søknadIder: Set<SøknadId>): Either<UgyldigBarnetillegg, Unit> {
    this.forEach {
        val kilde = it.kilde
        if (kilde is BarnMedBarnetillegg.Kilde.Søknad && kilde.søknadId !in søknadIder) {
            return UgyldigBarnetillegg.UkjentSøknad(it).left()
        }
    }
    return Unit.right()
}

/**
 * Kastes når barnetillegget saksbehandler sender inn, er ugyldig.
 * Gir 400 til frontend.
 * [UgyldigBarnetillegg.melding] kan inneholde barnets fødselsdato, så exception-meldingen inneholder bare feiltypen og kan logges vanlig.
 */
class UgyldigBarnetilleggException(val feil: UgyldigBarnetillegg) : RuntimeException("Ugyldig barnetillegg: ${feil::class.simpleName}")
