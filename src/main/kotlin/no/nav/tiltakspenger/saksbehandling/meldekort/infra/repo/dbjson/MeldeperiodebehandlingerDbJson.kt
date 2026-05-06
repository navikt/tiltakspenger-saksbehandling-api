package no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.dbjson

import arrow.core.toNonEmptyListOrThrow
import kotliquery.Session
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.json.deserializeList
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.saksbehandling.beregning.Beregning
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDag
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.UtfyltMeldeperiode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldeperiodebehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldeperiodebehandlinger
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.Meldeperiode
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.BrukersMeldekortPostgresRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.MeldeperiodePostgresRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.dbjson.MeldeperiodebehandlingDbJson.MeldekortDagDbJson
import java.time.LocalDate

/**
 * JSON-representasjon av én [Meldeperiodebehandling] i kolonnen `meldekortbehandling.meldeperioder`.
 *
 * Domenet håndhever foreløpig at vi kun har én meldeperiodebehandling per meldekortbehandling,
 * men kolonnen lagres som en jsonb-array slik at vi kan utvide til flere uten ny migrering.
 */
private data class MeldeperiodebehandlingDbJson(
    val meldeperiodeId: String,
    val kjedeId: String,
    val brukersMeldekortId: String?,
    val dager: List<MeldekortDagDbJson>,
) {

    data class MeldekortDagDbJson(
        val dato: LocalDate,
        val status: MeldekortDagStatusDb,
    )
}

fun Meldeperiodebehandlinger.tilDbJson(): String {
    return this.meldeperioder.map { it.tilDbJson() }.let { serialize(it) }
}

fun String.tilMeldeperiodebehandlinger(
    beregning: Beregning?,
    session: Session,
): Meldeperiodebehandlinger {
    val meldeperioder = deserializeList<MeldeperiodebehandlingDbJson>(this).map { it.tilDomene(session) }
    return Meldeperiodebehandlinger(
        meldeperioder = meldeperioder.toNonEmptyListOrThrow(),
        beregning = beregning,
    )
}

private fun Meldeperiodebehandling.tilDbJson(): MeldeperiodebehandlingDbJson {
    return MeldeperiodebehandlingDbJson(
        meldeperiodeId = meldeperiode.id.toString(),
        kjedeId = meldeperiode.kjedeId.toString(),
        brukersMeldekortId = brukersMeldekort?.id?.toString(),
        dager = dager.map { MeldekortDagDbJson(dato = it.dato, status = it.status.toDb()) },
    )
}

private fun MeldeperiodebehandlingDbJson.tilDomene(session: Session): Meldeperiodebehandling {
    val meldeperiode = MeldeperiodePostgresRepo.hentForMeldeperiodeId(
        MeldeperiodeId.fromString(this.meldeperiodeId),
        session,
    )!!
    val brukersMeldekort = this.brukersMeldekortId?.let {
        BrukersMeldekortPostgresRepo.hentForMeldekortId(MeldekortId.fromString(it), session)
    }
    val dager = serialize(this.dager).tilMeldekortDager(meldeperiode)
    return Meldeperiodebehandling(
        dager = dager,
        brukersMeldekort = brukersMeldekort,
    )
}

private fun String.tilMeldekortDager(meldeperiode: Meldeperiode): UtfyltMeldeperiode {
    val dager = deserializeList<MeldekortDagDbJson>(this).map {
        MeldekortDag(dato = it.dato, status = it.status.toMeldekortDagStatus())
    }
    return UtfyltMeldeperiode(dager, meldeperiode)
}
