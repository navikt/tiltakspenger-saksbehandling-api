@file:Suppress("ClassName", "unused")

package db.migration

import kotliquery.queryOf
import mu.KotlinLogging
import no.nav.tiltakspenger.felles.HendelseId
import no.nav.tiltakspenger.felles.Hendelsesversjon
import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.SessionCounter
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.meldekort.domene.MeldeperiodeBeregningDag
import no.nav.tiltakspenger.vedtak.repository.meldekort.MeldekortPostgresRepo
import no.nav.tiltakspenger.vedtak.repository.meldekort.MeldeperiodePostgresRepo
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

class V18__migrer_meldekort_til_meldeperiode : BaseJavaMigration() {
    val logger = KotlinLogging.logger { }

    @Throws(Exception::class)
    override fun migrate(context: Context) {
        // val statement = context.connection.createStatement()
        // / statement.execute("alter table behandling add column if not exists beregning jsonb")
        // statement.close()

        val dataSource = context.configuration.dataSource
        val sessionFactory = PostgresSessionFactory(dataSource, SessionCounter(logger))
        val meldekortRepo = MeldekortPostgresRepo(sessionFactory)
        val meldeperiodeRepo = MeldeperiodePostgresRepo(sessionFactory)

        val sakIder: List<SakId> = sessionFactory.withSession { session ->
            session.run(
                queryOf("select id from sak", mapOf()).map { row ->
                    SakId.fromString(row.string("id"))
                }.asList,
            )
        }

        val meldekort = sakIder.mapNotNull { sakId -> meldekortRepo.hentForSakId(sakId, null) }
        meldekort.flatten().forEach { meldekortBehandling ->
            val meldeperiode = Meldeperiode(
                id = MeldeperiodeId.fraPeriode(meldekortBehandling.periode),
                hendelseId = HendelseId.random(),
                versjon = Hendelsesversjon.ny(),
                sakId = meldekortBehandling.sakId,
                saksnummer = meldekortBehandling.saksnummer,
                fnr = meldekortBehandling.fnr,
                opprettet = meldekortBehandling.opprettet,
                periode = meldekortBehandling.periode,
                antallDagerForPeriode = 10,
                girRett = meldekortBehandling.beregning.dager.map {
                    it.dato to (it !is MeldeperiodeBeregningDag.Utfylt.Sperret)
                }.toMap(),
                sendtTilMeldekortApi = null,
            )
            meldeperiodeRepo.lagre(meldeperiode)
        }
    }
}
