package no.nav.tiltakspenger.saksbehandling.barnetillegg

import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.libs.periodisering.tilSammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.søknad.domene.BarnetilleggFraSøknad
import java.time.LocalDate

/**
 * Periodiserer antall barn med rett på barnetillegg i [periode], ut fra barna i en søknad.
 * Tilsvarer `periodiserBarnetilleggFraSøknad` i frontend, som brukes til å forhåndsutfylle barnetillegg i søknadsbehandlingen.
 *
 * Kun barn som oppholder seg i Norge eller et annet EØS-land, regnes med.
 * Et barn gir rett fra og med fødselsdatoen til og med dagen før det fyller 16 år, se [BarnetilleggFraSøknad.under16ForDato].
 *
 * I motsetning til frontend dekker resultatet hele [periode], der dager uten barn med rett får 0 barn.
 * Tilstøtende perioder med samme antall barn er slått sammen.
 */
fun List<BarnetilleggFraSøknad>.periodiserAntallBarn(periode: Periode): SammenhengendePeriodisering<AntallBarn> {
    val rettsperioder = this
        .filter { it.oppholderSegIEØS.erJa() }
        .mapNotNull { it.periodeMedRettPåBarnetillegg().overlappendePeriode(periode) }

    return rettsperioder.periodiserAntall(periode)
}

/**
 * Teller hvor mange av periodene i lista som dekker hver dag i [periode].
 * Dekker hele [periode], der dager uten noen perioder får 0 barn.
 * Tilstøtende perioder med samme antall barn er slått sammen.
 */
fun List<Periode>.periodiserAntall(periode: Periode): SammenhengendePeriodisering<AntallBarn> {
    val perioder = this.mapNotNull { it.overlappendePeriode(periode) }

    // Antallet kan bare endre seg på dagen en periode starter eller slutter, så det holder å telle på disse dagene.
    val endringsdager = (
        listOf(periode.fraOgMed) +
            perioder.map { it.fraOgMed } +
            perioder.map { it.tilOgMed.plusDays(1) }.filter { it <= periode.tilOgMed }
        ).distinct().sorted()

    return endringsdager
        .mapIndexed { index, fraOgMed ->
            val tilOgMed = endringsdager.getOrNull(index + 1)?.minusDays(1) ?: periode.tilOgMed
            PeriodeMedVerdi(
                verdi = AntallBarn(perioder.count { it.inneholder(fraOgMed) }),
                periode = fraOgMed til tilOgMed,
            )
        }
        .tilSammenhengendePeriodisering()
        .slåSammenTilstøtendePerioder()
}

/**
 * Om minst ett av barna i søknaden får rett på barnetillegg i løpet av [periode], altså blir født i den.
 * Som i [periodiserAntallBarn] regnes kun barn som oppholder seg i Norge eller et annet EØS-land, med.
 */
fun List<BarnetilleggFraSøknad>.harBarnSomFårRettI(periode: Periode): Boolean =
    this.any { it.oppholderSegIEØS.erJa() && periode.inneholder(it.periodeMedRettPåBarnetillegg().fraOgMed) }

/** Fra og med fødselsdatoen til og med dagen før barnet fyller 16 år. */
private fun BarnetilleggFraSøknad.periodeMedRettPåBarnetillegg(): Periode = periodeMedRettPåBarnetillegg(fødselsdato)

/** Fra og med [fødselsdato] til og med dagen før barnet fyller 16 år. */
fun periodeMedRettPåBarnetillegg(fødselsdato: LocalDate): Periode = fødselsdato til fødselsdato.plusYears(16).minusDays(1)
