package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.Session
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionContext.Companion.withSession
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RammebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlinger
import no.nav.tiltakspenger.saksbehandling.klage.infra.repo.KlagebehandlingPostgresRepo
import java.time.Clock
import java.time.LocalDateTime

private val log = KotlinLogging.logger {}

class RammebehandlingPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
    private val clock: Clock,
) : RammebehandlingRepo {

    override fun hent(
        behandlingId: RammebehandlingId,
        sessionContext: SessionContext?,
    ): Rammebehandling {
        return sessionFactory.withSession(sessionContext) { session ->
            hentOrNull(behandlingId, session)!!
        }
    }

    override fun lagre(
        behandling: Rammebehandling,
        transactionContext: TransactionContext?,
    ) {
        sessionFactory.withTransaction(transactionContext) { transactionalSession ->
            if (finnes(behandling.id, transactionalSession)) {
                oppdaterRammebehandling(behandling, transactionalSession)
            } else {
                opprettRammebehandling(behandling, transactionalSession)
            }
            if (behandling.klagebehandling != null) {
                KlagebehandlingPostgresRepo.lagreKlagebehandling(
                    klagebehandling = behandling.klagebehandling!!,
                    session = transactionalSession,
                )
            }
        }
    }

    /**
     * Siden vi ikke er interessert i og hente ut metadataene igjen, er dette en egen funksjon.
     * Denne blir kalt samtidig som [lagre] i en og samme transaksjon, så vi trenger ikke mutere sist_endret her.
     */
    override fun oppdaterSimuleringMetadata(
        behandlingId: RammebehandlingId,
        originalResponseBody: String?,
        sessionContext: SessionContext,
    ) {
        sessionContext.withSession { session ->
            session.run(
                queryOf(
                    """update behandling set simulering_metadata = :simulering_metadata where id = :id""",
                    mapOf(
                        "id" to behandlingId.toString(),
                        "simulering_metadata" to originalResponseBody,
                    ),
                ).asUpdate,
            )
        }
    }

    /**
     * Oppdaterer behandlingsstatus, og saksbehandler bare dersom den er null.
     * Skal du endre saksbehandler bruk [overtaSaksbehandler]
     */
    override fun taBehandlingSaksbehandler(
        rammebehandling: Rammebehandling,
        transactionContext: TransactionContext?,
    ): Boolean {
        return sessionFactory.withTransaction(transactionContext) { tx ->
            rammebehandling.klagebehandling?.also {
                KlagebehandlingPostgresRepo.taBehandling(it, tx)
            }
            tx.run(
                sqlQuery(
                    """
                    update behandling set
                        saksbehandler = :saksbehandler,
                        status = :status,
                        beslutter = CASE WHEN beslutter = :saksbehandler THEN null ELSE beslutter END,
                        sist_endret = :sist_endret
                    where id = :id and saksbehandler is null and status = 'KLAR_TIL_BEHANDLING'
                    """,
                    "id" to rammebehandling.id.toString(),
                    "saksbehandler" to rammebehandling.saksbehandler,
                    "status" to rammebehandling.status.toDb(),
                    "sist_endret" to rammebehandling.sistEndret,
                ).asUpdate,
            ) > 0
        }
    }

    /**
     * Oppdaterer behandlingsstatus, og beslutter bare dersom den er null.
     * Skal du endre beslutter bruk [overtaSaksbehandler]
     */
    override fun taBehandlingBeslutter(
        rammebehandling: Rammebehandling,
        sessionContext: SessionContext?,
    ): Boolean {
        return sessionFactory.withSession(sessionContext) { sx ->
            sx.run(
                sqlQuery(
                    """update behandling set beslutter = :beslutter, status = :status, sist_endret = :sist_endret where id = :id and beslutter is null and status = 'KLAR_TIL_BESLUTNING'""",
                    "id" to rammebehandling.id.toString(),
                    "beslutter" to rammebehandling.beslutter!!,
                    "status" to rammebehandling.status.toDb(),
                    "sist_endret" to rammebehandling.sistEndret,
                ).asUpdate,
            ) > 0
        }
    }

    override fun angreBehandling(
        rammebehandling: Rammebehandling,
        transactionContext: TransactionContext?,
    ): Boolean {
        return sessionFactory.withTransaction(transactionContext) { tx ->
            rammebehandling.klagebehandling?.also {
                KlagebehandlingPostgresRepo.taBehandling(it, tx)
            }
            tx.run(
                sqlQuery(
                    """
                    update behandling set
                        status = :status,
                        sist_endret = :sist_endret
                    where id = :id and saksbehandler is not null and status = 'KLAR_TIL_BESLUTNING'
                    """,
                    "id" to rammebehandling.id.toString(),
                    "saksbehandler" to rammebehandling.saksbehandler,
                    "status" to rammebehandling.status.toDb(),
                    "sist_endret" to rammebehandling.sistEndret,
                ).asUpdate,
            ) > 0
        }
    }

    /**
     * En ny saksbehandler overtar for [nåværendeSaksbehandler].
     * Dersom det ikke er en saksbehandler på behandlingen, bruk [taBehandlingSaksbehandler]
     * @param nåværendeSaksbehandler For å unngå at to saksbehandlere kan overta samtidig.
     */
    override fun overtaSaksbehandler(
        rammebehandling: Rammebehandling,
        nåværendeSaksbehandler: String,
        transactionContext: TransactionContext?,
    ): Boolean {
        return sessionFactory.withTransaction(transactionContext) { tx ->
            rammebehandling.klagebehandling?.also {
                KlagebehandlingPostgresRepo.overtaBehandling(it, nåværendeSaksbehandler, tx)
            }
            tx.run(
                sqlQuery(
                    """
                    update behandling set
                        saksbehandler = :nySaksbehandler,
                        status = :status,
                        beslutter = CASE WHEN beslutter = :nySaksbehandler THEN null ELSE beslutter END,
                        sist_endret = :sist_endret
                    where id = :id and saksbehandler = :lagretSaksbehandler and status in ('UNDER_BEHANDLING', 'UNDER_AUTOMATISK_BEHANDLING')
                    """,
                    "id" to rammebehandling.id.toString(),
                    "nySaksbehandler" to rammebehandling.saksbehandler,
                    "status" to rammebehandling.status.toDb(),
                    "lagretSaksbehandler" to nåværendeSaksbehandler,
                    "sist_endret" to rammebehandling.sistEndret,
                ).asUpdate,
            ) > 0
        }
    }

    /**
     * En ny beslutter overtar for [nåværendeBeslutter].
     * Dersom det ikke er en beslutter på behandlingen, bruk [taBehandlingBeslutter]
     * @param nåværendeBeslutter For å unngå at to besluttere kan overta samtidig.
     */
    override fun overtaBeslutter(
        rammebehandling: Rammebehandling,
        nåværendeBeslutter: String,
        sessionContext: SessionContext?,
    ): Boolean {
        return sessionFactory.withSession(sessionContext) { sx ->
            sx.run(
                sqlQuery(
                    """update behandling set beslutter = :nyBeslutter, sist_endret = :sist_endret where id = :id and beslutter = :lagretBeslutter and status = 'UNDER_BESLUTNING'""",
                    "id" to rammebehandling.id.toString(),
                    "nyBeslutter" to rammebehandling.beslutter!!,
                    "lagretBeslutter" to nåværendeBeslutter,
                    "sist_endret" to rammebehandling.sistEndret,
                ).asUpdate,
            ) > 0
        }
    }

    /** Siden dette er på tvers av saker, gir det ikke mening og bruke [Rammebehandlinger] */
    override fun hentBehandlingerTilDatadeling(limit: Int): List<Rammebehandling> {
        return sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    // language=SQL
                    """
                    select b.*,sak.saksnummer,sak.fnr
                    from behandling b
                    join sak on sak.id = b.sak_id
                    where (b.sendt_til_datadeling is null or b.sendt_til_datadeling < b.sist_endret) and sak.sendt_til_datadeling is not null
                    order by b.opprettet
                    limit :limit
                    """.trimIndent(),
                    mapOf(
                        "limit" to limit,
                    ),
                ).map { it.toBehandling(session) }.asList,
            )
        }
    }

    override fun markerSendtTilDatadeling(id: RammebehandlingId, tidspunkt: LocalDateTime) {
        sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    """
                    update behandling set sendt_til_datadeling = :tidspunkt where id = :id
                    """.trimIndent(),
                    mapOf(
                        "id" to id.toString(),
                        "tidspunkt" to tidspunkt,
                    ),
                ).asUpdate,
            )
        }
    }

    override fun hentAutomatiskeSoknadsbehandlingIder(limit: Int): List<RammebehandlingId> {
        return sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    //language=SQL
                    """
                    select b.id
                    from behandling b
                    where
                      b.behandlingstype = 'SØKNADSBEHANDLING' and
                      b.status = 'UNDER_AUTOMATISK_BEHANDLING' and
                      (b.venter_til is null or b.venter_til < :now)
                    order by b.opprettet
                    limit :limit
                    """.trimIndent(),
                    mapOf(
                        "now" to nå(clock),
                        "limit" to limit,
                    ),
                ).map { RammebehandlingId.fromString(it.string("id")) }.asList,
            )
        }
    }

    companion object {
        fun hentOrNull(
            behandlingId: RammebehandlingId,
            session: Session,
        ): Rammebehandling? =
            session.run(
                queryOf(
                    """
                    select b.*,s.fnr, s.saksnummer from behandling b join sak s on s.id = b.sak_id where b.id = :id
                    """.trimIndent(),
                    mapOf(
                        "id" to behandlingId.toString(),
                    ),
                ).map { it.toBehandling(session) }.asSingle,
            )

        fun hentForSakId(
            sakId: SakId,
            session: Session,
        ): Rammebehandlinger =
            session
                .run(
                    sqlQuery(
                        "select b.*,s.fnr, s.saksnummer from behandling b join sak s on s.id = b.sak_id where b.sak_id = :sak_id order by b.opprettet",
                        "sak_id" to sakId.toString(),
                    ).map { it.toBehandling(session) }.asList,
                )
                .let { Rammebehandlinger(it) }

        private fun oppdaterRammebehandling(
            behandling: Rammebehandling,
            session: Session,
        ) {
            log.info { "Oppdaterer behandling ${behandling.id} ${behandling.behandlingstype}" }

            session.run(
                queryOf(
                    """
                    update behandling set
                        vedtaksperiode = :vedtaksperiode::periode,
                        status = :status,
                        sist_endret = :sist_endret,
                        saksbehandler = :saksbehandler,
                        beslutter = :beslutter,
                        attesteringer = :attesteringer::jsonb,
                        iverksatt_tidspunkt = :iverksatt_tidspunkt,
                        sendt_til_beslutning = :sendt_til_beslutning,
                        sendt_til_datadeling = :sendt_til_datadeling,
                        oppgave_id = :oppgave_id,
                        valgt_hjemmel_har_ikke_rettighet = :valgt_hjemmel_har_ikke_rettighet::jsonb,
                        fritekst_vedtaksbrev = :fritekst_vedtaksbrev,
                        begrunnelse_vilkårsvurdering = :begrunnelse_vilkarsvurdering,
                        saksopplysninger = :saksopplysninger::jsonb,
                        barneTillegg = :barnetillegg::jsonb,
                        avbrutt = :avbrutt::jsonb,
                        ventestatus = :ventestatus::jsonb,
                        venter_til = :venter_til,
                        avslagsgrunner = :avslagsgrunner::jsonb,
                        resultat = :resultat,
                        soknad_id = :soknad_id,
                        automatisk_saksbehandlet = :automatisk_saksbehandlet,
                        manuelt_behandles_grunner = :manuelt_behandles_grunner::jsonb,
                        beregning = :beregning::jsonb,
                        simulering = :simulering::jsonb,
                        simulering_metadata = CASE WHEN :simulering::varchar IS NULL THEN NULL ELSE simulering_metadata END,
                        utbetalingskontroll = :utbetalingskontroll::jsonb,
                        navkontor = :navkontor,
                        navkontor_navn = :navkontor_navn,
                        har_valgt_stans_fra_første_dag_som_gir_rett = :har_valgt_stans_fra_forste_dag_som_gir_rett,
                        innvilgelsesperioder = :innvilgelsesperioder::jsonb,
                        omgjør_rammevedtak = :omgjoer_rammevedtak::jsonb,
                        klagebehandling_id = :klagebehandling_id,
                        automatisk_opprettet_grunn = :automatisk_opprettet_grunn::jsonb,
                        skal_sende_vedtaksbrev = :skal_sende_vedtaksbrev
                    where id = :id
                    """.trimIndent(),
                    behandling.tilDbParams(),
                ).asUpdate,
            )
        }

        private fun opprettRammebehandling(
            behandling: Rammebehandling,
            session: Session,
        ) {
            log.info { "Oppretter behandling ${behandling.id} ${behandling.behandlingstype}" }

            session.run(
                queryOf(
                    """
                    insert into behandling (
                        id,
                        sak_id,
                        vedtaksperiode,
                        status,
                        sist_endret,
                        opprettet,
                        saksbehandler,
                        beslutter,
                        attesteringer,
                        iverksatt_tidspunkt,
                        sendt_til_beslutning,
                        sendt_til_datadeling,
                        behandlingstype,
                        oppgave_id,
                        valgt_hjemmel_har_ikke_rettighet,
                        fritekst_vedtaksbrev,
                        begrunnelse_vilkårsvurdering,
                        saksopplysninger,
                        barnetillegg,
                        avbrutt,
                        ventestatus,
                        venter_til,
                        avslagsgrunner,
                        resultat,
                        soknad_id,
                        automatisk_saksbehandlet,
                        manuelt_behandles_grunner,
                        beregning,
                        simulering,
                        utbetalingskontroll,
                        navkontor,
                        navkontor_navn,
                        har_valgt_stans_fra_første_dag_som_gir_rett,
                        innvilgelsesperioder,
                        omgjør_rammevedtak,
                        klagebehandling_id,
                        automatisk_opprettet_grunn,
                        skal_sende_vedtaksbrev
                    ) values (
                        :id,
                        :sak_id,
                        :vedtaksperiode::periode,
                        :status,
                        :sist_endret,
                        :opprettet,
                        :saksbehandler,
                        :beslutter,
                        :attesteringer::jsonb,
                        :iverksatt_tidspunkt,
                        :sendt_til_beslutning,
                        :sendt_til_datadeling,
                        :behandlingstype,
                        :oppgave_id,
                        :valgt_hjemmel_har_ikke_rettighet::jsonb,
                        :fritekst_vedtaksbrev,
                        :begrunnelse_vilkarsvurdering,
                        :saksopplysninger::jsonb,
                        :barnetillegg::jsonb,
                        :avbrutt::jsonb,
                        :ventestatus::jsonb,
                        :venter_til,
                        :avslagsgrunner::jsonb,
                        :resultat,
                        :soknad_id,
                        :automatisk_saksbehandlet,
                        :manuelt_behandles_grunner::jsonb,
                        :beregning::jsonb,
                        :simulering::jsonb,
                        :utbetalingskontroll::jsonb,
                        :navkontor,
                        :navkontor_navn,
                        :har_valgt_stans_fra_forste_dag_som_gir_rett,
                        :innvilgelsesperioder::jsonb,
                        :omgjoer_rammevedtak::jsonb,
                        :klagebehandling_id,
                        :automatisk_opprettet_grunn::jsonb,
                        :skal_sende_vedtaksbrev
                    )
                    """.trimIndent(),
                    behandling.tilDbParams(),
                ).asUpdate,
            )
        }

        /** Avgjør om [lagre] skal gjøre en insert eller en update. */
        private fun finnes(
            behandlingId: RammebehandlingId,
            session: Session,
        ): Boolean =
            // `select exists` gir alltid nøyaktig én rad, så `asSingle` kan ikke gi null.
            session.run(
                queryOf(
                    "select exists(select 1 from behandling where id = :id)",
                    mapOf(
                        "id" to behandlingId.toString(),
                    ),
                ).map { it.boolean(1) }.asSingle,
            )!!
    }
}
