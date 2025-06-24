package no.nav.tiltakspenger.saksbehandling.søknad.infra.repo

import kotliquery.Row
import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.saksbehandling.felles.Avbrutt
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.toAvbrutt
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.toDbJson
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.søknad.Søknad

private const val KVP_FELT = "kvp"
private const val INTRO_FELT = "intro"
private const val INSTITUSJON_FELT = "institusjon"
private const val SYKEPENGER_FELT = "sykepenger"
private const val SUPPLERENDESTØNAD_ALDER_FELT = "supplerende_alder"
private const val SUPPLERENDESTØNAD_FLYKTNING_FELT = "supplerende_flyktning"
private const val JOBBSJANSEN_FELT = "jobbsjansen"
private const val GJENLEVENDEPENSJON_FELT = "gjenlevendepensjon"
private const val ALDERSPENSJON_FELT = "alderspensjon"
private const val TRYGD_OG_PENSJON_FELT = "trygd_og_pensjon"
private const val ETTERLØNN_FELT = "etterlonn"

internal object SøknadDAO {
    fun finnSakIdForTiltaksdeltakelse(
        eksternId: String,
        session: Session,
    ): SakId? =
        session.run(
            queryOf(
                "select sak_id from søknad s join søknadstiltak st on st.søknad_id = s.id where st.ekstern_id = ?",
                eksternId,
            )
                .map { row -> row.toSakId() }
                .asList,
        ).firstOrNull()

    fun finnSakId(
        søknadId: SøknadId,
        session: Session,
    ): SakId? =
        session.run(
            queryOf("select * from søknad s join sak on sak.id = s.sak_id where s.id = ?", søknadId.toString())
                .map { row -> row.toSakId() }
                .asSingle,
        )

    fun hentForSøknadId(
        søknadId: SøknadId,
        session: Session,
    ): Søknad? =
        session.run(
            // language=SQL
            queryOf(
                """
                select * 
                from søknad s join sak on sak.id = s.sak_id 
                where s.id = ?
                """.trimIndent(),
                søknadId.toString(),
            )
                .map { row -> row.toSøknad(session) }
                .asSingle,
        )

    fun hentForSakId(
        sakId: SakId,
        session: Session,
    ): List<Søknad> =
        session
            .run(
                queryOf(
                    // language=SQL
                    """
                        select * 
                        from søknad s 
                        join sak on sak.id = s.sak_id 
                        where s.sak_id = :sak_id
                    """.trimIndent(),
                    mapOf(
                        "sak_id" to sakId.toString(),
                    ),
                ).map { row ->
                    row.toSøknad(session)
                }.asList,
            )

    fun hentForFnr(
        fnr: Fnr,
        session: Session,
    ): List<Søknad> =
        session
            .run(
                sqlQuery(
                    """
                    select *
                    from søknad s
                    join sak on sak.id = s.sak_id where sak.fnr = :fnr
                    """.trimIndent(),
                    "fnr" to fnr.verdi,
                ).map { row ->
                    row.toSøknad(session)
                }.asList,
            )

    fun hentAlleUbehandledeSoknader(limit: Int, session: Session): List<Søknad> =
        session.run(
            sqlQuery(
                """
                    select *
                    from søknad soknad
                             join sak on soknad.sak_id = sak.id
                             left join behandling b on soknad.id = b.soknad_id
                    where b.id is null
                      and soknad.avbrutt is null
                      order by soknad.opprettet
                      limit :limit
                """.trimIndent(),
                "limit" to limit,
            ).map { row: Row ->
                row.toSøknad(session)
            }.asList,
        )

    private fun søknadFinnes(
        søknadId: SøknadId,
        session: Session,
    ): Boolean =
        session.run(
            queryOf(
                // language=SQL
                "select exists(select 1 from søknad where id = ?)",
                søknadId.toString(),
            ).map { row -> row.boolean("exists") }.asSingle,
        ) ?: throw RuntimeException("Failed to check if søknad exists")

    /**
     *  @param søknad Lagres dersom den ikke finnes fra før, hvis den finnes, oppdateres den ikke.
     */
    fun lagreHeleSøknaden(
        søknad: Søknad,
        txSession: TransactionalSession,
    ) {
        if (søknadFinnes(søknad.id, txSession)) return

        lagreSøknad(søknad, txSession)
        BarnetilleggDAO.lagre(søknad.id, søknad.barnetillegg, txSession)
        SøknadTiltakDAO.lagre(søknad.id, søknad.tiltak, txSession)
    }

