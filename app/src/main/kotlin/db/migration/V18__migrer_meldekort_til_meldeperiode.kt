@file:Suppress("ClassName", "unused")

package db.migration

import kotliquery.Row
import mu.KotlinLogging
import no.nav.tiltakspenger.felles.HendelseId
import no.nav.tiltakspenger.felles.Hendelsesversjon
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.SessionCounter
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandlingStatus
import no.nav.tiltakspenger.meldekort.domene.MeldeperiodeBeregningDag
import no.nav.tiltakspenger.vedtak.repository.meldekort.toIkkeUtfyltMeldekortperiode
import no.nav.tiltakspenger.vedtak.repository.meldekort.toMeldekortStatus
import no.nav.tiltakspenger.vedtak.repository.meldekort.toUtfyltMeldekortperiode
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import java.time.LocalDate
import java.time.LocalDateTime

class V18__migrer_meldekort_til_meldeperiode : BaseJavaMigration() {
    val logger = KotlinLogging.logger { }

    @Throws(Exception::class)
    override fun migrate(context: Context) {
        val dataSource = context.configuration.dataSource
        val sessionFactory = PostgresSessionFactory(dataSource, SessionCounter(logger))

        val småMeldekort = sessionFactory.withSession { session ->
            session.run(
                sqlQuery(
                    """
                    select
                        m.*,
                        s.ident as fnr,
                        s.saksnummer,
                        (b.stønadsdager -> 'registerSaksopplysning' ->> 'antallDager')::int as antall_dager_per_meldeperiode
                    from meldekort m
                    join sak s on s.id = m.sak_id
                    join rammevedtak r on r.id = m.rammevedtak_id
                    join behandling b on b.id = r.behandling_id
                """,
                ).map { row ->
                    fromRow(row)
                }.asList,
            )
        }

        logger.info { "Fant ${småMeldekort.size} meldekortbehandlinger for migrering" }

        småMeldekort.forEach { meldekortLiten ->
            val status = meldekortLiten.status
            val meldekortDager = meldekortLiten.meldekortDager
            val sakId = meldekortLiten.sakId
            val meldekortId = meldekortLiten.meldekortId
            val maksDagerMedTiltakspengerForPeriode = meldekortLiten.maksDagerMedTiltakspengerForPeriode
            val meldeperiodeId = meldekortLiten.meldeperiodeId
            val fraOgMed = meldekortLiten.fraOgMed
            val tilOgMed = meldekortLiten.tilOgMed
            val opprettet = meldekortLiten.opprettet

            val hendelseId = HendelseId.random().toString()

            val beregning = when (status) {
                MeldekortBehandlingStatus.GODKJENT, MeldekortBehandlingStatus.KLAR_TIL_BESLUTNING -> {
                    meldekortDager.toUtfyltMeldekortperiode(
                        sakId = sakId,
                        meldekortId = meldekortId,
                        maksDagerMedTiltakspengerForPeriode = maksDagerMedTiltakspengerForPeriode,
                    )
                }

                MeldekortBehandlingStatus.IKKE_BEHANDLET, MeldekortBehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER -> {
                    meldekortDager.toIkkeUtfyltMeldekortperiode(
                        sakId = sakId,
                        meldekortId = meldekortId,
                        maksDagerMedTiltakspengerForPeriode = maksDagerMedTiltakspengerForPeriode,
                    )
                }

                else -> throw IllegalStateException("Ukjent meldekortstatus $status for meldekort $meldekortId")
            }

            val girRett = beregning.dager.map {
                it.dato to (it !is MeldeperiodeBeregningDag.Utfylt.Sperret)
            }.toMap().toDbJson()

            sessionFactory.withSession { session ->
                session.run(
                    sqlQuery(
                        """
                        insert into meldeperiode (
                            id,
                            versjon,
                            hendelse_id,
                            sak_id,
                            opprettet,
                            fra_og_med,
                            til_og_med,
                            antall_dager_for_periode,
                            gir_rett
                        ) values (
                            :id,
                            :versjon,
                            :hendelse_id,
                            :sak_id,
                            :opprettet,
                            :fra_og_med,
                            :til_og_med,
                            :antall_dager_for_periode,
                            to_jsonb(:gir_rett::jsonb)
                        )
                    """,
                        "id" to meldeperiodeId.toString(),
                        "versjon" to Hendelsesversjon.ny().value,
                        "hendelse_id" to hendelseId,
                        "sak_id" to sakId.toString(),
                        "opprettet" to opprettet,
                        "fra_og_med" to fraOgMed,
                        "til_og_med" to tilOgMed,
                        "antall_dager_for_periode" to maksDagerMedTiltakspengerForPeriode,
                        "gir_rett" to girRett,
                    ).asUpdate,
                )

                session.run(
                    sqlQuery(
                        """
                        update meldekort
                            set meldeperiode_hendelse_id = :meldeperiode_hendelse_id
                        where id = :meldekort_id
                        """,
                        "meldeperiode_hendelse_id" to hendelseId,
                        "meldekort_id" to meldekortId.toString(),
                    ).asUpdate,
                )
            }
        }

        val medDefaultHendelseId = sessionFactory.withSession { session ->
            session.run(
                sqlQuery(
                    """
                    select
                        *
                    from meldekort m
                    where meldeperiode_hendelse_id = 'defaultid'
                """,
                ).map { it }.asList,
            )
        }

        if (medDefaultHendelseId.isNotEmpty()) {
            throw IllegalStateException("Skal ikke ha meldekort med default hendelseid! (fant ${medDefaultHendelseId.size})")
        }
    }
}

private fun fromRow(row: Row): MeldekortLiten {
    return MeldekortLiten(
        meldekortId = MeldekortId.fromString(row.string("id")),
        sakId = SakId.fromString(row.string("sak_id")),
        meldeperiodeId = MeldeperiodeId(row.string("meldeperiode_id")),
        maksDagerMedTiltakspengerForPeriode = row.int("antall_dager_per_meldeperiode"),
        opprettet = row.localDateTime("opprettet"),
        status = row.string("status").toMeldekortStatus(),
        fraOgMed = row.localDate("fra_og_med"),
        tilOgMed = row.localDate("til_og_med"),
        meldekortDager = row.string("meldekortdager"),
    )
}

private data class MeldekortLiten(
    val meldekortId: MeldekortId,
    val sakId: SakId,
    val meldeperiodeId: MeldeperiodeId,
    val maksDagerMedTiltakspengerForPeriode: Int,
    val opprettet: LocalDateTime,
    val status: MeldekortBehandlingStatus,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val meldekortDager: String,
)

private fun Map<LocalDate, Boolean>.toDbJson(): String {
    return entries.joinToString(
        prefix = "{",
        postfix = "}",
        separator = ",",
    ) { (date, value) ->
        "\"${date}\": $value"
    }
}
