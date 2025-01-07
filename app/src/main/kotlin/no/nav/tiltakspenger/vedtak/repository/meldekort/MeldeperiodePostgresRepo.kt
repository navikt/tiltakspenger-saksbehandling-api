package no.nav.tiltakspenger.vedtak.repository.meldekort

import arrow.core.nonEmptyListOf
import com.fasterxml.jackson.core.type.TypeReference
import kotliquery.Row
import no.nav.tiltakspenger.felles.HendelseId
import no.nav.tiltakspenger.felles.Hendelsesversjon
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.meldekort.domene.v2.Meldeperiode
import no.nav.tiltakspenger.meldekort.domene.v2.MeldeperiodeKjede
import no.nav.tiltakspenger.meldekort.domene.v2.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.domene.sak.Saksnummer
import java.time.LocalDate

internal class MeldeperiodePostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) {
    fun lagre(
        meldeperiode: Meldeperiode,
        sessionContext: SessionContext? = null,
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

    fun hentForSakId(
        sakId: SakId,
        sessionContext: SessionContext? = null,
    ): MeldeperiodeKjeder {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    select m.*,s.saksnummer,s.ident as fnr 
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
    }

    private fun fromRow(row: Row): Meldeperiode {
        return Meldeperiode(
            id = MeldeperiodeId(row.string("id")),
            versjon = Hendelsesversjon(row.int("versjon")),
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
        )
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

    private fun String.fromDbJsonToGirRett(): Map<LocalDate, Boolean> {
        val typeRef = object : TypeReference<Map<LocalDate, Boolean>>() {}
        return objectMapper.readValue(this, typeRef)
    }
}
