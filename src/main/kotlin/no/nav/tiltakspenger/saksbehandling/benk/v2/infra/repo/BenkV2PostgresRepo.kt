package no.nav.tiltakspenger.saksbehandling.benk.v2.infra.repo

import kotliquery.Row
import kotliquery.Session
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkKlageFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkKlageKolonne
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkKlagebehandling
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkKlagebehandlingResultat
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkMeldekort
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkMeldekortFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkMeldekortKolonne
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkMeldekortType
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkRevurdering
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkRevurderingResultat
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkRevurderingerFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkRevurderingerKolonne
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkSøknaderFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkSøknaderKolonne
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkSøknadsbehandlingResultat
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkSøknadstype
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkTilbakekreving
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkTilbakekrevingFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkTilbakekrevingKilde
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkTilbakekrevingKolonne
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkTilbakekrevingStatus
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkV2AntallPerFane
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkV2Behandling
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkV2Behandlingsfelles
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkV2Behandlingsstatus
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkV2Filtrering
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkV2Oversikt
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkV2Repo
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkV2Sortering
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkV2SorteringKolonne
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkV2Ventestatus
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.HentBenkV2Kommando
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.periode
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingId
import org.intellij.lang.annotations.Language
import java.time.LocalDate

/**
 * Spørringene bak benk v2.
 *
 * Hver fane er én base-spørring med et fast sett fellesfelter pluss sine egne kolonner.
 * Filtrene er null-safe i sql (`:param is null or ...`), slik at kotlin-siden slipper å bygge sql-fragmenter betinget.
 * Sorteringen går gjennom [toDbString], slik at et kolonnenavn aldri kommer rått fra en request og inn i spørringen.
 *
 * Parameternavnene er rene ascii, selv der feltet de fyller heter noe med æøå.
 * Kotliquery kjenner igjen et navngitt parameter med `(?<!:):(?!:)[a-zA-Z]\w+`, og `\w` dekker ikke æøå — `:søknadstype` ville blitt stående uerstattet i sql-en.
 *
 * Hvert fanekall er to spørringer: én som gir begge totalene, og én som gir radene.
 * Alternativet — window-funksjoner i radspørringen — mister det ufiltrerte totalet når filteret ikke treffer noe, og krever en fallback.
 */
class BenkV2PostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : BenkV2Repo {

    override fun hentSøknader(
        command: HentBenkV2Kommando<BenkSøknaderFiltrering, BenkSøknaderKolonne>,
        sessionContext: SessionContext?,
        limit: Int,
    ): BenkV2Oversikt<BenkSøknadsbehandling> = sessionFactory.withSession(sessionContext) { session ->
        session.hentFane(
            base = SØKNADER,
            filterSql = SØKNADER_FILTER,
            params = command.filtrering.tilParams() + arrayOf(
                "status" to command.filtrering.status.tilParam(),
                "soknadstype" to command.filtrering.søknadstype.tilParam(),
            ),
            sortering = command.sortering.tilOrderBy { it.toDbString() },
            limit = limit,
            map = { it.tilSøknadsbehandling() },
        )
    }

    override fun hentRevurderinger(
        command: HentBenkV2Kommando<BenkRevurderingerFiltrering, BenkRevurderingerKolonne>,
        sessionContext: SessionContext?,
        limit: Int,
    ): BenkV2Oversikt<BenkRevurdering> = sessionFactory.withSession(sessionContext) { session ->
        session.hentFane(
            base = REVURDERINGER,
            filterSql = REVURDERINGER_FILTER,
            params = command.filtrering.tilParams() + arrayOf(
                "status" to command.filtrering.status.tilParam(),
                "resultat" to command.filtrering.resultat.tilParam(),
            ),
            sortering = command.sortering.tilOrderBy { it.toDbString() },
            limit = limit,
            map = { it.tilRevurdering() },
        )
    }

