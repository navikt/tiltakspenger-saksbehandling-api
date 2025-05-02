package no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo

import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.PeriodeDbJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.toDbJson

data class RammevedtakMeldeperiodeDbJson(
    val perioderTilVedtakId: List<PeriodeTilVedtakId>,
)

data class PeriodeTilVedtakId(
    val periode: PeriodeDbJson,
    val vedtakId: String?,
)

fun String.toPeriodiserteVedtakId(): Periodisering<VedtakId?> {
    val rammevedtakMeldeperiodeDbJson = deserialize<RammevedtakMeldeperiodeDbJson>(this)
    return Periodisering(
        rammevedtakMeldeperiodeDbJson.perioderTilVedtakId.map {
            PeriodeMedVerdi(
                periode = it.periode.toDomain(),
                verdi = it.vedtakId?.let { v -> VedtakId.fromString(v) },
            )
        },
    )
}

fun Periodisering<VedtakId?>.toDbJson() = RammevedtakMeldeperiodeDbJson(
    perioderTilVedtakId = this.perioderMedVerdi.map {
        PeriodeTilVedtakId(
            periode = it.periode.toDbJson(),
            vedtakId = it.verdi?.toString(),
        )
    },
).let { serialize(it) }
