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

    // Antallet kan bare endre seg på dagen et barn får eller mister rett, så det holder å telle på disse dagene.
    val endringsdager = (
        listOf(periode.fraOgMed) +
            rettsperioder.map { it.fraOgMed } +
            rettsperioder.map { it.tilOgMed.plusDays(1) }.filter { it <= periode.tilOgMed }
        ).distinct().sorted()

    return endringsdager
        .mapIndexed { index, fraOgMed ->
            val tilOgMed = endringsdager.getOrNull(index + 1)?.minusDays(1) ?: periode.tilOgMed
            PeriodeMedVerdi(
                verdi = AntallBarn(rettsperioder.count { it.inneholder(fraOgMed) }),
                periode = fraOgMed til tilOgMed,
            )
        }
        .tilSammenhengendePeriodisering()
        .slåSammenTilstøtendePerioder()
}

/** Fra og med fødselsdatoen til og med dagen før barnet fyller 16 år. */
private fun BarnetilleggFraSøknad.periodeMedRettPåBarnetillegg(): Periode =
    fødselsdato til sisteDagMedRett()

private fun BarnetilleggFraSøknad.sisteDagMedRett(): LocalDate = fødselsdato.plusYears(16).minusDays(1)
