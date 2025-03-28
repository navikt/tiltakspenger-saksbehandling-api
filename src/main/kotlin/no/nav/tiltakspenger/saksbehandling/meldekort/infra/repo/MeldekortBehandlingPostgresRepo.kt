package no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.toNonEmptyListOrNull
import kotliquery.Row
import kotliquery.Session
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.toAttesteringer
import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.attesteringer.toAttesteringer
import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.attesteringer.toDbJson
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling.MeldekortBehandlet
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlinger
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBeregning
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortbehandlingBegrunnelse
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.tilMeldekortperioder
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortBehandlingRepo
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer

class MeldekortBehandlingPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : MeldekortBehandlingRepo {
    override fun lagre(
        meldekortBehandling: MeldekortBehandling,
        transactionContext: TransactionContext?,
    ) {
        sessionFactory.withTransaction(transactionContext) { tx ->
            tx.run(
                sqlQuery(
                    """
                    insert into meldekortbehandling (
                        id,
                        meldeperiode_kjede_id,
                        meldeperiode_id,
                        sak_id,
                        opprettet,
                        fra_og_med,
                        til_og_med,
                        meldekortdager,
                        beregninger,
                        saksbehandler,
                        beslutter,
                        status,
                        navkontor,
                        iverksatt_tidspunkt,
                        sendt_til_beslutning,
                        navkontor_navn,
                        type,
                        begrunnelse,
                        attesteringer
                    ) values (
                        :id,
                        :meldeperiode_kjede_id,
                        :meldeperiode_id,
                        :sak_id,
                        :opprettet,
                        :fra_og_med,
                        :til_og_med,
                        to_jsonb(:meldekortdager::jsonb),
                        to_jsonb(:beregninger::jsonb),
                        :saksbehandler,
                        :beslutter,
                        :status,
                        :navkontor,
                        :iverksatt_tidspunkt,
                        :sendt_til_beslutning,
                        :navkontor_navn,
                        :type,
                        :begrunnelse,
                        to_jsonb(:attesteringer::jsonb)
                    )
                    """,
                    "id" to meldekortBehandling.id.toString(),
                    "meldeperiode_kjede_id" to meldekortBehandling.kjedeId.toString(),
                    "meldeperiode_id" to meldekortBehandling.meldeperiode.id.toString(),
                    "sak_id" to meldekortBehandling.sakId.toString(),
                    "opprettet" to meldekortBehandling.opprettet,
                    "fra_og_med" to meldekortBehandling.fraOgMed,
                    "til_og_med" to meldekortBehandling.tilOgMed,
                    "meldekortdager" to meldekortBehandling.beregning.tilMeldekortdagerDbJson(),
                    "beregninger" to meldekortBehandling.beregning.tilBeregningerDbJson(),
                    "saksbehandler" to meldekortBehandling.saksbehandler,
                    "beslutter" to meldekortBehandling.beslutter,
                    "status" to meldekortBehandling.status.toDb(),
                    "navkontor" to meldekortBehandling.navkontor.kontornummer,
                    "iverksatt_tidspunkt" to meldekortBehandling.iverksattTidspunkt,
                    "sendt_til_beslutning" to meldekortBehandling.sendtTilBeslutning,
                    "navkontor_navn" to meldekortBehandling.navkontor.kontornavn,
                    "type" to meldekortBehandling.type.tilDb(),
                    "begrunnelse" to meldekortBehandling.begrunnelse?.verdi,
                    "attesteringer" to meldekortBehandling.attesteringer.toDbJson(),
                ).asUpdate,
            )
        }
    }

