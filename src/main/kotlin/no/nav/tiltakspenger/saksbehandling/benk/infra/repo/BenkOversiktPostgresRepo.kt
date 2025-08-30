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
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkSorteringKolonne
import no.nav.tiltakspenger.saksbehandling.benk.domene.HentÅpneBehandlingerCommand
import no.nav.tiltakspenger.saksbehandling.benk.ports.BenkOversiktRepo
import no.nav.tiltakspenger.saksbehandling.infra.repo.booleanOrNull
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
                        with 
    åpneSøknaderUtenBehandling as (select 
                                       sa.id                 as sakId,
                                       sø.fnr                as fnr,
                                       sa.saksnummer         as saksnummer,
                                       sø.opprettet          as startet,
                                       'SØKNADSBEHANDLING'   as behandlingstype,
                                       'KLAR_TIL_BEHANDLING' as status,
                                       null                  as saksbehandler,
                                       null                  as beslutter,
                                       null                  as erSattPåVent,
                                       null::timestamp with time zone as sist_endret
                                    from søknad sø
                                             join sak sa on sø.sak_id = sa.id
                                             left join behandling b on sø.id = b.soknad_id
                                    where b.id is null
                                      and sø.avbrutt is null),
     åpneSøknadsbehandlinger AS (select sa.id               as sakId,
                                        sa.fnr              as fnr,
                                        sa.saksnummer       as saksnummer,
                                        b.opprettet         as startet,
                                        'SØKNADSBEHANDLING' as behandlingstype,
                                        b.status            as status,
                                        b.saksbehandler     as saksbehandler,
                                        b.beslutter         as beslutter,
                                        b.ventestatus->'ventestatusHendelser'->-1->>'erSattPåVent' as erSattPåVent,
                                        b.sist_endret       as sist_endret
                                 from behandling b
                                          join søknad s on s.id = b.soknad_id
                                          join sak sa on b.sak_id = sa.id
                                 where b.avbrutt is null
                                   and b.behandlingstype = 'SØKNADSBEHANDLING'
                                   and b.status in ('KLAR_TIL_BEHANDLING', 'UNDER_BEHANDLING', 'KLAR_TIL_BESLUTNING',
                                                    'UNDER_BESLUTNING')),
     åpneRevurderinger AS (select sa.id           as sakId,
                                  sa.fnr          as fnr,
                                  sa.saksnummer   as saksnummer,
                                  b.opprettet     as startet,
                                  'REVURDERING'   as behandlingstype,
                                  b.status        as status,
                                  b.saksbehandler as saksbehandler,
                                  b.beslutter     as beslutter,
                                  b.ventestatus->'ventestatusHendelser'->-1->>'erSattPåVent' as erSattPåVent,
                                  b.sist_endret   as sist_endret
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
                                          m.beslutter           as beslutter,
                                          null                  as erSattPåVent,
                                          null::timestamp with time zone as sist_endret
                                   from meldekortbehandling m
                                            join sak s on m.sak_id = s.id
                                   where m.avbrutt is null
                                     and m.status in ('KLAR_TIL_BEHANDLING', 'UNDER_BEHANDLING', 'KLAR_TIL_BESLUTNING',
                                                      'UNDER_BESLUTNING')),
åpneMeldekortTilBehandling AS (
/* 
 * meldekortMetadata er en hjelpe-tabell som vi bruker for å finne riktig behandlingstype, og
 * for at vi ikke skal gi ut 'duplikate' rader til benken
 */
WITH meldekortMetadata AS (
    SELECT 
        mbr.sak_id AS sakId,
        mbr.meldeperiode_kjede_id AS kjedeId,
        COUNT(*) OVER (PARTITION BY mbr.sak_id, mbr.meldeperiode_kjede_id) AS antallInnsendteMeldekort,
        mbr.id AS meldekortId,
        ROW_NUMBER() OVER (PARTITION BY mbr.sak_id, mbr.meldeperiode_kjede_id ORDER BY mbr.mottatt DESC) AS sisteMeldekortNr
    FROM meldekort_bruker mbr
)
SELECT s.id AS sakId,
       s.fnr AS fnr,
       s.saksnummer AS saksnummer,
       mbr.mottatt AS startet,
       CASE 
           WHEN mdk.sisteMeldekortNr = mdk.antallInnsendteMeldekort AND mdk.sakId = mbr.sak_id AND mdk.meldekortId = mbr.id 
               THEN 'INNSENDT_MELDEKORT'
           ELSE 'KORRIGERT_MELDEKORT'
       END AS behandlingstype,
       'KLAR_TIL_BEHANDLING' AS status,
       NULL AS saksbehandler,
       NULL AS beslutter,
       NULL AS erSattPåVent,
       NULL::timestamp with time zone AS sist_endret
FROM meldekort_bruker mbr
JOIN sak s ON mbr.sak_id = s.id
JOIN meldekortMetadata mdk ON mbr.id = mdk.meldekortId AND mbr.sak_id = mdk.sakId
LEFT JOIN meldekortbehandling mbh1
       ON mbh1.sak_id = s.id
       AND (mbh1.brukers_meldekort_id = mbr.id OR mbh1.meldeperiode_kjede_id = mbr.meldeperiode_kjede_id)
WHERE behandles_automatisk = false
  AND mdk.sisteMeldekortNr = 1
  AND (
        mbh1.id IS NULL
        OR EXISTS (
            SELECT 1
            FROM meldekortbehandling mbh
            WHERE mbh.sak_id = s.id
              AND mbh.avbrutt IS NULL
              AND (mbh.brukers_meldekort_id = mbr.id OR mbh.meldeperiode_kjede_id = mbr.meldeperiode_kjede_id)
              AND mbh.iverksatt_tidspunkt IS NOT NULL
              AND mbr.mottatt > mbh.iverksatt_tidspunkt
        )
      )
),
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
                     from åpneMeldekortBehandlinger
                     union all
                     select *
                     from åpneMeldekortTilBehandling)
