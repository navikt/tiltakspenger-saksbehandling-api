@file:Suppress("unused", "ktlint")

package db.migration

import arrow.core.toNonEmptyListOrThrow
import com.fasterxml.jackson.core.type.TypeReference
import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.Row
import kotliquery.Session
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.HendelseVersjon
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.zoneIdOslo
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresTransactionContext.Companion.withSession
import no.nav.tiltakspenger.libs.persistering.infrastruktur.SessionCounter
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.BehandlingPostgresRepo
import no.nav.tiltakspenger.saksbehandling.distribusjon.DistribusjonId
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeKjede
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.toDbJson
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.toPeriodiserteVedtakId
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import no.nav.tiltakspenger.saksbehandling.vedtak.Vedtaksliste
import no.nav.tiltakspenger.saksbehandling.vedtak.Vedtakstype
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import java.time.Clock
import java.time.LocalDate

class V113__migrer_meldeperiode_rammevedtak_id : BaseJavaMigration() {
    override fun migrate(context: Context) {
        val logger = KotlinLogging.logger {}
        val dataSource = context.configuration.dataSource
        val clock = Clock.system(zoneIdOslo)
        val sessionFactory = PostgresSessionFactory(dataSource, SessionCounter(logger))

        sessionFactory.withTransactionContext { tx ->
            val meldeperioder: Map<SakId, MeldeperiodeKjeder> = tx.withSession { session ->
                session.run(
                    sqlQuery("""select m.*,s.saksnummer,s.fnr from meldeperiode m join sak s on s.id = m.sak_id order by m.sak_id, m.kjede_id, m.versjon""")
                        .map { row -> fromRowToMeldeperiode(row) }.asList,
                )
            }.groupBy { it.sakId }.map {
                it.key to it.value.groupBy { it.kjedeId }.map {
                    MeldeperiodeKjede(it.value.sortedBy { it.versjon }.toNonEmptyListOrThrow())
                }.let { MeldeperiodeKjeder(it) }
            }.toMap()

            val vedtak = tx.withSession { session ->
                session.run(
                    sqlQuery("""select * from rammevedtak where vedtakstype in ('INNVILGELSE','STANS') order by sak_id, opprettet""")
                        .map { row -> row.toVedtak(session) }.asList,
                )
            }
            tx.withSession { transactionalSession ->
                vedtak.groupBy { it.sakId }.forEach { (sakId, vedtak) ->
                    val eksisterendeMeldeperiodeKjeder = meldeperioder[sakId]!!
                    val sorterteVedtak = vedtak.sortedBy { it.opprettet }
                    var meldeperiodeKjeder = MeldeperiodeKjeder()
                    // Vi må sjekke meldeperioden per vedtak som er opprettet.
                    sorterteVedtak.forEachIndexed { index, rammevedtak ->
                        // Det er ikke en garanti for at et nytt vedtak fører til en ny meldeperiode,
                        val vedtaksliste = Vedtaksliste(sorterteVedtak.subList(0, index + 1))
                        val (oppdaterteMeldeperiodeKjeder, nyeMeldeperioder) = meldeperiodeKjeder.genererMeldeperioder(
                            vedtaksliste,
                            clock,
                        )
                        meldeperiodeKjeder = oppdaterteMeldeperiodeKjeder
                        nyeMeldeperioder.forEach { nyMeldeperiode ->
                            require(nyMeldeperiode.rammevedtak!!.isNotEmpty())
                            val eksisterendeKjede: MeldeperiodeKjede =
                                eksisterendeMeldeperiodeKjeder.hentMeldeperiodeKjedeForPeriode(nyMeldeperiode.periode)!!
                            val eksisterendeMeldeperiode: Meldeperiode =
                                eksisterendeKjede.single { it.versjon == nyMeldeperiode.versjon }
                            require(
                                eksisterendeMeldeperiode == nyMeldeperiode.copy(
                                    // Vi gjør bare en rask sjekk på at vi snakker om den samme meldeperioden som er generert tidligere. Vi må ignorere id, opprettet og rammevedtak.
                                    rammevedtak = eksisterendeMeldeperiode.rammevedtak,
                                    id = eksisterendeMeldeperiode.id,
                                    opprettet = eksisterendeMeldeperiode.opprettet,
                                ),
                            ) {
                                "Kunne ikke migrere meldeperiode ${nyMeldeperiode.id} for sak ${sakId} fordi den ikke samsvarer med eksisterende meldeperiode ${eksisterendeMeldeperiode.id} i kjede ${eksisterendeKjede.kjedeId}. " +
                                    "Eksisterende: $eksisterendeMeldeperiode, ny: $nyMeldeperiode"
                            }
                            if (eksisterendeMeldeperiode.rammevedtak != null && eksisterendeMeldeperiode.rammevedtak != nyMeldeperiode.rammevedtak) {
                                throw IllegalStateException("Rammevedtak for meldeperiode ${nyMeldeperiode.id} er endret. Eksisterende: ${eksisterendeMeldeperiode.rammevedtak}, ny: ${nyMeldeperiode.rammevedtak}")
                            }

                            transactionalSession.run(
                                sqlQuery(
                                    """update meldeperiode SET rammevedtak = to_jsonb(:rammevedtak::jsonb)""",
                                    "rammevedtak" to nyMeldeperiode.rammevedtak.toDbJson(),
                                ).asUpdate,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun fromRowToMeldeperiode(row: Row): Meldeperiode {
    return Meldeperiode(
        kjedeId = MeldeperiodeKjedeId(row.string("kjede_id")),
        versjon = HendelseVersjon(row.int("versjon")),
        id = MeldeperiodeId.fromString(row.string("id")),
        sakId = SakId.fromString(row.string("sak_id")),
        saksnummer = Saksnummer(row.string("saksnummer")),
        fnr = Fnr.fromString(row.string("fnr")),
        opprettet = row.localDateTime("opprettet"),
        periode = Periode(
            fraOgMed = row.localDate("fra_og_med"),
            tilOgMed = row.localDate("til_og_med"),
        ),
        maksAntallDagerForMeldeperiode = row.int("antall_dager_for_periode"),
        girRett = row.string("gir_rett").fromDbJsonToGirRett(),
        rammevedtak = row.stringOrNull("rammevedtak")?.toPeriodiserteVedtakId(),
    )
}

private fun String.fromDbJsonToGirRett(): Map<LocalDate, Boolean> {
    val typeRef = object : TypeReference<Map<LocalDate, Boolean>>() {}
    return objectMapper.readValue(this, typeRef)
}

private fun Row.toVedtak(session: Session): Rammevedtak {
    val id = VedtakId.fromString(string("id"))
    return Rammevedtak(
        id = id,
        sakId = SakId.fromString(string("sak_id")),
        behandling =
            BehandlingPostgresRepo.hentOrNull(
                BehandlingId.fromString(string("behandling_id")),
                session,
            )!!,
        vedtaksdato = localDateOrNull("vedtaksdato"),
        vedtakstype = Vedtakstype.valueOf(string("vedtakstype")),
        periode = Periode(fraOgMed = localDate("fra_og_med"), tilOgMed = localDate("til_og_med")),
        journalpostId = stringOrNull("journalpost_id")?.let { JournalpostId(it) },
        journalføringstidspunkt = localDateTimeOrNull("journalføringstidspunkt"),
        distribusjonId = stringOrNull("distribusjon_id")?.let { DistribusjonId(it) },
        distribusjonstidspunkt = localDateTimeOrNull("distribusjonstidspunkt"),
        sendtTilDatadeling = localDateTimeOrNull("sendt_til_datadeling"),
        brevJson = stringOrNull("brev_json"),
        opprettet = localDateTime("opprettet"),
    )
}