    fun lagreAvbruttSøknad(
        søknadId: SøknadId,
        avbrutt: Avbrutt,
        txSession: TransactionalSession,
    ) {
        val oppdaterteRader = txSession.run(
            queryOf(
                """update søknad set avbrutt = to_jsonb(:avbrutt::jsonb) where id = :soknad_id""",
                mapOf(
                    "avbrutt" to avbrutt.toDbJson(),
                    "soknad_id" to søknadId.toString(),
                ),
            ).asUpdate,
        )
        if (oppdaterteRader == 0) {
            throw RuntimeException("Kunne ikke lagre avbrutt søknad.")
        }
    }

    fun oppdaterFnr(
        gammeltFnr: Fnr,
        nyttFnr: Fnr,
        session: Session,
    ) {
        session.run(
            queryOf(
                // language=SQL
                """update søknad set fnr = :nytt_fnr where fnr = :gammelt_fnr""",
                mapOf(
                    "nytt_fnr" to nyttFnr.verdi,
                    "gammelt_fnr" to gammeltFnr.verdi,
                ),
            ).asUpdate,
        )
    }

    private fun lagreSøknad(
        søknad: Søknad,
        session: Session,
    ) {
        val periodeSpmParamMap =
            mapOf(
                KVP_FELT to søknad.kvp,
                INTRO_FELT to søknad.intro,
                INSTITUSJON_FELT to søknad.institusjon,
                SYKEPENGER_FELT to søknad.sykepenger,
                GJENLEVENDEPENSJON_FELT to søknad.gjenlevendepensjon,
                SUPPLERENDESTØNAD_ALDER_FELT to søknad.supplerendeStønadAlder,
                SUPPLERENDESTØNAD_FLYKTNING_FELT to søknad.supplerendeStønadFlyktning,
                JOBBSJANSEN_FELT to søknad.jobbsjansen,
                TRYGD_OG_PENSJON_FELT to søknad.trygdOgPensjon,
            ).toPeriodeSpmParams()

        val fraOgMedDatoSpmParamMap =
            mapOf(
                ALDERSPENSJON_FELT to søknad.alderspensjon,
            ).toFraOgMedDatoSpmParams()

        val jaNeiSpmParamMap =
            mapOf(
                ETTERLØNN_FELT to søknad.etterlønn,
            ).toJaNeiSpmParams()

        session.run(
            queryOf(
                // language=SQL
                """
                insert into søknad (
                    id,
                    versjon,
                    sak_id,
                    journalpost_id,
                    fornavn, 
                    etternavn, 
                    fnr, 
                    opprettet,
                    tidsstempel_hos_oss,
                    kvp_type,
                    kvp_ja,
                    kvp_fom,
                    kvp_tom,
                    intro_type,
                    intro_ja,
                    intro_fom,
                    intro_tom,
                    institusjon_type,
                    institusjon_ja,
                    institusjon_fom,
                    institusjon_tom,
                    sykepenger_type,
                    sykepenger_ja,
                    sykepenger_fom,
                    sykepenger_tom,
                    supplerende_alder_type,
                    supplerende_alder_ja,
                    supplerende_alder_fom,
                    supplerende_alder_tom,
                    supplerende_flyktning_type,
                    supplerende_flyktning_ja,
                    supplerende_flyktning_fom,
                    supplerende_flyktning_tom,
                    jobbsjansen_type,
                    jobbsjansen_ja,
                    jobbsjansen_fom,
                    jobbsjansen_tom,
                    gjenlevendepensjon_type,
                    gjenlevendepensjon_ja,
                    gjenlevendepensjon_fom,
                    gjenlevendepensjon_tom,
                    alderspensjon_type,
                    alderspensjon_ja,
                    alderspensjon_fom,
                    trygd_og_pensjon_type,
                    trygd_og_pensjon_ja,
                    trygd_og_pensjon_fom,
                    trygd_og_pensjon_tom,
                    etterlonn_type,
                    vedlegg,
                    oppgave_id
                ) values (
                    :id,
                    :versjon,
                    :sak_id,
                    :journalpost_id,
                    :fornavn, 
                    :etternavn,
                    :fnr,
                    :opprettet,
                    :tidsstempel_hos_oss,
                    :kvp_type,
                    :kvp_ja,
                    :kvp_fom,
                    :kvp_tom,
                    :intro_type,
                    :intro_ja,
                    :intro_fom,
                    :intro_tom,
                    :institusjon_type,
                    :institusjon_ja,
                    :institusjon_fom,
                    :institusjon_tom,
                    :sykepenger_type,
                    :sykepenger_ja,
                    :sykepenger_fom,
                    :sykepenger_tom,
                    :supplerende_alder_type,
                    :supplerende_alder_ja,
                    :supplerende_alder_fom,
                    :supplerende_alder_tom,
                    :supplerende_flyktning_type,
                    :supplerende_flyktning_ja,
                    :supplerende_flyktning_fom,
                    :supplerende_flyktning_tom,
                    :jobbsjansen_type,
                    :jobbsjansen_ja,
                    :jobbsjansen_fom,
                    :jobbsjansen_tom,
                    :gjenlevendepensjon_type,
                    :gjenlevendepensjon_ja,
                    :gjenlevendepensjon_fom,
                    :gjenlevendepensjon_tom,
                    :alderspensjon_type,
                    :alderspensjon_ja,
                    :alderspensjon_fom,
                    :trygd_og_pensjon_type,
                    :trygd_og_pensjon_ja,
                    :trygd_og_pensjon_fom,
                    :trygd_og_pensjon_tom,
                    :etterlonn_type,
                    :vedlegg,
                    :oppgave_id
                )
                """.trimIndent(),
                periodeSpmParamMap +
                    fraOgMedDatoSpmParamMap +
                    jaNeiSpmParamMap +
                    mapOf(
                        "id" to søknad.id.toString(),
                        "versjon" to søknad.versjon,
                        "sak_id" to søknad.sakId.toString(),
                        "fornavn" to søknad.personopplysninger.fornavn,
                        "etternavn" to søknad.personopplysninger.etternavn,
                        "fnr" to søknad.fnr.verdi,
                        "journalpost_id" to søknad.journalpostId,
                        "vedlegg" to søknad.vedlegg,
                        "opprettet" to søknad.opprettet,
                        "tidsstempel_hos_oss" to søknad.tidsstempelHosOss,
                        "oppgave_id" to søknad.oppgaveId?.toString(),
                    ),
            ).asUpdate,
        )
    }

