package no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.infra.repo

import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.SaksstatistikkDTO
import org.intellij.lang.annotations.Language

/**
 * Skriver `statistikk_sak`, som Team Sak (DVH) leser; feltene og reglene for tabellen står på [no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.SaksstatistikkDTO].
 * Team Sak ser bare nye rader, så retting gjøres med nye rader som beskrevet i `docs/statistikk/resending_team_sak.md`.
 * Hvordan Team Sak bruker funksjonell, teknisk og lastet tid: https://confluence.adeo.no/spaces/DVH/pages/459904637/Funksjonell+tid+teknisk+tid+og+lastet+tids+rolle+i+modell
 */
object SaksstatistikkPostgresRepo {
    /**
     * DVH håndterer selv adressebeskyttelse, så dette gjør vi mest for vår egen del.
     */
    fun oppdaterFnr(
        gammeltFnr: Fnr,
        nyttFnr: Fnr,
        session: Session,
    ) {
        session.run(
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

    fun oppdaterAdressebeskyttelse(sakId: SakId, session: Session) {
        session.run(
            queryOf(
                """
                        update statistikk_sak set opprettetAv = :verdi, saksbehandler = :verdi, ansvarligbeslutter = :verdi, ansvarligenhet = :verdi where sak_id = :sak_id
                """.trimIndent(),
                mapOf(
                    "verdi" to "-5",
                    "sak_id" to sakId.toString(),
                ),
            ).asUpdate,
        )
    }

    /**
     * `teknisktidspunkt` settes av databasen med `clock_timestamp()` i selve inserten, samtidig som `id` trekkes fra sekvensen.
     * Settes tidspunktet i applikasjonen, kan det gå tid før raden settes inn, og samtidige transaksjoner kan da gi rader der rekkefølgen på `id` og `teknisktidspunkt` er ulik.
     * `now()` er ikke godt nok, siden den gir starttidspunktet til transaksjonen.
     */
    fun lagre(dto: SaksstatistikkDTO, tx: TransactionalSession) {
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
                    "soknadsformat" to dto.søknadsformat.name,
                    "forventetOppstartTidspunkt" to dto.forventetOppstartTidspunkt,
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
                    "relatertfagsystem" to dto.relatertFagsystem,
                    "sakutland" to dto.sakUtland,
                    "ansvarligenhet" to dto.ansvarligenhet,
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
            behandling_aarsak,
            relatertfagsystem,
            sakutland,
            ansvarligenhet
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
            clock_timestamp(),
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
            :behandling_aarsak,
            :relatertfagsystem,
            :sakutland,
            :ansvarligenhet
        )
    """.trimIndent()
}
