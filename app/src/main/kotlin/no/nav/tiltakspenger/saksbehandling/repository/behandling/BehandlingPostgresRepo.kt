package no.nav.tiltakspenger.saksbehandling.repository.behandling

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.felles.OppgaveId
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.IkkeFunnetException
import no.nav.tiltakspenger.saksbehandling.felles.sikkerlogg
import no.nav.tiltakspenger.saksbehandling.repository.behandling.attesteringer.toAttesteringer
import no.nav.tiltakspenger.saksbehandling.repository.behandling.attesteringer.toDbJson
import no.nav.tiltakspenger.saksbehandling.repository.søknad.SøknadDAO
import no.nav.tiltakspenger.saksbehandling.repository.toAvbrutt
import no.nav.tiltakspenger.saksbehandling.repository.toDbJson
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.BegrunnelseVilkårsvurdering
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.Behandling
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.Behandlinger
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.Søknad
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.BehandlingRepo
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime

class BehandlingPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : BehandlingRepo {
    override fun hent(
        behandlingId: BehandlingId,
        sessionContext: SessionContext?,
    ): Behandling =
        hentOrNull(behandlingId, sessionContext)
            ?: throw IkkeFunnetException("Behandling med id $behandlingId ikke funnet")

    override fun hentOrNull(
        behandlingId: BehandlingId,
        sessionContext: SessionContext?,
    ): Behandling? =
        sessionFactory.withSession(sessionContext) { session ->
            hentOrNull(behandlingId, session)
        }

