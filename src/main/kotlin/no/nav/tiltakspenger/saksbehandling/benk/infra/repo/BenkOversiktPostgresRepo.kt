package no.nav.tiltakspenger.saksbehandling.benk.infra.repo

import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.benk.domene.Behandlingssammendrag
import no.nav.tiltakspenger.saksbehandling.benk.domene.BehandlingssammendragStatus
import no.nav.tiltakspenger.saksbehandling.benk.domene.BehandlingssammendragType
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkOversikt
import no.nav.tiltakspenger.saksbehandling.benk.domene.HentÅpneBehandlingerCommand
import no.nav.tiltakspenger.saksbehandling.benk.ports.BenkOversiktRepo
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer

data class BehandlingssamendragMedCount(
    val behandlingssammendrag: Behandlingssammendrag,
    val totalAntall: Int,
)

class BenkOversiktPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : BenkOversiktRepo {

    override fun hentÅpneBehandlinger(
        command: HentÅpneBehandlingerCommand,
        sessionContext: SessionContext?,
        limit: Int,
    ): BenkOversikt {
        return sessionFactory.withSession(sessionContext) { session ->
            val rows = session.run(
                queryOf(
                    //language="sql"
                    """
                        with åpneSøknaderUtenBehandling AS (select sa.id                 as sakId,
                                                                   sø.fnr                as fnr,
                                                                   sa.saksnummer         as saksnummer,
                                                                   sø.opprettet          as startet,
                                                                   'SØKNADSBEHANDLING'   AS behandlingstype,
                                                                   'KLAR_TIL_BEHANDLING' AS status,
                                                                   null                  AS saksbehandler,
                                                                   null                  AS beslutter
                                                            from søknad sø
                                                                     join sak sa on sø.sak_id = sa.id
                                                            where behandling_id is null
                                                              and avbrutt is null),
                             åpneSøknadsbehandlinger AS (select sa.id               as sakId,
                                                                sa.fnr              as fnr,
                                                                sa.saksnummer       as saksnummer,
                                                                b.opprettet         as startet,
                                                                'SØKNADSBEHANDLING' as behandlingstype,
                                                                b.status            as status,
                                                                b.saksbehandler     as saksbehandler,
                                                                b.beslutter         as beslutter
                                                         from behandling b
                                                                  join søknad s on b.id = s.behandling_id
                                                                  join sak sa on b.sak_id = sa.id
                                                         where b.avbrutt is null
                                                           and b.behandlingstype = 'FØRSTEGANGSBEHANDLING'
                                                           and b.status in ('KLAR_TIL_BEHANDLING', 'UNDER_BEHANDLING', 'KLAR_TIL_BESLUTNING',
                                                                            'UNDER_BESLUTNING')),
                             åpneRevurderinger AS (select sa.id           as sakId,
                                                          sa.fnr          as fnr,
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
                             åpneMeldekortBehandlinger AS (select s.id                  as sakId,
                                                                  s.fnr                 as fnr,
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
                    select *,
                           count(*) over () as total_count
                    from slåttSammen
                    where behandlingstype = any (:behandlingstype)
                        and status = any (:status)
                        and ((cardinality(:identer::text[]) is null OR saksbehandler = ANY (:identer)) OR (cardinality(:identer::text[]) is null OR beslutter = ANY (:identer)))
                    order by startet ${command.sortering}
                    limit :limit;
                    """.trimIndent(),
                    mapOf(
                        "limit" to 500,
                        "behandlingstype" to if (command.åpneBehandlingerFiltrering.behandlingstype == null) {
                            arrayOf("SØKNADSBEHANDLING", "REVURDERING", "MELDEKORTBEHANDLING")
                        } else {
                            command.åpneBehandlingerFiltrering.behandlingstype.map { it.toString() }.toTypedArray()
                        },
                        "status" to if (command.åpneBehandlingerFiltrering.status == null) {
                            arrayOf(
                                "KLAR_TIL_BEHANDLING",
                                "UNDER_BEHANDLING",
                                "KLAR_TIL_BESLUTNING",
                                "UNDER_BESLUTNING",
                                "KLAR_TIL_UTFYLLING",
                            )
                        } else {
                            command.åpneBehandlingerFiltrering.status.map { it.toString() }.toTypedArray()
                        },
                        "identer" to command.åpneBehandlingerFiltrering.identer?.toTypedArray(),
                    ),
                ).map { row ->
                    val sakId = SakId.fromString(row.string("sakId"))
                    val fnr = Fnr.fromString(row.string("fnr"))
                    val saksnummer = Saksnummer(row.string("saksnummer"))
                    val startet = row.localDateTime("startet")
                    val behandlingstype =
                        row.string("behandlingstype").let { BehandlingssammendragTypeDb.valueOf(it) }.toDomain()
                    val status = row.stringOrNull("status")?.toBehandlingssammendragStatus()
                    val saksbehandler = row.stringOrNull("saksbehandler")
                    val beslutter = row.stringOrNull("beslutter")
                    val count = row.int("total_count")

                    BehandlingssamendragMedCount(
                        Behandlingssammendrag(
                            sakId = sakId,
                            fnr = fnr,
                            saksnummer = saksnummer,
                            startet = startet,
                            kravtidspunkt = if (behandlingstype == BehandlingssammendragType.SØKNADSBEHANDLING) startet else null,
                            behandlingstype = behandlingstype,
                            status = status,
                            saksbehandler = saksbehandler,
                            beslutter = beslutter,
                        ),
                        totalAntall = count,
                    )
                }.asList,
            )

            val behandlingssammendrag = rows.map { it.behandlingssammendrag }
            val totalAntall = rows.firstOrNull()?.totalAntall ?: 0

            BenkOversikt(behandlingssammendrag, totalAntall)
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

private fun String.toBehandlingssammendragStatus(): BehandlingssammendragStatus =
    BehandlingssammendragStatus.valueOf(this)
