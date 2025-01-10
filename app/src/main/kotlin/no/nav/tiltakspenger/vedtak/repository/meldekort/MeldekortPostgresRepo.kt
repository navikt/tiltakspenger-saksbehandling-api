package no.nav.tiltakspenger.vedtak.repository.meldekort

import arrow.core.toNonEmptyListOrNull
import kotliquery.Row
import kotliquery.Session
import no.nav.tiltakspenger.felles.Navkontor
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandling.IkkeUtfyltMeldekort
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandling.UtfyltMeldekort
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandlingStatus
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandlinger
import no.nav.tiltakspenger.meldekort.domene.tilMeldekortperioder
import no.nav.tiltakspenger.meldekort.ports.MeldekortRepo
import no.nav.tiltakspenger.saksbehandling.domene.sak.Saksnummer

class MeldekortPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : MeldekortRepo {
    override fun lagre(
        meldekort: MeldekortBehandling,
        transactionContext: TransactionContext?,
    ) {
        sessionFactory.withTransaction(transactionContext) { tx ->
            tx.run(
                sqlQuery(
                    """
                    insert into meldekort (
                        id,
                        forrige_meldekort_id,
                        meldeperiode_id,
                        meldeperiode_hendelse_id,
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
                        sendt_til_beslutning
                    ) values (
                        :id,
                        :forrige_meldekort_id,
                        :meldeperiode_id,
                        :meldeperiode_hendelse_id,
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
                        :sendt_til_beslutning                        
                    )
                    """,
                    "id" to meldekort.id.toString(),
                    "forrige_meldekort_id" to meldekort.forrigeMeldekortId?.toString(),
                    "meldeperiode_id" to meldekort.meldeperiodeId.toString(),
                    "meldeperiode_hendelse_id" to meldekort.meldeperiode.hendelseId.toString(),
                    "sak_id" to meldekort.sakId.toString(),
                    "rammevedtak_id" to meldekort.rammevedtakId.toString(),
                    "opprettet" to meldekort.opprettet,
                    "fra_og_med" to meldekort.fraOgMed,
                    "til_og_med" to meldekort.periode.tilOgMed,
                    "meldekortdager" to meldekort.beregning.toDbJson(),
                    "saksbehandler" to meldekort.saksbehandler,
                    "beslutter" to meldekort.beslutter,
                    "status" to meldekort.status.toDb(),
                    "navkontor" to meldekort.navkontor?.kontornummer,
                    "iverksatt_tidspunkt" to meldekort.iverksattTidspunkt,
                    "sendt_til_beslutning" to meldekort.sendtTilBeslutning,
                ).asUpdate,
            )
        }
    }

    override fun oppdater(
        meldekort: MeldekortBehandling,
        transactionContext: TransactionContext?,
    ) {
        sessionFactory.withTransaction(transactionContext) { tx ->
            tx.run(
                sqlQuery(
                    """
                    update meldekort set 
                        meldekortdager = to_jsonb(:meldekortdager::jsonb),
                        saksbehandler = :saksbehandler,
                        beslutter = :beslutter,
                        status = :status,
                        navkontor = :navkontor,
                        iverksatt_tidspunkt = :iverksatt_tidspunkt,
                        sendt_til_beslutning = :sendt_til_beslutning,
                        meldeperiode_hendelse_id = :meldeperiode_hendelse_id
                    where id = :id
                    """,
                    "id" to meldekort.id.toString(),
                    "meldekortdager" to meldekort.beregning.toDbJson(),
                    "saksbehandler" to meldekort.saksbehandler,
                    "beslutter" to meldekort.beslutter,
                    "status" to meldekort.status.toDb(),
                    "navkontor" to meldekort.navkontor?.kontornummer,
                    "iverksatt_tidspunkt" to meldekort.iverksattTidspunkt,
                    "sendt_til_beslutning" to meldekort.sendtTilBeslutning,
                    "meldeperiode_hendelse_id" to meldekort.meldeperiode.hendelseId,
                ).asUpdate,
            )
        }
    }

    fun hentForSakId(
        sakId: SakId,
        sessionContext: SessionContext?,
    ): MeldekortBehandlinger? {
        return sessionFactory.withSession(sessionContext) { session ->
            hentForSakId(sakId, session)
        }
    }

