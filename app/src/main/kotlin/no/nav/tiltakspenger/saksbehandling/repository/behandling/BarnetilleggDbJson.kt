package no.nav.tiltakspenger.saksbehandling.repository.behandling

import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.repository.felles.PeriodeDbJson
import no.nav.tiltakspenger.saksbehandling.repository.felles.toDbJson
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.BegrunnelseVilkårsvurdering

private data class BarnetilleggDbJson(
    val value: List<BarnetilleggPeriodeMedVerdi>,
    val begrunnelse: String?,
)

private data class BarnetilleggPeriodeMedVerdi(
    val periode: PeriodeDbJson,
    val verdi: Int,
)

fun String.toBarnetillegg(): Barnetillegg {
    val barnetilleggDbJson = deserialize<BarnetilleggDbJson>(this)
    return Barnetillegg(
        periodisering = Periodisering(
            barnetilleggDbJson.value.map {
                PeriodeMedVerdi(
                    periode = it.periode.toDomain(),
                    verdi = AntallBarn(it.verdi),
                )
            },
        ),
        begrunnelse = barnetilleggDbJson.begrunnelse?.let { BegrunnelseVilkårsvurdering(it) },
    )
}

fun Barnetillegg.toDbJson(): String = BarnetilleggDbJson(
    value = this.periodisering.perioderMedVerdi.map {
        BarnetilleggPeriodeMedVerdi(
            periode = it.periode.toDbJson(),
            verdi = it.verdi.value,
        )
    },
    begrunnelse = this.begrunnelse?.verdi,
).let { serialize(it) }