    override fun oppdater(
        meldekortBehandling: MeldekortBehandling,
        transactionContext: TransactionContext?,
    ) {
        sessionFactory.withTransaction(transactionContext) { tx ->
            tx.run(
                sqlQuery(
                    """
                    update meldekortbehandling set
                        meldekortdager = to_jsonb(:meldekortdager::jsonb),
                        beregninger = to_jsonb(:beregninger::jsonb),
                        saksbehandler = :saksbehandler,
                        beslutter = :beslutter,
                        status = :status,
                        navkontor = :navkontor,
                        iverksatt_tidspunkt = :iverksatt_tidspunkt,
                        sendt_til_beslutning = :sendt_til_beslutning,
                        meldeperiode_id = :meldeperiode_id,
                        begrunnelse = :begrunnelse,
                        attesteringer = to_json(:attesteringer::jsonb)
                    where id = :id
                    """,
                    "id" to meldekortBehandling.id.toString(),
                    "meldekortdager" to meldekortBehandling.beregning.tilMeldekortdagerDbJson(),
                    "beregninger" to meldekortBehandling.beregning.tilBeregningerDbJson(),
                    "saksbehandler" to meldekortBehandling.saksbehandler,
                    "beslutter" to meldekortBehandling.beslutter,
                    "status" to meldekortBehandling.status.toDb(),
                    "navkontor" to meldekortBehandling.navkontor.kontornummer,
                    "iverksatt_tidspunkt" to meldekortBehandling.iverksattTidspunkt,
                    "sendt_til_beslutning" to meldekortBehandling.sendtTilBeslutning,
                    "meldeperiode_id" to meldekortBehandling.meldeperiode.id.toString(),
                    "begrunnelse" to meldekortBehandling.begrunnelse?.verdi,
                    "attesteringer" to meldekortBehandling.attesteringer.toDbJson(),
                ).asUpdate,
            )
        }
    }

    override fun hentForSakId(
        sakId: SakId,
        sessionContext: SessionContext?,
    ): MeldekortBehandlinger? {
        return sessionFactory.withSession(sessionContext) { session ->
            hentForSakId(sakId, session)
        }
    }

