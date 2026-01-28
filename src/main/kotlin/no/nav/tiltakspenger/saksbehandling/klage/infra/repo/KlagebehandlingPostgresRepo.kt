package no.nav.tiltakspenger.saksbehandling.klage.infra.repo

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.toAvbrutt
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.toDbJson
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlinger
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer

class KlagebehandlingPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : KlagebehandlingRepo {
    /**
     * Oppretter eller oppdaterer en klagebehandling i databasen.
     *
     * @param sessionContext vi gjør bare en insert i dette leddet og trenger derfor ikke transaksjonskontekst, selvom det støttes og sende den inn.
     */
    override fun lagreKlagebehandling(
        klagebehandling: Klagebehandling,
        sessionContext: SessionContext?,
    ) {
        sessionFactory.withSession(sessionContext) { sx ->
            sx.run(
                queryOf(
                    //language=SQL
                    """
                    insert into klagebehandling (
                        id,
                        sak_id,
                        opprettet,
                        sist_endret,
                        status,
                        formkrav,
                        saksbehandler,
                        journalpost_id,
                        journalpost_opprettet,
                        resultat,
                        brevtekst,
                        iverksatt_tidspunkt,
                        avbrutt
                    ) values (
                        :id,
                        :sak_id,
                        :opprettet,
                        :sist_endret,
                        :status,
                        to_jsonb(:formkrav::jsonb),
                        :saksbehandler,
                        :journalpost_id,
                        :journalpost_opprettet,
                        to_jsonb(:resultat::jsonb),
                        to_jsonb(:brevtekst::jsonb),
                        :iverksatt_tidspunkt,
                        to_jsonb(:avbrutt::jsonb)
                    ) on conflict (id) do update set
                        sak_id = :sak_id,
                        opprettet = :opprettet,
                        sist_endret = :sist_endret,
                        status = :status,
                        formkrav = to_jsonb(:formkrav::jsonb),
                        saksbehandler = :saksbehandler,
                        journalpost_id = :journalpost_id,
                        journalpost_opprettet = :journalpost_opprettet,
                        resultat = to_jsonb(:resultat::jsonb),
                        brevtekst = to_jsonb(:brevtekst::jsonb),
                        iverksatt_tidspunkt = :iverksatt_tidspunkt,
                        avbrutt = to_jsonb(:avbrutt::jsonb)
                    """.trimIndent(),
                    mapOf(
                        "id" to klagebehandling.id.toString(),
                        "sak_id" to klagebehandling.sakId.toString(),
                        "opprettet" to klagebehandling.opprettet,
                        "sist_endret" to klagebehandling.sistEndret,
                        "status" to klagebehandling.status.toDbEnum(),
                        "formkrav" to klagebehandling.formkrav.toDbJson(),
                        "saksbehandler" to klagebehandling.saksbehandler,
                        "journalpost_id" to klagebehandling.journalpostId.toString(),
                        "journalpost_opprettet" to klagebehandling.journalpostOpprettet,
                        "resultat" to klagebehandling.resultat?.toDbJson(),
                        "brevtekst" to klagebehandling.brevtekst?.toDbJson(),
                        "iverksatt_tidspunkt" to klagebehandling.iverksattTidspunkt,
                        "avbrutt" to klagebehandling.avbrutt?.toDbJson(),
                    ),
                ).asUpdate,
            )
        }
    }

    override fun hentForRammebehandlingId(rammebehandlingId: BehandlingId): Klagebehandling? {
        return sessionFactory.withSession { session ->
            session.run(
                sqlQuery(
                    """
                    select
                      k.*,
                      s.fnr,
                      s.saksnummer
                    from klagebehandling k
                    join sak s on s.id = k.sak_id
                    where k.resultat->>'rammebehandlingId' = :rammebehandlingId
                    """,
                    "rammebehandlingId" to rammebehandlingId.toString(),
                ).map { fromRow(it) }.asSingle,
            )
        }
    }

    companion object {
        fun hentOrNull(
            klagebehandlingId: KlagebehandlingId,
            session: Session,
        ): Klagebehandling? {
            return session.run(
                queryOf(
                    """
                    select
                      k.*,
                      s.fnr,
                      s.saksnummer
                    from klagebehandling k
                    join sak s on s.id = k.sak_id
                    where k.id = :id
                    order by k.opprettet
                    """,
                    mapOf(
                        "id" to klagebehandlingId.toString(),
                    ),
                ).map { fromRow(it) }.asSingle,
            )
        }

        fun hentForSakId(sakId: SakId, session: Session): Klagebehandlinger {
            return session.run(
                sqlQuery(
                    """
                    select
                      k.*,
                      s.fnr,
                      s.saksnummer
                    from klagebehandling k
                    join sak s on s.id = k.sak_id
                    where s.id = :sakId
                    order by k.opprettet
                    """,
                    "sakId" to sakId.toString(),
                ).map { fromRow(it) }.asList,
            ).let { Klagebehandlinger(it) }
        }

        private fun fromRow(
            row: Row,
        ): Klagebehandling {
            return Klagebehandling(
                id = KlagebehandlingId.fromString(row.string("id")),
                sakId = SakId.fromString(row.string("sak_id")),
                saksnummer = Saksnummer(row.string("saksnummer")),
                fnr = Fnr.fromString(row.string("fnr")),
                opprettet = row.localDateTime("opprettet"),
                sistEndret = row.localDateTime("sist_endret"),
                status = row.string("status").toKlagebehandlingsstatus(),
                formkrav = row.string("formkrav").toKlageFormkrav(),
                saksbehandler = row.stringOrNull("saksbehandler"),
                journalpostId = JournalpostId(row.string("journalpost_id")),
                journalpostOpprettet = row.localDateTime("journalpost_opprettet"),
                resultat = row.stringOrNull("resultat")?.toKlagebehandlingResultat(
                    brevtekst = row.stringOrNull("brevtekst")?.toKlageBrevtekst(),
                ),
                iverksattTidspunkt = row.localDateTimeOrNull("iverksatt_tidspunkt"),
                avbrutt = row.stringOrNull("avbrutt")?.toAvbrutt(),
            )
        }
    }
}
