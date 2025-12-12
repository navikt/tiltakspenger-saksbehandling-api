package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periodisering.tilIkkeTomPeriodisering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Innvilgelsesperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.InnvilgelsesperioderDbJson.InnvilgelsesperiodeDbJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.PeriodeDbJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.toDbJson

private data class InnvilgelsesperioderDbJson(
    val value: List<InnvilgelsesperiodeDbJson>,
) {

    data class InnvilgelsesperiodeDbJson(
        val periode: PeriodeDbJson,
        // eksternDeltagelseId fra tiltaksdeltakelsen. Skal byttes ut med en intern id på sikt.
        val tiltaksdeltakelseId: String,
        val antallDagerPerMeldeperiode: Int,
    )

    fun tilDomene(saksopplysninger: Saksopplysninger): Innvilgelsesperioder {
        return Innvilgelsesperioder(
            value.map {
                val tiltaksdeltakelse = saksopplysninger.getTiltaksdeltakelse(it.tiltaksdeltakelseId)

                requireNotNull(tiltaksdeltakelse) {
                    "Fant ikke tiltaksdeltakelse med id ${it.tiltaksdeltakelseId} i saksopplysningene"
                }

                Innvilgelsesperiode(
                    periode = it.periode.toDomain(),
                    valgtTiltaksdeltakelse = tiltaksdeltakelse,
                    antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(it.antallDagerPerMeldeperiode),
                ).tilPeriodeMedVerdi()
            }.tilIkkeTomPeriodisering(),
        )
    }
}

fun String.tilInnvilgelsesperioder(saksopplysninger: Saksopplysninger): Innvilgelsesperioder {
    return deserialize<InnvilgelsesperioderDbJson>(this).tilDomene(saksopplysninger)
}

fun Innvilgelsesperioder.tilInnvilgelsesperioderDbJson(): String {
    return InnvilgelsesperioderDbJson(
        this.periodisering.perioderMedVerdi.map {
            InnvilgelsesperiodeDbJson(
                periode = it.periode.toDbJson(),
                tiltaksdeltakelseId = it.verdi.valgtTiltaksdeltakelse.eksternDeltakelseId,
                antallDagerPerMeldeperiode = it.verdi.antallDagerPerMeldeperiode.value,
            )
        },
    ).let { serialize(it) }
}
