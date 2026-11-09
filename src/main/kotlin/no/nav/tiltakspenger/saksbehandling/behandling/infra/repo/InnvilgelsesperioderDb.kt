package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.InnvilgelsesperiodeVerdi
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.behandling.domene.reversertOpphør.ReversertOpphør
import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.InnvilgelsesperioderDbJson.InnvilgelsesperiodeDbJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.PeriodeDbJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.toDbJson
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.TiltaksdeltakelseDb
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.toDbJson

private data class InnvilgelsesperioderDbJson(
    val value: List<InnvilgelsesperiodeDbJson>,
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class InnvilgelsesperiodeDbJson(
        val periode: PeriodeDbJson,
        val valgtTiltaksdeltakelse: TiltaksdeltakelseDb,
        val antallDagerPerMeldeperiode: Int,
        val reversertOpphør: ReversertOpphørDbJson? = null,
    )

    data class ReversertOpphørDbJson(
        val opprinneligVedtakId: String,
    )

    fun tilDomene(): Innvilgelsesperioder {
        return Innvilgelsesperioder(
            value.map {
                InnvilgelsesperiodeVerdi(
                    valgtTiltaksdeltakelse = it.valgtTiltaksdeltakelse.toDomain(),
                    antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(it.antallDagerPerMeldeperiode),
                    reversertOpphør = it.reversertOpphør?.let { reversertOpphør ->
                        ReversertOpphør(opprinneligVedtakId = VedtakId.fromString(reversertOpphør.opprinneligVedtakId))
                    },
                ) to it.periode.toDomain()
            },
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
                reversertOpphør = it.verdi.reversertOpphør?.let { reversertOpphør ->
                    InnvilgelsesperioderDbJson.ReversertOpphørDbJson(
                        opprinneligVedtakId = reversertOpphør.opprinneligVedtakId.toString(),
                    )
                },
            )
        },
    ).let { serialize(it) }
}
