package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import arrow.core.toNonEmptyListOrThrow
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.tilIkkeTomPeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.barnetillegg.BarnMedBarnetillegg
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.barnetillegg.BarnetilleggV1
import no.nav.tiltakspenger.saksbehandling.barnetillegg.BarnetilleggV2
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.PeriodeDbJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.toDbJson
import java.time.LocalDate

/**
 * Barnetillegget lagres som json i samme kolonne for begge versjoner, og leses med denne klassen.
 * Json uten [versjon] er [BarnetilleggV1], der [value] er antall barn per periode, se [BarnetilleggV1DbJson].
 * [BarnetilleggV2] har versjon "V2", og lagrer barna og innvilgelsesperiodene, se [BarnetilleggV2DbJson].
 * Antall barn utledes fra dem.
 */
private data class BarnetilleggDbJson(
    val versjon: String? = null,
    val value: List<BarnetilleggPeriodeMedVerdi>? = null,
    val innvilgelsesperioder: List<PeriodeDbJson>? = null,
    val barn: List<BarnDbJson>? = null,
    val begrunnelse: String?,
)

/** Skrives uten versjon, som før V2. */
private data class BarnetilleggV1DbJson(
    val value: List<BarnetilleggPeriodeMedVerdi>,
    val begrunnelse: String?,
)

private data class BarnetilleggV2DbJson(
    val innvilgelsesperioder: List<PeriodeDbJson>,
    val barn: List<BarnDbJson>,
    val begrunnelse: String?,
) {
    val versjon: String = V2
}

private data class BarnetilleggPeriodeMedVerdi(
    val periode: PeriodeDbJson,
    val verdi: Int,
)

private data class BarnDbJson(
    val fnr: String?,
    val fornavn: String?,
    val mellomnavn: String?,
    val etternavn: String?,
    val fødselsdato: LocalDate,
    val kilde: KildeDbJson,
    val søknadId: String?,
    val periode: PeriodeDbJson?,
) {
    enum class KildeDbJson { SØKNAD, SAKSBEHANDLER }

    fun toDomain(): BarnMedBarnetillegg = BarnMedBarnetillegg(
        fnr = fnr?.let { Fnr.fromString(it) },
        fornavn = fornavn,
        mellomnavn = mellomnavn,
        etternavn = etternavn,
        fødselsdato = fødselsdato,
        kilde = when (kilde) {
            KildeDbJson.SØKNAD -> BarnMedBarnetillegg.Kilde.Søknad(SøknadId.fromString(søknadId!!))
            KildeDbJson.SAKSBEHANDLER -> BarnMedBarnetillegg.Kilde.Saksbehandler
        },
        periode = periode?.toDomain(),
    )
}

private const val V2 = "V2"

fun String.toBarnetillegg(): Barnetillegg {
    val json = deserialize<BarnetilleggDbJson>(this)
    val begrunnelse = json.begrunnelse?.let { Begrunnelse.create(it) }
    return when (json.versjon) {
        null -> BarnetilleggV1(
            periodisering = json.value!!.map {
                PeriodeMedVerdi(
                    periode = it.periode.toDomain(),
                    verdi = AntallBarn(it.verdi),
                )
            }.tilIkkeTomPeriodisering(),
            begrunnelse = begrunnelse,
        )

        V2 -> BarnetilleggV2(
            barn = json.barn!!.map { it.toDomain() },
            innvilgelsesperioder = json.innvilgelsesperioder!!.map { it.toDomain() }.toNonEmptyListOrThrow(),
            begrunnelse = begrunnelse,
        )

        else -> throw IllegalStateException("Ukjent versjon av barnetillegg: ${json.versjon}")
    }
}

fun Barnetillegg.toDbJson(): String = when (this) {
    is BarnetilleggV1 -> BarnetilleggV1DbJson(
        value = this.periodisering.perioderMedVerdi.toList().map {
            BarnetilleggPeriodeMedVerdi(
                periode = it.periode.toDbJson(),
                verdi = it.verdi.value,
            )
        },
        begrunnelse = this.begrunnelse?.verdi,
    )

    is BarnetilleggV2 -> BarnetilleggV2DbJson(
        innvilgelsesperioder = this.innvilgelsesperioder.map { it.toDbJson() },
        barn = this.barn.map { it.toDbJson() },
        begrunnelse = this.begrunnelse?.verdi,
    )
}.let { serialize(it) }

private fun BarnMedBarnetillegg.toDbJson(): BarnDbJson = BarnDbJson(
    fnr = fnr?.verdi,
    fornavn = fornavn,
    mellomnavn = mellomnavn,
    etternavn = etternavn,
    fødselsdato = fødselsdato,
    kilde = when (kilde) {
        is BarnMedBarnetillegg.Kilde.Søknad -> BarnDbJson.KildeDbJson.SØKNAD
        BarnMedBarnetillegg.Kilde.Saksbehandler -> BarnDbJson.KildeDbJson.SAKSBEHANDLER
    },
    søknadId = (kilde as? BarnMedBarnetillegg.Kilde.Søknad)?.let { it.søknadId.toString() },
    periode = periode?.toDbJson(),
)
