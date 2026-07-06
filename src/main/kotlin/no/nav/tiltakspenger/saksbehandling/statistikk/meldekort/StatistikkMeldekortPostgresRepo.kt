package no.nav.tiltakspenger.saksbehandling.statistikk.meldekort

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.json.deserializeList
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.saksbehandling.statistikk.meldekort.StatistikkMeldekortDTO.StatistikkMeldekortDag
import no.nav.tiltakspenger.saksbehandling.statistikk.meldekort.StatistikkMeldekortDTO.StatistikkMeldeperiode
import no.nav.tiltakspenger.saksbehandling.statistikk.meldekort.StatistikkMeldeperiodeDbJson.MeldekortdagDbJson
import org.intellij.lang.annotations.Language
import org.jetbrains.annotations.TestOnly
import java.time.Clock
import java.time.LocalDate

class StatistikkMeldekortPostgresRepo {

    companion object {
        fun oppdaterFnr(
            gammeltFnr: Fnr,
            nyttFnr: Fnr,
            clock: Clock,
            session: Session,
        ) {
            session.run(
                sqlQuery(
                    """
                        update statistikk_meldekort set
                            bruker_id = :nytt_fnr,
                            sist_endret = :sist_endret
                        where bruker_id = :gammelt_fnr
                    """.trimIndent(),
                    "nytt_fnr" to nyttFnr.verdi,
                    "gammelt_fnr" to gammeltFnr.verdi,
                    "sist_endret" to nå(clock),
                ).asUpdate,
            )
        }

        fun lagre(
            dto: StatistikkMeldekortDTO,
            session: Session,
        ) {
            session.run(
                queryOf(
                    lagreMeldekortSql,
                    mapOf(
                        "meldeperiode_kjede_id" to dto.meldeperiodeKjedeId,
                        "sak_id" to dto.sakId,
                        "meldekortbehandling_id" to dto.meldekortbehandlingId,
                        "bruker_id" to dto.brukerId,
                        "saksnummer" to dto.saksnummer,
                        "vedtatt_tidspunkt" to dto.vedtattTidspunkt,
                        "behandlet_automatisk" to dto.behandletAutomatisk,
                        "fra_og_med" to dto.fraOgMed,
                        "til_og_med" to dto.tilOgMed,
                        "meldekortdager" to dto.meldekortdager.tilMeldekortdagerDbJson().let { serialize(it) },
                        "meldeperioder" to dto.meldeperioder.tilMeldeperioderDbJson().let { serialize(it) },
                        "opprettet" to dto.opprettet,
                        "sist_endret" to dto.sistEndret,
                    ),
                ).asUpdate,
            )
        }

        /**
         * Kun for test. I produksjon leses denne tabellen kun av eksterne konsumenter.
         */
        @TestOnly
        fun hentForMeldekortbehandlingId(
            meldekortbehandlingId: String,
            session: Session,
        ): StatistikkMeldekortDTO? {
            return session.run(
                sqlQuery(
                    "select * from statistikk_meldekort where meldekortbehandling_id = :meldekortbehandling_id",
                    "meldekortbehandling_id" to meldekortbehandlingId,
                ).map { row -> row.toStatistikkMeldekortDTO() }.asSingle,
            )
        }

        private fun Row.toStatistikkMeldekortDTO(): StatistikkMeldekortDTO {
            return StatistikkMeldekortDTO(
                sakId = string("sak_id"),
                meldekortbehandlingId = string("meldekortbehandling_id"),
                brukerId = string("bruker_id"),
                saksnummer = string("saksnummer"),
                vedtattTidspunkt = localDateTime("vedtatt_tidspunkt"),
                behandletAutomatisk = boolean("behandlet_automatisk"),
                fraOgMed = localDate("fra_og_med"),
                tilOgMed = localDate("til_og_med"),
                opprettet = localDateTime("opprettet"),
                sistEndret = localDateTime("sist_endret"),
                meldeperioder = deserializeList<StatistikkMeldeperiodeDbJson>(string("meldeperioder")).tilStatistikkMeldeperioder(),
                meldeperiodeKjedeId = string("meldeperiode_kjede_id"),
                meldekortdager = deserializeList<MeldekortdagDbJson>(string("meldekortdager")).tilStatistikkMeldekortdager(),
            )
        }
    }
}

@Language("SQL")
private val lagreMeldekortSql =
    """
        insert into statistikk_meldekort (
        meldeperiode_kjede_id,
        sak_id,
        meldekortbehandling_id,
        bruker_id,
        saksnummer,
        vedtatt_tidspunkt,
        behandlet_automatisk,
        fra_og_med,
        til_og_med,
        meldekortdager,
        meldeperioder,
        opprettet,
        sist_endret
        ) values (
        :meldeperiode_kjede_id,
        :sak_id,
        :meldekortbehandling_id,
        :bruker_id,
        :saksnummer,
        :vedtatt_tidspunkt,
        :behandlet_automatisk,
        :fra_og_med,
        :til_og_med,
        to_jsonb(:meldekortdager::jsonb),
        to_jsonb(:meldeperioder::jsonb),
        :opprettet,
        :sist_endret
        ) on conflict (meldekortbehandling_id) do update set
        meldeperiode_kjede_id = :meldeperiode_kjede_id,
        sak_id = :sak_id,
        vedtatt_tidspunkt = :vedtatt_tidspunkt,
        behandlet_automatisk = :behandlet_automatisk,
        fra_og_med = :fra_og_med,
        til_og_med = :til_og_med,
        meldekortdager = to_jsonb(:meldekortdager::jsonb),
        meldeperioder = to_jsonb(:meldeperioder::jsonb),
        sist_endret = :sist_endret
    """.trimIndent()

private data class StatistikkMeldeperiodeDbJson(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val meldeperiodeKjedeId: String,
    val meldekortdager: List<MeldekortdagDbJson>,
) {
    data class MeldekortdagDbJson(
        val dato: LocalDate,
        val status: String,
        val reduksjon: String,
    )
}

private fun List<StatistikkMeldeperiode>.tilMeldeperioderDbJson(): List<StatistikkMeldeperiodeDbJson> {
    return this.map {
        StatistikkMeldeperiodeDbJson(
            fraOgMed = it.fraOgMed,
            tilOgMed = it.tilOgMed,
            meldeperiodeKjedeId = it.meldeperiodeKjedeId,
            meldekortdager = it.meldekortdager.tilMeldekortdagerDbJson(),
        )
    }
}

private fun List<StatistikkMeldekortDag>.tilMeldekortdagerDbJson(): List<MeldekortdagDbJson> {
    return this.map { dag ->
        MeldekortdagDbJson(
            dato = dag.dato,
            status = dag.status.name,
            reduksjon = dag.reduksjon.name,
        )
    }
}

private fun List<StatistikkMeldeperiodeDbJson>.tilStatistikkMeldeperioder(): List<StatistikkMeldeperiode> {
    return this.map {
        StatistikkMeldeperiode(
            fraOgMed = it.fraOgMed,
            tilOgMed = it.tilOgMed,
            meldeperiodeKjedeId = it.meldeperiodeKjedeId,
            meldekortdager = it.meldekortdager.tilStatistikkMeldekortdager(),
        )
    }
}

private fun List<MeldekortdagDbJson>.tilStatistikkMeldekortdager(): List<StatistikkMeldekortDag> {
    return this.map {
        StatistikkMeldekortDag(
            dato = it.dato,
            status = StatistikkMeldekortDag.MeldekortDagStatus.valueOf(it.status),
            reduksjon = StatistikkMeldekortDag.Reduksjon.valueOf(it.reduksjon),
        )
    }
}
