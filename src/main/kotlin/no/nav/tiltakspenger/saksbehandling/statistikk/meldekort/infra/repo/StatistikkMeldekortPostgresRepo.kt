package no.nav.tiltakspenger.saksbehandling.statistikk.meldekort.infra.repo

import kotliquery.Session
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.saksbehandling.statistikk.meldekort.StatistikkMeldekortDTO
import no.nav.tiltakspenger.saksbehandling.statistikk.meldekort.StatistikkMeldekortDTO.StatistikkMeldekortDag
import no.nav.tiltakspenger.saksbehandling.statistikk.meldekort.StatistikkMeldekortDTO.StatistikkMeldeperiode
import no.nav.tiltakspenger.saksbehandling.statistikk.meldekort.infra.repo.StatistikkMeldeperiodeDbJson.MeldekortdagDbJson
import org.intellij.lang.annotations.Language
import java.time.Clock
import java.time.LocalDate

object StatistikkMeldekortPostgresRepo {
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
                    "sak_id" to dto.sakId,
                    "meldekortbehandling_id" to dto.meldekortbehandlingId,
                    "bruker_id" to dto.brukerId,
                    "saksnummer" to dto.saksnummer,
                    "vedtatt_tidspunkt" to dto.vedtattTidspunkt,
                    "behandlet_automatisk" to dto.behandletAutomatisk,
                    "fra_og_med" to dto.fraOgMed,
                    "til_og_med" to dto.tilOgMed,
                    "meldeperioder" to dto.meldeperioder.tilMeldeperioderDbJson().let { serialize(it) },
                    "opprettet" to dto.opprettet,
                    "sist_endret" to dto.sistEndret,
                ),
            ).asUpdate,
        )
    }
}

@Language("SQL")
private val lagreMeldekortSql =
    """
        insert into statistikk_meldekort (
        sak_id,
        meldekortbehandling_id,
        bruker_id,
        saksnummer,
        vedtatt_tidspunkt,
        behandlet_automatisk,
        fra_og_med,
        til_og_med,
        meldeperioder,
        opprettet,
        sist_endret
        ) values (
        :sak_id,
        :meldekortbehandling_id,
        :bruker_id,
        :saksnummer,
        :vedtatt_tidspunkt,
        :behandlet_automatisk,
        :fra_og_med,
        :til_og_med,
        :meldeperioder::jsonb,
        :opprettet,
        :sist_endret
        ) on conflict (meldekortbehandling_id) do update set
        sak_id = :sak_id,
        vedtatt_tidspunkt = :vedtatt_tidspunkt,
        behandlet_automatisk = :behandlet_automatisk,
        fra_og_med = :fra_og_med,
        til_og_med = :til_og_med,
        meldeperioder = :meldeperioder::jsonb,
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