    private fun Row.toSakId() = stringOrNull("sak_id")?.let { SakId.fromString(it) }

    private fun Row.toSøknad(session: Session): Søknad {
        val id = SøknadId.fromString(string("id"))
        val versjon = string("versjon")
        val fornavn = string("fornavn")
        val etternavn = string("etternavn")
        val fnr = Fnr.fromString(string("fnr"))
        val opprettet = localDateTime("opprettet")
        val tidsstempelHosOss = localDateTime("tidsstempel_hos_oss")
        val journalpostId = string("journalpost_id")
        val barnetillegg = BarnetilleggDAO.hentBarnetilleggListe(id, session)
        val søknadstiltak = SøknadTiltakDAO.hent(id, session)
        val vedlegg = int("vedlegg")
        val sakId = SakId.fromString(string("sak_id"))
        val saksnummer = Saksnummer(string("saksnummer"))
        val oppgaveId = stringOrNull("oppgave_id")?.let { OppgaveId(it) }
        val kvp = periodeSpm(KVP_FELT)
        val intro = periodeSpm(INTRO_FELT)
        val institusjon = periodeSpm(INSTITUSJON_FELT)
        val etterlønn = jaNeiSpm(ETTERLØNN_FELT)
        val gjenlevendepensjon = periodeSpm(GJENLEVENDEPENSJON_FELT)
        val alderspensjon = fraOgMedDatoSpm(ALDERSPENSJON_FELT)
        val sykepenger = periodeSpm(SYKEPENGER_FELT)
        val supplerendeStønadAlder = periodeSpm(SUPPLERENDESTØNAD_ALDER_FELT)
        val supplerendeStønadFlyktning = periodeSpm(SUPPLERENDESTØNAD_FLYKTNING_FELT)
        val jobbsjansen = periodeSpm(JOBBSJANSEN_FELT)
        val trygdOgPensjon = periodeSpm(TRYGD_OG_PENSJON_FELT)
        val avbrutt = stringOrNull("avbrutt")?.toAvbrutt()
        return Søknad(
            versjon = versjon,
            id = id,
            journalpostId = journalpostId,
            personopplysninger =
            Søknad.Personopplysninger(
                fnr = fnr,
                fornavn = fornavn,
                etternavn = etternavn,
            ),
            tiltak = søknadstiltak,
            barnetillegg = barnetillegg,
            opprettet = opprettet,
            tidsstempelHosOss = tidsstempelHosOss,
            vedlegg = vedlegg,
            kvp = kvp,
            intro = intro,
            institusjon = institusjon,
            etterlønn = etterlønn,
            gjenlevendepensjon = gjenlevendepensjon,
            alderspensjon = alderspensjon,
            sykepenger = sykepenger,
            supplerendeStønadAlder = supplerendeStønadAlder,
            supplerendeStønadFlyktning = supplerendeStønadFlyktning,
            jobbsjansen = jobbsjansen,
            trygdOgPensjon = trygdOgPensjon,
            sakId = sakId,
            saksnummer = saksnummer,
            oppgaveId = oppgaveId,
            avbrutt = avbrutt,
        )
    }
}
