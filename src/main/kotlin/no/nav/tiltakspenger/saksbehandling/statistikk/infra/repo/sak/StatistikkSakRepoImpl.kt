package no.nav.tiltakspenger.saksbehandling.statistikk.infra.repo.sak

import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.statistikk.sak.StatistikkSakDTO
import org.intellij.lang.annotations.Language

internal class StatistikkSakRepoImpl(
    private val sessionFactory: PostgresSessionFactory,
) : StatistikkSakRepo {
    override fun lagre(dto: StatistikkSakDTO, context: TransactionContext?) {
        sessionFactory.withTransaction(context) { tx ->
            lagre(dto, tx)
        }
    }

    companion object {
        fun lagre(dto: StatistikkSakDTO, tx: TransactionalSession) {
            tx.run(
                queryOf(
                    lagreSql,
                    mapOf(
                        "sakId" to dto.sakId,
                        "saksnummer" to dto.saksnummer,
                        "behandlingId" to dto.behandlingId,
                        "relatertBehandlingId" to dto.relatertBehandlingId,
                        "fnr" to dto.fnr,
                        "mottattTidspunkt" to dto.mottattTidspunkt,
                        "registrertTidspunkt" to dto.registrertTidspunkt,
                        "ferdigBehandletTidspunkt" to dto.ferdigBehandletTidspunkt,
                        "vedtakTidspunkt" to dto.vedtakTidspunkt,
                        "utbetaltTidspunkt" to dto.utbetaltTidspunkt,
                        "endretTidspunkt" to dto.endretTidspunkt,
                        "soknadsformat" to dto.søknadsformat,
                        "forventetOppstartTidspunkt" to dto.forventetOppstartTidspunkt,
                        "tekniskTidspunkt" to dto.tekniskTidspunkt,
                        "sakYtelse" to dto.sakYtelse,
                        "behandlingType" to dto.behandlingType.toString(),
                        "behandlingStatus" to dto.behandlingStatus.toString(),
                        "behandlingResultat" to dto.behandlingResultat.toString(),
                        "resultatBegrunnelse" to dto.resultatBegrunnelse,
                        "behandlingMetode" to dto.behandlingMetode,
                        "opprettetAv" to dto.opprettetAv,
                        "saksbehandler" to dto.saksbehandler,
                        "ansvarligBeslutter" to dto.ansvarligBeslutter,
                        "tilbakekrevingsbelop" to dto.tilbakekrevingsbeløp,
                        "funksjonellPeriodeFom" to dto.funksjonellPeriodeFom,
                        "funksjonellPeriodeTom" to dto.funksjonellPeriodeTom,
                        "hendelse" to dto.hendelse,
                        "avsender" to dto.avsender,
                        "versjon" to dto.versjon,
                    ),
                ).asUpdateAndReturnGeneratedKey,
            )
        }

        @Language("SQL")
        private val lagreSql = """
        insert into statistikk_sak (
            sak_id,
            saksnummer,
            behandlingId,
            relatertBehandlingId,
            fnr,
            mottatt_tidspunkt,
            registrertTidspunkt,
            ferdigBehandletTidspunkt,
            vedtakTidspunkt,
            utbetaltTidspunkt,
            endretTidspunkt,
            soknadsformat,
            forventetOppstartTidspunkt,
            tekniskTidspunkt,
            sakYtelse,
            behandlingType,
            behandlingStatus,
            behandlingResultat,
            resultatBegrunnelse,
            behandlingMetode,
            opprettetAv,
            saksbehandler,
            ansvarligBeslutter,
            tilbakekrevingsbelop,
            funksjonellperiode_fra_og_med,
            funksjonellperiode_til_og_med,
            hendelse,
            avsender,
            versjon
        ) values (
            :sakId,
            :saksnummer,
            :behandlingId,
            :relatertBehandlingId,
            :fnr,
            :mottattTidspunkt,
            :registrertTidspunkt,
            :ferdigBehandletTidspunkt,
            :vedtakTidspunkt,
            :utbetaltTidspunkt,
            :endretTidspunkt,
            :soknadsformat,
            :forventetOppstartTidspunkt,
            :tekniskTidspunkt,
            :sakYtelse,
            :behandlingType,
            :behandlingStatus,
            :behandlingResultat,
            :resultatBegrunnelse,
            :behandlingMetode,
            :opprettetAv,
            :saksbehandler,
            :ansvarligBeslutter,
            :tilbakekrevingsbelop,
            :funksjonellPeriodeFom,
            :funksjonellPeriodeTom,
            :hendelse,
            :avsender,
            :versjon
        )
        """.trimIndent()
    }
}
