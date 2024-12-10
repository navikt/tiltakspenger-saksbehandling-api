package no.nav.tiltakspenger.vedtak.repository.meldekort

import arrow.core.toNonEmptyListOrNull
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.tiltakspenger.felles.Navkontor
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.Meldekort.IkkeUtfyltMeldekort
import no.nav.tiltakspenger.meldekort.domene.Meldekort.UtfyltMeldekort
import no.nav.tiltakspenger.meldekort.domene.Meldeperioder
import no.nav.tiltakspenger.meldekort.domene.tilMeldekortperioder
import no.nav.tiltakspenger.meldekort.ports.MeldekortRepo
import no.nav.tiltakspenger.saksbehandling.domene.sak.Saksnummer
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime

class MeldekortPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : MeldekortRepo {
    override fun lagre(
        meldekort: Meldekort,
        transactionContext: TransactionContext?,
    ) {
        sessionFactory.withTransaction(transactionContext) { tx ->
            tx.run(
                queryOf(
                    """
                    insert into meldekort (
                        id,
                        forrige_meldekort_id,
                        meldeperiode_id,
                        sak_id,
                        rammevedtak_id,
                        opprettet,
                        fra_og_med,
                        til_og_med,
                        meldekortdager,
                        saksbehandler,
                        beslutter,
                        status,
                        navkontor,
                        iverksatt_tidspunkt,
                        sendt_til_beslutning,
                        sendt_til_meldekort_api
                    ) values (
                        :id,
                        :forrige_meldekort_id,
                        :meldeperiode_id,
                        :sak_id,
                        :rammevedtak_id,
                        :opprettet,
                        :fra_og_med,
                        :til_og_med,
                        to_jsonb(:meldekortdager::jsonb),
                        :saksbehandler,
                        :beslutter,
                        :status,
                        :navkontor,
                        :iverksatt_tidspunkt,
                        :sendt_til_beslutning,                        
                        :sendt_til_meldekort_api
                    )
                    """.trimIndent(),
                    mapOf(
                        "id" to meldekort.id.toString(),
                        "forrige_meldekort_id" to meldekort.forrigeMeldekortId?.toString(),
                        "meldeperiode_id" to meldekort.meldeperiodeId.toString(),
                        "sak_id" to meldekort.sakId.toString(),
                        "rammevedtak_id" to meldekort.rammevedtakId.toString(),
                        "opprettet" to meldekort.opprettet,
                        "fra_og_med" to meldekort.fraOgMed,
                        "til_og_med" to meldekort.periode.tilOgMed,
                        "meldekortdager" to meldekort.meldeperiode.toDbJson(),
                        "saksbehandler" to meldekort.saksbehandler,
                        "beslutter" to meldekort.beslutter,
                        "status" to meldekort.status.toDb(),
                        "navkontor" to meldekort.navkontor?.kontornummer,
                        "iverksatt_tidspunkt" to meldekort.iverksattTidspunkt,
                        "sendt_til_beslutning" to meldekort.sendtTilBeslutning,
                        "sendt_til_meldekort_api" to meldekort.sendtTilMeldekortApi,
                    ),
                ).asUpdate,
            )
        }
    }

    override fun oppdater(
        meldekort: Meldekort,
        transactionContext: TransactionContext?,
    ) {
        sessionFactory.withTransaction(transactionContext) { tx ->
            tx.run(
                queryOf(
                    """
                    update meldekort set 
                        meldekortdager = to_jsonb(:meldekortdager::jsonb),
                        saksbehandler = :saksbehandler,
                        beslutter = :beslutter,
                        status = :status,
                        navkontor = :navkontor,
                        iverksatt_tidspunkt = :iverksatt_tidspunkt,
                        sendt_til_beslutning = :sendt_til_beslutning
                    where id = :id
                    """.trimIndent(),
                    mapOf(
                        "id" to meldekort.id.toString(),
                        "meldekortdager" to meldekort.meldeperiode.toDbJson(),
                        "saksbehandler" to meldekort.saksbehandler,
                        "beslutter" to meldekort.beslutter,
                        "status" to meldekort.status.toDb(),
                        "navkontor" to meldekort.navkontor?.kontornummer,
                        "iverksatt_tidspunkt" to meldekort.iverksattTidspunkt,
                        "sendt_til_beslutning" to meldekort.sendtTilBeslutning,
                    ),
                ).asUpdate,
            )
        }
    }

