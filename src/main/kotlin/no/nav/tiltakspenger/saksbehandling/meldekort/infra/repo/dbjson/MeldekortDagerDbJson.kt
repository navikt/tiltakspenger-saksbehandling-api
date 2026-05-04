package no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.dbjson

import no.nav.tiltakspenger.libs.json.deserializeList
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDag
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.UtfyltMeldeperiode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.Meldeperiode
import java.time.LocalDate

data class MeldekortDagDbJson(
    val dato: LocalDate,
    val status: MeldekortDagStatusDb,
)

fun UtfyltMeldeperiode.tilMeldekortDagerDbJson(): String {
    return this.map {
        MeldekortDagDbJson(dato = it.dato, status = it.status.toDb())
    }.let { serialize(it) }
}

fun String.tilMeldekortDager(meldeperiode: Meldeperiode): UtfyltMeldeperiode {
    val dager = deserializeList<MeldekortDagDbJson>(this).map {
        MeldekortDag(dato = it.dato, status = it.status.toMeldekortDagStatus())
    }
    return UtfyltMeldeperiode(dager, meldeperiode)
}
