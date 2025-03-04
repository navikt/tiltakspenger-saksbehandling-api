package no.nav.tiltakspenger.vedtak.repository.behandling

import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.ValgteTiltaksdeltakelser
import no.nav.tiltakspenger.vedtak.repository.felles.PeriodeDbJson
import no.nav.tiltakspenger.vedtak.repository.felles.toDbJson

private data class ValgteTiltaksdeltakelserDbJson(
    val value: List<TiltaksdeltakelsePeriodeMedVerdi>,
)

private data class TiltaksdeltakelsePeriodeMedVerdi(
    val periode: PeriodeDbJson,
    val eksternDeltagelseId: String,
)

fun String.toValgteTiltaksdeltakelser(saksopplysninger: Saksopplysninger): ValgteTiltaksdeltakelser {
    val valgteTiltaksdeltakelserDbJson = deserialize<ValgteTiltaksdeltakelserDbJson>(this)
    return ValgteTiltaksdeltakelser(
        periodisering = Periodisering(
            valgteTiltaksdeltakelserDbJson.value.map {
                PeriodeMedVerdi(
                    periode = it.periode.toDomain(),
                    verdi = saksopplysninger.getTiltaksdeltagelse(it.eksternDeltagelseId)
                        ?: throw IllegalStateException("Fant ikke tiltaksdeltakelse med id ${it.eksternDeltagelseId} fra saksopplysninger"),
                )
            },
        ),
    )
}

fun ValgteTiltaksdeltakelser.toDbJson(): String = ValgteTiltaksdeltakelserDbJson(
    value = this.periodisering.perioderMedVerdi.map {
        TiltaksdeltakelsePeriodeMedVerdi(
            periode = it.periode.toDbJson(),
            eksternDeltagelseId = it.verdi.eksternDeltagelseId,
        )
    },
).let { serialize(it) }
