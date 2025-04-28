package no.nav.tiltakspenger.saksbehandling.statistikk.infra.repo.sak

import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.statistikk.sak.BehandlingAarsak
import no.nav.tiltakspenger.saksbehandling.behandling.service.statistikk.sak.BehandlingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.service.statistikk.sak.BehandlingStatus
import no.nav.tiltakspenger.saksbehandling.behandling.service.statistikk.sak.BehandlingType
import no.nav.tiltakspenger.saksbehandling.behandling.service.statistikk.sak.StatistikkSakDTO
import org.intellij.lang.annotations.Language

/**
 * Denne tabellen brukes for å dele data med DVH. Se DTO-klassen for lenke til grensesnitt.
 * DVH fanger kun opp nye rader i tabellen, ikke oppdateringer, så endringer som man ønsker at DVH skal få med seg må
 * komme som nye rader. DVH bruker kombinasjonen behandlingid + endrettidspunkt for å identifisere en hendelse, så
 * ved f.eks. teknisk patching av data må man inserte en ny rad med samme behandlingid + endrettidspunkt og endringene
 * man ønsker å gjøre pr rad som skal patches.
 */
internal class StatistikkSakRepoImpl(
    private val sessionFactory: PostgresSessionFactory,
) : StatistikkSakRepo {
    override fun lagre(dto: StatistikkSakDTO, context: TransactionContext?) {
        sessionFactory.withTransaction(context) { tx ->
            lagre(dto, tx)
        }
    }

    // DVH håndterer selv adressebeskyttelse, så dette gjør vi mest for vår egen del.
    override fun oppdaterAdressebeskyttelse(sakId: SakId) {
        sessionFactory.withSession {
            it.run(
                queryOf(
                    """
                        update statistikk_sak set opprettetAv = :verdi, saksbehandler = :verdi, ansvarligbeslutter = :verdi where sak_id = :sak_id
                    """.trimIndent(),
                    mapOf(
                        "verdi" to "-5",
                        "sak_id" to sakId.toString(),
                    ),
                ).asUpdate,
            )
        }
    }

    // Denne brukes kun for tester
    override fun hent(sakId: SakId): List<StatistikkSakDTO> = sessionFactory.withSession {
        it.run(
            queryOf(
                """
                    select *
                    from statistikk_sak
                    where sak_id = :sak_id
                """.trimIndent(),
                mapOf(
                    "sak_id" to sakId.toString(),
                ),
            ).map { row -> row.toStatistikkSakDTO() }
                .asList,
        )
    }

    override fun oppdaterFnr(gammeltFnr: Fnr, nyttFnr: Fnr) {
        sessionFactory.withSession {
            it.run(
                queryOf(
                    """
                        update statistikk_sak set fnr = :nytt_fnr where fnr = :gammelt_fnr
                    """.trimIndent(),
                    mapOf(
                        "nytt_fnr" to nyttFnr.verdi,
                        "gammelt_fnr" to gammeltFnr.verdi,
                    ),
                ).asUpdate,
            )
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
                        "behandlingType" to dto.behandlingType.name,
                        "behandlingStatus" to dto.behandlingStatus.name,
                        "behandlingResultat" to dto.behandlingResultat?.name,
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
                        "behandling_aarsak" to dto.behandlingAarsak?.name,
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
            versjon,
            behandling_aarsak
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
            :versjon,
            :behandling_aarsak
        )
        """.trimIndent()
    }

    private fun Row.toStatistikkSakDTO() =
        StatistikkSakDTO(
            sakId = string("sak_id"),
            saksnummer = string("saksnummer"),
            behandlingId = string("behandlingid"),
            relatertBehandlingId = stringOrNull("relatertbehandlingid"),
            fnr = string("fnr"),
            mottattTidspunkt = localDateTime("mottatt_tidspunkt"),
            registrertTidspunkt = localDateTime("registrerttidspunkt"),
            ferdigBehandletTidspunkt = localDateTimeOrNull("ferdigbehandlettidspunkt"),
            vedtakTidspunkt = localDateTimeOrNull("vedtaktidspunkt"),
            endretTidspunkt = localDateTime("endrettidspunkt"),
            utbetaltTidspunkt = localDateTimeOrNull("utbetalttidspunkt"),
            søknadsformat = string("soknadsformat"),
            forventetOppstartTidspunkt = localDateOrNull("forventetoppstarttidspunkt"),
            tekniskTidspunkt = localDateTime("teknisktidspunkt"),
            sakYtelse = string("sakytelse"),
            behandlingType = BehandlingType.valueOf(string("behandlingtype")),
            behandlingStatus = BehandlingStatus.valueOf(string("behandlingstatus")),
            behandlingResultat = stringOrNull("behandlingresultat")?.let { BehandlingResultat.valueOf(it) },
            resultatBegrunnelse = stringOrNull("resultatbegrunnelse"),
            behandlingMetode = string("behandlingmetode"),
            opprettetAv = string("opprettetav"),
            saksbehandler = stringOrNull("saksbehandler"),
            ansvarligBeslutter = stringOrNull("ansvarligbeslutter"),
            tilbakekrevingsbeløp = doubleOrNull("tilbakekrevingsbelop"),
            funksjonellPeriodeFom = localDateOrNull("funksjonellperiode_fra_og_med"),
            funksjonellPeriodeTom = localDateOrNull("funksjonellperiode_til_og_med"),
            avsender = string("avsender"),
            versjon = string("versjon"),
            hendelse = string("hendelse"),
            behandlingAarsak = stringOrNull("behandling_aarsak")?.let { BehandlingAarsak.valueOf(it) },
        )
}
