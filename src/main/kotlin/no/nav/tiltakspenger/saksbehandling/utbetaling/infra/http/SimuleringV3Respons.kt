package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simuleringsfeil
import tools.jackson.databind.JsonNode
import java.time.Clock
import java.time.LocalDateTime

fun lesHelvedSimuleringsresponsV3(
    responseBody: JsonNode,
    originalResponseBody: String,
    sakId: SakId,
    saksnummer: Saksnummer,
    meldeperiodeKjeder: MeldeperiodeKjeder,
    clock: Clock,
    simuleringstidspunkt: LocalDateTime,
): Either<Simuleringsfeil, Simulering> {
    val type = responseBody.get("@type")
        ?.takeIf { it.isString }
        ?.asString()
        ?: error("Simuleringssvaret mangler en tekstlig @type")

    return when (type) {
        "v1" -> deserialize<SimuleringResponseDTO>(originalResponseBody)
            .tilSimuleringFraHelvedV3Response(
                meldeperiodeKjeder = meldeperiodeKjeder,
                clock = clock,
                simuleringstidspunkt = simuleringstidspunkt,
                sakId = sakId,
                saksnummer = saksnummer,
            )

        "info" -> deserialize<HelvedInfoResponsDTO>(originalResponseBody)
            .tilSimulering(simuleringstidspunkt)

        else -> error("Ukjent @type i simuleringssvaret: $type")
    }
}

private fun SimuleringResponseDTO.tilSimuleringFraHelvedV3Response(
    meldeperiodeKjeder: MeldeperiodeKjeder,
    clock: Clock,
    simuleringstidspunkt: LocalDateTime,
    sakId: SakId,
    saksnummer: Saksnummer,
): Either<Simuleringsfeil, Simulering> {
    if (Fnr.fromString(detaljer.gjelderId) != meldeperiodeKjeder.fnr) {
        return Simuleringsfeil.GjelderAnnenPerson(
            sakId = sakId,
            saksnummer = saksnummer,
        ).left()
    }
    if (detaljer.perioder.isEmpty()) {
        return Simulering.IngenEndring(simuleringstidspunkt).right()
    }

    return normaliserV3Fagområder()
        .toSimuleringFraHelvedResponse(meldeperiodeKjeder, clock)
}

private fun SimuleringResponseDTO.normaliserV3Fagområder(): SimuleringResponseDTO =
    SimuleringResponseDTO(
        oppsummeringer = oppsummeringer,
        detaljer = SimuleringResponseDTO.SimuleringDetaljer(
            gjelderId = detaljer.gjelderId,
            datoBeregnet = detaljer.datoBeregnet,
            totalBeløp = detaljer.totalBeløp,
            perioder = detaljer.perioder.map { periode ->
                SimuleringResponseDTO.SimuleringDetaljer.PosteringerForPeriode(
                    fom = periode.fom,
                    tom = periode.tom,
                    posteringer = periode.posteringer.map { postering ->
                        SimuleringResponseDTO.SimuleringDetaljer.PosteringerForPeriode.Postering(
                            fagområde = postering.fagområde.tilSimuleringFagområde(),
                            sakId = postering.sakId,
                            fom = postering.fom,
                            tom = postering.tom,
                            beløp = postering.beløp,
                            type = postering.type,
                            klassekode = postering.klassekode,
                        )
                    },
                )
            },
        ),
    )

private fun String.tilSimuleringFagområde(): String = when {
    this in tiltakspengerFagområder -> "TILTAKSPENGER"
    this in andreFagområder -> this
    else -> error("Ukjent fagområde i simuleringssvaret: $this")
}

private val tiltakspengerFagområder = setOf("TILTPENG", "TPARENA", "MTPARENA")

private val andreFagområder = setOf(
    "TILLSTPB",
    "TILLSTLM",
    "TILLSTBO",
    "TILLSTDR",
    "TILLSTRS",
    "TILLSTRO",
    "TILLSTRA",
    "TILLSTFL",
    "TILLST",
    "TSTARENA",
    "MTSTAREN",
    "DP",
    "MDP",
    "DPARENA",
    "MDPARENA",
    "AAP",
    "HELSREF",
)

private data class HelvedInfoResponsDTO(
    val status: String,
    val fagsystem: String,
    val message: String,
) {
    fun tilSimulering(simuleringstidspunkt: LocalDateTime): Either<Simuleringsfeil, Simulering> {
        if (fagsystem != "TILTAKSPENGER") {
            error("Simuleringssvaret gjelder et uventet fagsystem: $fagsystem")
        }

        return when (status) {
            "OK_UTEN_ENDRING" -> Simulering.IngenEndring(simuleringstidspunkt).right()
            "FEILET", "UTILGJENGELIG", "UGYLDIG_REQUEST" -> error("Hel Ved meldte simuleringsstatus $status")
            else -> error("Ukjent simuleringsstatus fra Hel Ved: $status")
        }
    }
}
