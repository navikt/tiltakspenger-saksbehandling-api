package no.nav.tiltakspenger.saksbehandling.statistikk

import kotliquery.Row
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.SaksstatistikkDTO
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkBehandlingAarsak
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkBehandlingResultat
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkBehandlingStatus
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkBehandlingType
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkFormat
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.infra.repo.SaksstatistikkPostgresRepo
import no.nav.tiltakspenger.saksbehandling.statistikk.stønadsstatistikk.StatistikkStønadDTO
import no.nav.tiltakspenger.saksbehandling.statistikk.stønadsstatistikk.infra.repo.StatistikkStønadPostgresRepo
import java.time.Clock

// Statistikktabellene skrives av oss og leses kun av eksterne konsumenter (DVH), aldri av prodkoden vår.
// Lesespørringene hører derfor i testlaget med egen SQL, ikke som `@TestOnly` på repoene, jf. «Data som skrives, men aldri leses ut i domenet» i `../AGENTS-backend.md`.
// Skrivehjelperne finnes fordi noen tester må etablere en statistikkrad før de kjører jobben som endrer den, typisk identhendelser som bytter fnr.

/** Etablerer en saksstatistikkrad, for tester som skal verifisere at en jobb endrer den. */
fun PostgresSessionFactory.lagreSaksstatistikk(dto: SaksstatistikkDTO) {
    withTransaction { tx -> SaksstatistikkPostgresRepo.lagre(dto, tx) }
}

/** Etablerer en stønadsstatistikkrad, for tester som skal verifisere at en jobb endrer den. */
fun PostgresSessionFactory.lagreStønadsstatistikk(dto: StatistikkStønadDTO, clock: Clock) {
    withTransaction { tx -> StatistikkStønadPostgresRepo.lagre(dto, clock, tx) }
}

/** Radene DVH ville sett for saken, eldst først. */
fun PostgresSessionFactory.hentSaksstatistikk(sakId: SakId): List<SaksstatistikkDTO> = withSession { session ->
    session.run(
        queryOf(
            """
                select *
                from statistikk_sak
                where sak_id = :sak_id
                order by teknisktidspunkt asc
            """.trimIndent(),
            mapOf("sak_id" to sakId.toString()),
        ).map { row -> row.tilSaksstatistikkDTO() }.asList,
    )
}

private fun Row.tilSaksstatistikkDTO() =
    SaksstatistikkDTO(
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
        søknadsformat = StatistikkFormat.valueOf(string("soknadsformat")),
        forventetOppstartTidspunkt = localDateOrNull("forventetoppstarttidspunkt"),
        tekniskTidspunkt = localDateTime("teknisktidspunkt"),
        sakYtelse = string("sakytelse"),
        behandlingType = StatistikkBehandlingType.valueOf(string("behandlingtype")),
        behandlingStatus = StatistikkBehandlingStatus.valueOf(string("behandlingstatus")),
        behandlingResultat = stringOrNull("behandlingresultat")?.let { StatistikkBehandlingResultat.valueOf(it) },
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
        behandlingAarsak = stringOrNull("behandling_aarsak")?.let { StatistikkBehandlingAarsak.valueOf(it) },
        relatertFagsystem = string("relatertfagsystem"),
        sakUtland = string("sakutland"),
        ansvarligenhet = string("ansvarligenhet"),
    )
