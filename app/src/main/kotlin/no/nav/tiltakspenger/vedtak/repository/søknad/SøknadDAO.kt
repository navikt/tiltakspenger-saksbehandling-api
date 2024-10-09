package no.nav.tiltakspenger.vedtak.repository.søknad

import kotliquery.Row
import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Søknad
import org.intellij.lang.annotations.Language

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
    fun finnIdent(
        søknadId: String,
        session: Session,
    ): String? =
        session.run(
            queryOf(sqlHentIdent, søknadId)
                .map { row -> row.toIdent() }
                .asSingle,
        )

    fun finnJournalpostId(
        søknadId: String,
        session: Session,
    ): String? =
        session.run(
            queryOf(sqlHentIdent, søknadId)
                .map { row -> row.toJournalpostId() }
                .asSingle,
        )

    fun hentForBehandlingId(
        behandlingId: BehandlingId,
        session: Session,
    ): Søknad? =
        session.run(
            queryOf(sqlHent, behandlingId.toString())
                .map { row -> row.toSøknad(session) }
                .asSingle,
        )

    fun hentForSøknadId(
        søknadId: SøknadId,
        session: Session,
    ): Søknad =
        session.run(
            queryOf("select * from søknad where id = ?", søknadId.toString())
                .map { row -> row.toSøknad(session) }
                .asSingle,
        )!!

    fun knyttSøknadTilBehandling(
        behandlingId: BehandlingId,
        søknadId: SøknadId,
        session: Session,
    ) {
        session.run(
            queryOf(
                """update søknad set behandling_id = :behandlingId where id = :soknadId""",
                mapOf(
                    "behandlingId" to behandlingId.toString(),
                    "soknadId" to søknadId.toString(),
                ),
            ).asUpdate,
        )
    }

    private fun søknadFinnes(
        søknadId: SøknadId,
        session: Session,
    ): Boolean =
        session.run(
            queryOf(sqlFinnes, søknadId.toString()).map { row -> row.boolean("exists") }.asSingle,
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
                sqlLagreSøknad,
                periodeSpmParamMap +
                    fraOgMedDatoSpmParamMap +
                    jaNeiSpmParamMap +
                    mapOf(
                        "id" to søknad.id.toString(),
                        "versjon" to søknad.versjon,
                        "behandlingId" to null,
                        "fornavn" to søknad.personopplysninger.fornavn,
                        "etternavn" to søknad.personopplysninger.etternavn,
                        "ident" to søknad.fnr.verdi,
                        "journalpostId" to søknad.journalpostId,
                        "vedlegg" to søknad.vedlegg,
                        "opprettet" to søknad.opprettet,
                        "tidsstempelHosOss" to søknad.tidsstempelHosOss,
                    ),
            ).asUpdate,
        )
    }

    private fun Row.toIdent() = string("ident")

    private fun Row.toJournalpostId() = string("journalpost_id")

    private fun Row.toSøknad(session: Session): Søknad {
        val id = SøknadId.fromString(string("id"))
        val versjon = string("versjon")
        val fornavn = string("fornavn")
        val etternavn = string("etternavn")
        val fnr = Fnr.fromString(string("ident"))
        val opprettet = localDateTime("opprettet")
        val tidsstempelHosOss = localDateTime("tidsstempel_hos_oss")
        val journalpostId = string("journalpost_id")
        val barnetillegg = BarnetilleggDAO.hentBarnetilleggListe(id, session)
        val søknadstiltak = SøknadTiltakDAO.hent(id, session)
        val vedlegg = int("vedlegg")
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
        )
    }

    @Language("SQL")
    private val sqlLagreSøknad =
        """
        insert into søknad (
            id,
            versjon,
            behandling_id,
            journalpost_id,
            fornavn, 
            etternavn, 
            ident, 
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
            vedlegg
        ) values (
            :id,
            :versjon,
            :behandlingId,
            :journalpostId,
            :fornavn, 
            :etternavn,
            :ident,
            :opprettet,
            :tidsstempelHosOss,
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
            :vedlegg
        )
        """.trimIndent()

    @Language("SQL")
    private val sqlFinnes = "select exists(select 1 from søknad where id = ?)"

    @Language("SQL")
    private val sqlHent = "select * from søknad where behandling_id = ?"

    @Language("SQL")
    private val sqlHentIdent = "select * from søknad where id = ?"
}
