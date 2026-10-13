package no.nav.tiltakspenger.saksbehandling.lokal.tiltaksdeltakelse.infra.repo

import kotliquery.Row
import kotliquery.Session
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.saksbehandling.lokal.tiltaksdeltakelse.LokalTiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.TiltaksdeltakelseDb
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.TiltaksdeltakelseSomIkkeGirRettDbJson
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.toDbJson
import java.time.LocalDateTime

/**
 * Tiltaksdeltakelser satt via dev-endepunktet ved lokal kjøring.
 * Tabellen finnes kun lokalt, se [no.nav.tiltakspenger.saksbehandling.lokal.infra.repo.lokalFlywayMigrate].
 * Kolonnen `gir_rett` avgjør hvilket json-format deltakelsen er lagret med.
 */
object LokalTiltaksdeltakelsePostgresRepo {
    fun lagre(
        fnr: Fnr,
        tiltaksdeltakelse: LokalTiltaksdeltakelse,
        sistEndret: LocalDateTime,
        session: Session,
    ) {
        val deltakelseJson = when (tiltaksdeltakelse) {
            is LokalTiltaksdeltakelse.GirRett -> serialize(tiltaksdeltakelse.deltakelse.toDbJson())
            is LokalTiltaksdeltakelse.GirIkkeRett -> serialize(tiltaksdeltakelse.deltakelse.toDbJson())
        }
        session.run(
            sqlQuery(
                """
                    insert into lokal.tiltaksdeltakelse (ekstern_id, fnr, deltakelse, gir_rett, sist_endret)
                    values (:ekstern_id, :fnr, :deltakelse::jsonb, :gir_rett, :sist_endret)
                    on conflict (ekstern_id) do update set
                        fnr = excluded.fnr,
                        deltakelse = excluded.deltakelse,
                        gir_rett = excluded.gir_rett,
                        sist_endret = excluded.sist_endret
                """.trimIndent(),
                "ekstern_id" to tiltaksdeltakelse.eksternDeltakelseId,
                "fnr" to fnr.verdi,
                "deltakelse" to deltakelseJson,
                "gir_rett" to (tiltaksdeltakelse is LokalTiltaksdeltakelse.GirRett),
                "sist_endret" to sistEndret,
            ).asUpdate,
        )
    }

    fun hentForFnr(fnr: Fnr, session: Session): List<LokalTiltaksdeltakelse> =
        session.run(
            sqlQuery(
                "select deltakelse, gir_rett from lokal.tiltaksdeltakelse where fnr = :fnr",
                "fnr" to fnr.verdi,
            ).map { it.toLokalTiltaksdeltakelse() }.asList,
        )

    fun hentFnr(eksternId: String, session: Session): Fnr? =
        session.run(
            sqlQuery(
                "select fnr from lokal.tiltaksdeltakelse where ekstern_id = :ekstern_id",
                "ekstern_id" to eksternId,
            ).map { Fnr.fromString(it.string("fnr")) }.asSingle,
        )

    private fun Row.toLokalTiltaksdeltakelse(): LokalTiltaksdeltakelse {
        val json = string("deltakelse")
        return if (boolean("gir_rett")) {
            LokalTiltaksdeltakelse.GirRett(deserialize<TiltaksdeltakelseDb>(json).toDomain())
        } else {
            LokalTiltaksdeltakelse.GirIkkeRett(deserialize<TiltaksdeltakelseSomIkkeGirRettDbJson>(json).toDomain())
        }
    }
}
