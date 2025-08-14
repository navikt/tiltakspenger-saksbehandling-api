@file:Suppress("unused", "ktlint")

package db.migration

import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.Row
import kotliquery.Session
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresTransactionContext.Companion.withSession
import no.nav.tiltakspenger.libs.persistering.infrastruktur.SessionCounter
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.InnmeldtStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.MeldeperiodePostgresRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.tilMeldekortBehandletAutomatiskStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.toDbJson
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.toMeldekortDager
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

class V111__fiks_ikke_rett_meldekort_bruker : BaseJavaMigration() {
    override fun migrate(context: Context) {
        val logger = KotlinLogging.logger {}
        val dataSource = context.configuration.dataSource
        val sessionFactory = PostgresSessionFactory(dataSource, SessionCounter(logger))

        sessionFactory.withTransactionContext { tx ->
            val alleMeldekortBruker = tx.withSession { session ->
                session.run(
                    sqlQuery("""select * from meldekort_bruker""")
                        .map { row -> fromRowTilBrukersMeldekort(row, session) }.asList,
                )
            }

            alleMeldekortBruker.forEach { meldekort ->
                val meldeperiode = meldekort.meldeperiode

                val dager = meldeperiode.girRett.toList().zip(meldekort.dager).map { (girRett, meldekortDag) ->
                    val harRettPåDenneDagen = girRett.second

                    if (harRettPåDenneDagen) {
                        //hvis bruker har rett, så skal dagen være utfylt med noe fra før (??)
                        BrukersMeldekort.BrukersMeldekortDag(
                            dato = meldekortDag.dato,
                            status = meldekortDag.status,
                        )
                    } else {
                        //har ikke rett - skal sette status til IKKE_RETT_TIL_TILTAKSPENGER (denne skal være IKKE_BESVART)
                        BrukersMeldekort.BrukersMeldekortDag(
                            dato = meldekortDag.dato,
                            status = InnmeldtStatus.IKKE_RETT_TIL_TILTAKSPENGER,
                        )
                    }
                }

                tx.withSession { session ->
                    session.run(
                        sqlQuery(
                            """update meldekort_bruker set dager = to_jsonb(:dager::jsonb) where id = :id""",
                            "dager" to dager.toDbJson(),
                            "id" to meldekort.id.toString(),
                        ).asUpdate,
                    )
                }
            }
        }
    }

}


//copy-pasta fra V109__ikke_rett_status.kt
private fun fromRowTilBrukersMeldekort(
    row: Row,
    session: Session,
): BrukersMeldekort {
    return BrukersMeldekort(
        id = MeldekortId.fromString(row.string("id")),
        mottatt = row.localDateTime("mottatt"),
        meldeperiode = MeldeperiodePostgresRepo.hentForMeldeperiodeId(
            MeldeperiodeId.fromString(row.string("meldeperiode_id")),
            session,
        )!!,
        sakId = SakId.fromString(row.string("sak_id")),
        dager = row.string("dager").toMeldekortDager(),
        journalpostId = JournalpostId(row.string("journalpost_id")),
        oppgaveId = row.stringOrNull("oppgave_id")?.let { OppgaveId(it) },
        behandlesAutomatisk = row.boolean("behandles_automatisk"),
        behandletAutomatiskStatus = row.stringOrNull("behandlet_automatisk_status")
            ?.tilMeldekortBehandletAutomatiskStatus(),
    )
}
