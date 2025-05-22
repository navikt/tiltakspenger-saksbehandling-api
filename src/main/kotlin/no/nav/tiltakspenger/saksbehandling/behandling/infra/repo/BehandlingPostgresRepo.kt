package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BegrunnelseVilkårsvurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingUtfall
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlinger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingstype
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingUtfall
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingUtfallType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingUtfall
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingUtfallType
import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.attesteringer.toAttesteringer
import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.attesteringer.toDbJson
import no.nav.tiltakspenger.saksbehandling.behandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.IkkeFunnetException
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.toAvbrutt
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.toDbJson
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.søknad.infra.repo.SøknadDAO
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime

private val log = KotlinLogging.logger {}

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
                ).map { it.toBehandling(session) }.asList,
            )
        }
    }

    override fun hentForSøknadId(søknadId: SøknadId): Behandling? =
        sessionFactory.withSession { session ->
            session.run(
                sqlQuery(
                    """
                    select b.*, sak.saksnummer, sak.fnr
                    from behandling b
                    join søknad s on b.id = s.behandling_id
                    join sak on sak.id = b.sak_id
                    where s.id = :id
                    """.trimIndent(),
                    "id" to søknadId.toString(),
                ).map { it.toBehandling(session) }.asSingle,
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
                if (behandling is Søknadsbehandling) {
                    SøknadDAO.knyttSøknadTilBehandling(behandling.id, behandling.søknad.id, tx)
                }
            } else {
                oppdaterBehandling(sistEndret, behandling, tx)
            }
        }
    }

    /**
     * Oppdaterer behandlingsstatus, og saksbehandler bare dersom den er null. Skal du endre saksbehandler bruk [overtaSaksbehandler]
     */
    override fun taBehandlingSaksbehandler(
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler,
        behandlingsstatus: Behandlingsstatus,
        sessionContext: SessionContext?,
    ): Boolean {
        return sessionFactory.withSession(sessionContext) { sx ->
            sx.run(
                queryOf(
                    """update behandling set saksbehandler = :saksbehandler, status = :status where id = :id and saksbehandler is null""",
                    mapOf(
                        "id" to behandlingId.toString(),
                        "saksbehandler" to saksbehandler.navIdent,
                        "status" to behandlingsstatus.toDb(),
                    ),
                ).asUpdate,
            ) > 0
        }
    }

    /**
     * Oppdaterer behandlingsstatus, og beslutter bare dersom den er null. Skal du endre beslutter bruk [overtaSaksbehandler]
     */
    override fun taBehandlingBeslutter(
        behandlingId: BehandlingId,
        beslutter: Saksbehandler,
        behandlingsstatus: Behandlingsstatus,
        sessionContext: SessionContext?,
    ): Boolean {
        return sessionFactory.withSession(sessionContext) { sx ->
            sx.run(
                queryOf(
                    """update behandling set beslutter = :beslutter, status = :status where id = :id and beslutter is null""",
                    mapOf(
                        "id" to behandlingId.toString(),
                        "beslutter" to beslutter.navIdent,
                        "status" to behandlingsstatus.toDb(),
                    ),
                ).asUpdate,
            ) > 0
        }
    }

    /**
     * Oppdaterer saksbehandler på behandlingen. Skal du inserte saksbehandler bruk [taBehandlingSaksbehandler]
     */
    override fun overtaSaksbehandler(
        behandlingId: BehandlingId,
        nySaksbehandler: Saksbehandler,
        nåværendeSaksbehandler: String,
        sessionContext: SessionContext?,
    ): Boolean {
        return sessionFactory.withSession(sessionContext) { sx ->
            sx.run(
                queryOf(
                    """update behandling set saksbehandler = :nySaksbehandler where id = :id and saksbehandler = :lagretSaksbehandler""",
                    mapOf(
                        "id" to behandlingId.toString(),
                        "nySaksbehandler" to nySaksbehandler.navIdent,
                        "lagretSaksbehandler" to nåværendeSaksbehandler,
                    ),
                ).asUpdate,
            ) > 0
        }
    }

    /**
     * Oppdaterer saksbehandler på behandlingen. Skal du inserte saksbehandler bruk [taBehandlingSaksbehandler]
     * TODO - test
     */
    override fun overtaBeslutter(
        behandlingId: BehandlingId,
        nyBeslutter: Saksbehandler,
        nåværendeBeslutter: String,
        sessionContext: SessionContext?,
    ): Boolean {
        return sessionFactory.withSession(sessionContext) { sx ->
            sx.run(
                queryOf(
                    """update behandling set beslutter = :nyBeslutter where id = :id and beslutter = :lagretBeslutter""",
                    mapOf(
                        "id" to behandlingId.toString(),
                        "nyBeslutter" to nyBeslutter.navIdent,
                        "lagretBeslutter" to nåværendeBeslutter,
                    ),
                ).asUpdate,
            ) > 0
        }
    }

    override fun leggTilbakeBehandlingSaksbehandler(
        behandlingId: BehandlingId,
        nåværendeSaksbehandler: Saksbehandler,
        behandlingsstatus: Behandlingsstatus,
        sessionContext: SessionContext?,
    ): Boolean {
        return sessionFactory.withSession(sessionContext) { sx ->
            sx.run(
                queryOf(
                    """update behandling set saksbehandler = null, status = :status where id = :id and saksbehandler = :lagretSaksbehandler""",
                    mapOf(
                        "id" to behandlingId.toString(),
                        "lagretSaksbehandler" to nåværendeSaksbehandler.navIdent,
                        "status" to behandlingsstatus.toDb(),
                    ),
                ).asUpdate,
            ) > 0
        }
    }

    override fun leggTilbakeBehandlingBeslutter(
        behandlingId: BehandlingId,
        nåværendeBeslutter: Saksbehandler,
        behandlingsstatus: Behandlingsstatus,
        sessionContext: SessionContext?,
    ): Boolean {
        return sessionFactory.withSession(sessionContext) { sx ->
            sx.run(
                queryOf(
                    """update behandling set beslutter = null, status = :status where id = :id and beslutter = :lagretBeslutter""",
                    mapOf(
                        "id" to behandlingId.toString(),
                        "lagretBeslutter" to nåværendeBeslutter.navIdent,
                        "status" to behandlingsstatus.toDb(),
                    ),
                ).asUpdate,
            ) > 0
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
                ).map { it.toBehandling(session) }.asSingle,
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
                    ).map { it.toBehandling(session) }.asList,
                )
                .let { Behandlinger(it) }

        private fun oppdaterBehandling(
            sistEndret: LocalDateTime,
            behandling: Behandling,
            session: Session,
        ) {
            log.info { "Oppdaterer behandling ${behandling.id} ${behandling.behandlingstype}" }

            val antRaderOppdatert = session.run(
                queryOf(
                    sqlOppdaterBehandling,
                    behandling.tilDbParams()
                        .plus("sist_endret_old" to sistEndret),
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
            log.info { "Oppretter behandling ${behandling.id} ${behandling.behandlingstype}" }

            session.run(
                queryOf(
                    sqlOpprettBehandling,
                    behandling.tilDbParams(),
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
                ).map { it.localDateTime("sist_endret") }.asSingle,
            )

        private fun Row.toBehandling(session: Session): Behandling {
            val behandlingstype = string("behandlingstype").toBehandlingstype()
            val id = BehandlingId.fromString(string("id"))
            val sakId = SakId.fromString(string("sak_id"))
            val status = string("status").toBehandlingsstatus()
            val saksbehandler = stringOrNull("saksbehandler")
            val beslutter = stringOrNull("beslutter")
            val attesteringer = string("attesteringer").toAttesteringer()
            val fnr = Fnr.fromString(string("fnr"))
            val saksnummer = Saksnummer(string("saksnummer"))
            val sendtTilBeslutning = localDateTimeOrNull("sendt_til_beslutning")
            val opprettet = localDateTime("opprettet")
            val iverksattTidspunkt = localDateTimeOrNull("iverksatt_tidspunkt")
            val sistEndret = localDateTime("sist_endret")
            val oppgaveId = stringOrNull("oppgave_id")?.let { OppgaveId(it) }
            val avbrutt = stringOrNull("avbrutt")?.toAvbrutt()
            val sendtTilDatadeling = localDateTimeOrNull("sendt_til_datadeling")
            val fritekstTilVedtaksbrev = stringOrNull("fritekst_vedtaksbrev")?.let { FritekstTilVedtaksbrev(it) }
            val begrunnelseVilkårsvurdering = stringOrNull("begrunnelse_vilkårsvurdering")?.let {
                BegrunnelseVilkårsvurdering(
                    it,
                )
            }

            val saksopplysningsperiode = Periode(
                localDate("saksopplysningsperiode_fra_og_med"),
                localDate("saksopplysningsperiode_til_og_med"),
            )

            val saksopplysninger = string("saksopplysninger").toSaksopplysninger()

            val virkningsperiodeFraOgMed = localDateOrNull("virkningsperiode_fra_og_med")
            val virkningsperiodeTilOgMed = localDateOrNull("virkningsperiode_til_og_med")

            if ((virkningsperiodeFraOgMed == null).xor(virkningsperiodeTilOgMed == null)) {
                throw IllegalStateException("Både fra og med og til og med for virkningsperiode må være satt, eller ingen av dem")
            }

            val virkningsperiode =
                virkningsperiodeFraOgMed?.let { Periode(virkningsperiodeFraOgMed, virkningsperiodeTilOgMed!!) }

            val antallDagerPerMeldeperiode = intOrNull("antall_dager_per_meldeperiode")

            when (behandlingstype) {
                Behandlingstype.SØKNADSBEHANDLING -> {
                    val utfallType = stringOrNull("utfall")?.tilSøknadsbehandlingUtfallType()

                    val utfall = when (utfallType) {
                        SøknadsbehandlingUtfallType.INNVILGELSE -> SøknadsbehandlingUtfall.Innvilgelse(
                            valgteTiltaksdeltakelser = string("valgte_tiltaksdeltakelser")
                                .toValgteTiltaksdeltakelser(saksopplysninger),
                            barnetillegg = stringOrNull("barnetillegg")?.toBarnetillegg(),
                        )

                        SøknadsbehandlingUtfallType.AVSLAG -> SøknadsbehandlingUtfall.Avslag(
                            avslagsgrunner = string("avslagsgrunner").toAvslagsgrunnlag(),
                        )

                        null -> null
                    }

                    return Søknadsbehandling(
                        id = id,
                        status = status,
                        opprettet = opprettet,
                        sistEndret = sistEndret,
                        iverksattTidspunkt = iverksattTidspunkt,
                        sendtTilDatadeling = sendtTilDatadeling,
                        sakId = sakId,
                        oppgaveId = oppgaveId,
                        saksnummer = saksnummer,
                        fnr = fnr,
                        saksopplysninger = saksopplysninger,
                        saksopplysningsperiode = saksopplysningsperiode,
                        søknad = SøknadDAO.hentForBehandlingId(id, session)!!,
                        virkningsperiode = virkningsperiode,
                        saksbehandler = saksbehandler,
                        sendtTilBeslutning = sendtTilBeslutning,
                        beslutter = beslutter,
                        attesteringer = attesteringer,
                        fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
                        begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering,
                        avbrutt = avbrutt,
                        utfall = utfall,
                        antallDagerPerMeldeperiode = antallDagerPerMeldeperiode,
                    )
                }

                Behandlingstype.REVURDERING -> {
                    val utfallType = stringOrNull("utfall")?.tilRevurderingUtfallType()

                    val utfall = when (utfallType) {
                        RevurderingUtfallType.STANS -> RevurderingUtfall.Stans(
                            valgtHjemmel = stringOrNull("valgt_hjemmel_har_ikke_rettighet")?.tilHjemmelForStans()
                                ?: emptyList(),
                        )

                        null -> null
                    }

                    return Revurdering(
                        id = id,
                        status = status,
                        opprettet = opprettet,
                        sistEndret = sistEndret,
                        iverksattTidspunkt = iverksattTidspunkt,
                        sendtTilDatadeling = sendtTilDatadeling,
                        sakId = sakId,
                        oppgaveId = oppgaveId,
                        saksnummer = saksnummer,
                        fnr = fnr,
                        saksopplysninger = saksopplysninger,
                        saksopplysningsperiode = saksopplysningsperiode,
                        virkningsperiode = virkningsperiode,
                        saksbehandler = saksbehandler,
                        sendtTilBeslutning = sendtTilBeslutning,
                        beslutter = beslutter,
                        attesteringer = attesteringer,
                        fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
                        begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering,
                        avbrutt = avbrutt,
                        utfall = utfall,
                        antallDagerPerMeldeperiode = antallDagerPerMeldeperiode,
                    )
                }
            }
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
                saksopplysningsperiode_fra_og_med,
                saksopplysningsperiode_til_og_med,
                barnetillegg,
                valgte_tiltaksdeltakelser,
                avbrutt,
                antall_dager_per_meldeperiode,
                avslagsgrunner,
                utfall
            ) values (
                :id,
                :sak_id,
                :virkningsperiode_fra_og_med,
                :virkningsperiode_til_og_med,
                :status,
                :sist_endret,
                :opprettet,
                :saksbehandler,
                :beslutter,
                to_jsonb(:attesteringer::jsonb),
                :iverksatt_tidspunkt,
                :sendt_til_beslutning,
                :sendt_til_datadeling,
                :behandlingstype,
                :oppgave_id,
                to_jsonb(:valgt_hjemmel_har_ikke_rettighet::jsonb),
                :fritekst_vedtaksbrev,
                :begrunnelse_vilkarsvurdering,
                to_jsonb(:saksopplysninger::jsonb),
                :saksopplysningsperiode_fra_og_med,
                :saksopplysningsperiode_til_og_med,
                to_jsonb(:barnetillegg::jsonb),
                to_jsonb(:valgte_tiltaksdeltakelser::jsonb),
                to_jsonb(:avbrutt::jsonb),
                :antall_dager_per_meldeperiode,
                to_jsonb(:avslagsgrunner::jsonb),
                :utfall
            )
            """.trimIndent()

        @Language("SQL")
        private val sqlOppdaterBehandling =
            """
            update behandling set 
                virkningsperiode_fra_og_med = :virkningsperiode_fra_og_med,
                virkningsperiode_til_og_med = :virkningsperiode_til_og_med,
                status = :status,
                sist_endret = :sist_endret,
                saksbehandler = :saksbehandler,
                beslutter = :beslutter,
                attesteringer = to_jsonb(:attesteringer::json),
                iverksatt_tidspunkt = :iverksatt_tidspunkt,
                sendt_til_beslutning = :sendt_til_beslutning,
                sendt_til_datadeling = :sendt_til_datadeling, 
                oppgave_id = :oppgave_id,
                valgt_hjemmel_har_ikke_rettighet = to_json(:valgt_hjemmel_har_ikke_rettighet::jsonb),
                fritekst_vedtaksbrev = :fritekst_vedtaksbrev,
                begrunnelse_vilkårsvurdering = :begrunnelse_vilkarsvurdering,
                saksopplysninger = to_jsonb(:saksopplysninger::jsonb),
                saksopplysningsperiode_fra_og_med = :saksopplysningsperiode_fra_og_med,
                saksopplysningsperiode_til_og_med = :saksopplysningsperiode_til_og_med,
                barneTillegg = to_jsonb(:barnetillegg::jsonb),
                valgte_tiltaksdeltakelser = to_jsonb(:valgte_tiltaksdeltakelser::jsonb),
                avbrutt = to_jsonb(:avbrutt::jsonb),
                antall_dager_per_meldeperiode = :antall_dager_per_meldeperiode,
                avslagsgrunner = to_jsonb(:avslagsgrunner::jsonb),
                utfall = :utfall
            where id = :id and sist_endret = :sist_endret_old
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
    override fun hentSøknadsbehandlingerTilDatadeling(limit: Int): List<Behandling> {
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
                ).map { it.toBehandling(session) }.asList,
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

private fun Behandling.tilDbParams(): Map<String, Any?> = mapOf(
    "id" to this.id.toString(),
    "status" to this.status.toDb(),
    "sist_endret" to this.sistEndret,
    "iverksatt_tidspunkt" to this.iverksattTidspunkt,
    "sendt_til_datadeling" to this.sendtTilDatadeling,
    "oppgave_id" to this.oppgaveId?.toString(),
    "virkningsperiode_fra_og_med" to this.virkningsperiode?.fraOgMed,
    "virkningsperiode_til_og_med" to this.virkningsperiode?.tilOgMed,
    "saksbehandler" to this.saksbehandler,
    "beslutter" to this.beslutter,
    "attesteringer" to this.attesteringer.toDbJson(),
    "sendt_til_beslutning" to this.sendtTilBeslutning,
    "fritekst_vedtaksbrev" to this.fritekstTilVedtaksbrev?.verdi,
    "begrunnelse_vilkarsvurdering" to this.begrunnelseVilkårsvurdering?.verdi,
    "saksopplysninger" to this.saksopplysninger.toDbJson(),
    "saksopplysningsperiode_fra_og_med" to this.saksopplysningsperiode.fraOgMed,
    "saksopplysningsperiode_til_og_med" to this.saksopplysningsperiode.tilOgMed,
    "avbrutt" to this.avbrutt?.toDbJson(),
    "utfall" to this.utfall?.toDb(),
    "antall_dager_per_meldeperiode" to this.antallDagerPerMeldeperiode,
    "opprettet" to this.opprettet,
    "sak_id" to this.sakId.toString(),
    "behandlingstype" to this.behandlingstype.toDbValue(),
    *this.utfall.tilDbParams(),
)

private fun BehandlingUtfall?.tilDbParams(): Array<Pair<String, Any?>> = when (this) {
    is SøknadsbehandlingUtfall.Avslag -> arrayOf(
        "avslagsgrunner" to this.avslagsgrunner.toDb(),
    )

    is SøknadsbehandlingUtfall.Innvilgelse -> arrayOf(
        "barnetillegg" to this.barnetillegg?.toDbJson(),
        "valgte_tiltaksdeltakelser" to this.valgteTiltaksdeltakelser.toDbJson(),
    )

    is RevurderingUtfall.Stans -> arrayOf(
        "valgt_hjemmel_har_ikke_rettighet" to this.valgtHjemmel.toDbJson(),
    )

    null -> emptyArray()
}