    override fun hentMeldekort(
        command: HentBenkV2Kommando<BenkMeldekortFiltrering, BenkMeldekortKolonne>,
        sessionContext: SessionContext?,
        limit: Int,
    ): BenkV2Oversikt<BenkMeldekort> = sessionFactory.withSession(sessionContext) { session ->
        session.hentFane(
            base = MELDEKORT,
            filterSql = MELDEKORT_FILTER,
            params = command.filtrering.tilParams() + arrayOf(
                "status" to command.filtrering.status.tilParam(),
                "type" to command.filtrering.type.tilParam(),
            ),
            sortering = command.sortering.tilOrderBy { it.toDbString() },
            limit = limit,
            map = { it.tilMeldekort() },
        )
    }

    override fun hentKlager(
        command: HentBenkV2Kommando<BenkKlageFiltrering, BenkKlageKolonne>,
        sessionContext: SessionContext?,
        limit: Int,
    ): BenkV2Oversikt<BenkKlagebehandling> = sessionFactory.withSession(sessionContext) { session ->
        session.hentFane(
            base = KLAGE,
            filterSql = KLAGE_FILTER,
            params = command.filtrering.tilParams() + arrayOf(
                "status" to command.filtrering.status.tilParam(),
                "resultat" to command.filtrering.resultat.tilParam(),
            ),
            sortering = command.sortering.tilOrderBy { it.toDbString() },
            limit = limit,
            map = { it.tilKlagebehandling() },
        )
    }

    override fun hentTilbakekrevinger(
        command: HentBenkV2Kommando<BenkTilbakekrevingFiltrering, BenkTilbakekrevingKolonne>,
        sessionContext: SessionContext?,
        limit: Int,
    ): BenkV2Oversikt<BenkTilbakekreving> = sessionFactory.withSession(sessionContext) { session ->
        session.hentFane(
            base = TILBAKEKREVING,
            filterSql = TILBAKEKREVING_FILTER,
            params = command.filtrering.tilParams() + arrayOf(
                "status" to command.filtrering.status.tilParam(),
                "kilde" to command.filtrering.kilde.tilParam(),
                "minstebelop" to command.filtrering.minstebeløp,
            ),
            sortering = command.sortering.tilOrderBy { it.toDbString() },
            limit = limit,
            map = { it.tilTilbakekreving() },
        )
    }

