package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ManueltBehandlesGrunn

private data class ManueltBehandlesGrunnerDbJson(
    val manueltBehandlesGrunner: List<String>,
)

fun String.toManueltBehandlesGrunner(): List<ManueltBehandlesGrunn> {
    return deserialize<ManueltBehandlesGrunnerDbJson>(this)
        .manueltBehandlesGrunner
        .map { ManueltBehandlesGrunn.valueOf(it) }
}

fun List<ManueltBehandlesGrunn>.toDbJson(): String {
    return serialize(
        ManueltBehandlesGrunnerDbJson(
            manueltBehandlesGrunner = this.map { it.name },
        ),
    )
}
