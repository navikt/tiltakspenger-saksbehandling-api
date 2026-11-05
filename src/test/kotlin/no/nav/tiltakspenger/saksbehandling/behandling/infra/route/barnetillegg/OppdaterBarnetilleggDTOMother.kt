package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg

import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.toDTO
import no.nav.tiltakspenger.libs.periodisering.IkkeTomPeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.barnetillegg.BarnMedBarnetillegg
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.barnetillegg.BarnetilleggV1
import no.nav.tiltakspenger.saksbehandling.barnetillegg.BarnetilleggV2

/**
 * Barnetillegget slik frontenden ville sendt det inn.
 * Et [BarnetilleggV1] gjøres om til syntetiske barn som gir samme antall barn per dag, se [tilSyntetiskeBarn].
 */
fun Barnetillegg.tilOppdaterBarnetilleggDTO(): OppdaterBarnetilleggDTO = OppdaterBarnetilleggDTO(
    barn = when (this) {
        is BarnetilleggV2 -> barn
        is BarnetilleggV1 -> periodisering.tilSyntetiskeBarn()
    }.map { it.tilOppdaterDTO() },
    begrunnelse = begrunnelse?.verdi,
)

fun BarnMedBarnetillegg.tilOppdaterDTO(): OppdaterBarnMedBarnetilleggDTO {
    val kilde = kilde
    return OppdaterBarnMedBarnetilleggDTO(
        fnr = fnr?.verdi,
        fornavn = fornavn,
        mellomnavn = mellomnavn,
        etternavn = etternavn,
        fødselsdato = fødselsdato,
        kilde = when (kilde) {
            is BarnMedBarnetillegg.Kilde.Søknad -> BarnKildeDTO.SØKNAD
            BarnMedBarnetillegg.Kilde.Saksbehandler -> BarnKildeDTO.SAKSBEHANDLER
        },
        søknadId = (kilde as? BarnMedBarnetillegg.Kilde.Søknad)?.søknadId?.toString(),
        periode = periode?.toDTO(),
    )
}

/**
 * Lager ett barn, lagt til av saksbehandler, for hvert sammenhengende intervall med minst n barn, for n = 1 til høyeste antall.
 * Da blir antall barn per dag det samme som i periodiseringen, også når antallet går opp og ned.
 */
fun IkkeTomPeriodisering<AntallBarn>.tilSyntetiskeBarn(): List<BarnMedBarnetillegg> {
    val perioder = perioderMedVerdi.toList()
    val høyesteAntall = perioder.maxOf { it.verdi.value }
    return (1..høyesteAntall).flatMap { nivå ->
        val intervaller = mutableListOf<Periode>()
        perioder.filter { it.verdi.value >= nivå }.forEach {
            val forrige = intervaller.lastOrNull()
            if (forrige != null && forrige.tilOgMed.plusDays(1) == it.periode.fraOgMed) {
                intervaller[intervaller.lastIndex] = Periode(forrige.fraOgMed, it.periode.tilOgMed)
            } else {
                intervaller.add(it.periode)
            }
        }
        intervaller.mapIndexed { index, periode ->
            BarnMedBarnetillegg(
                fnr = null,
                fornavn = "Barn",
                mellomnavn = null,
                etternavn = "Nummer$nivå-$index",
                fødselsdato = periode.fraOgMed.minusYears(1),
                kilde = BarnMedBarnetillegg.Kilde.Saksbehandler,
                periode = periode,
            )
        }
    }
}
