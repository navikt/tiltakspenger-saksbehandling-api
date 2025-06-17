package no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo

import no.nav.tiltakspenger.libs.json.deserializeList
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDag
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDager
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldeperiode
import java.time.LocalDate

data class MeldekortDagDbJson(
    val dato: LocalDate,
    val status: MeldekortstatusDb,
)

fun MeldekortDager.tilMeldekortDagerDbJson(): String {
    return this.map {
        MeldekortDagDbJson(dato = it.dato, status = it.status.toDb())
    }.let { serialize(it) }
}

fun String.tilMeldekortDager(meldeperiode: Meldeperiode): MeldekortDager {
    val dager = deserializeList<MeldekortDagDbJson>(this).map {
        MeldekortDag(dato = it.dato, status = it.status.toMeldekortDagStatus())
    }
    return MeldekortDager(dager, meldeperiode)
}
