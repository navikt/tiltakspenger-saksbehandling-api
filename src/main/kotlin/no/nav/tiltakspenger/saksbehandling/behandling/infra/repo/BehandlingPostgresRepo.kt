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
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionContext.Companion.withSession
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingUtbetaling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingstype
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlinger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingType
import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.attesteringer.toAttesteringer
import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.attesteringer.toDbJson
import no.nav.tiltakspenger.saksbehandling.behandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.beregning.Beregning
import no.nav.tiltakspenger.saksbehandling.beregning.infra.repo.tilBeregningerDbJson
import no.nav.tiltakspenger.saksbehandling.beregning.infra.repo.tilMeldeperiodeBeregningerFraBehandling
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringer
import no.nav.tiltakspenger.saksbehandling.felles.Ventestatus
import no.nav.tiltakspenger.saksbehandling.infra.repo.booleanOrNull
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.toAvbrutt
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.toDbJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.toVentestatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.MeldeperiodePostgresRepo
import no.nav.tiltakspenger.saksbehandling.omgjøring.infra.repo.toDbJson
import no.nav.tiltakspenger.saksbehandling.omgjøring.infra.repo.toOmgjørRammevedtak
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.søknad.infra.repo.SøknadDAO
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo.toDbJson
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo.toSimuleringFraDbJson
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime

private val log = KotlinLogging.logger {}

class BehandlingPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : BehandlingRepo {

    override fun hent(
        behandlingId: BehandlingId,
        sessionContext: SessionContext?,
    ): Rammebehandling {
        return sessionFactory.withSession(sessionContext) { session ->
            hentOrNull(behandlingId, session)!!
        }
    }

