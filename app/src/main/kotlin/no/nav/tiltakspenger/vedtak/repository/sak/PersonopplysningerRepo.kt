package no.nav.tiltakspenger.vedtak.repository.sak

import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.queryOf
import mu.KotlinLogging
import no.nav.tiltakspenger.domene.behandling.Personopplysninger
import no.nav.tiltakspenger.felles.SakId
import no.nav.tiltakspenger.felles.UlidBase
import no.nav.tiltakspenger.vedtak.db.booleanOrNull
import org.intellij.lang.annotations.Language

internal class PersonopplysningerRepo(
    private val barnMedIdentDAO: PersonopplysningerBarnMedIdentRepo = PersonopplysningerBarnMedIdentRepo(),
    private val barnUtenIdentDAO: PersonopplysningerBarnUtenIdentRepo = PersonopplysningerBarnUtenIdentRepo(),
) {
    private val log = KotlinLogging.logger {}
    private val securelog = KotlinLogging.logger("tjenestekall")

    fun hent(
        sakId: SakId,
        txSession: TransactionalSession,
    ): List<Personopplysninger> {
        val søker = hentPersonopplysningerForInnsending(sakId, txSession) ?: return emptyList()
        val barnMedIdent = barnMedIdentDAO.hent(sakId, txSession)
        val barnUtenIdent = barnUtenIdentDAO.hent(sakId, txSession)

        return listOf(søker) + barnMedIdent + barnUtenIdent
    }

    fun lagre(
        sakId: SakId,
        personopplysninger: List<Personopplysninger>,
        txSession: TransactionalSession,
    ) {
        log.info { "Sletter personopplysninger før lagring" }
        slett(sakId, txSession)
        barnMedIdentDAO.slett(sakId, txSession)
        barnUtenIdentDAO.slett(sakId, txSession)

        log.info { "Lagre personopplysninger" }
        personopplysninger.forEach {
            when (it) {
                is Personopplysninger.Søker -> lagre(sakId, it, txSession)
                is Personopplysninger.BarnMedIdent -> barnMedIdentDAO.lagre(sakId, it, txSession)
                is Personopplysninger.BarnUtenIdent -> barnUtenIdentDAO.lagre(
                    sakId,
                    it,
                    txSession,
                )
            }
        }
    }

    private fun hentPersonopplysningerForInnsending(sakId: SakId, txSession: TransactionalSession) =
        txSession.run(queryOf(hentSql, sakId.toString()).map(toPersonopplysninger).asSingle)

    private fun lagre(
        sakId: SakId,
        personopplysninger: Personopplysninger.Søker,
        txSession: TransactionalSession,
    ) {
        securelog.info { "Lagre personopplysninger for søker $personopplysninger" }
        txSession.run(
            queryOf(
                lagreSql,
                mapOf(
                    "id" to UlidBase.random(ULID_PREFIX_PERSONOPPLYSNINGER).toString(),
                    "sakId" to sakId.toString(),
                    "ident" to personopplysninger.ident,
                    "fodselsdato" to personopplysninger.fødselsdato,
                    "fornavn" to personopplysninger.fornavn,
                    "mellomnavn" to personopplysninger.mellomnavn,
                    "etternavn" to personopplysninger.etternavn,
                    "fortrolig" to personopplysninger.fortrolig,
                    "strengtFortrolig" to personopplysninger.strengtFortrolig,
                    "strengtFortroligUtland" to personopplysninger.strengtFortroligUtland,
                    "skjermet" to personopplysninger.skjermet,
                    "kommune" to personopplysninger.kommune,
                    "bydel" to personopplysninger.bydel,
                    "tidsstempelHosOss" to personopplysninger.tidsstempelHosOss,
                ),
            ).asUpdate,
        )
    }

    private fun slett(sakId: SakId, txSession: TransactionalSession) =
        txSession.run(queryOf(slettSql, sakId.toString()).asUpdate)

    private val toPersonopplysninger: (Row) -> Personopplysninger.Søker = { row ->
        Personopplysninger.Søker(
            ident = row.string("ident"),
            fødselsdato = row.localDate("fødselsdato"),
            fornavn = row.string("fornavn"),
            mellomnavn = row.stringOrNull("mellomnavn"),
            etternavn = row.string("etternavn"),
            fortrolig = row.boolean("fortrolig"),
            strengtFortrolig = row.boolean("strengt_fortrolig"),
            strengtFortroligUtland = row.boolean("strengt_fortrolig_utland"),
            skjermet = row.booleanOrNull("skjermet"),
            kommune = row.stringOrNull("kommune"),
            bydel = row.stringOrNull("bydel"),
            tidsstempelHosOss = row.localDateTime("tidsstempel_hos_oss"),
        )
    }

    @Language("SQL")
    private val slettSql = "delete from sak_personopplysninger_søker where sakId = ?"

    @Language("SQL")
    private val hentSql = "select * from sak_personopplysninger_søker where sakId = ?"

    @Language("SQL")
    private val lagreSql = """
        insert into sak_personopplysninger_søker (
            id,
            sakId,        
            ident,           
            fødselsdato,     
            fornavn,         
            mellomnavn,      
            etternavn,       
            fortrolig,       
            strengt_fortrolig,
            strengt_fortrolig_utland,
            skjermet,        
            kommune,         
            bydel,           
            tidsstempel_hos_oss            
        ) values (
            :id,
            :sakId,
            :ident,             
            :fodselsdato,   
            :fornavn,           
            :mellomnavn,        
            :etternavn,         
            :fortrolig,         
            :strengtFortrolig, 
            :strengtFortroligUtland, 
            :skjermet,          
            :kommune,           
            :bydel,             
            :tidsstempelHosOss
        )
    """.trimIndent()

    companion object {
        private const val ULID_PREFIX_PERSONOPPLYSNINGER = "poppl"
    }
}
