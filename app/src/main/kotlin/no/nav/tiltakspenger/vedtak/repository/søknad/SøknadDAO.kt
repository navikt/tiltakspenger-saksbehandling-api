package no.nav.tiltakspenger.vedtak.repository.søknad

import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.tiltakspenger.felles.InnsendingId
import no.nav.tiltakspenger.felles.SøknadId
import no.nav.tiltakspenger.vedtak.IntroduksjonsprogrammetDetaljer
import no.nav.tiltakspenger.vedtak.Søknad
import no.nav.tiltakspenger.vedtak.TypeInstitusjon
import no.nav.tiltakspenger.vedtak.db.booleanOrNull
import org.intellij.lang.annotations.Language

internal class SøknadDAO(
    private val barnetilleggDAO: BarnetilleggDAO = BarnetilleggDAO(),
    private val tiltakDAO: TiltakDAO = TiltakDAO(),
    private val trygdOgPensjonDAO: TrygdOgPensjonDAO = TrygdOgPensjonDAO(),
    private val vedleggDAO: VedleggDAO = VedleggDAO(),
) {
    fun finnIdent(søknadId: String, txSession: TransactionalSession): String? {
        return txSession.run(
            queryOf(hentIdent, søknadId)
                .map { row -> row.toIdent() }
                .asSingle
        )
    }

    fun finnJournalpostId(søknadId: String, txSession: TransactionalSession): String? {
        return txSession.run(
            queryOf(hentIdent, søknadId)
                .map { row -> row.toJournalpostId() }
                .asSingle
        )
    }

    fun hent(innsendingId: InnsendingId, txSession: TransactionalSession): Søknad? {
        return txSession.run(
            queryOf(hent, innsendingId.toString())
                .map { row -> row.toSøknad(txSession) }
                .asSingle
        )
    }

    fun lagre(innsendingId: InnsendingId, søknad: Søknad?, txSession: TransactionalSession) {
        søknad?.let { lagreHeleSøknaden(innsendingId, it, txSession) } // TODO: Burde vel egentlig slette søknaden..
    }

    private fun søknadFinnes(søknadId: SøknadId, txSession: TransactionalSession): Boolean = txSession.run(
        queryOf(finnes, søknadId.toString()).map { row -> row.boolean("exists") }.asSingle
    ) ?: throw RuntimeException("Failed to check if søknad exists")

    private fun lagreHeleSøknaden(innsendingId: InnsendingId, søknad: Søknad, txSession: TransactionalSession) {
        if (søknadFinnes(søknad.id, txSession)) {
            oppdaterSøknad(søknad, txSession)
        } else {
            lagreSøknad(innsendingId, søknad, txSession)
        }
        barnetilleggDAO.lagre(søknad.id, søknad.barnetillegg, txSession)
        tiltakDAO.lagre(søknad.id, søknad.tiltak, txSession)
        trygdOgPensjonDAO.lagre(søknad.id, søknad.trygdOgPensjon, txSession)
        vedleggDAO.lagre(søknad.id, søknad.vedlegg, txSession)
    }

    private fun oppdaterSøknad(søknad: Søknad, txSession: TransactionalSession) {
        txSession.run(
            queryOf(
                oppdaterSøknad,
                mapOf(
                    "id" to søknad.id.toString(),
                    "fornavn" to søknad.fornavn,
                    "etternavn" to søknad.etternavn,
                    "ident" to søknad.ident,
                    "deltarKvp" to søknad.deltarKvp,
                    "deltarIntro" to søknad.deltarIntroduksjonsprogrammet,
                    "introFom" to søknad.introduksjonsprogrammetDetaljer?.fom,
                    "introTom" to søknad.introduksjonsprogrammetDetaljer?.tom,
                    "instOpphold" to søknad.oppholdInstitusjon,
                    "instType" to søknad.typeInstitusjon?.name,
                    "fritekst" to søknad.fritekst,
                    "journalpostId" to søknad.journalpostId,
                    "dokumentinfoId" to søknad.dokumentInfoId,
                    "opprettet" to søknad.opprettet,
                    "tidsstempelHosOss" to søknad.tidsstempelHosOss,
                )
            ).asUpdate
        )
    }

    private fun lagreSøknad(innsendingId: InnsendingId, søknad: Søknad, txSession: TransactionalSession) {
        txSession.run(
            queryOf(
                lagreSøknad,
                mapOf(
                    "id" to søknad.id.toString(),
                    "innsendingId" to innsendingId.toString(),
                    "eksternSoknadId" to søknad.søknadId,
                    "fornavn" to søknad.fornavn,
                    "etternavn" to søknad.etternavn,
                    "ident" to søknad.ident,
                    "deltarKvp" to søknad.deltarKvp,
                    "deltarIntro" to søknad.deltarIntroduksjonsprogrammet,
                    "introFom" to søknad.introduksjonsprogrammetDetaljer?.fom,
                    "introTom" to søknad.introduksjonsprogrammetDetaljer?.tom,
                    "instOpphold" to søknad.oppholdInstitusjon,
                    "instType" to søknad.typeInstitusjon?.name,
                    "fritekst" to søknad.fritekst,
                    "journalpostId" to søknad.journalpostId,
                    "dokumentinfoId" to søknad.dokumentInfoId,
                    "opprettet" to søknad.opprettet,
                    "tidsstempelHosOss" to søknad.tidsstempelHosOss,
                )
            ).asUpdate
        )
    }

    private fun Row.toIdent() = string("ident")

    private fun Row.toJournalpostId() = string("journalpost_id")

    private fun Row.toSøknad(txSession: TransactionalSession): Søknad {
        val id = SøknadId.fromDb(string("id"))
        val søknadId = string("søknad_id")
        val fornavn = stringOrNull("fornavn")
        val etternavn = stringOrNull("etternavn")
        val ident = string("ident")
        val deltarKvp = boolean("deltar_kvp")
        val deltarIntroduksjonsprogrammet = booleanOrNull("deltar_intro")
        val introduksjonsprogrammetFom = localDateOrNull("intro_fom")
        val introduksjonsprogrammetTom = localDateOrNull("intro_tom")
        val oppholdInstitusjon = booleanOrNull("institusjon_opphold")
        val typeInstitusjon = stringOrNull("institusjon_type")?.let { TypeInstitusjon.valueOf(it) }
        val opprettet = localDateTimeOrNull("opprettet")
        val tidsstempelHosOss = localDateTime("tidsstempel_hos_oss")
        val dokumentInfoId = string("dokumentinfo_id")
        val journalpostId = string("journalpost_id")
        val fritekst = stringOrNull("fritekst")
        val barnetillegg = barnetilleggDAO.hentBarnetilleggListe(id, txSession)
        val tiltak = tiltakDAO.hent(id, txSession)
        val trygdOgPensjon = trygdOgPensjonDAO.hentTrygdOgPensjonListe(id, txSession)
        val vedlegg = vedleggDAO.hentVedleggListe(id, txSession)

        return Søknad(
            id = id,
            søknadId = søknadId,
            fornavn = fornavn,
            etternavn = etternavn,
            ident = ident,
            deltarKvp = deltarKvp,
            deltarIntroduksjonsprogrammet = deltarIntroduksjonsprogrammet,
            introduksjonsprogrammetDetaljer = introduksjonsprogrammetFom?.let {
                IntroduksjonsprogrammetDetaljer(
                    introduksjonsprogrammetFom,
                    introduksjonsprogrammetTom
                )
            },
            oppholdInstitusjon = oppholdInstitusjon,
            typeInstitusjon = typeInstitusjon,
            opprettet = opprettet,
            barnetillegg = barnetillegg,
            tidsstempelHosOss = tidsstempelHosOss,
            tiltak = tiltak,
            trygdOgPensjon = trygdOgPensjon,
            fritekst = fritekst,
            dokumentInfoId = dokumentInfoId,
            journalpostId = journalpostId,
            vedlegg = vedlegg,
        )
    }

    @Language("SQL")
    private val lagreSøknad = """
        insert into søknad (
            id,
            innsending_id,
            søknad_id,
            fornavn, 
            etternavn, 
            ident, 
            deltar_kvp, 
            deltar_intro,
            intro_fom,
            intro_tom,
            institusjon_opphold, 
            institusjon_type,
            fritekst,
            journalpost_id,
            dokumentinfo_id,
            opprettet,
            tidsstempel_hos_oss
        ) values (
            :id, 
            :innsendingId,
            :eksternSoknadId,
            :fornavn, 
            :etternavn,
            :ident,
            :deltarKvp,
            :deltarIntro,
            :introFom,
            :introTom,
            :instOpphold,
            :instType,
            :fritekst,
            :journalpostId,
            :dokumentinfoId,
            :opprettet,
            :tidsstempelHosOss
        )
    """.trimIndent()

    @Language("SQL")
    private val oppdaterSøknad = """
        update søknad set  
            fornavn = :fornavn, 
            etternavn = :etternavn, 
            ident = :ident, 
            deltar_kvp = :deltarKvp, 
            deltar_intro = :deltarIntro,
            intro_fom = :introFom,
            intro_tom = :introTom,
            institusjon_opphold = :instOpphold, 
            institusjon_type = :instType,
            fritekst = :fritekst,
            journalpost_id = :journalpostId,
            dokumentinfo_id = :dokumentinfoId,
            opprettet = :opprettet,
            tidsstempel_hos_oss = :tidsstempelHosOss
        where id = :id
    """.trimIndent()

    @Language("SQL")
    private val finnes = "select exists(select 1 from søknad where id = ?)"

    @Language("SQL")
    private val hent = "select * from søknad where innsending_id = ?"

    @Language("SQL")
    private val hentIdent = "select * from søknad where søknad_id = ?"
}