    /**
     * `!!` er trygt: spørringen er en ren aggregering uten `from`-tabell, og gir alltid nøyaktig én rad — også når alle fanene er tomme.
     */
    override fun hentAntallPerFane(sessionContext: SessionContext?): BenkV2AntallPerFane =
        sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    select
                        (select count(*) from ($SØKNADER) s)        as søknader,
                        (select count(*) from ($REVURDERINGER) r)   as revurderinger,
                        (select count(*) from ($MELDEKORT) m)       as meldekort,
                        (select count(*) from ($KLAGE) k)           as klage,
                        (select count(*) from ($TILBAKEKREVING) t)  as tilbakekreving
                    """.trimIndent(),
                ).map {
                    BenkV2AntallPerFane(
                        søknader = it.int("søknader"),
                        revurderinger = it.int("revurderinger"),
                        meldekort = it.int("meldekort"),
                        klage = it.int("klage"),
                        tilbakekreving = it.int("tilbakekreving"),
                    )
                }.asSingle,
            )!!
        }

    /**
     * Kjører de to spørringene én fane består av.
     *
     * `!!` på tellespørringen er trygt: den er en ren aggregering uten `from`-tabell, og gir alltid nøyaktig én rad.
     */
    private fun <T : BenkV2Behandling> Session.hentFane(
        @Language("PostgreSQL") base: String,
        filterSql: String,
        params: Array<Pair<String, Any?>>,
        sortering: String,
        limit: Int,
        map: (Row) -> T,
    ): BenkV2Oversikt<T> {
        val totaler = run(
            sqlQuery(
                """
                select
                    (select count(*) from ($base) ufiltrert)                     as total_ufiltrert,
                    (select count(*) from ($base) filtrert where $filterSql)     as total_filtrert
                """.trimIndent(),
                *params,
            ).map { it.int("total_ufiltrert") to it.int("total_filtrert") }.asSingle,
        )!!

        val behandlinger = run(
            sqlQuery(
                """
                select * from ($base) fane
                where $filterSql
                order by $sortering
                limit :limit
                """.trimIndent(),
                *params,
                "limit" to limit,
            ).map(map).asList,
        )

        return BenkV2Oversikt(
            behandlinger = behandlinger,
            totalAntall = totaler.second,
            totalAntallUfiltrert = totaler.first,
        )
    }

    private companion object {

        /**
         * Fellesfeltene hver fane må produsere, i tillegg til sine egne.
         * Aliasene er kontrakten mellom spørringene og [tilFelles].
         *
         * Søknadsfanen viser bare åpne søknadsbehandlinger.
         * Søknader som ingen har tatt tak i ennå, er ikke behandlinger og hører ikke hjemme på benken.
         */
        @Language("PostgreSQL")
        const val SØKNADER = """
            select
                sa.id                       as sak_id,
                sa.fnr                      as fnr,
                sa.saksnummer               as saksnummer,
                b.opprettet                 as startet,
                b.sist_endret               as sist_endret,
                b.saksbehandler             as saksbehandler,
                b.beslutter                 as beslutter,
                coalesce(b.attesteringer->'attesteringer'->-1->>'status' = 'SENDT_TILBAKE', false) as er_underkjent,
                coalesce((b.ventestatus->'ventestatusHendelser'->-1->>'erSattPåVent')::boolean, false) as er_satt_på_vent,
                b.ventestatus->'ventestatusHendelser'->-1->>'begrunnelse'   as vente_begrunnelse,
                (b.ventestatus->'ventestatusHendelser'->-1->>'frist')::date as vente_frist,
                b.status::text              as status,
                sø.soknadstype::text        as søknadstype,
                sø.opprettet                as kravtidspunkt,
                b.resultat::text            as resultat,
                b.id                        as id
            from behandling b
                join søknad sø on sø.id = b.soknad_id
                join sak sa on b.sak_id = sa.id
            where b.avbrutt is null
              and b.behandlingstype = 'SØKNADSBEHANDLING'
              and b.status in ('UNDER_AUTOMATISK_BEHANDLING', 'KLAR_TIL_BEHANDLING', 'UNDER_BEHANDLING',
                               'KLAR_TIL_BESLUTNING', 'UNDER_BESLUTNING')
        """

        @Language("PostgreSQL")
        const val REVURDERINGER = """
            select
                sa.id                       as sak_id,
                sa.fnr                      as fnr,
                sa.saksnummer               as saksnummer,
                b.opprettet                 as startet,
                b.sist_endret               as sist_endret,
                b.saksbehandler             as saksbehandler,
                b.beslutter                 as beslutter,
                coalesce(b.attesteringer->'attesteringer'->-1->>'status' = 'SENDT_TILBAKE', false) as er_underkjent,
                coalesce((b.ventestatus->'ventestatusHendelser'->-1->>'erSattPåVent')::boolean, false) as er_satt_på_vent,
                b.ventestatus->'ventestatusHendelser'->-1->>'begrunnelse'   as vente_begrunnelse,
                (b.ventestatus->'ventestatusHendelser'->-1->>'frist')::date as vente_frist,
                b.status::text              as status,
                b.resultat::text            as resultat,
                b.id                        as id
            from behandling b
                join sak sa on b.sak_id = sa.id
            where b.avbrutt is null
              and b.behandlingstype = 'REVURDERING'
              and b.status in ('KLAR_TIL_BEHANDLING', 'UNDER_BEHANDLING', 'KLAR_TIL_BESLUTNING', 'UNDER_BESLUTNING')
        """

        /**
         * Meldekortfanen samler to kilder: meldekortbehandlingene saksbehandler har startet, og meldekortene fra bruker som ingen har tatt tak i.
         * Beløpet finnes bare for behandlinger som er beregnet, og summeres ut av `beregninger`-jsonb-en.
         */
        @Language("PostgreSQL")
        const val MELDEKORT = """
            select
                s.id                            as sak_id,
                s.fnr                           as fnr,
                s.saksnummer                    as saksnummer,
                m.opprettet                     as startet,
                m.sist_endret                   as sist_endret,
                m.saksbehandler                 as saksbehandler,
                m.beslutter                     as beslutter,
                coalesce(m.attesteringer->'attesteringer'->-1->>'status' = 'SENDT_TILBAKE', false) as er_underkjent,
                coalesce((m.ventestatus->'ventestatusHendelser'->-1->>'erSattPåVent')::boolean, false) as er_satt_på_vent,
                m.ventestatus->'ventestatusHendelser'->-1->>'begrunnelse'   as vente_begrunnelse,
                (m.ventestatus->'ventestatusHendelser'->-1->>'frist')::date as vente_frist,
                m.status::text                  as status,
                'MELDEKORTBEHANDLING'::text     as type,
                (
                    select jsonb_agg(el ->> 'kjedeId' order by el ->> 'kjedeId')
                    from jsonb_array_elements(m.meldeperioder) el
                )                               as meldeperioder,
                (
                    select sum(
                        (dag->'beregningsdag'->>'beløp')::int
                        + coalesce((dag->'beregningsdag'->>'beløpBarnetillegg')::int, 0)
                    )::int
                    from jsonb_array_elements(m.beregninger->'beregninger') beregning,
                         jsonb_array_elements(beregning->'dager') dag
                )                               as beløp,
                null::timestamp with time zone  as mottatt_tidspunkt,
                m.id                            as id
            from meldekortbehandling m
                join sak s on m.sak_id = s.id
            where m.avbrutt is null
              and m.status in ('KLAR_TIL_BEHANDLING', 'UNDER_BEHANDLING', 'KLAR_TIL_BESLUTNING', 'UNDER_BESLUTNING')
            union all
            select
                s.id,
                s.fnr,
                s.saksnummer,
                siste.mottatt,
                siste.mottatt,
                null::text,
                null::text,
                false,
                false,
                null::text,
                null::date,
                'KLAR_TIL_BEHANDLING'::text,
                case when exists (
                    select 1 from meldekort_bruker tidligere
                    where tidligere.sak_id = siste.sak_id
                      and tidligere.meldeperiode_kjede_id = siste.meldeperiode_kjede_id
                      and tidligere.id != siste.id
                ) then 'KORRIGERT_MELDEKORT'::text else 'INNSENDT_MELDEKORT'::text end,
                jsonb_build_array(mp.kjede_id),
                null::int,
                siste.mottatt,
                siste.id
            from (
                select distinct on (sak_id, meldeperiode_kjede_id)
                    id, sak_id, meldeperiode_id, meldeperiode_kjede_id, mottatt
                from meldekort_bruker
                where behandlet_automatisk_status != 'BEHANDLET' and behandles_automatisk = false
                order by sak_id, meldeperiode_kjede_id, mottatt desc
            ) siste
                join sak s on s.id = siste.sak_id
                join meldeperiode mp on mp.id = siste.meldeperiode_id
                /*
                 * Filtrerer bort meldekort der det allerede finnes en meldekortbehandling som er nyere enn eller samtidig med innsendingen.
                 * Da er meldekortet potensielt allerede tatt stilling til.
                 *
                 * Skrevet som en lateral framfor `not exists` for å tvinge ett indeksoppslag per meldekort.
                 * Som anti-join la planleggeren en hash over hele meldekortbehandling — inkludert all historikk — fordi `@>` ikke kan være en hash-condition.
                 * Målt mot Postgres 17 ved 50k meldekortbehandlinger: 21ms som anti-join, 7ms som lateral.
                 * sak_id-betingelsen er bærende: kjedeId er bare datointervallet, og er ikke unik på tvers av saker.
                 * Det telles framfor `limit 1` + null-sjekk fordi IDE-analysen feilaktig flagger null-sjekken som alltid false — den modellerer ikke null-paddingen fra left join.
                 */
                left join lateral (
                    select count(*) as antall
                    from meldekortbehandling mb
                    where mb.sak_id = siste.sak_id
                      and mb.meldeperioder @> jsonb_build_array(jsonb_build_object('kjedeId', siste.meldeperiode_kjede_id))
                      and mb.sist_endret >= siste.mottatt
                ) behandlet on true
            where behandlet.antall = 0
        """

        /**
         * Klagebehandling har ingen beslutter og ingen attesteringer, så de to fellesfeltene er konstante her.
         * Kravtidspunktet er innsendingsdatoen fra formkravene, som er en dato — den løftes til midnatt, slik at benken kan vise alle kravtidspunkt likt.
         */
        @Language("PostgreSQL")
        const val KLAGE = """
            select
                k.sak_id                    as sak_id,
                s.fnr                       as fnr,
                s.saksnummer                as saksnummer,
                k.opprettet                 as startet,
                k.sist_endret               as sist_endret,
                k.saksbehandler             as saksbehandler,
                null::text                  as beslutter,
                false                       as er_underkjent,
                coalesce((k.ventestatus->'ventestatusHendelser'->-1->>'erSattPåVent')::boolean, false) as er_satt_på_vent,
                k.ventestatus->'ventestatusHendelser'->-1->>'begrunnelse'   as vente_begrunnelse,
                (k.ventestatus->'ventestatusHendelser'->-1->>'frist')::date as vente_frist,
                case when k.status = 'MOTTATT_FRA_KLAGEINSTANS'
                    then 'KLAR_TIL_FERDIGSTILLING'::text
                    else k.status::text
                end                         as status,
                coalesce(
                    (k.formkrav->>'innsendingsdato')::date::timestamp with time zone,
                    k.opprettet
                )                           as kravtidspunkt,
                k.resultat->>'type'         as resultat,
                k.id                        as id
            from klagebehandling k
                join sak s on k.sak_id = s.id
            where k.status in ('KLAR_TIL_BEHANDLING', 'UNDER_BEHANDLING', 'MOTTATT_FRA_KLAGEINSTANS')
        """

        /**
         * Tilbakekreving har egen flyt og egen status.
         * Statusen som lagres sier hva behandlingen venter på; benken skiller i tillegg på om noen har tatt den, og utleder derfor `UNDER_*` her.
         *
         * `startet` er utbetalingens opprettelsestidspunkt, fordi tilbakekrevingsbehandlingene får nesten identiske tidspunkt av batchingen.
         */
        @Language("PostgreSQL")
        const val TILBAKEKREVING = """
            select
                tb.sak_id                       as sak_id,
                s.fnr                           as fnr,
                s.saksnummer                    as saksnummer,
                u.opprettet                     as startet,
                tb.sist_endret                  as sist_endret,
                tb.saksbehandler_ident          as saksbehandler,
                tb.beslutter_ident              as beslutter,
                false                           as er_underkjent,
                (tb.venter is not null)         as er_satt_på_vent,
                tb.venter->>'grunn'             as vente_begrunnelse,
                (tb.venter->>'gjenopptas')::date as vente_frist,
                case
                    when tb.status = 'OPPRETTET' then 'OPPRETTET'
                    when tb.status = 'TIL_FORHÅNDSVARSEL' and tb.saksbehandler_ident is not null then 'UNDER_FORHÅNDSVARSLING'
                    when tb.status = 'TIL_FORHÅNDSVARSEL' then 'TIL_FORHÅNDSVARSEL'
                    when tb.status = 'TIL_BEHANDLING' and tb.saksbehandler_ident is not null then 'UNDER_BEHANDLING'
                    when tb.status = 'TIL_BEHANDLING' then 'TIL_BEHANDLING'
                    when tb.beslutter_ident is not null then 'UNDER_GODKJENNING'
                    else 'TIL_GODKJENNING'
                end                             as status,
                tb.totalt_feilutbetalt_beløp    as beløp,
                case when u.rammevedtak_id is not null
                    then 'RAMMEVEDTAK'::text
                    else 'MELDEKORT'::text
                end                             as kilde,
                tb.kravgrunnlag_periode         as kravgrunnlag_periode,
                tb.id                           as id,
                tb.url                          as url
            from tilbakekreving_behandling tb
                join sak s on tb.sak_id = s.id
                join utbetaling u on tb.utbetaling_id = u.id
            where tb.status in ('OPPRETTET', 'TIL_FORHÅNDSVARSEL', 'TIL_BEHANDLING', 'TIL_GODKJENNING')
        """

        /**
         * Én nedtrekksliste på benken dekker både saksbehandler og beslutter, så filteret treffer begge.
         * `IKKE_TILDELT` betyr at ingen har plukket opp behandlingen, altså at saksbehandler er tom.
         */
        const val SAKSBEHANDLER_FILTER = """
            (
                :saksbehandler::text is null
                or (:saksbehandler::text = 'IKKE_TILDELT' and saksbehandler is null)
                or (:saksbehandler::text <> 'IKKE_TILDELT'
                    and (saksbehandler = :saksbehandler::text or beslutter = :saksbehandler::text))
            )
        """

        /**
         * Alle basespørringene produserer `er_satt_på_vent`, så filteret er felles.
         */
        const val PÅ_VENT_FILTER = "(not :skjul_pa_vent or not er_satt_på_vent)"

        const val SØKNADER_FILTER = """
            (:status::text is null or status = :status::text)
            and (:soknadstype::text is null or søknadstype = :soknadstype::text)
            and $SAKSBEHANDLER_FILTER
            and $PÅ_VENT_FILTER
        """

        const val REVURDERINGER_FILTER = """
            (:status::text is null or status = :status::text)
            and (:resultat::text is null or resultat = :resultat::text)
            and $SAKSBEHANDLER_FILTER
            and $PÅ_VENT_FILTER
        """

        const val MELDEKORT_FILTER = """
            (:status::text is null or status = :status::text)
            and (:type::text is null or type = :type::text)
            and $SAKSBEHANDLER_FILTER
            and $PÅ_VENT_FILTER
        """

        const val KLAGE_FILTER = """
            (:status::text is null or status = :status::text)
            and (:resultat::text is null or resultat = :resultat::text)
            and $SAKSBEHANDLER_FILTER
            and $PÅ_VENT_FILTER
        """

        const val TILBAKEKREVING_FILTER = """
            (:status::text is null or status = :status::text)
            and (:kilde::text is null or kilde = :kilde::text)
            and beløp >= :minstebelop
            and $SAKSBEHANDLER_FILTER
            and $PÅ_VENT_FILTER
        """
    }
}

/**
 * Sorteringen er alltid `nulls last`: en rad uten verdi i kolonnen det sorteres på, hører nederst uansett retning.
 * Kolonnen sorteres sekundært på `sak_id`, slik at to rader med lik verdi kommer i samme rekkefølge hver gang — uten det ville pagineringen i praksis vært ustabil.
 */
private fun <K : BenkV2SorteringKolonne> BenkV2Sortering<K>.tilOrderBy(kolonneTilDb: (K) -> String): String =
    "${kolonneTilDb(kolonne)} ${retning.toDbString()} nulls last, sak_id asc"

/**
 * Null-sjekken bor i én funksjon framfor på hvert kallsted, slik at den er ett sted å lese og ett sted å teste.
 */
private fun Enum<*>?.tilParam(): String? = this?.name

private fun BenkV2Filtrering.tilParams(): Array<Pair<String, Any?>> = arrayOf(
    "saksbehandler" to saksbehandler,
    "skjul_pa_vent" to skjulPåVent,
)

private fun <T : Enum<T>> Row.enumOrNull(column: String, entries: List<T>): T? {
    val verdi = stringOrNull(column) ?: return null
    return entries.associateBy { it.name }.getValue(verdi)
}

private fun <T : Enum<T>> Row.enum(column: String, entries: List<T>): T =
    entries.associateBy { it.name }.getValue(string(column))

private fun Row.tilFelles(): BenkV2Behandlingsfelles = BenkV2Behandlingsfelles(
    sakId = SakId.fromString(string("sak_id")),
    fnr = Fnr.fromString(string("fnr")),
    saksnummer = Saksnummer(string("saksnummer")),
    startet = localDateTime("startet"),
    sistEndret = localDateTime("sist_endret"),
    saksbehandler = stringOrNull("saksbehandler"),
    beslutter = stringOrNull("beslutter"),
    erUnderkjent = boolean("er_underkjent"),
    ventestatus = BenkV2Ventestatus(
        erSattPåVent = boolean("er_satt_på_vent"),
        begrunnelse = stringOrNull("vente_begrunnelse"),
        frist = localDateOrNull("vente_frist"),
    ),
)

private fun Row.tilBehandlingsstatus(): BenkV2Behandlingsstatus =
    enum("status", BenkV2Behandlingsstatus.entries)

private fun Row.tilSøknadsbehandling(): BenkSøknadsbehandling = BenkSøknadsbehandling(
    felles = tilFelles(),
    id = RammebehandlingId.fromString(string("id")),
    status = tilBehandlingsstatus(),
    søknadstype = enum("søknadstype", BenkSøknadstype.entries),
    kravtidspunkt = localDateTime("kravtidspunkt"),
    resultat = enumOrNull("resultat", BenkSøknadsbehandlingResultat.entries),
)

private fun Row.tilRevurdering(): BenkRevurdering = BenkRevurdering(
    felles = tilFelles(),
    id = RammebehandlingId.fromString(string("id")),
    status = tilBehandlingsstatus(),
    resultat = enumOrNull("resultat", BenkRevurderingResultat.entries),
)

private fun Row.tilMeldekort(): BenkMeldekort = BenkMeldekort(
    felles = tilFelles(),
    id = MeldekortId.fromString(string("id")),
    status = tilBehandlingsstatus(),
    type = enum("type", BenkMeldekortType.entries),
    meldeperioder = tilMeldeperioder(),
    beløp = intOrNull("beløp"),
    mottattTidspunkt = localDateTimeOrNull("mottatt_tidspunkt"),
)

/**
 * Kjede-id-ene er periode-strenger på formatet «fraOgMed/tilOgMed», og parses direkte framfor å gå via MeldeperiodeKjedeId.
 * Init-blokken der krever fjorten dager fra mandag til søndag, og benken skal ikke feile på en fremtidig kjede som ikke følger det.
 */
private fun Row.tilMeldeperioder(): List<Periode> =
    deserialize<List<String>>(string("meldeperioder")).map { kjedeId ->
        val (fraOgMed, tilOgMed) = kjedeId.split("/")
        Periode(LocalDate.parse(fraOgMed), LocalDate.parse(tilOgMed))
    }

private fun Row.tilKlagebehandling(): BenkKlagebehandling = BenkKlagebehandling(
    felles = tilFelles(),
    id = KlagebehandlingId.fromString(string("id")),
    status = tilBehandlingsstatus(),
    kravtidspunkt = localDateTime("kravtidspunkt"),
    resultat = enumOrNull("resultat", BenkKlagebehandlingResultat.entries),
)

private fun Row.tilTilbakekreving(): BenkTilbakekreving = BenkTilbakekreving(
    felles = tilFelles(),
    id = TilbakekrevingId.fromString(string("id")),
    status = enum("status", BenkTilbakekrevingStatus.entries),
    beløp = bigDecimal("beløp"),
    kilde = enum("kilde", BenkTilbakekrevingKilde.entries),
    kravgrunnlagPeriode = periode("kravgrunnlag_periode"),
    url = string("url"),
)
