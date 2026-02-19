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
import no.nav.tiltakspenger.saksbehandling.felles.Ventestatus
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.toAvbrutt
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.toDbJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.toVentestatus
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalførBrevMetadata
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.journalføring.infra.repo.toDbJson
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlinger
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppretthold.OversendtKlageTilKabalMetadata
import no.nav.tiltakspenger.saksbehandling.klage.infra.repo.KlagebehandlingPostgresRepo.Companion.overtaBehandling
import no.nav.tiltakspenger.saksbehandling.klage.infra.repo.KlagebehandlingPostgresRepo.Companion.taBehandling
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer

class KlagebehandlingPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : KlagebehandlingRepo {
    /**
     * Oppretter eller oppdaterer en klagebehandling i databasen.
     *
     * @param sessionContext vi gjør bare én insert i dette leddet og trenger derfor ikke transaksjonskontekst, selvom det støttes og sende den inn.
     */
    override fun lagreKlagebehandling(
        klagebehandling: Klagebehandling,
        sessionContext: SessionContext?,
    ) {
        sessionFactory.withSession(sessionContext) { sx ->
            lagreKlagebehandling(klagebehandling, sx)
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

    /**
     * Oppdaterer behandlingsstatus, og saksbehandler bare dersom den er null.
     * Skal du endre saksbehandler bruk [overtaBehandling]
     */
    override fun taBehandling(
        klagebehandling: Klagebehandling,
        sessionContext: SessionContext?,
    ): Boolean {
        return sessionFactory.withSession(sessionContext) { session ->
            taBehandling(klagebehandling, session)
        }
    }

    /**
     * En ny saksbehandler overtar for [nåværendeSaksbehandler].
     * Dersom det ikke er en saksbehandler på behandlingen, bruk [taBehandling]
     * @param nåværendeSaksbehandler For å unngå at to saksbehandlere kan overta samtidig.
     */
    override fun overtaBehandling(
        klagebehandling: Klagebehandling,
        nåværendeSaksbehandler: String,
        sessionContext: SessionContext?,
    ): Boolean {
        return sessionFactory.withSession(sessionContext) { session ->
            overtaBehandling(klagebehandling, nåværendeSaksbehandler, session)
        }
    }

    /**
     * Henter de som har ligget lengst basert på sist_endret.
     */
    override fun hentInnstillingsbrevSomSkalJournalføres(
        limit: Int,
    ): List<Klagebehandling> {
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
                    where k.status = 'OPPRETTHOLDT'
                    and k.resultat->>'journalpostIdInnstillingsbrev' is null
                    order by k.sist_endret, k.sak_id
                    limit $limit
                    """.trimIndent(),
                ).map { fromRow(it) }.asList,
            )
        }
    }

    override fun markerInnstillingsbrevJournalført(
        klagebehandling: Klagebehandling,
        metadata: JournalførBrevMetadata,
    ) {
        sessionFactory.withTransaction { tx ->
            lagreKlagebehandling(klagebehandling, tx)
            tx.run(
                queryOf(
                    """
                    update klagebehandling set 
                        brev_metadata = to_jsonb(:brev_metadata::jsonb)
                    where id = :id
                    """.trimIndent(),
                    mapOf(
                        "id" to klagebehandling.id.toString(),
                        "brev_metadata" to metadata.toDbJson(),
                    ),
                ).asUpdate,
            )
        }
    }

    override fun hentInnstillingsbrevSomSkalDistribueres(limit: Int): List<Klagebehandling> {
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
                    where k.status = 'OPPRETTHOLDT'
                    and k.resultat->>'journalpostIdInnstillingsbrev' is not null
                    and k.resultat->>'distribusjonId' is null
                    order by k.sist_endret
                    limit $limit
                    """.trimIndent(),
                ).map { fromRow(it) }.asList,
            )
        }
    }

    /**
     * Henter de som har ligget lengst basert på sist_endret.
     */
    override fun hentSakerSomSkalOversendesKlageinstansen(
        limit: Int,
    ): List<SakId> {
        return sessionFactory.withSession { session ->
            session.run(
                sqlQuery(
                    """
                    select distinct k.sak_id, k.sist_endret
                    from klagebehandling k
                    where k.status = 'OPPRETTHOLDT'
                    and k.resultat->>'journalpostIdInnstillingsbrev' is not null
                    and k.resultat->>'distribusjonIdInnstillingsbrev' is not null
                    and k.resultat->>'oversendtKlageinstansenTidspunkt' is null
                    order by k.sist_endret
                    limit $limit
                    """.trimIndent(),
                ).map { SakId.fromString(it.string("sak_id")) }.asList,
            )
        }
    }

    override fun markerOversendtTilKlageinstans(
        klagebehandling: Klagebehandling,
        metadata: OversendtKlageTilKabalMetadata,
    ) {
        sessionFactory.withTransaction { tx ->
            tx.run(
                sqlQuery(
                    """
                    update klagebehandling set
                        kabal_metadata = to_jsonb(:metadata::jsonb)
                    where id = :id
                    """,
                    "id" to klagebehandling.id.toString(),
                    "metadata" to """
                    {
                        "request": ${metadata.request},
                        "response": "${metadata.response}",
                        "oversendtTidspunkt": "${metadata.oversendtTidspunkt}"
                    }
                    """.trimIndent(),
                ).asUpdate,
            )
            lagreKlagebehandling(klagebehandling, tx)
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

        fun lagreKlagebehandling(
            klagebehandling: Klagebehandling,
            session: Session,
        ): Int {
            return session.run(
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
                        avbrutt,
                        ventestatus
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
                        to_jsonb(:avbrutt::jsonb),
                        to_jsonb(:ventestatus::jsonb)
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
                        avbrutt = to_jsonb(:avbrutt::jsonb),
                        ventestatus = to_jsonb(:ventestatus::jsonb)
                    """.trimIndent(),
                    mapOf(
                        "id" to klagebehandling.id.toString(),
                        "sak_id" to klagebehandling.sakId.toString(),
                        "opprettet" to klagebehandling.opprettet,
                        "sist_endret" to klagebehandling.sistEndret,
                        "status" to klagebehandling.status.toDbEnum(),
                        "formkrav" to klagebehandling.formkrav.toDbJson(),
                        "saksbehandler" to klagebehandling.saksbehandler,
                        "journalpost_id" to klagebehandling.klagensJournalpostId.toString(),
                        "journalpost_opprettet" to klagebehandling.klagensJournalpostOpprettet,
                        "resultat" to klagebehandling.resultat?.toDbJson(),
                        "brevtekst" to klagebehandling.brevtekst?.toDbJson(),
                        "iverksatt_tidspunkt" to klagebehandling.iverksattTidspunkt,
                        "avbrutt" to klagebehandling.avbrutt?.toDbJson(),
                        "ventestatus" to klagebehandling.ventestatus.toDbJson(),
                    ),
                ).asUpdate,
            )
        }

        /**
         * Oppdaterer behandlingsstatus, og saksbehandler bare dersom den er null.
         * Skal du endre saksbehandler bruk [overtaBehandling]
         */
        fun taBehandling(
            klagebehandling: Klagebehandling,
            session: Session,
        ): Boolean {
            return session.run(
                sqlQuery(
                    """
                    update klagebehandling set
                        saksbehandler = :saksbehandler,
                        status = :status,
                        sist_endret = :sist_endret
                    where id = :id and saksbehandler is null and status = 'KLAR_TIL_BEHANDLING'
                    """,
                    "id" to klagebehandling.id.toString(),
                    "saksbehandler" to klagebehandling.saksbehandler,
                    "status" to klagebehandling.status.toDbEnum(),
                    "sist_endret" to klagebehandling.sistEndret,
                ).asUpdate,
            ) > 0
        }

        /**
         * En ny saksbehandler overtar for [nåværendeSaksbehandler].
         * Dersom det ikke er en saksbehandler på behandlingen, bruk [taBehandling]
         * @param nåværendeSaksbehandler For å unngå at to saksbehandlere kan overta samtidig.
         */
        fun overtaBehandling(
            klagebehandling: Klagebehandling,
            nåværendeSaksbehandler: String,
            session: Session,
        ): Boolean {
            return session.run(
                sqlQuery(
                    """
                    update klagebehandling set
                        saksbehandler = :nySaksbehandler,
                        sist_endret = :sist_endret
                    where id = :id and saksbehandler = :lagretSaksbehandler and status = 'UNDER_BEHANDLING'
                    """,
                    "id" to klagebehandling.id.toString(),
                    "nySaksbehandler" to klagebehandling.saksbehandler,
                    "lagretSaksbehandler" to nåværendeSaksbehandler,
                    "sist_endret" to klagebehandling.sistEndret,
                ).asUpdate,
            ) > 0
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
                klagensJournalpostId = JournalpostId(row.string("journalpost_id")),
                klagensJournalpostOpprettet = row.localDateTime("journalpost_opprettet"),
                resultat = row.stringOrNull("resultat")?.toKlagebehandlingResultat(
                    brevtekst = row.stringOrNull("brevtekst")?.toKlageBrevtekst(),
                ),
                iverksattTidspunkt = row.localDateTimeOrNull("iverksatt_tidspunkt"),
                avbrutt = row.stringOrNull("avbrutt")?.toAvbrutt(),
                ventestatus = row.stringOrNull("ventestatus")?.toVentestatus() ?: Ventestatus(),
            )
        }
    }
}