    /**
     * Denne returnerer ikke [Behandlinger] siden vi ikke har avklart om en person kan ha flere saker. I så fall vil dette bli en liste med [Behandlinger].
     */
    override fun hentAlleForFnr(fnr: Fnr): List<Behandling> {
        return sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    sqlHentBehandlingForFnr,
                    mapOf(
                        "fnr" to fnr.verdi,
                    ),
                ).map { row ->
                    row.toBehandling(session)
                }.asList,
            )
        }
    }

    override fun hentForSøknadId(søknadId: SøknadId): Behandling? =
        sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    """
                    select b.*,sak.saksnummer,sak.fnr
                      from behandling b
                      join søknad s on b.id = s.behandling_id
                      join sak on sak.id = b.sak_id
                      where s.id = :id
                    """.trimIndent(),
                    mapOf(
                        "id" to søknadId.toString(),
                    ),
                ).map { row ->
                    row.toBehandling(session)
                }.asSingle,
            )
        }

    override fun lagre(
        behandling: Behandling,
        transactionContext: TransactionContext?,
    ) {
        sessionFactory.withTransaction(transactionContext) { tx ->
            val sistEndret = hentSistEndret(behandling.id, tx)
            if (sistEndret == null) {
                opprettBehandling(behandling, tx)
                if (behandling.erFørstegangsbehandling && behandling.søknad != null) {
                    SøknadDAO.knyttSøknadTilBehandling(behandling.id, behandling.søknad!!.id, tx)
                }
            } else {
                oppdaterBehandling(sistEndret, behandling, tx)
            }
        }
    }

    companion object {
        fun hentOrNull(
            behandlingId: BehandlingId,
            session: Session,
        ): Behandling? =
            session.run(
                queryOf(
                    sqlHentBehandling,
                    mapOf(
                        "id" to behandlingId.toString(),
                    ),
                ).map { row ->
                    row.toBehandling(session)
                }.asSingle,
            )

        internal fun hentForSakId(
            sakId: SakId,
            session: Session,
        ): Behandlinger =
            session
                .run(
                    queryOf(
                        "select b.*,s.fnr, s.saksnummer from behandling b join sak s on s.id = b.sak_id where b.sak_id = :sak_id order by b.opprettet",
                        mapOf(
                            "sak_id" to sakId.toString(),
                        ),
                    ).map { row ->
                        row.toBehandling(session)
                    }.asList,
                )
                .let { Behandlinger(it) }

        private fun oppdaterBehandling(
            sistEndret: LocalDateTime,
            behandling: Behandling,
            session: Session,
        ) {
            sikkerlogg.info { "Oppdaterer behandling ${behandling.id}" }

            val antRaderOppdatert =
                session.run(
                    queryOf(
                        sqlOppdaterBehandling,
                        mapOf(
                            "id" to behandling.id.toString(),
                            "sak_id" to behandling.sakId.toString(),
                            "virkningsperiode_fra_og_med" to behandling.virkningsperiode?.fraOgMed,
                            "virkningsperiode_til_og_med" to behandling.virkningsperiode?.tilOgMed,
                            "status" to behandling.status.toDb(),
                            "sist_endret_old" to sistEndret,
                            "sist_endret" to behandling.sistEndret,
                            "saksbehandler" to behandling.saksbehandler,
                            "beslutter" to behandling.beslutter,
                            "attesteringer" to behandling.attesteringer.toDbJson(),
                            "iverksatt_tidspunkt" to behandling.iverksattTidspunkt,
                            "sendt_til_beslutning" to behandling.sendtTilBeslutning,
                            "sendt_til_datadeling" to behandling.sendtTilDatadeling,
                            "behandlingstype" to behandling.behandlingstype.toDbValue(),
                            "oppgave_id" to behandling.oppgaveId?.toString(),
                            "fritekst_vedtaksbrev" to behandling.fritekstTilVedtaksbrev?.verdi,
                            "begrunnelse_vilkarsvurdering" to behandling.begrunnelseVilkårsvurdering?.verdi,
                            "saksopplysninger" to behandling.saksopplysninger.toDbJson(),
                            "saksopplysningsperiode_fra_og_med" to behandling.saksopplysningsperiode?.fraOgMed,
                            "saksopplysningsperiode_til_og_med" to behandling.saksopplysningsperiode?.tilOgMed,
                            "barnetillegg" to behandling.barnetillegg?.toDbJson(),
                            "valgte_tiltaksdeltakelser" to behandling.valgteTiltaksdeltakelser?.toDbJson(),
                            "avbrutt" to behandling.avbrutt?.toDbJson(),
                        ),
                    ).asUpdate,
                )
            if (antRaderOppdatert == 0) {
                throw IllegalStateException("Noen andre har endret denne behandlingen ${behandling.id}")
            }
        }

        private fun opprettBehandling(
            behandling: Behandling,
            session: Session,
        ) {
            sikkerlogg.info { "Oppretter behandling ${behandling.id}" }

            session.run(
                queryOf(
                    sqlOpprettBehandling,
                    mapOf(
                        "id" to behandling.id.toString(),
                        "sak_id" to behandling.sakId.toString(),
                        "virkningsperiode_fra_og_med" to behandling.virkningsperiode?.fraOgMed,
                        "virkningsperiode_til_og_med" to behandling.virkningsperiode?.tilOgMed,
                        "status" to behandling.status.toDb(),
                        "opprettet" to behandling.opprettet,
                        "saksopplysninger" to behandling.saksopplysninger.toDbJson(),
                        "saksbehandler" to behandling.saksbehandler,
                        "beslutter" to behandling.beslutter,
                        "attesteringer" to behandling.attesteringer.toDbJson(),
                        "iverksatt_tidspunkt" to behandling.iverksattTidspunkt,
                        "sendt_til_beslutning" to behandling.sendtTilBeslutning,
                        "sendt_til_datadeling" to behandling.sendtTilDatadeling,
                        "sist_endret" to behandling.sistEndret,
                        "behandlingstype" to behandling.behandlingstype.toDbValue(),
                        "oppgave_id" to behandling.oppgaveId?.toString(),
                        "fritekst_vedtaksbrev" to behandling.fritekstTilVedtaksbrev?.verdi,
                        "begrunnelse_vilkarsvurdering" to behandling.begrunnelseVilkårsvurdering?.verdi,
                        "saksopplysningsperiode_fra_og_med" to behandling.saksopplysningsperiode?.fraOgMed,
                        "saksopplysningsperiode_til_og_med" to behandling.saksopplysningsperiode?.tilOgMed,
                        "barnetillegg" to behandling.barnetillegg?.toDbJson(),
                        "valgte_tiltaksdeltakelser" to behandling.valgteTiltaksdeltakelser?.toDbJson(),
                        "avbrutt" to behandling.avbrutt?.toDbJson(),
                    ),
                ).asUpdate,
            )
        }

        private fun hentSistEndret(
            behandlingId: BehandlingId,
            session: Session,
        ): LocalDateTime? =
            session.run(
                queryOf(
                    "select sist_endret from behandling where id = :id",
                    mapOf(
                        "id" to behandlingId.toString(),
                    ),
                ).map { row -> row.localDateTime("sist_endret") }.asSingle,
            )

        private fun Row.toBehandling(session: Session): Behandling {
            val id = BehandlingId.fromString(string("id"))
            val sakId = SakId.fromString(string("sak_id"))
            val virkningsperiodeFraOgMed = localDateOrNull("virkningsperiode_fra_og_med")
            val virkningsperiodeTilOgMed = localDateOrNull("virkningsperiode_til_og_med")
            if ((virkningsperiodeFraOgMed == null).xor(virkningsperiodeTilOgMed == null)) {
                throw IllegalStateException("Både fra og med og til og med må være satt, eller ingen av dem")
            }
            val virkningsperiode =
                virkningsperiodeFraOgMed?.let { Periode(virkningsperiodeFraOgMed, virkningsperiodeTilOgMed!!) }
            val status = string("status")
            val saksbehandler = stringOrNull("saksbehandler")
            val beslutter = stringOrNull("beslutter")
            // Kan være null for revurderinger. Domeneobjektet passer på dette selv.
            val søknad: Søknad? = SøknadDAO.hentForBehandlingId(id, session)

            val attesteringer = string("attesteringer").toAttesteringer()
            val fnr = Fnr.fromString(string("fnr"))
            val saksnummer = Saksnummer(string("saksnummer"))

            val sendtTilBeslutning = localDateTimeOrNull("sendt_til_beslutning")
            val opprettet = localDateTime("opprettet")
            val iverksattTidspunkt = localDateTimeOrNull("iverksatt_tidspunkt")
            val sistEndret = localDateTime("sist_endret")
            val oppgaveId = stringOrNull("oppgave_id")?.let { OppgaveId(it) }
            val saksopplysningsperiodeFraOgMed = localDateOrNull("saksopplysningsperiode_fra_og_med")
            val saksopplysningsperiodeTilOgMed = localDateOrNull("saksopplysningsperiode_til_og_med")
            val barnetillegg = stringOrNull("barnetillegg")?.toBarnetillegg()
            val saksopplysninger = string("saksopplysninger").toSaksopplysninger()
            val valgteTiltaksdeltakelser = stringOrNull("valgte_tiltaksdeltakelser")?.toValgteTiltaksdeltakelser(saksopplysninger)
            val avbrutt = stringOrNull("avbrutt")?.toAvbrutt()

            return Behandling(
                id = id,
                sakId = sakId,
                saksnummer = saksnummer,
                fnr = fnr,
                søknad = søknad,
                virkningsperiode = virkningsperiode,
                saksopplysninger = saksopplysninger,
                saksbehandler = saksbehandler,
                sendtTilBeslutning = sendtTilBeslutning,
                beslutter = beslutter,
                attesteringer = attesteringer,
                status = status.toBehandlingsstatus(),
                opprettet = opprettet,
                iverksattTidspunkt = iverksattTidspunkt,
                sendtTilDatadeling = localDateTimeOrNull("sendt_til_datadeling"),
                sistEndret = sistEndret,
                behandlingstype = string("behandlingstype").toBehandlingstype(),
                oppgaveId = oppgaveId,
                fritekstTilVedtaksbrev = stringOrNull("fritekst_vedtaksbrev")?.let { FritekstTilVedtaksbrev(it) },
                begrunnelseVilkårsvurdering = stringOrNull("begrunnelse_vilkårsvurdering")?.let {
                    BegrunnelseVilkårsvurdering(
                        it,
                    )
                },
                saksopplysningsperiode = saksopplysningsperiodeFraOgMed?.let {
                    Periode(
                        saksopplysningsperiodeFraOgMed,
                        saksopplysningsperiodeTilOgMed!!,
                    )
                },
                barnetillegg = barnetillegg,
                valgteTiltaksdeltakelser = valgteTiltaksdeltakelser,
                avbrutt = avbrutt,
            )
        }

        @Language("SQL")
        private val sqlOpprettBehandling =
            """
            insert into behandling (
                id,
                sak_id,
                virkningsperiode_fra_og_med,
                virkningsperiode_til_og_med,
                status,
                sist_endret,
                opprettet,
                vilkårssett,
                stønadsdager,
                saksbehandler,
                beslutter,
                attesteringer,
                iverksatt_tidspunkt,
                sendt_til_beslutning,
                sendt_til_datadeling,
                behandlingstype,
                oppgave_id,
                fritekst_vedtaksbrev,
                begrunnelse_vilkårsvurdering,
                saksopplysninger,
                saksopplysningsperiode_fra_og_med,
                saksopplysningsperiode_til_og_med,
                barnetillegg,
                valgte_tiltaksdeltakelser,
                avbrutt
            ) values (
                :id,
                :sak_id,
                :virkningsperiode_fra_og_med,
                :virkningsperiode_til_og_med,
                :status,
                :sist_endret,
                :opprettet,
                to_jsonb(:vilkaarssett::jsonb),
                to_jsonb(:stonadsdager::jsonb),
                :saksbehandler,
                :beslutter,
                to_jsonb(:attesteringer::jsonb),
                :iverksatt_tidspunkt,
                :sendt_til_beslutning,
                :sendt_til_datadeling,
                :behandlingstype,
                :oppgave_id,
                :fritekst_vedtaksbrev,
                :begrunnelse_vilkarsvurdering,
                to_jsonb(:saksopplysninger::jsonb),
                :saksopplysningsperiode_fra_og_med,
                :saksopplysningsperiode_til_og_med,
                to_jsonb(:barnetillegg::jsonb),
                to_jsonb(:valgte_tiltaksdeltakelser::jsonb),
                to_jsonb(:avbrutt::jsonb)
            )
            """.trimIndent()

        @Language("SQL")
        private val sqlOppdaterBehandling =
            """
            update behandling set 
                virkningsperiode_fra_og_med = :virkningsperiode_fra_og_med,
                virkningsperiode_til_og_med = :virkningsperiode_til_og_med,
                sak_id = :sak_id,
                status = :status,
                sist_endret = :sist_endret,
                saksbehandler = :saksbehandler,
                beslutter = :beslutter,
                vilkårssett = to_jsonb(:vilkaarssett::json),
                stønadsdager = to_jsonb(:stonadsdager::json),
                attesteringer = to_jsonb(:attesteringer::json),
                iverksatt_tidspunkt = :iverksatt_tidspunkt,
                sendt_til_beslutning = :sendt_til_beslutning,
                sendt_til_datadeling = :sendt_til_datadeling,
                behandlingstype = :behandlingstype,
                oppgave_id = :oppgave_id,
                fritekst_vedtaksbrev = :fritekst_vedtaksbrev,
                begrunnelse_vilkårsvurdering = :begrunnelse_vilkarsvurdering,
                saksopplysninger = to_jsonb(:saksopplysninger::jsonb),
                saksopplysningsperiode_fra_og_med = :saksopplysningsperiode_fra_og_med,
                saksopplysningsperiode_til_og_med = :saksopplysningsperiode_til_og_med,
                barneTillegg = to_jsonb(:barnetillegg::jsonb),
                valgte_tiltaksdeltakelser = to_jsonb(:valgte_tiltaksdeltakelser::jsonb),
                avbrutt = to_jsonb(:avbrutt::jsonb)
            where id = :id
              and sist_endret = :sist_endret_old
            """.trimIndent()

        @Language("SQL")
        private val sqlHentBehandling =
            """
            select b.*,s.fnr, s.saksnummer from behandling b join sak s on s.id = b.sak_id where b.id = :id
            """.trimIndent()

        @Language("SQL")
        private val sqlHentBehandlingForFnr =
            """
            select b.*,s.fnr, s.saksnummer from behandling b
              join sak s on s.id = b.sak_id
              where s.fnr = :fnr
              order by b.opprettet 
            """.trimIndent()
    }

    /** Siden dette er på tvers av saker, gir det ikke mening og bruke [Behandlinger] */
    override fun hentFørstegangsbehandlingerTilDatadeling(limit: Int): List<Behandling> {
        return sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    """
                    select b.*,sak.saksnummer,sak.fnr
                    from behandling b
                    join sak on sak.id = b.sak_id
                    where
                      b.behandlingstype = 'FØRSTEGANGSBEHANDLING' and
                      (b.sendt_til_datadeling is null or b.sendt_til_datadeling < b.sist_endret)
                    order by b.opprettet
                    limit :limit
                    """.trimIndent(),
                    mapOf(
                        "limit" to limit,
                    ),
                ).map { row ->
                    row.toBehandling(session)
                }.asList,
            )
        }
    }

    override fun markerSendtTilDatadeling(id: BehandlingId, tidspunkt: LocalDateTime) {
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
}