    override fun hentTilBrukerUtfylling(): List<Meldekort> {
        return sessionFactory.withSession { session ->
            @Language("PostgreSQL")
            val query =
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
                    where sendt_til_meldekort_api is null                           
                """.trimIndent()
            session.run(
                queryOf(query, mapOf()).map { fromRow(it) }.asList,
            )
        }
    }

    override fun markerSomSendtTilBrukerUtfylling(meldekortId: MeldekortId, tidspunkt: LocalDateTime) {
        return sessionFactory.withSession { session ->
            @Language("PostgreSQL")
            val query =
                """
                    update meldekort set
                        sendt_til_meldekort_api = :tidspunkt
                    where id = :id                                    
                """.trimIndent()
            session.run(
                queryOf(
                    query,
                    mapOf(
                        "id" to meldekortId.toString(),
                        "tidspunkt" to tidspunkt,
                    ),
                ).asUpdate,
            )
        }
    }

    fun hentForSakId(
        sakId: SakId,
        sessionContext: SessionContext?,
    ): Meldeperioder? {
        return sessionFactory.withSession(sessionContext) { session ->
            hentForSakId(sakId, session)
        }
    }

    companion object {
        internal fun hentForMeldekortId(
            meldekortId: MeldekortId,
            session: Session,
        ): Meldekort? {
            return session.run(
                queryOf(
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
                        where m.id = :id
                    """.trimIndent(),
                    mapOf("id" to meldekortId.toString()),
                ).map { row ->
                    fromRow(row)
                }.asSingle,
            )
        }

        internal fun hentForSakId(
            sakId: SakId,
            session: Session,
        ): Meldeperioder? {
            return session.run(
                queryOf(
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
                        where s.id = :sakId
                        order by m.fra_og_med
                    """.trimIndent(),
                    mapOf("sakId" to sakId.toString()),
                ).map { fromRow(it) }.asList,
            ).let { it.toNonEmptyListOrNull()?.tilMeldekortperioder() }
        }

        private fun fromRow(
            row: Row,
        ): Meldekort {
            val id = MeldekortId.fromString(row.string("id"))
            val sakId = SakId.fromString(row.string("sak_id"))
            val saksnummer = Saksnummer(row.string("saksnummer"))
            val meldeperiodeId = MeldeperiodeId(row.string("meldeperiode_id"))
            val navkontor = row.stringOrNull("navkontor")?.let { Navkontor(it) }
            val rammevedtakId = VedtakId.fromString(row.string("rammevedtak_id"))
            val fnr = Fnr.fromString(row.string("fnr"))
            val forrigeMeldekortId = row.stringOrNull("forrige_meldekort_id")?.let { MeldekortId.fromString(it) }
            val maksDagerMedTiltakspengerForPeriode = row.int("antall_dager_per_meldeperiode")
            val opprettet = row.localDateTime("opprettet")
            return when (val status = row.string("status")) {
                "GODKJENT", "KLAR_TIL_BESLUTNING" -> {
                    val meldekortperiode = row.string("meldekortdager").toUtfyltMeldekortperiode(
                        sakId = sakId,
                        meldekortId = id,
                        maksDagerMedTiltakspengerForPeriode = maksDagerMedTiltakspengerForPeriode,
                    )

                    UtfyltMeldekort(
                        id = id,
                        meldeperiodeId = meldeperiodeId,
                        sakId = sakId,
                        saksnummer = saksnummer,
                        fnr = fnr,
                        rammevedtakId = rammevedtakId,
                        opprettet = opprettet,
                        meldeperiode = meldekortperiode,
                        saksbehandler = row.string("saksbehandler"),
                        sendtTilBeslutning = row.localDateTimeOrNull("sendt_til_beslutning"),
                        beslutter = row.stringOrNull("beslutter"),
                        forrigeMeldekortId = forrigeMeldekortId,
                        tiltakstype = meldekortperiode.tiltakstype,
                        status = row.string("status").toMeldekortStatus(),
                        iverksattTidspunkt = row.localDateTimeOrNull("iverksatt_tidspunkt"),
                        navkontor = navkontor!!,
                        ikkeRettTilTiltakspengerTidspunkt = row.localDateTimeOrNull("ikke_rett_til_tiltakspenger_tidspunkt"),
                        sendtTilMeldekortApi = row.localDateTimeOrNull("sendt_til_meldekort_api"),
                    )
                }

                "KLAR_TIL_UTFYLLING" -> {
                    val meldekortperiode =
                        row.string("meldekortdager").toIkkeUtfyltMeldekortperiode(
                            sakId = sakId,
                            meldekortId = id,
                            maksDagerMedTiltakspengerForPeriode = maksDagerMedTiltakspengerForPeriode,
                        )
                    IkkeUtfyltMeldekort(
                        id = id,
                        meldeperiodeId = meldeperiodeId,
                        sakId = sakId,
                        saksnummer = saksnummer,
                        fnr = fnr,
                        rammevedtakId = rammevedtakId,
                        opprettet = opprettet,
                        meldeperiode = meldekortperiode,
                        forrigeMeldekortId = forrigeMeldekortId,
                        tiltakstype = meldekortperiode.tiltakstype,
                        navkontor = navkontor,
                        ikkeRettTilTiltakspengerTidspunkt = row.localDateTimeOrNull("ikke_rett_til_tiltakspenger_tidspunkt"),
                    )
                }

                else -> throw IllegalStateException("Ukjent meldekortstatus $status for meldekort $id")
            }
        }
    }
}
