package no.nav.tiltakspenger.saksbehandling.lokal.dagpenger.infra.repo

import kotliquery.Session
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.json.deserializeList
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.persistering.infra.sqlQuery
import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.DagpengeperioderDbJson
import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.toDbJson
import no.nav.tiltakspenger.saksbehandling.dagpenger.domene.Dagpengeperiode
import java.time.LocalDateTime

/**
 * Dagpengeperioder satt via dev-endepunktet ved lokal kjøring, én rad per person.
 * Tabellen finnes kun lokalt, se [no.nav.tiltakspenger.saksbehandling.lokal.infra.repo.lokalFlywayMigrate].
 */
object LokalDagpengerPostgresRepo {
    fun lagre(
        fnr: Fnr,
        perioder: List<Dagpengeperiode>,
        sistEndret: LocalDateTime,
        session: Session,
    ) {
        session.run(
            sqlQuery(
                """
                    insert into lokal.dagpengeperioder (fnr, perioder, sist_endret)
                    values (:fnr, :perioder::jsonb, :sist_endret)
                    on conflict (fnr) do update set
                        perioder = excluded.perioder,
                        sist_endret = excluded.sist_endret
                """.trimIndent(),
                "fnr" to fnr.verdi,
                "perioder" to serialize(perioder.map { it.toDbJson() }),
                "sist_endret" to sistEndret,
            ).asUpdate,
        )
    }

    fun hentForFnr(fnr: Fnr, session: Session): List<Dagpengeperiode> =
        session.run(
            sqlQuery(
                "select perioder from lokal.dagpengeperioder where fnr = :fnr",
                "fnr" to fnr.verdi,
            ).map { row ->
                deserializeList<DagpengeperioderDbJson.DagpengeperiodeDbJson>(row.string("perioder")).map { it.toDomain() }
            }.asSingle,
        ) ?: emptyList()
}
