package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg

import arrow.core.NonEmptyList
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.PeriodeDTO
import no.nav.tiltakspenger.libs.periode.toDTO
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.tilPeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.barnetillegg.BarnMedBarnetillegg
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.barnetillegg.BarnetilleggV1
import no.nav.tiltakspenger.saksbehandling.barnetillegg.BarnetilleggV2
import no.nav.tiltakspenger.saksbehandling.infra.route.SladdbarVerdi
import no.nav.tiltakspenger.saksbehandling.infra.route.ikkeSladdet
import java.time.LocalDate

/**
 * Utgående barnetillegg.
 * Begrunnelsen er saksbehandlers fritekst og kan sladdes, det samme kan personopplysningene om barna.
 * Barnetillegget som kommer inn fra frontenden er [OppdaterBarnetilleggDTO], som har rå verdier.
 *
 * @param perioder Antall barn per periode.
 * Finnes for begge versjoner, og er utledet fra [barn] for V2.
 * @param barn Barna det er gitt barnetillegg for.
 * Null for V1, som ikke vet hvilke barn det gjelder.
 */
data class BarnetilleggDTO(
    val versjon: BarnetilleggVersjonDTO,
    val perioder: List<BarnetilleggPeriodeDTO>,
    val begrunnelse: SladdbarVerdi<String?>,
    val barn: List<BarnMedBarnetilleggDTO>?,
)

enum class BarnetilleggVersjonDTO { V1, V2 }

/**
 * @param perioder Periodene saksbehandler har valgt for barnet, eller null for hele innvilgelsesperioden.
 * @param perioderMedBarnetillegg Dagene barnet faktisk gir barnetillegg, etter avkorting for fødsel, 16 år og innvilgelsesperiodene.
 */
data class BarnMedBarnetilleggDTO(
    val fnr: SladdbarVerdi<String?>,
    val fornavn: SladdbarVerdi<String?>,
    val mellomnavn: SladdbarVerdi<String?>,
    val etternavn: SladdbarVerdi<String?>,
    val fødselsdato: SladdbarVerdi<LocalDate>,
    val kilde: BarnKildeDTO,
    val søknadId: String?,
    val perioder: List<PeriodeDTO>?,
    val perioderMedBarnetillegg: List<PeriodeDTO>,
)

enum class BarnKildeDTO { SØKNAD, SAKSBEHANDLER }

data class BarnetilleggPeriodeDTO(
    val antallBarn: Int,
    val periode: PeriodeDTO,
)

/**
 * @param innvilgelsesperioder Behandlingens innvilgelsesperioder, som antall barn utledes for.
 * Mangler de, har [BarnetilleggV2] ingen perioder med barnetillegg ennå.
 */
fun Barnetillegg.toBarnetilleggDTO(innvilgelsesperioder: NonEmptyList<Periode>?): BarnetilleggDTO = BarnetilleggDTO(
    versjon = when (this) {
        is BarnetilleggV1 -> BarnetilleggVersjonDTO.V1
        is BarnetilleggV2 -> BarnetilleggVersjonDTO.V2
    },
    perioder = when (this) {
        is BarnetilleggV1 -> periodisering
        is BarnetilleggV2 -> innvilgelsesperioder?.let { periodisering(it) }
    }?.tilBarnetilleggPerioderDTO().orEmpty(),
    begrunnelse = begrunnelse?.verdi.ikkeSladdet(),
    barn = when (this) {
        is BarnetilleggV1 -> null
        is BarnetilleggV2 -> barn.map { it.toDTO(innvilgelsesperioder?.let { perioder -> perioderMedBarnetillegg(it, perioder) }.orEmpty()) }
    },
)

private fun BarnMedBarnetillegg.toDTO(perioderMedBarnetillegg: List<Periode>): BarnMedBarnetilleggDTO = BarnMedBarnetilleggDTO(
    fnr = fnr?.verdi.ikkeSladdet(),
    fornavn = fornavn.ikkeSladdet(),
    mellomnavn = mellomnavn.ikkeSladdet(),
    etternavn = etternavn.ikkeSladdet(),
    fødselsdato = fødselsdato.ikkeSladdet(),
    kilde = when (kilde) {
        is BarnMedBarnetillegg.Kilde.Søknad -> BarnKildeDTO.SØKNAD
        BarnMedBarnetillegg.Kilde.Saksbehandler -> BarnKildeDTO.SAKSBEHANDLER
    },
    søknadId = (kilde as? BarnMedBarnetillegg.Kilde.Søknad)?.søknadId?.toString(),
    perioder = perioder?.map { it.toDTO() },
    perioderMedBarnetillegg = perioderMedBarnetillegg.map { it.toDTO() },
)

fun List<BarnetilleggPeriodeDTO>.tilPeriodisering(): Periodisering<AntallBarn> {
    // Vi ønsker ikke fylle hull med 0 på dette tidspunktet.
    // Det gjøres av domenet siden man skal bruke innvilgelsesperiode på behandlingen dersom den er satt.
    return this.map { Pair(it.periode.toDomain(), AntallBarn(it.antallBarn)) }.tilPeriodisering()
}

fun Periodisering<AntallBarn>.tilBarnetilleggPerioderDTO(): List<BarnetilleggPeriodeDTO> {
    return this.perioderMedVerdi.map {
        BarnetilleggPeriodeDTO(
            antallBarn = it.verdi.value,
            periode = it.periode.toDTO(),
        )
    }
}