    override fun hent(meldekortId: MeldekortId, sessionContext: SessionContext?): MeldekortBehandling? {
        return sessionFactory.withSession(sessionContext) { session ->
            hentForMeldekortId(meldekortId, session)
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
                      s.fnr,
                      s.saksnummer
                    from meldekortbehandling m
                    join sak s on s.id = m.sak_id
                    where m.id = :id
                    """,
                    "id" to meldekortId.toString(),
                ).map { fromRow(it, session) }.asSingle,
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
                      s.fnr,
                      s.saksnummer
                    from meldekortbehandling m
                    join sak s on s.id = m.sak_id
                    where s.id = :sakId
                    order by m.fra_og_med
                    """,
                    "sakId" to sakId.toString(),
                ).map { fromRow(it, session) }.asList,
            ).let { it.toNonEmptyListOrNull()?.tilMeldekortperioder() }
        }

        private fun fromRow(
            row: Row,
            session: Session,
        ): MeldekortBehandling {
            val meldeperiodeId = MeldeperiodeId.fromString(row.string("meldeperiode_id"))
            val meldeperiode = MeldeperiodePostgresRepo.hentForMeldeperiodeId(meldeperiodeId, session)!!

            val id = MeldekortId.fromString(row.string("id"))
            val sakId = SakId.fromString(row.string("sak_id"))
            val saksnummer =
                Saksnummer(row.string("saksnummer"))
            val navkontorEnhetsnummer = row.string("navkontor")
            val navkontorNavn = row.stringOrNull("navkontor_navn")
            val fnr = Fnr.fromString(row.string("fnr"))
            val maksDagerMedTiltakspengerForPeriode = meldeperiode.antallDagerForPeriode
            val opprettet = row.localDateTime("opprettet")
            val ikkeRettTilTiltakspengerTidspunkt = row.localDateTimeOrNull("ikke_rett_til_tiltakspenger_tidspunkt")
            val type = row.string("type").tilMeldekortBehandlingType()
            val begrunnelse = row.stringOrNull("begrunnelse")?.let { MeldekortbehandlingBegrunnelse(verdi = it) }

            val navkontor = Navkontor(kontornummer = navkontorEnhetsnummer, kontornavn = navkontorNavn)
            val attesteringer = row.string("attesteringer").toAttesteringer().toAttesteringer()

            val saksbehandler = row.string("saksbehandler")

            val brukersMeldekort = BrukersMeldekortPostgresRepo.hentForMeldeperiodeId(
                meldeperiodeId,
                session,
            )
            val meldekortdager = row.string("meldekortdager")

            return when (val status = row.string("status").toMeldekortBehandlingStatus()) {
                MeldekortBehandlingStatus.GODKJENT, MeldekortBehandlingStatus.KLAR_TIL_BESLUTNING -> {
                    val beregninger = row.string("beregninger").tilberegninger()

                    val meldekortBeregning = MeldekortBeregning.UtfyltMeldeperiode(
                        sakId = sakId,
                        maksDagerMedTiltakspengerForPeriode = maksDagerMedTiltakspengerForPeriode,
                        beregninger = beregninger,
                    )

                    MeldekortBehandlet(
                        id = id,
                        sakId = sakId,
                        saksnummer = saksnummer,
                        fnr = fnr,
                        opprettet = opprettet,
                        beregning = meldekortBeregning,
                        navkontor = navkontor,
                        ikkeRettTilTiltakspengerTidspunkt = ikkeRettTilTiltakspengerTidspunkt,
                        brukersMeldekort = brukersMeldekort,
                        meldeperiode = meldeperiode,
                        saksbehandler = saksbehandler,
                        type = type,
                        begrunnelse = begrunnelse,
                        attesteringer = attesteringer,
                        sendtTilBeslutning = row.localDateTimeOrNull("sendt_til_beslutning"),
                        beslutter = row.stringOrNull("beslutter"),
                        status = status,
                        iverksattTidspunkt = row.localDateTimeOrNull("iverksatt_tidspunkt"),
                    )
                }
                // TODO jah: Her blander vi sammen behandlingsstatus og om man har rett/ikke-rett. Det er mulig at man har startet en meldekortbehandling også endres statusen til IKKE_RETT_TIL_TILTAKSPENGER. Da vil behandlingen sånn som koden er nå implisitt avsluttes. Det kan hende vi bør endre dette når vi skiller grunnlag, innsending og behandling.
                MeldekortBehandlingStatus.IKKE_BEHANDLET, MeldekortBehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER -> {
                    val beregning = Either.catch {
                        MeldekortBeregning.IkkeUtfyltMeldeperiode(
                            sakId = sakId,
                            maksDagerMedTiltakspengerForPeriode = maksDagerMedTiltakspengerForPeriode,
                            dager = meldekortdager.tilIkkeUtfylteMeldekortDager(id),
                        )
                    }.getOrElse {
                        val beregninger = row.string("beregninger").tilberegninger()

                        MeldekortBeregning.UtfyltMeldeperiode(
                            sakId = sakId,
                            maksDagerMedTiltakspengerForPeriode = maksDagerMedTiltakspengerForPeriode,
                            beregninger = beregninger,
                        )
                    }
                    MeldekortUnderBehandling(
                        id = id,
                        sakId = sakId,
                        saksnummer = saksnummer,
                        fnr = fnr,
                        opprettet = opprettet,
                        beregning = beregning,
                        navkontor = navkontor,
                        ikkeRettTilTiltakspengerTidspunkt = ikkeRettTilTiltakspengerTidspunkt,
                        brukersMeldekort = brukersMeldekort,
                        meldeperiode = meldeperiode,
                        saksbehandler = saksbehandler,
                        type = type,
                        begrunnelse = begrunnelse,
                        attesteringer = attesteringer,
                        sendtTilBeslutning = row.localDateTimeOrNull("sendt_til_beslutning"),
                    )
                }
            }
        }
    }
}
