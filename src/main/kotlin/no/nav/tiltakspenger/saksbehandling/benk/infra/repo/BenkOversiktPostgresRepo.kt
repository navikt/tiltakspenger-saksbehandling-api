package no.nav.tiltakspenger.saksbehandling.benk.infra.repo

import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.toBehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.benk.domene.Behandlingssammendrag
import no.nav.tiltakspenger.saksbehandling.benk.domene.BehandlingssammendragType
import no.nav.tiltakspenger.saksbehandling.benk.ports.BenkOversiktRepo
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer

class BenkOversiktPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : BenkOversiktRepo {
    override fun hentÅpneBehandlinger(sessionContext: SessionContext?, limit: Int): List<Behandlingssammendrag> {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                queryOf(
                    //language="sql"
                    """
                        with åpneSøknaderUtenBehandling AS (select sø.fnr                  as fnr,
                                                                   sa.saksnummer           as saksnummer,
                                                                   sø.opprettet            as startet,
                                                                   'SØKNADSBEHANDLING' AS behandlingstype,
                                                                   null                    AS status,
                                                                   null                    AS saksbehandler,
                                                                   null                    AS beslutter
                                                            from søknad sø
                                                                     join sak sa on sø.sak_id = sa.id
                                                            where behandling_id is null
                                                              and avbrutt is null),
                             åpneSøknadsbehandlinger AS (select sa.fnr            as fnr,
                                                                sa.saksnummer     as saksnummer,
                                                                b.opprettet       as startet,
                                                                'SØKNADSBEHANDLING' as behandlingstype,
                                                                b.status          as status,
                                                                b.saksbehandler   as saksbehandler,
                                                                b.beslutter       as beslutter
                                                         from behandling b
                                                                  join søknad s on b.id = s.behandling_id
                                                                  join sak sa on b.sak_id = sa.id
                                                         where b.avbrutt is null
                                                           and b.behandlingstype = 'FØRSTEGANGSBEHANDLING'
                                                           and b.status in ('KLAR_TIL_BEHANDLING', 'UNDER_BEHANDLING', 'KLAR_TIL_BESLUTNING',
                                                                            'UNDER_BESLUTNING')),
                             åpneRevurderinger AS (select sa.fnr          as fnr,
                                                          sa.saksnummer   as saksnummer,
                                                          b.opprettet     as startet,
                                                          'REVURDERING'   as behandlingstype,
                                                          b.status        as status,
                                                          b.saksbehandler as saksbehandler,
                                                          b.beslutter     as beslutter
                                                   from behandling b
                                                            join sak sa on b.sak_id = sa.id
                                                   where b.behandlingstype = 'REVURDERING'
                                                     and b.avbrutt is null 
                                                     and b.status in ('KLAR_TIL_BEHANDLING', 'UNDER_BEHANDLING', 'KLAR_TIL_BESLUTNING',
                                                                            'UNDER_BESLUTNING')),
                             åpneMeldekortBehandlinger AS (select s.fnr                 as fnr,
                                                                  s.saksnummer          as saksnummer,
                                                                  m.opprettet           as startet,
                                                                  'MELDEKORTBEHANDLING' as behandlingstype,
                                                                  m.status              as status,
                                                                  m.saksbehandler       as saksbehandler,
                                                                  m.beslutter           as beslutter
                                                           from meldekortbehandling m
                                                                    join sak s on m.sak_id = s.id
                                                           where m.avbrutt is null
                                                             and m.status in ('KLAR_TIL_UTFYLLING', 'KLAR_TIL_BESLUTNING', 'UNDER_BESLUTNING')),
                             slåttSammen AS (select *
                                             from åpneSøknaderUtenBehandling
                                             union all
                                             select *
                                             from åpneSøknadsbehandlinger
                                             union all
                                             select *
                                             from åpneRevurderinger
                                             union all
                                             select *
                                             from åpneMeldekortBehandlinger)
                        select *
                        from slåttSammen order by startet limit :limit;
                    """.trimIndent(),
                    mapOf(
                        "limit" to limit,
                    ),
                ).map { row ->
                    val fnr = Fnr.fromString(row.string("fnr"))
                    val saksnummer = Saksnummer(row.string("saksnummer"))
                    val startet = row.localDateTime("startet")
                    val behandlingstype = row.string("behandlingstype").let { BehandlingssammendragTypeDb.valueOf(it) }
                    val status = row.stringOrNull("status")?.toBehandlingsstatus()
                    val saksbehandler = row.stringOrNull("saksbehandler")
                    val beslutter = row.stringOrNull("beslutter")

                    Behandlingssammendrag(
                        fnr = fnr,
                        saksnummer = saksnummer,
                        startet = startet,
                        behandlingstype = behandlingstype.toDomain(),
                        status = status,
                        saksbehandler = saksbehandler,
                        beslutter = beslutter,
                    )
                }.asList,
            )
        }
    }
}

private enum class BehandlingssammendragTypeDb {
    SØKNADSBEHANDLING,
    REVURDERING,
    MELDEKORTBEHANDLING,
    ;

    fun toDomain(): BehandlingssammendragType = when (this) {
        SØKNADSBEHANDLING -> BehandlingssammendragType.SØKNADSBEHANDLING
        REVURDERING -> BehandlingssammendragType.REVURDERING
        MELDEKORTBEHANDLING -> BehandlingssammendragType.MELDEKORTBEHANDLING
    }
}