    companion object {
        internal fun hentForMeldekortId(
            meldekortId: MeldekortId,
            session: Session,
        ): MeldekortBehandling? {
            return session.run(
                sqlQuery(
                    """
                    select
                      m.*,
                      mp.*,
                      s.ident as fnr,
                      s.saksnummer,
                      (b.stønadsdager -> 'registerSaksopplysning' ->> 'antallDager')::int as antall_dager_per_meldeperiode
                    from meldekort m
                    join sak s on s.id = m.sak_id
                    join rammevedtak r on r.id = m.rammevedtak_id
                    join behandling b on b.id = r.behandling_id
                    join meldeperiode mp on m.meldeperiode_hendelse_id = mp.hendelse_id
                    where m.id = :id
                    """,
                    "id" to meldekortId.toString(),
                ).map { row ->
                    fromRow(row)
                }.asSingle,
            )
        }

        internal fun hentForSakId(
            sakId: SakId,
            session: Session,
        ): MeldekortBehandlinger? {
            return session.run(
                sqlQuery(
                    """
                    select
                      m.*,
                      mp.*,
                      s.ident as fnr,
                      s.saksnummer,
                      (b.stønadsdager -> 'registerSaksopplysning' ->> 'antallDager')::int as antall_dager_per_meldeperiode
                    from meldekort m
                    join sak s on s.id = m.sak_id
                    join rammevedtak r on r.id = m.rammevedtak_id
                    join behandling b on b.id = r.behandling_id
                    join meldeperiode mp on m.meldeperiode_hendelse_id = mp.hendelse_id
                    where s.id = :sakId
                    order by m.fra_og_med
                    """,
                    "sakId" to sakId.toString(),
                ).map { fromRow(it) }.asList,
            ).let { it.toNonEmptyListOrNull()?.tilMeldekortperioder() }
        }

        private fun fromRow(
            row: Row,
        ): MeldekortBehandling {
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
            val meldeperiode = MeldeperiodePostgresRepo.fromRow(row)

            return when (val status = row.string("status").toMeldekortStatus()) {
                MeldekortBehandlingStatus.GODKJENT, MeldekortBehandlingStatus.KLAR_TIL_BESLUTNING -> {
                    val meldeperiodeBeregning = row.string("meldekortdager").toUtfyltMeldekortperiode(
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
                        beregning = meldeperiodeBeregning,
                        saksbehandler = row.string("saksbehandler"),
                        sendtTilBeslutning = row.localDateTimeOrNull("sendt_til_beslutning"),
                        beslutter = row.stringOrNull("beslutter"),
                        forrigeMeldekortId = forrigeMeldekortId,
                        tiltakstype = meldeperiodeBeregning.tiltakstype,
                        status = status,
                        iverksattTidspunkt = row.localDateTimeOrNull("iverksatt_tidspunkt"),
                        navkontor = navkontor!!,
                        ikkeRettTilTiltakspengerTidspunkt = row.localDateTimeOrNull("ikke_rett_til_tiltakspenger_tidspunkt"),
                        brukersMeldekort = null,
                        meldeperiode = meldeperiode,
                    )
                }
                // TODO jah: Her blander vi sammen behandlingsstatus og om man har rett/ikke-rett. Det er mulig at man har startet en meldekortbehandling også endres statusen til IKKE_RETT_TIL_TILTAKSPENGER. Da vil behandlingen sånn som koden er nå implisitt avsluttes. Det kan hende vi bør endre dette når vi skiller grunnlag, innsending og behandling.
                MeldekortBehandlingStatus.IKKE_BEHANDLET, MeldekortBehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER -> {
                    val meldeperiodeBeregning = row.string("meldekortdager").toIkkeUtfyltMeldekortperiode(
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
                        beregning = meldeperiodeBeregning,
                        forrigeMeldekortId = forrigeMeldekortId,
                        tiltakstype = meldeperiodeBeregning.tiltakstype,
                        navkontor = navkontor,
                        ikkeRettTilTiltakspengerTidspunkt = row.localDateTimeOrNull("ikke_rett_til_tiltakspenger_tidspunkt"),
                        brukersMeldekort = null,
                        meldeperiode = meldeperiode,
                    )
                }

                else -> throw IllegalStateException("Ukjent meldekortstatus $status for meldekort $id")
            }
        }
    }
}
