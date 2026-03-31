package no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo

import arrow.core.toNonEmptyListOrNull
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.attesteringer.toAttesteringer
import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.attesteringer.toDbJson
import no.nav.tiltakspenger.saksbehandling.beregning.infra.repo.tilBeregningFraMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.beregning.infra.repo.tilBeregningerDbJsonString
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.felles.toAttesteringer
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.toAvbrutt
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.toDbJson
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortBehandletAutomatisk
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingAvbrutt
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingManuell
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandlinger
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.dbjson.tilDb
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.dbjson.tilMeldekortDager
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.dbjson.tilMeldekortDagerDbJson
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.dbjson.tilMeldekortbehandlingType
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.dbjson.toDb
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.dbjson.toMeldekortbehandlingStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortbehandlingRepo
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringMedMetadata
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo.toDbJson
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo.toSimuleringFraDbJson
import java.time.LocalDateTime

class MeldekortbehandlingPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : MeldekortbehandlingRepo {
    override fun lagre(
        meldekortbehandling: Meldekortbehandling,
        simuleringMedMetadata: SimuleringMedMetadata?,
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
                        simulering,
                        simulering_metadata,
                        saksbehandler,
                        beslutter,
                        status,
                        navkontor,
                        iverksatt_tidspunkt,
                        sendt_til_beslutning,
                        navkontor_navn,
                        type,
                        begrunnelse,
                        attesteringer,
                        brukers_meldekort_id,
                        avbrutt,
                        sist_endret,
                        skalSendeVedtaksbrev
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
                        to_jsonb(:simulering::jsonb),
                        :simulering_metadata,
                        :saksbehandler,
                        :beslutter,
                        :status,
                        :navkontor,
                        :iverksatt_tidspunkt,
                        :sendt_til_beslutning,
                        :navkontor_navn,
                        :type,
                        :begrunnelse,
                        to_jsonb(:attesteringer::jsonb),
                        :brukers_meldekort_id,
                        to_jsonb(:avbrutt::jsonb),
                        :sist_endret,
                        :skal_sende_vedtaksbrev
                    )
                    """,
                    "id" to meldekortbehandling.id.toString(),
                    "meldeperiode_kjede_id" to meldekortbehandling.kjedeId.toString(),
                    "meldeperiode_id" to meldekortbehandling.meldeperiode.id.toString(),
                    "sak_id" to meldekortbehandling.sakId.toString(),
                    "opprettet" to meldekortbehandling.opprettet,
                    "fra_og_med" to meldekortbehandling.fraOgMed,
                    "til_og_med" to meldekortbehandling.tilOgMed,
                    "meldekortdager" to meldekortbehandling.dager.tilMeldekortDagerDbJson(),
                    "beregninger" to meldekortbehandling.beregning?.tilBeregningerDbJsonString(),
                    "simulering" to simuleringMedMetadata?.toDbJson(),
                    // den er ferdig serialisert
                    "simulering_metadata" to simuleringMedMetadata?.originalResponseBody,
                    "saksbehandler" to meldekortbehandling.saksbehandler,
                    "beslutter" to meldekortbehandling.beslutter,
                    "status" to meldekortbehandling.status.toDb(),
                    "navkontor" to meldekortbehandling.navkontor.kontornummer,
                    "iverksatt_tidspunkt" to meldekortbehandling.iverksattTidspunkt,
                    "sendt_til_beslutning" to meldekortbehandling.sendtTilBeslutning,
                    "navkontor_navn" to meldekortbehandling.navkontor.kontornavn,
                    "type" to meldekortbehandling.type.tilDb(),
                    "begrunnelse" to meldekortbehandling.begrunnelse?.verdi,
                    "attesteringer" to meldekortbehandling.attesteringer.toDbJson(),
                    "brukers_meldekort_id" to meldekortbehandling.brukersMeldekort?.id?.toString(),
                    "avbrutt" to meldekortbehandling.avbrutt?.toDbJson(),
                    "sist_endret" to meldekortbehandling.sistEndret,
                    "skal_sende_vedtaksbrev" to meldekortbehandling.skalSendeVedtaksbrev,
                ).asUpdate,
            )
        }
    }

    override fun oppdater(
        meldekortbehandling: Meldekortbehandling,
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
                        attesteringer = to_json(:attesteringer::jsonb),
                        avbrutt = to_jsonb(:avbrutt::jsonb),
                        sist_endret = :sist_endret
                    where id = :id
                    """,
                    "id" to meldekortbehandling.id.toString(),
                    "meldekortdager" to meldekortbehandling.dager.tilMeldekortDagerDbJson(),
                    "beregninger" to meldekortbehandling.beregning?.tilBeregningerDbJsonString(),
                    "saksbehandler" to meldekortbehandling.saksbehandler,
                    "beslutter" to meldekortbehandling.beslutter,
                    "status" to meldekortbehandling.status.toDb(),
                    "navkontor" to meldekortbehandling.navkontor.kontornummer,
                    "iverksatt_tidspunkt" to meldekortbehandling.iverksattTidspunkt,
                    "sendt_til_beslutning" to meldekortbehandling.sendtTilBeslutning,
                    "meldeperiode_id" to meldekortbehandling.meldeperiode.id.toString(),
                    "begrunnelse" to meldekortbehandling.begrunnelse?.verdi,
                    "attesteringer" to meldekortbehandling.attesteringer.toDbJson(),
                    "avbrutt" to meldekortbehandling.avbrutt?.toDbJson(),
                    "sist_endret" to meldekortbehandling.sistEndret,
                ).asUpdate,
            )
        }
    }

    override fun oppdater(
        meldekortbehandling: Meldekortbehandling,
        simuleringMedMetadata: SimuleringMedMetadata?,
        transactionContext: TransactionContext?,
    ) {
        sessionFactory.withTransaction(transactionContext) { tx ->
            tx.run(
                sqlQuery(
                    """
                    update meldekortbehandling set
                        meldekortdager = to_jsonb(:meldekortdager::jsonb),
                        beregninger = to_jsonb(:beregninger::jsonb),
                        simulering = to_jsonb(:simulering::jsonb),
                        simulering_metadata = :simulering_metadata,
                        saksbehandler = :saksbehandler,
                        beslutter = :beslutter,
                        status = :status,
                        navkontor = :navkontor,
                        iverksatt_tidspunkt = :iverksatt_tidspunkt,
                        sendt_til_beslutning = :sendt_til_beslutning,
                        ikke_rett_til_tiltakspenger_tidspunkt = :ikke_rett_til_tiltakspenger_tidspunkt,
                        meldeperiode_id = :meldeperiode_id,
                        begrunnelse = :begrunnelse,
                        attesteringer = to_json(:attesteringer::jsonb),
                        avbrutt = to_jsonb(:avbrutt::jsonb),
                        sist_endret = :sist_endret,
                        tekst_til_vedtaksbrev = :tekst_til_vedtaksbrev,
                        skal_sende_vedtaksbrev = :skal_sende_vedtaksbrev
                    where id = :id
                    """,
                    "id" to meldekortbehandling.id.toString(),
                    "meldekortdager" to meldekortbehandling.dager.tilMeldekortDagerDbJson(),
                    "beregninger" to meldekortbehandling.beregning?.tilBeregningerDbJsonString(),
                    "simulering" to simuleringMedMetadata?.toDbJson(),
                    "simulering_metadata" to simuleringMedMetadata?.originalResponseBody,
                    "saksbehandler" to meldekortbehandling.saksbehandler,
                    "beslutter" to meldekortbehandling.beslutter,
                    "status" to meldekortbehandling.status.toDb(),
                    "navkontor" to meldekortbehandling.navkontor.kontornummer,
                    "iverksatt_tidspunkt" to meldekortbehandling.iverksattTidspunkt,
                    "ikke_rett_til_tiltakspenger_tidspunkt" to meldekortbehandling.ikkeRettTilTiltakspengerTidspunkt,
                    "sendt_til_beslutning" to meldekortbehandling.sendtTilBeslutning,
                    "meldeperiode_id" to meldekortbehandling.meldeperiode.id.toString(),
                    "begrunnelse" to meldekortbehandling.begrunnelse?.verdi,
                    "attesteringer" to meldekortbehandling.attesteringer.toDbJson(),
                    "avbrutt" to meldekortbehandling.avbrutt?.toDbJson(),
                    "sist_endret" to meldekortbehandling.sistEndret,
                    "tekst_til_vedtaksbrev" to meldekortbehandling.fritekstTilVedtaksbrev?.verdi,
                    "skal_sende_vedtaksbrev" to meldekortbehandling.skalSendeVedtaksbrev,
                ).asUpdate,
            )
        }
    }

    override fun hentForSakId(
        sakId: SakId,
        sessionContext: SessionContext?,
    ): Meldekortbehandlinger? {
        return sessionFactory.withSession(sessionContext) { session ->
            hentForSakId(sakId, session)
        }
    }

    override fun hent(meldekortId: MeldekortId, sessionContext: SessionContext?): Meldekortbehandling? {
        return sessionFactory.withSession(sessionContext) { session ->
            hentForMeldekortId(meldekortId, session)
        }
    }

    override fun overtaSaksbehandler(
        meldekortId: MeldekortId,
        nySaksbehandler: Saksbehandler,
        nåværendeSaksbehandler: String,
        sistEndret: LocalDateTime,
        sessionContext: SessionContext?,
    ): Boolean {
        return sessionFactory.withSession(sessionContext) { sx ->
            sx.run(
                sqlQuery(
                    """
                    update meldekortbehandling set
                        saksbehandler = :nySaksbehandler,
                        beslutter = CASE WHEN beslutter = :nySaksbehandler THEN null ELSE beslutter END,
                        sist_endret = :sist_endret
                    where id = :id and saksbehandler = :lagretSaksbehandler
                    """,
                    "id" to meldekortId.toString(),
                    "nySaksbehandler" to nySaksbehandler.navIdent,
                    "lagretSaksbehandler" to nåværendeSaksbehandler,
                    "sist_endret" to sistEndret,
                ).asUpdate,
            ) > 0
        }
    }

    override fun overtaBeslutter(
        meldekortId: MeldekortId,
        nyBeslutter: Saksbehandler,
        nåværendeBeslutter: String,
        sistEndret: LocalDateTime,
        sessionContext: SessionContext?,
    ): Boolean {
        return sessionFactory.withSession(sessionContext) { sx ->
            sx.run(
                queryOf(
                    """update meldekortbehandling set beslutter = :nyBeslutter, sist_endret = :sist_endret where id = :id and beslutter = :lagretBeslutter""",
                    mapOf(
                        "id" to meldekortId.toString(),
                        "nyBeslutter" to nyBeslutter.navIdent,
                        "lagretBeslutter" to nåværendeBeslutter,
                        "sist_endret" to sistEndret,
                    ),
                ).asUpdate,
            ) > 0
        }
    }

    override fun taBehandlingSaksbehandler(
        meldekortId: MeldekortId,
        saksbehandler: Saksbehandler,
        meldekortbehandlingStatus: MeldekortbehandlingStatus,
        sistEndret: LocalDateTime,
        sessionContext: SessionContext?,
    ): Boolean {
        return sessionFactory.withSession(sessionContext) { sx ->
            sx.run(
                sqlQuery(
                    """
                    update meldekortbehandling set
                        saksbehandler = :saksbehandler,
                        status = :status,
                        beslutter = CASE WHEN beslutter = :saksbehandler THEN null ELSE beslutter END,
                        sist_endret = :sist_endret
                    where id = :id and saksbehandler is null
                    """,
                    "id" to meldekortId.toString(),
                    "saksbehandler" to saksbehandler.navIdent,
                    "status" to meldekortbehandlingStatus.toDb(),
                    "sist_endret" to sistEndret,
                ).asUpdate,
            ) > 0
        }
    }

    override fun taBehandlingBeslutter(
        meldekortId: MeldekortId,
        beslutter: Saksbehandler,
        meldekortbehandlingStatus: MeldekortbehandlingStatus,
        sistEndret: LocalDateTime,
        sessionContext: SessionContext?,
    ): Boolean {
        return sessionFactory.withSession(sessionContext) { sx ->
            sx.run(
                queryOf(
                    """update meldekortbehandling set beslutter = :beslutter, status = :status, sist_endret = :sist_endret where id = :id and beslutter is null""",
                    mapOf(
                        "id" to meldekortId.toString(),
                        "beslutter" to beslutter.navIdent,
                        "status" to meldekortbehandlingStatus.toDb(),
                        "sist_endret" to sistEndret,
                    ),
                ).asUpdate,
            ) > 0
        }
    }

    override fun leggTilbakeBehandlingSaksbehandler(
        meldekortId: MeldekortId,
        nåværendeSaksbehandler: Saksbehandler,
        meldekortbehandlingStatus: MeldekortbehandlingStatus,
        sistEndret: LocalDateTime,
        sessionContext: SessionContext?,
    ): Boolean {
        return sessionFactory.withSession(sessionContext) { sx ->
            sx.run(
                queryOf(
                    """update meldekortbehandling set saksbehandler = null, status = :status, sist_endret = :sist_endret where id = :id and saksbehandler = :lagretSaksbehandler""",
                    mapOf(
                        "id" to meldekortId.toString(),
                        "lagretSaksbehandler" to nåværendeSaksbehandler.navIdent,
                        "status" to meldekortbehandlingStatus.toDb(),
                        "sist_endret" to sistEndret,
                    ),
                ).asUpdate,
            ) > 0
        }
    }

    override fun leggTilbakeBehandlingBeslutter(
        meldekortId: MeldekortId,
        nåværendeBeslutter: Saksbehandler,
        meldekortbehandlingStatus: MeldekortbehandlingStatus,
        sistEndret: LocalDateTime,
        sessionContext: SessionContext?,
    ): Boolean {
        return sessionFactory.withSession(sessionContext) { sx ->
            sx.run(
                queryOf(
                    """update meldekortbehandling set beslutter = null, status = :status, sist_endret = :sist_endret where id = :id and beslutter = :lagretBeslutter""",
                    mapOf(
                        "id" to meldekortId.toString(),
                        "lagretBeslutter" to nåværendeBeslutter.navIdent,
                        "status" to meldekortbehandlingStatus.toDb(),
                        "sist_endret" to sistEndret,
                    ),
                ).asUpdate,
            ) > 0
        }
    }

    override fun hentBehandlingerTilDatadeling(limit: Int): List<Meldekortbehandling> {
        return sessionFactory.withSession { session ->
            session.run(
                sqlQuery(
                    """
                        select
                          m.*,
                          s.fnr,
                          s.saksnummer
                        from meldekortbehandling m
                        join sak s on s.id = m.sak_id
                        where (m.behandling_sendt_til_datadeling is null or m.behandling_sendt_til_datadeling < m.sist_endret) and s.sendt_til_datadeling is not null
                        order by m.opprettet
                        limit $limit
                    """.trimIndent(),
                ).map { fromRow(it, session) }.asList,
            )
        }
    }

    override fun markerBehandlingSendtTilDatadeling(meldekortId: MeldekortId, tidspunkt: LocalDateTime) {
        sessionFactory.withSession { session ->
            session.run(
                sqlQuery(
                    """
                    update meldekortbehandling
                    set behandling_sendt_til_datadeling = :tidspunkt
                    where id = :id
                    """.trimIndent(),
                    "id" to meldekortId.toString(),
                    "tidspunkt" to tidspunkt,
                ).asUpdate,
            )
        }
    }

    companion object {
        internal fun hentForMeldekortId(
            meldekortId: MeldekortId,
            session: Session,
        ): Meldekortbehandling? {
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
        ): Meldekortbehandlinger? {
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
                    order by m.opprettet
                    """,
                    "sakId" to sakId.toString(),
                ).map { fromRow(it, session) }.asList,
            ).let { behandlinger ->
                behandlinger.toNonEmptyListOrNull()?.let {
                    Meldekortbehandlinger(it)
                }
            }
        }

        private fun fromRow(
            row: Row,
            session: Session,
        ): Meldekortbehandling {
            val meldeperiodeId = MeldeperiodeId.fromString(row.string("meldeperiode_id"))
            val meldeperiode = MeldeperiodePostgresRepo.hentForMeldeperiodeId(meldeperiodeId, session)!!

            val id = MeldekortId.fromString(row.string("id"))
            val sakId = SakId.fromString(row.string("sak_id"))
            val saksnummer =
                Saksnummer(row.string("saksnummer"))
            val navkontorEnhetsnummer = row.string("navkontor")
            val navkontorNavn = row.stringOrNull("navkontor_navn")
            val fnr = Fnr.fromString(row.string("fnr"))
            val opprettet = row.localDateTime("opprettet")
            val ikkeRettTilTiltakspengerTidspunkt = row.localDateTimeOrNull("ikke_rett_til_tiltakspenger_tidspunkt")
            val type = row.string("type").tilMeldekortbehandlingType()
            val begrunnelse = row.stringOrNull("begrunnelse")?.let { Begrunnelse.create(it) }

            val navkontor = Navkontor(kontornummer = navkontorEnhetsnummer, kontornavn = navkontorNavn)
            val attesteringer = row.string("attesteringer").toAttesteringer().toAttesteringer()

            val saksbehandler = row.stringOrNull("saksbehandler")

            val dager = row.string("meldekortdager").tilMeldekortDager(meldeperiode)
            val simulering = row.stringOrNull("simulering")
                ?.toSimuleringFraDbJson(MeldeperiodePostgresRepo.hentMeldeperiodekjederForSakId(sakId, session))

            val brukersMeldekort = row.stringOrNull("brukers_meldekort_id")?.let {
                BrukersMeldekortPostgresRepo.hentForMeldekortId(
                    MeldekortId.fromString(it),
                    session,
                )
            }

            val iverksattTidspunkt = row.localDateTimeOrNull("iverksatt_tidspunkt")
            val sistEndret = row.localDateTime("sist_endret")

            val beregning = row.stringOrNull("beregninger")
                ?.tilBeregningFraMeldekortbehandling(id)

            val fritekstTilVedtaksbrev = row.stringOrNull("tekst_til_vedtaksbrev")?.let {
                FritekstTilVedtaksbrev.create(it)
            }

            val skalSendeVedtaksbrev = row.boolean("skal_sende_vedtaksbrev")

            return when (val status = row.string("status").toMeldekortbehandlingStatus()) {
                MeldekortbehandlingStatus.AUTOMATISK_BEHANDLET -> {
                    requireNotNull(brukersMeldekort) {
                        "Fant ikke brukers meldekort for automatisk meldekortbehandling $id"
                    }

                    MeldekortBehandletAutomatisk(
                        id = id,
                        sakId = sakId,
                        saksnummer = saksnummer,
                        fnr = fnr,
                        opprettet = opprettet,
                        navkontor = navkontor,
                        brukersMeldekort = brukersMeldekort,
                        meldeperiode = meldeperiode,
                        beregning = beregning!!,
                        simulering = simulering,
                        dager = dager,
                        type = type,
                        status = status,
                        sistEndret = sistEndret,
                    )
                }

                MeldekortbehandlingStatus.GODKJENT, MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING, MeldekortbehandlingStatus.UNDER_BESLUTNING -> {
                    MeldekortbehandlingManuell(
                        id = id,
                        sakId = sakId,
                        saksnummer = saksnummer,
                        fnr = fnr,
                        opprettet = opprettet,
                        navkontor = navkontor,
                        ikkeRettTilTiltakspengerTidspunkt = ikkeRettTilTiltakspengerTidspunkt,
                        brukersMeldekort = brukersMeldekort,
                        meldeperiode = meldeperiode,
                        saksbehandler = saksbehandler!!,
                        type = type,
                        begrunnelse = begrunnelse,
                        attesteringer = attesteringer,
                        sendtTilBeslutning = row.localDateTimeOrNull("sendt_til_beslutning"),
                        beslutter = row.stringOrNull("beslutter"),
                        status = status,
                        iverksattTidspunkt = iverksattTidspunkt,
                        beregning = beregning!!,
                        simulering = simulering,
                        dager = dager,
                        sistEndret = sistEndret,
                        fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
                        skalSendeVedtaksbrev = skalSendeVedtaksbrev,
                    )
                }

                MeldekortbehandlingStatus.UNDER_BEHANDLING, MeldekortbehandlingStatus.KLAR_TIL_BEHANDLING -> {
                    MeldekortUnderBehandling(
                        id = id,
                        sakId = sakId,
                        saksnummer = saksnummer,
                        fnr = fnr,
                        opprettet = opprettet,
                        navkontor = navkontor,
                        ikkeRettTilTiltakspengerTidspunkt = ikkeRettTilTiltakspengerTidspunkt,
                        brukersMeldekort = brukersMeldekort,
                        meldeperiode = meldeperiode,
                        saksbehandler = saksbehandler,
                        type = type,
                        begrunnelse = begrunnelse,
                        attesteringer = attesteringer,
                        sendtTilBeslutning = row.localDateTimeOrNull("sendt_til_beslutning"),
                        beregning = beregning,
                        simulering = simulering,
                        dager = dager,
                        status = status,
                        sistEndret = sistEndret,
                        fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
                        skalSendeVedtaksbrev = skalSendeVedtaksbrev,
                    )
                }

                MeldekortbehandlingStatus.AVBRUTT, MeldekortbehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER -> {
                    MeldekortbehandlingAvbrutt(
                        id = id,
                        sakId = sakId,
                        saksnummer = saksnummer,
                        fnr = fnr,
                        opprettet = opprettet,
                        navkontor = navkontor,
                        ikkeRettTilTiltakspengerTidspunkt = ikkeRettTilTiltakspengerTidspunkt,
                        brukersMeldekort = brukersMeldekort,
                        meldeperiode = meldeperiode,
                        saksbehandler = saksbehandler,
                        type = type,
                        begrunnelse = begrunnelse,
                        attesteringer = attesteringer,
                        beregning = beregning,
                        simulering = simulering,
                        dager = dager,
                        avbrutt = row.stringOrNull("avbrutt")?.toAvbrutt(),
                        sistEndret = sistEndret,
                        fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
                        skalSendeVedtaksbrev = skalSendeVedtaksbrev,
                    )
                }
            }
        }
    }
}