select *,
       count(*) over () as total_count
from slåttSammen
where behandlingstype = any (:behandlingstype)
  and status = any (:status)
  and (
    case
      when :identer::text[] is null then true
      when :identer::text[] = array['IKKE_TILDELT']::text[] then (saksbehandler is null or beslutter is null)
      else (saksbehandler = any(:identer::text[]) or beslutter = any(:identer::text[]))
    end
  )
and (
  case
    when :benktype::text[] is null then true
    when 'KLAR' = any(:benktype::text[]) and (erSattPåVent is null or erSattPåVent != 'true') then true
    when 'VENTER' = any(:benktype::text[]) and erSattPåVent = 'true' then true
    else false
  end
)
                    order by ${command.sortering.kolonne.toDbString()} ${command.sortering.retning}
                    limit :limit;
                    """.trimIndent(),
                    mapOf(
                        "limit" to 500,
                        "behandlingstype" to if (command.åpneBehandlingerFiltrering.behandlingstype == null) {
                            arrayOf("KORRIGERT_MELDEKORT", "INNSENDT_MELDEKORT", "SØKNADSBEHANDLING", "REVURDERING", "MELDEKORTBEHANDLING")
                        } else {
                            command.åpneBehandlingerFiltrering.behandlingstype.map { it.toString() }.toTypedArray()
                        },
                        "status" to if (command.åpneBehandlingerFiltrering.status == null) {
                            arrayOf(
                                "KLAR_TIL_BEHANDLING",
                                "UNDER_BEHANDLING",
                                "KLAR_TIL_BESLUTNING",
                                "UNDER_BESLUTNING",
                            )
                        } else {
                            command.åpneBehandlingerFiltrering.status.map { it.toString() }.toTypedArray()
                        },
                        "identer" to command.åpneBehandlingerFiltrering.identer?.toTypedArray(),
                        "benktype" to if (command.åpneBehandlingerFiltrering.benktype == null) {
                            arrayOf("KLAR", "VENTER")
                        } else {
                            command.åpneBehandlingerFiltrering.benktype.map { it.toString() }.toTypedArray()
                        },
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
                    val sistEndret = row.localDateTimeOrNull("sist_endret")
                    val count = row.int("total_count")
                    val erSattPåVent = row.booleanOrNull("erSattPåVent") == true

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
                            sistEndret = sistEndret,
                            erSattPåVent = erSattPåVent,
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

enum class BehandlingssammendragTypeDb {
    SØKNADSBEHANDLING,
    REVURDERING,
    MELDEKORTBEHANDLING,
    INNSENDT_MELDEKORT,
    KORRIGERT_MELDEKORT,
    ;

    fun toDomain(): BehandlingssammendragType = when (this) {
        SØKNADSBEHANDLING -> BehandlingssammendragType.SØKNADSBEHANDLING
        REVURDERING -> BehandlingssammendragType.REVURDERING
        MELDEKORTBEHANDLING -> BehandlingssammendragType.MELDEKORTBEHANDLING
        INNSENDT_MELDEKORT -> BehandlingssammendragType.INNSENDT_MELDEKORT
        KORRIGERT_MELDEKORT -> BehandlingssammendragType.KORRIGERT_MELDEKORT
    }
}

fun BenkSorteringKolonne.toDbString(): String =
    when (this) {
        BenkSorteringKolonne.STARTET -> "startet"
        BenkSorteringKolonne.SIST_ENDRET -> "sist_endret"
    }

fun String.toBehandlingssammendragStatus(): BehandlingssammendragStatus =
    BehandlingssammendragStatus.valueOf(this)
