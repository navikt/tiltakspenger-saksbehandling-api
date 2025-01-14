package no.nav.tiltakspenger.vedtak.repository.meldekort

import kotliquery.Row
import kotliquery.Session
import no.nav.tiltakspenger.libs.common.HendelseId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.meldekort.domene.MeldekortBrukerRepo

class MeldekortBrukerPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : MeldekortBrukerRepo {
    override fun lagre(
        brukersMeldekort: BrukersMeldekort,
        sessionContext: SessionContext?,
    ) {
        sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    insert into meldekort_bruker (
                        id,
                        meldeperiode_hendelse_id,
                        meldeperiode_id,
                        meldeperiode_versjon,
                        sak_id,
                        mottatt,
                        dager
                    ) values (
                        :id,
                        :meldeperiode_hendelse_id,
                        :meldeperiode_id,
                        :meldeperiode_versjon,
                        :sak_id,
                        :mottatt,
                        to_jsonb(:dager::jsonb)
                    )
                    """,
                    "id" to brukersMeldekort.id.toString(),
                    "meldeperiode_hendelse_id" to brukersMeldekort.meldeperiode.hendelseId.toString(),
                    "meldeperiode_id" to brukersMeldekort.meldeperiodeId.toString(),
                    "meldeperiode_versjon" to brukersMeldekort.meldeperiode.versjon.value,
                    "sak_id" to brukersMeldekort.sakId.toString(),
                    "mottatt" to brukersMeldekort.mottatt,
                    "dager" to brukersMeldekort.toDagerJson(),
                ).asUpdate,
            )
        }
    }

    override fun hentForSakId(
        sakId: SakId,
        sessionContext: SessionContext?,
    ): List<BrukersMeldekort> {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    select m.*
                    from meldekort_bruker m 
                    where m.sak_id = :sak_id
                    """,
                    "sak_id" to sakId.toString(),
                ).map { row -> fromRow(row, session) }.asList,
            )
        }
    }

    companion object {
        private fun fromRow(
            row: Row,
            session: Session,
        ): BrukersMeldekort {
            return BrukersMeldekort(
                id = MeldekortId.fromString(row.string("id")),
                mottatt = row.localDateTime("mottatt"),
                meldeperiode = MeldeperiodePostgresRepo.hentForHendelseId(
                    HendelseId.fromString(row.string("meldeperiode_hendelse_id")),
                    session,
                )!!,
                sakId = SakId.fromString(row.string("sak_id")),
                dager = row.string("dager").toMeldekortDager(),
            )
        }
    }
}
