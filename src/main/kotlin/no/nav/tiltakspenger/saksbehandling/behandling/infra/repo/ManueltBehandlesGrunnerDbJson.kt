package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize

// TODO: Bruk ny enumtype for grunnene
data class ManueltBehandlesGrunnerDbJson(
    val manueltBehandlesGrunner: List<String>,
)

fun String.toManueltBehandlesGrunner(): List<String> =
    deserialize<ManueltBehandlesGrunnerDbJson>(this).manueltBehandlesGrunner

fun List<String>.toDbJson(): String =
    serialize(
        ManueltBehandlesGrunnerDbJson(
            manueltBehandlesGrunner = this,
        ),
    )