    /**
     * Denne returnerer ikke [Rammebehandlinger] siden vi ikke har avklart om en person kan ha flere saker. I så fall vil dette bli en liste med [Rammebehandlinger].
     */
    override fun hentAlleForFnr(fnr: Fnr): List<Rammebehandling> {
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

    override fun lagre(
        behandling: Rammebehandling,
        transactionContext: TransactionContext?,
    ) {
        sessionFactory.withTransaction(transactionContext) { tx ->
            val sistEndret = hentSistEndret(behandling.id, tx)

            if (sistEndret == null) {
                opprettBehandling(behandling, tx)
            } else {
                oppdaterBehandling(sistEndret, behandling, tx)
            }
        }
    }

    /**
     * Siden vi ikke er interessert i og hente ut metadataene igjen, er dette en egen funksjon.
     * Denne blir kalt samtidig som [lagre] i en og samme transaksjon, så vi trenger ikke mutere sist_endret her.
     */
    override fun oppdaterSimuleringMetadata(
        behandlingId: BehandlingId,
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
     * Oppdaterer behandlingsstatus, og saksbehandler bare dersom den er null. Skal du endre saksbehandler bruk [overtaSaksbehandler]
     */
    override fun taBehandlingSaksbehandler(
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler,
        behandlingsstatus: Rammebehandlingsstatus,
        sistEndret: LocalDateTime,
        sessionContext: SessionContext?,
    ): Boolean {
        return sessionFactory.withSession(sessionContext) { sx ->
            sx.run(
                sqlQuery(
                    """
                    update behandling set
                        saksbehandler = :saksbehandler,
                        status = :status,
                        beslutter = CASE WHEN beslutter = :saksbehandler THEN null ELSE beslutter END,
                        sist_endret = :sist_endret
                    where id = :id and saksbehandler is null
                    """,
                    "id" to behandlingId.toString(),
                    "saksbehandler" to saksbehandler.navIdent,
                    "status" to behandlingsstatus.toDb(),
                    "sist_endret" to sistEndret,
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
        behandlingsstatus: Rammebehandlingsstatus,
        sistEndret: LocalDateTime,
        sessionContext: SessionContext?,
    ): Boolean {
        return sessionFactory.withSession(sessionContext) { sx ->
            sx.run(
                sqlQuery(
                    """update behandling set beslutter = :beslutter, status = :status, sist_endret = :sist_endret where id = :id and beslutter is null""",
                    "id" to behandlingId.toString(),
                    "beslutter" to beslutter.navIdent,
                    "status" to behandlingsstatus.toDb(),
                    "sist_endret" to sistEndret,
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
        sistEndret: LocalDateTime,
        sessionContext: SessionContext?,
    ): Boolean {
        return sessionFactory.withSession(sessionContext) { sx ->
            sx.run(
                sqlQuery(
                    """
                    update behandling set
                        saksbehandler = :nySaksbehandler,
                        beslutter = CASE WHEN beslutter = :nySaksbehandler THEN null ELSE beslutter END,
                        sist_endret = :sist_endret
                    where id = :id and saksbehandler = :lagretSaksbehandler
                    """,
                    "id" to behandlingId.toString(),
                    "nySaksbehandler" to nySaksbehandler.navIdent,
                    "lagretSaksbehandler" to nåværendeSaksbehandler,
                    "sist_endret" to sistEndret,
                ).asUpdate,
            ) > 0
        }
    }

    /**
     * Oppdaterer saksbehandler på behandlingen. Skal du inserte saksbehandler bruk [taBehandlingSaksbehandler]
     */
    override fun overtaBeslutter(
        behandlingId: BehandlingId,
        nyBeslutter: Saksbehandler,
        nåværendeBeslutter: String,
        sistEndret: LocalDateTime,
        sessionContext: SessionContext?,
    ): Boolean {
        return sessionFactory.withSession(sessionContext) { sx ->
            sx.run(
                sqlQuery(
                    """update behandling set beslutter = :nyBeslutter, sist_endret = :sist_endret where id = :id and beslutter = :lagretBeslutter""",
                    "id" to behandlingId.toString(),
                    "nyBeslutter" to nyBeslutter.navIdent,
                    "lagretBeslutter" to nåværendeBeslutter,
                    "sist_endret" to sistEndret,
                ).asUpdate,
            ) > 0
        }
    }

    override fun leggTilbakeBehandlingSaksbehandler(
        behandlingId: BehandlingId,
        nåværendeSaksbehandler: Saksbehandler,
        behandlingsstatus: Rammebehandlingsstatus,
        sistEndret: LocalDateTime,
        sessionContext: SessionContext?,
    ): Boolean {
        return sessionFactory.withSession(sessionContext) { sx ->
            sx.run(
                sqlQuery(
                    """update behandling set saksbehandler = null, status = :status, sist_endret = :sist_endret where id = :id and saksbehandler = :lagretSaksbehandler""",
                    "id" to behandlingId.toString(),
                    "lagretSaksbehandler" to nåværendeSaksbehandler.navIdent,
                    "status" to behandlingsstatus.toDb(),
                    "sist_endret" to sistEndret,
                ).asUpdate,
            ) > 0
        }
    }

    override fun leggTilbakeBehandlingBeslutter(
        behandlingId: BehandlingId,
        nåværendeBeslutter: Saksbehandler,
        behandlingsstatus: Rammebehandlingsstatus,
        sistEndret: LocalDateTime,
        sessionContext: SessionContext?,
    ): Boolean {
        return sessionFactory.withSession(sessionContext) { sx ->
            sx.run(
                sqlQuery(
                    """update behandling set beslutter = null, status = :status, sist_endret = :sist_endret where id = :id and beslutter = :lagretBeslutter""",
                    "id" to behandlingId.toString(),
                    "lagretBeslutter" to nåværendeBeslutter.navIdent,
                    "status" to behandlingsstatus.toDb(),
                    "sist_endret" to sistEndret,
                ).asUpdate,
            ) > 0
        }
    }

    companion object {
        fun hentOrNull(
            behandlingId: BehandlingId,
            session: Session,
        ): Rammebehandling? =
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
        ): Rammebehandlinger =
            session
                .run(
                    sqlQuery(
                        "select b.*,s.fnr, s.saksnummer from behandling b join sak s on s.id = b.sak_id where b.sak_id = :sak_id order by b.opprettet",
                        "sak_id" to sakId.toString(),
                    ).map { it.toBehandling(session) }.asList,
                )
                .let { Rammebehandlinger(it) }

        private fun oppdaterBehandling(
            sistEndret: LocalDateTime,
            behandling: Rammebehandling,
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
            behandling: Rammebehandling,
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

        fun Row.toBehandling(session: Session): Rammebehandling {
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
            val avbrutt = stringOrNull("avbrutt")?.toAvbrutt()
            val ventestatus = stringOrNull("ventestatus")?.toVentestatus() ?: Ventestatus()
            val venterTil = localDateTimeOrNull("venter_til")
            val sendtTilDatadeling = localDateTimeOrNull("sendt_til_datadeling")
            val fritekstTilVedtaksbrev = stringOrNull("fritekst_vedtaksbrev")?.let { FritekstTilVedtaksbrev.create(it) }
            val begrunnelseVilkårsvurdering = stringOrNull("begrunnelse_vilkårsvurdering")?.let {
                Begrunnelse.create(it)
            }

            val saksopplysninger = string("saksopplysninger").toSaksopplysninger()

            val virkningsperiodeFraOgMed = localDateOrNull("virkningsperiode_fra_og_med")
            val virkningsperiodeTilOgMed = localDateOrNull("virkningsperiode_til_og_med")

            if ((virkningsperiodeFraOgMed == null).xor(virkningsperiodeTilOgMed == null)) {
                throw IllegalStateException("Både fra og med og til og med for virkningsperiode må være satt, eller ingen av dem")
            }

            val virkningsperiode =
                virkningsperiodeFraOgMed?.let { Periode(virkningsperiodeFraOgMed, virkningsperiodeTilOgMed!!) }
            val søknadId = stringOrNull("soknad_id")?.let { SøknadId.fromString(it) }
            val omgjørRammevedtak = stringOrNull("omgjør_rammevedtak").toOmgjørRammevedtak()

            val innvilgelsesperioder = stringOrNull("innvilgelsesperioder")?.tilInnvilgelsesperioder()

            when (behandlingstype) {
                Behandlingstype.SØKNADSBEHANDLING -> {
                    val automatiskSaksbehandlet = boolean("automatisk_saksbehandlet")
                    val manueltBehandlesGrunner =
                        stringOrNull("manuelt_behandles_grunner")?.toManueltBehandlesGrunner() ?: emptyList()
                    val resultatType = stringOrNull("resultat")?.tilSøknadsbehandlingResultatType()

                    val resultat = when (resultatType) {
                        SøknadsbehandlingType.INNVILGELSE -> SøknadsbehandlingResultat.Innvilgelse(
                            barnetillegg = stringOrNull("barnetillegg")?.toBarnetillegg(),
                            innvilgelsesperioder = innvilgelsesperioder!!,
                            omgjørRammevedtak = omgjørRammevedtak,
                        )

                        SøknadsbehandlingType.AVSLAG -> SøknadsbehandlingResultat.Avslag(
                            avslagsgrunner = string("avslagsgrunner").toAvslagsgrunnlag(),
                            avslagsperiode = virkningsperiode,
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
                        saksnummer = saksnummer,
                        fnr = fnr,
                        saksopplysninger = saksopplysninger,
                        søknad = søknadId?.let { SøknadDAO.hentForSøknadId(it, session) }
                            ?: throw IllegalStateException("Fant ikke søknad for søknadsbehandling, behandlingsid $id"),
                        saksbehandler = saksbehandler,
                        sendtTilBeslutning = sendtTilBeslutning,
                        beslutter = beslutter,
                        attesteringer = Attesteringer(attesteringer),
                        fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
                        begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering,
                        avbrutt = avbrutt,
                        ventestatus = ventestatus,
                        venterTil = venterTil,
                        resultat = resultat,
                        automatiskSaksbehandlet = automatiskSaksbehandlet,
                        manueltBehandlesGrunner = manueltBehandlesGrunner,
                        utbetaling = stringOrNull("beregning")?.let {
                            BehandlingUtbetaling(
                                beregning = Beregning(it.tilMeldeperiodeBeregningerFraBehandling(id)),
                                navkontor = Navkontor(
                                    kontornummer = string("navkontor"),
                                    kontornavn = stringOrNull("navkontor_navn"),
                                ),
                                simulering = stringOrNull("simulering")?.toSimuleringFraDbJson(
                                    MeldeperiodePostgresRepo.hentMeldeperiodekjederForSakId(
                                        sakId = sakId,
                                        session = session,
                                    ),
                                ),
                            )
                        },
                    )
                }

                Behandlingstype.REVURDERING -> {
                    val resultatType = string("resultat").tilRevurderingResultatType()

                    val resultat = when (resultatType) {
                        RevurderingType.STANS -> RevurderingResultat.Stans(
                            valgtHjemmel = stringOrNull("valgt_hjemmel_har_ikke_rettighet")?.tilHjemmelForStans()
                                ?: emptyList(),
                            harValgtStansFraFørsteDagSomGirRett = booleanOrNull("har_valgt_stans_fra_første_dag_som_gir_rett"),
                            harValgtStansTilSisteDagSomGirRett = booleanOrNull("har_valgt_stans_til_siste_dag_som_gir_rett"),
                            stansperiode = virkningsperiode,
                            omgjørRammevedtak = omgjørRammevedtak,
                        )

                        RevurderingType.INNVILGELSE -> RevurderingResultat.Innvilgelse(
                            barnetillegg = stringOrNull("barnetillegg")?.toBarnetillegg(),
                            innvilgelsesperioder = innvilgelsesperioder,
                            omgjørRammevedtak = omgjørRammevedtak,
                        )

                        RevurderingType.OMGJØRING -> {
                            RevurderingResultat.Omgjøring(
                                virkningsperiode = virkningsperiode!!,
                                innvilgelsesperioder = innvilgelsesperioder,
                                barnetillegg = stringOrNull("barnetillegg")?.toBarnetillegg(),
                                omgjørRammevedtak = omgjørRammevedtak,
                            )
                        }
                    }

                    return Revurdering(
                        id = id,
                        status = status,
                        opprettet = opprettet,
                        sistEndret = sistEndret,
                        iverksattTidspunkt = iverksattTidspunkt,
                        sendtTilDatadeling = sendtTilDatadeling,
                        sakId = sakId,
                        saksnummer = saksnummer,
                        fnr = fnr,
                        saksopplysninger = saksopplysninger,
                        saksbehandler = saksbehandler,
                        sendtTilBeslutning = sendtTilBeslutning,
                        beslutter = beslutter,
                        attesteringer = Attesteringer(attesteringer),
                        fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
                        begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering,
                        avbrutt = avbrutt,
                        ventestatus = ventestatus,
                        venterTil = venterTil,
                        resultat = resultat,
                        utbetaling = stringOrNull("beregning")?.let {
                            BehandlingUtbetaling(
                                beregning = Beregning(it.tilMeldeperiodeBeregningerFraBehandling(id)),
                                navkontor = Navkontor(
                                    kontornummer = string("navkontor"),
                                    kontornavn = stringOrNull("navkontor_navn"),
                                ),
                                simulering = stringOrNull("simulering")?.toSimuleringFraDbJson(
                                    MeldeperiodePostgresRepo.hentMeldeperiodekjederForSakId(
                                        sakId = sakId,
                                        session = session,
                                    ),
                                ),
                            )
                        },
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
                navkontor,
                navkontor_navn,
                har_valgt_stans_fra_første_dag_som_gir_rett,
                har_valgt_stans_til_siste_dag_som_gir_rett,
                innvilgelsesperioder,
                omgjør_rammevedtak
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
                to_jsonb(:barnetillegg::jsonb),
                to_jsonb(:avbrutt::jsonb),
                to_jsonb(:ventestatus::jsonb),
                :venter_til,
                to_jsonb(:avslagsgrunner::jsonb),
                :resultat,
                :soknad_id,
                :automatisk_saksbehandlet,
                to_jsonb(:manuelt_behandles_grunner::jsonb),
                to_jsonb(:beregning::jsonb),
                to_jsonb(:simulering::jsonb),
                :navkontor,
                :navkontor_navn,
                :har_valgt_stans_fra_forste_dag_som_gir_rett,
                :har_valgt_stans_til_siste_dag_som_gir_rett,
                to_jsonb(:innvilgelsesperioder::jsonb),
                to_jsonb(:omgjoer_rammevedtak::jsonb)
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
                barneTillegg = to_jsonb(:barnetillegg::jsonb),
                avbrutt = to_jsonb(:avbrutt::jsonb),
                ventestatus = to_jsonb(:ventestatus::jsonb),
                venter_til = :venter_til,
                avslagsgrunner = to_jsonb(:avslagsgrunner::jsonb),
                resultat = :resultat,
                soknad_id = :soknad_id,
                automatisk_saksbehandlet = :automatisk_saksbehandlet,
                manuelt_behandles_grunner = to_jsonb(:manuelt_behandles_grunner::jsonb),
                beregning = to_jsonb(:beregning::jsonb),
                simulering = to_jsonb(:simulering::jsonb),
                simulering_metadata = CASE WHEN :simulering::varchar IS NULL THEN NULL ELSE simulering_metadata END,
                navkontor = :navkontor,
                navkontor_navn = :navkontor_navn,
                har_valgt_stans_fra_første_dag_som_gir_rett = :har_valgt_stans_fra_forste_dag_som_gir_rett,
                har_valgt_stans_til_siste_dag_som_gir_rett = :har_valgt_stans_til_siste_dag_som_gir_rett,
                innvilgelsesperioder = to_jsonb(:innvilgelsesperioder::jsonb),
                omgjør_rammevedtak = to_jsonb(:omgjoer_rammevedtak::jsonb)
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
            select b.*, s.fnr, s.saksnummer from behandling b
              join sak s on s.id = b.sak_id
              where s.fnr = :fnr
              order by b.opprettet 
            """.trimIndent()
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
                    where b.sendt_til_datadeling is null or b.sendt_til_datadeling < b.sist_endret
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

    override fun hentAlleAutomatiskeSoknadsbehandlinger(limit: Int): List<Søknadsbehandling> {
        return sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    //language=SQL
                    """
                    select b.*,
                    sak.saksnummer,
                    sak.fnr
                    from behandling b
                    join sak on sak.id = b.sak_id
                    where
                      b.behandlingstype = 'SØKNADSBEHANDLING' and
                      b.status = 'UNDER_AUTOMATISK_BEHANDLING' and
                      (b.venter_til is null or b.venter_til < :now)
                    order by b.opprettet
                    limit :limit
                    """.trimIndent(),
                    mapOf(
                        "now" to LocalDateTime.now(),
                        "limit" to limit,
                    ),
                ).map { it.toBehandling(session) }.asList,
            )
        }.filterIsInstance<Søknadsbehandling>()
    }
}

private fun Rammebehandling.tilDbParams(): Map<String, Any?> {
    val søknadId = when (this) {
        is Søknadsbehandling -> this.søknad.id.toString()
        is Revurdering -> null
    }
    val automatiskSaksbehandlet = when (this) {
        is Søknadsbehandling -> this.automatiskSaksbehandlet
        is Revurdering -> false
    }

    val manueltBehandlesGrunner = when (this) {
        is Søknadsbehandling -> this.manueltBehandlesGrunner
        is Revurdering -> null
    }
    return mapOf(
        "id" to this.id.toString(),
        "status" to this.status.toDb(),
        "sist_endret" to this.sistEndret,
        "iverksatt_tidspunkt" to this.iverksattTidspunkt,
        "sendt_til_datadeling" to this.sendtTilDatadeling,
        "oppgave_id" to null,
        "virkningsperiode_fra_og_med" to this.virkningsperiode?.fraOgMed,
        "virkningsperiode_til_og_med" to this.virkningsperiode?.tilOgMed,
        "saksbehandler" to this.saksbehandler,
        "beslutter" to this.beslutter,
        "attesteringer" to this.attesteringer.toDbJson(),
        "sendt_til_beslutning" to this.sendtTilBeslutning,
        "fritekst_vedtaksbrev" to this.fritekstTilVedtaksbrev?.verdi,
        "begrunnelse_vilkarsvurdering" to this.begrunnelseVilkårsvurdering?.verdi,
        "saksopplysninger" to this.saksopplysninger.toDbJson(),
        "avbrutt" to this.avbrutt?.toDbJson(),
        "ventestatus" to this.ventestatus.toDbJson(),
        "venter_til" to this.venterTil,
        "resultat" to this.resultat?.toDb(),
        "opprettet" to this.opprettet,
        "sak_id" to this.sakId.toString(),
        "behandlingstype" to this.behandlingstype.toDbValue(),
        "soknad_id" to søknadId,
        "automatisk_saksbehandlet" to automatiskSaksbehandlet,
        "manuelt_behandles_grunner" to manueltBehandlesGrunner?.toDbJson(),
        "beregning" to this.utbetaling?.beregning?.tilBeregningerDbJson(),
        "simulering" to this.utbetaling?.simulering?.toDbJson(),
        "navkontor" to this.utbetaling?.navkontor?.kontornummer,
        "navkontor_navn" to this.utbetaling?.navkontor?.kontornavn,

        *this.resultat.tilDbParams(),
    )
}

private fun BehandlingResultat?.tilDbParams(): Array<Pair<String, Any?>> = when (this) {
    is SøknadsbehandlingResultat.Avslag -> arrayOf(
        "avslagsgrunner" to this.avslagsgrunner.toDb(),
        "omgjoer_rammevedtak" to null,
    )

    is SøknadsbehandlingResultat.Innvilgelse -> arrayOf(
        "innvilgelsesperioder" to this.innvilgelsesperioder.tilInnvilgelsesperioderDbJson(),
        "barnetillegg" to this.barnetillegg?.toDbJson(),
        "omgjoer_rammevedtak" to this.omgjørRammevedtak.toDbJson(),
    )

    is RevurderingResultat.Omgjøring -> arrayOf(
        "innvilgelsesperioder" to this.innvilgelsesperioder?.tilInnvilgelsesperioderDbJson(),
        "barnetillegg" to this.barnetillegg?.toDbJson(),
        "omgjoer_rammevedtak" to this.omgjørRammevedtak.toDbJson(),
    )

    is RevurderingResultat.Innvilgelse -> arrayOf(
        "innvilgelsesperioder" to this.innvilgelsesperioder?.tilInnvilgelsesperioderDbJson(),
        "barnetillegg" to this.barnetillegg?.toDbJson(),
        "omgjoer_rammevedtak" to this.omgjørRammevedtak.toDbJson(),
    )

    is RevurderingResultat.Stans -> arrayOf(
        "valgt_hjemmel_har_ikke_rettighet" to this.valgtHjemmel.toDbJson(),
        "har_valgt_stans_fra_forste_dag_som_gir_rett" to this.harValgtStansFraFørsteDagSomGirRett,
        "har_valgt_stans_til_siste_dag_som_gir_rett" to this.harValgtStansTilSisteDagSomGirRett,
        "omgjoer_rammevedtak" to this.omgjørRammevedtak.toDbJson(),
    )

    null -> emptyArray()
}
