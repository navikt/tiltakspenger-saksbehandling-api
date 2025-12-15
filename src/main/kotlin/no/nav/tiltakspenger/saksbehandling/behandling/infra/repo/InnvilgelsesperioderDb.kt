package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periodisering.tilIkkeTomPeriodisering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Innvilgelsesperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.InnvilgelsesperioderDbJson.InnvilgelsesperiodeDbJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.PeriodeDbJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.toDbJson
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.TiltaksdeltakelseDb
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.toDbJson

private data class InnvilgelsesperioderDbJson(
    val value: List<InnvilgelsesperiodeDbJson>,
) {

    data class InnvilgelsesperiodeDbJson(
        val periode: PeriodeDbJson,
        val valgtTiltaksdeltakelse: TiltaksdeltakelseDb,
        val antallDagerPerMeldeperiode: Int,
    )

    fun tilDomene(): Innvilgelsesperioder {
        return Innvilgelsesperioder(
            value.map {
                Innvilgelsesperiode(
                    periode = it.periode.toDomain(),
                    valgtTiltaksdeltakelse = it.valgtTiltaksdeltakelse.toDomain(),
                    antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(it.antallDagerPerMeldeperiode),
                ).tilPeriodeMedVerdi()
            }.tilIkkeTomPeriodisering(),
        )
    }
}

fun String.tilInnvilgelsesperioder(): Innvilgelsesperioder {
    return deserialize<InnvilgelsesperioderDbJson>(this).tilDomene()
}

fun Innvilgelsesperioder.tilInnvilgelsesperioderDbJson(): String {
    return InnvilgelsesperioderDbJson(
        this.periodisering.perioderMedVerdi.map {
            InnvilgelsesperiodeDbJson(
                periode = it.periode.toDbJson(),
                valgtTiltaksdeltakelse = it.verdi.valgtTiltaksdeltakelse.toDbJson(),
                antallDagerPerMeldeperiode = it.verdi.antallDagerPerMeldeperiode.value,
            )
        },
    ).let { serialize(it) }
}
