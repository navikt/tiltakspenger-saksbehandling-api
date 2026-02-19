package no.nav.tiltakspenger.saksbehandling.benk.infra.repo

import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.benk.domene.Behandlingssammendrag
import no.nav.tiltakspenger.saksbehandling.benk.domene.BehandlingssammendragBenktype
import no.nav.tiltakspenger.saksbehandling.benk.domene.BehandlingssammendragStatus
import no.nav.tiltakspenger.saksbehandling.benk.domene.BehandlingssammendragType
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkOversikt
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkSorteringKolonne
import no.nav.tiltakspenger.saksbehandling.benk.domene.HentÅpneBehandlingerCommand
import no.nav.tiltakspenger.saksbehandling.benk.domene.SorteringRetning
import no.nav.tiltakspenger.saksbehandling.benk.ports.BenkOversiktRepo
import no.nav.tiltakspenger.saksbehandling.benk.ports.BenkOversiktRepo.Companion.IKKE_TILDELT
import no.nav.tiltakspenger.saksbehandling.infra.repo.booleanOrNull
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer

data class BehandlingssamendragMedCount(
    val behandlingssammendrag: Behandlingssammendrag,
    val totalAntall: Int,
)

class BenkOversiktPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : BenkOversiktRepo {

    private object BenkSpørringer {
        const val ÅPNE_SØKNADER_UTEN_BEHANDLING = """
            select 
                sa.id                 as sakId,
                sø.fnr                as fnr,
                sa.saksnummer         as saksnummer,
                sø.opprettet          as startet,
                'SØKNADSBEHANDLING'   as behandlingstype,
                'KLAR_TIL_BEHANDLING' as status,
                null                  as saksbehandler,
                null                  as beslutter,
                null::boolean         as erSattPåVent,
                null                  as sattPåVentBegrunnelse,
                null::date            as sattPåVentFrist,
                null::timestamp with time zone as sist_endret
            from søknad sø
                join sak sa on sø.sak_id = sa.id
            where 
                not exists (
                    select 1 from behandling b where b.soknad_id = sø.id
                )
                and sø.avbrutt is null
        """

        const val ÅPNE_SØKNADSBEHANDLINGER = """
            select sa.id               as sakId,
                sa.fnr              as fnr,
                sa.saksnummer       as saksnummer,
                b.opprettet         as startet,
                'SØKNADSBEHANDLING' as behandlingstype,
                b.status            as status,
                b.saksbehandler     as saksbehandler,
                b.beslutter         as beslutter,
                (b.ventestatus->'ventestatusHendelser'->-1->>'erSattPåVent')::boolean  as erSattPåVent,
                b.ventestatus->'ventestatusHendelser'->-1->>'begrunnelse'   as sattPåVentBegrunnelse,
                (b.ventestatus->'ventestatusHendelser'->-1->>'frist')::date as sattPåVentFrist,
                b.sist_endret       as sist_endret
            from behandling b
                join søknad s on s.id = b.soknad_id
                join sak sa on b.sak_id = sa.id
            where b.avbrutt is null
              and b.behandlingstype = 'SØKNADSBEHANDLING'
              and b.status in ('KLAR_TIL_BEHANDLING', 'UNDER_BEHANDLING', 'KLAR_TIL_BESLUTNING',
                               'UNDER_BESLUTNING', 'UNDER_AUTOMATISK_BEHANDLING')
        """

        const val ÅPNE_REVURDERINGER = """
            select sa.id           as sakId,
                sa.fnr          as fnr,
                sa.saksnummer   as saksnummer,
                b.opprettet     as startet,
                'REVURDERING'   as behandlingstype,
                b.status        as status,
                b.saksbehandler as saksbehandler,
                b.beslutter     as beslutter,
                (b.ventestatus->'ventestatusHendelser'->-1->>'erSattPåVent')::boolean as erSattPåVent,
                b.ventestatus->'ventestatusHendelser'->-1->>'begrunnelse'  as sattPåVentBegrunnelse,
                (b.ventestatus->'ventestatusHendelser'->-1->>'frist')::date as sattPåVentFrist,
                b.sist_endret   as sist_endret
            from behandling b
                join sak sa on b.sak_id = sa.id
            where b.behandlingstype = 'REVURDERING'
              and b.avbrutt is null
              and b.status in ('KLAR_TIL_BEHANDLING', 'UNDER_BEHANDLING', 'KLAR_TIL_BESLUTNING',
                               'UNDER_BESLUTNING')
        """

