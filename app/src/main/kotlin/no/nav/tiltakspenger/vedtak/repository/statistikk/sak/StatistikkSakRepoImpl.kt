package no.nav.tiltakspenger.vedtak.repository.statistikk.sak

import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.vedtak.saksbehandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.vedtak.saksbehandling.service.statistikk.sak.StatistikkSakDTO
import no.nav.tiltakspenger.vedtak.saksbehandling.service.statistikk.sak.VilkårStatistikkDTO
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
                        "sakUtland" to false,
                        "behandlingType" to dto.behandlingType.toString(),
                        "behandlingStatus" to dto.behandlingStatus.toString(),
                        "behandlingResultat" to dto.behandlingResultat.toString(),
                        "resultatBegrunnelse" to dto.resultatBegrunnelse,
                        "behandlingMetode" to dto.behandlingMetode,
                        "opprettetAv" to dto.opprettetAv,
                        "saksbehandler" to dto.saksbehandler,
                        "ansvarligBeslutter" to dto.ansvarligBeslutter,
                        "ansvarligEnhet" to dto.ansvarligEnhet,
                        "tilbakekrevingsbelop" to dto.tilbakekrevingsbeløp,
                        "funksjonellPeriodeFom" to dto.funksjonellPeriodeFom,
                        "funksjonellPeriodeTom" to dto.funksjonellPeriodeTom,
                        "hendelse" to dto.hendelse,
                        "avsender" to dto.avsender,
                        "versjon" to dto.versjon,
                    ),
                ).asUpdateAndReturnGeneratedKey,
            ).also { id ->
                if (id != null) {
                    dto.vilkår.forEach { vilkår ->
                        lagreVilkår(id.toInt(), vilkår, tx)
                    }
                }
            }
        }

        private fun lagreVilkår(id: Int, dto: VilkårStatistikkDTO, tx: TransactionalSession) {
            tx.run(
                queryOf(
                    lagreVilkårSql,
                    mapOf(
                        "statistikkSakId" to id,
                        "vilkar" to dto.vilkår,
                        "beskrivelse" to dto.beskrivelse,
                        "resultat" to dto.resultat.name,
                    ),
                ).asUpdate,
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
            søknadsformat,
            forventetOppstartTidspunkt,
            tekniskTidspunkt,
            sakYtelse,
            sakUtland,
            behandlingType,
            behandlingStatus,
            behandlingResultat,
            resultatBegrunnelse,
            behandlingMetode,
            opprettetAv,
            saksbehandler,
            ansvarligBeslutter,
            ansvarligEnhet,
            tilbakekrevingsbeløp,
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
            :sakUtland,
            :behandlingType,
            :behandlingStatus,
            :behandlingResultat,
            :resultatBegrunnelse,
            :behandlingMetode,
            :opprettetAv,
            :saksbehandler,
            :ansvarligBeslutter,
            :ansvarligEnhet,
            :tilbakekrevingsbelop,
            :funksjonellPeriodeFom,
            :funksjonellPeriodeTom,
            :hendelse,
            :avsender,
            :versjon
        ) returning id
        """.trimIndent()

        @Language("SQL")
        private val lagreVilkårSql = """
        insert into statistikk_sak_vilkår (
            statistikk_sak_id,
            vilkår,
            beskrivelse,
            resultat  
        ) values (
            :statistikkSakId,
            :vilkar,
            :beskrivelse,
            :resultat
        )
        """.trimIndent()
    }
}
