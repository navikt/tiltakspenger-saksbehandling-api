@file:Suppress("unused", "ktlint")

package db.migration

import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.Session
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.periodisering.overlappendePerioder
import no.nav.tiltakspenger.libs.periodisering.toTidslinje
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresTransactionContext.Companion.withSession
import no.nav.tiltakspenger.libs.persistering.infrastruktur.SessionCounter
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjortAvRammevedtak
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjørRammevedtak
import no.nav.tiltakspenger.saksbehandling.omgjøring.Omgjøringsgrad
import no.nav.tiltakspenger.saksbehandling.omgjøring.Omgjøringsperiode
import no.nav.tiltakspenger.saksbehandling.omgjøring.infra.repo.toDbJson
import no.nav.tiltakspenger.saksbehandling.sak.infra.repo.SakPostgresRepo
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import no.nav.tiltakspenger.saksbehandling.vedtak.infra.repo.RammevedtakPostgresRepo.Companion.toRammevedtak
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

class V152__migrer_omgjort_rammevedtak : BaseJavaMigration() {
    override fun migrate(context: Context) {
        val logger = KotlinLogging.logger {}
        val dataSource = context.configuration.dataSource
        val sessionFactory = PostgresSessionFactory(dataSource, SessionCounter(logger))

        sessionFactory.withTransactionContext { tx ->
            tx.withSession { session ->
                hentAlleRammevedtak(session).groupBy { it.sakId }.forEach { (sakId, rammevedtaksliste) ->
                    val sortertUtenAvslag = rammevedtaksliste.sortedBy { it.opprettet }.filterNot { it.erAvslag }
                    sortertUtenAvslag.forEachIndexed { index, vedtak ->
                        val tidslinjeFørDetteVedtaket = sortertUtenAvslag.take(index).toTidslinje()
                        val overlapp = tidslinjeFørDetteVedtaket.overlappendePeriode(vedtak.periode)
                        val omgjør = OmgjørRammevedtak(
                            overlapp.perioderMedVerdi.map {
                                Omgjøringsperiode(
                                    rammevedtakId = it.verdi.id,
                                    periode = it.periode,
                                    omgjøringsgrad = if (it.verdi.periode == it.periode) Omgjøringsgrad.HELT else Omgjøringsgrad.DELVIS,
                                )
                            },
                        )
                        persisterOmgjørRammevedtak(vedtak.behandling.id, omgjør, session)

                        val omgjortAvRammevedtak =
                            sortertUtenAvslag.drop(index + 1).fold(OmgjortAvRammevedtak.empty) { acc, omgjortAvRammevedtak ->
                                (omgjortAvRammevedtak.periode.trekkFra(acc.perioder)).overlappendePerioder(listOf(vedtak.periode))
                                    .map {
                                        Omgjøringsperiode(
                                            rammevedtakId = omgjortAvRammevedtak.id,
                                            periode = it,
                                            omgjøringsgrad = if (vedtak.periode == it) Omgjøringsgrad.HELT else Omgjøringsgrad.DELVIS,
                                        )
                                    }.let { acc.leggTil(it) }
                            }
                        markerOmgjortAv(vedtak.id, omgjortAvRammevedtak, session)
                    }
                    // Ekstra init sjekk
                    SakPostgresRepo.hentForSakId(sakId, tx)
                }
            }
        }
    }
}

private fun hentAlleRammevedtak(
    session: Session,
): List<Rammevedtak> {
    return session.run(queryOf("select * from rammevedtak").map { it.toRammevedtak(session) }.asList)
}

private fun markerOmgjortAv(
    vedtakId: VedtakId,
    omgjortAvRammevedtak: OmgjortAvRammevedtak,
    session: Session,
) {
    session.run(
        queryOf(
            """
                    update rammevedtak
                    set omgjort_av_rammevedtak = to_jsonb(:omgjort_av_rammevedtak::jsonb)
                    where id = :id
                    """.trimIndent(),
            mapOf(
                "id" to vedtakId.toString(),
                "omgjort_av_rammevedtak" to omgjortAvRammevedtak.toDbJson(),
            ),
        ).asUpdate,
    )
}

private fun persisterOmgjørRammevedtak(
    behandlingId: BehandlingId,
    omgjørRammevedtak: OmgjørRammevedtak,
    session: Session,
) {
    session.run(
        sqlQuery(
            """
                 UPDATE behandling
                 SET omgjør_rammevedtak = to_jsonb(:omgjoer_rammevedtak::jsonb)
                 WHERE id = :behandling_id
                 """.trimIndent(),
            "behandling_id" to behandlingId.toString(),
            "omgjoer_rammevedtak" to omgjørRammevedtak.toDbJson(),
        ).asUpdate,
    )
}