        const val ÅPNE_MELDEKORTBEHANDLINGER = """
            select s.id                  as sakId,
                s.fnr                 as fnr,
                s.saksnummer          as saksnummer,
                m.opprettet           as startet,
                'MELDEKORTBEHANDLING' as behandlingstype,
                m.status              as status,
                m.saksbehandler       as saksbehandler,
                m.beslutter           as beslutter,
                null::boolean         as erSattPåVent,
                null                  as sattPåVentBegrunnelse,
                null::date            as sattPåVentFrist,
                m.sist_endret as sist_endret
            from meldekortbehandling m
                join sak s on m.sak_id = s.id
            where m.avbrutt is null
              and m.status in ('KLAR_TIL_BEHANDLING', 'UNDER_BEHANDLING', 'KLAR_TIL_BESLUTNING',
                               'UNDER_BESLUTNING')
        """

        const val ÅPNE_MELDEKORT_TIL_BEHANDLING = """
            /*
             * meldekortMetadata er en hjelpe-tabell som vi bruker for å finne riktig behandlingstype, og
             * for at vi ikke skal gi ut 'duplikate' rader til benken
             */
            with meldekortMetadata as (
                select 
                    mbr.sak_id,
                    mbr.meldeperiode_kjede_id,
                    count(*) over (partition by mbr.sak_id, mbr.meldeperiode_kjede_id) as antallInnsendteMeldekort,
                    mbr.id as meldekortId,
                    row_number() over (partition by mbr.sak_id, mbr.meldeperiode_kjede_id order by mbr.mottatt desc) as sisteMeldekortNr
                from meldekort_bruker mbr
                where behandles_automatisk = false
            ),
            /*
            Hjelpe-tabell for å finne siste opprettede meldekortbehandling på en sak/kjede - dette er for å vite om et meldekort er potensielt tatt stilling til
             */
            sisteMeldekortBehandlingForKjede as (
                select 
                    sak_id,
                    meldeperiode_kjede_id,
                    max(sist_endret) as sist_endret_tidspunkt
                from meldekortbehandling
                group by sak_id, meldeperiode_kjede_id
            )
            select 
                s.id                           as sakId,
                s.fnr                          as fnr,
                s.saksnummer                   as saksnummer,
                mbr.mottatt                    as startet,
                case
                    when mdk.sisteMeldekortNr = mdk.antallInnsendteMeldekort
                        then 'INNSENDT_MELDEKORT'
                    else 'KORRIGERT_MELDEKORT'
                end                            as behandlingstype,
                'KLAR_TIL_BEHANDLING'          as status,
                null                           as saksbehandler,
                null                           as beslutter,
                null::boolean                  as erSattPåVent,
                null                           as sattPåVentBegrunnelse,
                null::date                     as sattPåVentFrist,
                null::timestamp with time zone as sist_endret
            from meldekort_bruker mbr
            join sak s on mbr.sak_id = s.id
            join meldekortMetadata mdk on mbr.id = mdk.meldekortId
            left join sisteMeldekortBehandlingForKjede smbh
                on smbh.sak_id = mbr.sak_id 
                and smbh.meldeperiode_kjede_id = mbr.meldeperiode_kjede_id
            where mdk.sisteMeldekortNr = 1
              and (mbr.mottatt > smbh.sist_endret_tidspunkt or smbh.sist_endret_tidspunkt is null)
        """

        const val ÅPNE_KLAGER = """
            select k.sak_id          as sakId,
                s.fnr             as fnr,
                s.saksnummer      as saksnummer,
                k.opprettet       as startet,
                'KLAGEBEHANDLING' as behandlingstype,
                k.status          as status,
                k.saksbehandler   as saksbehandler,
                null              as beslutter,
                (k.ventestatus->'ventestatusHendelser'->-1->>'erSattPåVent')::boolean  as erSattPåVent,
                k.ventestatus->'ventestatusHendelser'->-1->>'begrunnelse'   as sattPåVentBegrunnelse,
                (k.ventestatus->'ventestatusHendelser'->-1->>'frist')::date as sattPåVentFrist,
                k.sist_endret     as sist_endret
            from klagebehandling k
                join sak s on k.sak_id = s.id
            where k.status in ('KLAR_TIL_BEHANDLING', 'UNDER_BEHANDLING')
        """
    }

