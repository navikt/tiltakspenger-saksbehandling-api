package no.nav.tiltakspenger.vedtak.repository.meldekort

import arrow.core.nonEmptyListOf
import com.fasterxml.jackson.core.type.TypeReference
import kotliquery.Row
import kotliquery.Session
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.HendelseId
import no.nav.tiltakspenger.libs.common.HendelseVersjon
import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.meldekort.domene.MeldeperiodeKjede
import no.nav.tiltakspenger.meldekort.domene.MeldeperiodeKjeder
import no.nav.tiltakspenger.meldekort.ports.MeldeperiodeRepo
import no.nav.tiltakspenger.saksbehandling.domene.sak.Saksnummer
import java.time.LocalDate
import java.time.LocalDateTime

internal class MeldeperiodePostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : MeldeperiodeRepo {
    override fun lagre(
        meldeperiode: Meldeperiode,
        sessionContext: SessionContext?,
    ) {
        sessionFactory.withSession(sessionContext) { session ->
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
                    "id" to meldeperiode.id.toString(),
                    "versjon" to meldeperiode.versjon.value,
                    "hendelse_id" to meldeperiode.hendelseId.toString(),
                    "sak_id" to meldeperiode.sakId.toString(),
                    "opprettet" to meldeperiode.opprettet,
                    "fra_og_med" to meldeperiode.periode.fraOgMed,
                    "til_og_med" to meldeperiode.periode.tilOgMed,
                    "antall_dager_for_periode" to meldeperiode.antallDagerForPeriode,
                    "gir_rett" to meldeperiode.girRett.toDbJson(),
                ).asUpdate,
            )
        }
    }

    override fun hentForSakId(
        sakId: SakId,
        sessionContext: SessionContext?,
    ): MeldeperiodeKjeder {
        return sessionFactory.withSession(sessionContext) { session ->
            hentForSakId(sakId, session)
        }
    }

    override fun hentUsendteTilBruker(): List<Meldeperiode> {
        return sessionFactory.withSession { session ->
            session.run(
                sqlQuery(
                    """
                    select
                        m.*,
                        s.saksnummer,
                        s.ident as fnr 
                    from meldeperiode m 
                    join sak s on s.id = m.sak_id 
                    where m.sendt_til_meldekort_api is null
                    limit 100
                    """,
                ).map { fromRow(it) }.asList,
            )
        }
    }

    override fun markerSomSendtTilBruker(hendelseId: HendelseId, tidspunkt: LocalDateTime) {
        return sessionFactory.withSession { session ->
            session.run(
                sqlQuery(
                    """
                    update meldeperiode set
                        sendt_til_meldekort_api = :tidspunkt
                    where hendelse_id = :id                                    
                    """,
                    "id" to hendelseId.toString(),
                    "tidspunkt" to tidspunkt,
                ).asUpdate,
            )
        }
    }

    private fun Map<LocalDate, Boolean>.toDbJson(): String {
        return entries.joinToString(
            prefix = "{",
            postfix = "}",
            separator = ",",
        ) { (date, value) ->
            "\"${date}\": $value"
        }
    }

    companion object {
        internal fun hentForHendelseId(
            hendelseId: HendelseId,
            session: Session,
        ): Meldeperiode? {
            return session.run(
                sqlQuery(
                    """
                    select
                        m.*,
                        s.saksnummer,
                        s.ident as fnr 
                    from meldeperiode m 
                    join sak s on s.id = m.sak_id
                    where m.hendelse_id = :hendelse_id
                    """,
                    "hendelse_id" to hendelseId.toString(),
                ).map { row -> fromRow(row) }.asSingle,
            )
        }

        internal fun hentForSakId(
            sakId: SakId,
            session: Session,
        ): MeldeperiodeKjeder {
            return session.run(
                sqlQuery(
                    """
                    select
                        m.*,
                        s.saksnummer,
                        s.ident as fnr 
                    from meldeperiode m 
                    join sak s on s.id = m.sak_id 
                    where m.sak_id = :sak_id
                    """,
                    "sak_id" to sakId.toString(),
                ).map { row -> fromRow(row) }.asList,
            ).map {
                MeldeperiodeKjede(nonEmptyListOf(it))
            }.let {
                MeldeperiodeKjeder(it)
            }
        }

        private fun fromRow(row: Row): Meldeperiode {
            return Meldeperiode(
                id = MeldeperiodeId(row.string("id")),
                versjon = HendelseVersjon(row.int("versjon")),
                hendelseId = HendelseId.fromString(row.string("hendelse_id")),
                sakId = SakId.fromString(row.string("sak_id")),
                saksnummer = Saksnummer(row.string("saksnummer")),
                fnr = Fnr.fromString(row.string("fnr")),
                opprettet = row.localDateTime("opprettet"),
                periode = Periode(
                    fraOgMed = row.localDate("fra_og_med"),
                    tilOgMed = row.localDate("til_og_med"),
                ),
                antallDagerForPeriode = row.int("antall_dager_for_periode"),
                girRett = row.string("gir_rett").fromDbJsonToGirRett(),
                sendtTilMeldekortApi = row.localDateTimeOrNull("sendt_til_meldekort_api"),
            )
        }

        private fun String.fromDbJsonToGirRett(): Map<LocalDate, Boolean> {
            val typeRef = object : TypeReference<Map<LocalDate, Boolean>>() {}
            return objectMapper.readValue(this, typeRef)
        }
    }
}