    private fun mapQueryParams(command: HentÅpneBehandlingerCommand, limit: Int): Map<String, Any?> {
        return mapOf(
            "limit" to limit,
            "behandlingstype" to (
                command.åpneBehandlingerFiltrering.behandlingstype?.map { it.toString() }
                    ?: BehandlingssammendragType.entries.map { it.toString() }
                ).toTypedArray(),
            "status" to (
                command.åpneBehandlingerFiltrering.status?.map { it.toString() }
                    ?: BehandlingssammendragStatus.entries.map { it.toString() }
                ).toTypedArray(),
            "benktype" to (
                command.åpneBehandlingerFiltrering.benktype?.map { it.toString() }
                    ?: BehandlingssammendragBenktype.entries.map { it.toString() }
                ).toTypedArray(),
            "identer" to command.åpneBehandlingerFiltrering.identer?.toTypedArray(),
        )
    }

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
                        åpneSøknaderUtenBehandling as (${BenkSpørringer.ÅPNE_SØKNADER_UTEN_BEHANDLING}),
                        åpneSøknadsbehandlinger as (${BenkSpørringer.ÅPNE_SØKNADSBEHANDLINGER}),
                        åpneRevurderinger as (${BenkSpørringer.ÅPNE_REVURDERINGER}),
                        åpneMeldekortBehandlinger as (${BenkSpørringer.ÅPNE_MELDEKORTBEHANDLINGER}),
                        åpneMeldekortTilBehandling as (${BenkSpørringer.ÅPNE_MELDEKORT_TIL_BEHANDLING}),
                        åpneKlager as (${BenkSpørringer.ÅPNE_KLAGER}),
                        slåttSammen as (
                            select * from åpneSøknaderUtenBehandling
                            union all
                            select * from åpneSøknadsbehandlinger
                            union all
                            select * from åpneRevurderinger
                            union all
                            select * from åpneMeldekortBehandlinger
                            union all
                            select * from åpneMeldekortTilBehandling
                            union all
                            select * from åpneKlager
                        )
                    select *,
                        count(*) over () as total_count
                    from slåttSammen
                    where behandlingstype = any (:behandlingstype)
                      and status = any (:status)
                      and (
                        case
                          when :identer::text[] is null then true
                          when :identer::text[] = array['$IKKE_TILDELT']::text[] then (saksbehandler is null or beslutter is null)
                          else (saksbehandler = any(:identer::text[]) or beslutter = any(:identer::text[]))
                        end
                      )
                    and (
                      :benktype::text[] is null
                      or (
                        ('KLAR' = any(:benktype::text[]) and (erSattPåVent is null or erSattPåVent = false))
                        or
                        ('VENTER' = any(:benktype::text[]) and erSattPåVent = true)
                      )
                    )
                    order by ${command.sortering.kolonne.toDbString()} ${command.sortering.retning.toDbString()} nulls last
                    limit :limit;
                    """.trimIndent(),
                    mapQueryParams(command, limit),
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
                    val erSattPåVent = row.booleanOrNull("erSattPåVent") ?: false
                    val sattPåVentBegrunnelse = row.stringOrNull("sattPåVentBegrunnelse")
                    val sattPåVentFrist = row.localDateOrNull("sattPåVentFrist")

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
                            sattPåVentBegrunnelse = sattPåVentBegrunnelse,
                            sattPåVentFrist = sattPåVentFrist,
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
    KLAGEBEHANDLING,
    ;

    fun toDomain(): BehandlingssammendragType = when (this) {
        SØKNADSBEHANDLING -> BehandlingssammendragType.SØKNADSBEHANDLING
        REVURDERING -> BehandlingssammendragType.REVURDERING
        MELDEKORTBEHANDLING -> BehandlingssammendragType.MELDEKORTBEHANDLING
        INNSENDT_MELDEKORT -> BehandlingssammendragType.INNSENDT_MELDEKORT
        KORRIGERT_MELDEKORT -> BehandlingssammendragType.KORRIGERT_MELDEKORT
        KLAGEBEHANDLING -> BehandlingssammendragType.KLAGEBEHANDLING
    }
}

fun BenkSorteringKolonne.toDbString(): String =
    when (this) {
        BenkSorteringKolonne.STARTET -> "startet"
        BenkSorteringKolonne.SIST_ENDRET -> "sist_endret"
        BenkSorteringKolonne.FRIST -> "sattPåVentFrist"
    }

fun SorteringRetning.toDbString(): String =
    when (this) {
        SorteringRetning.ASC -> "ASC"
        SorteringRetning.DESC -> "DESC"
    }

fun String.toBehandlingssammendragStatus(): BehandlingssammendragStatus =
    BehandlingssammendragStatus.valueOf(this)
