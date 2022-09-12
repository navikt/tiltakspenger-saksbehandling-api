package no.nav.tiltakspenger.vedtak.repository.søknad

import kotliquery.Row
import kotliquery.queryOf
import no.nav.tiltakspenger.vedtak.Barnetillegg
import no.nav.tiltakspenger.vedtak.Søknad
import no.nav.tiltakspenger.vedtak.db.DataSource.session
import org.intellij.lang.annotations.Language

internal class PostgresSøknadRepository: SøknadRepository {

    @Language("SQL")
    private val lagre = "insert into søknad (id, ident, tilstand) values (:id, :ident, :tilstand)"

    @Language("SQL")
    private val slett = "delete from søknad where id = ?"

    @Language("SQL")
    private val finnes = "select exists(select 1 from søknad where ident = ?)"

    @Language("SQL")
    private val hentAlle = "select * from søknad where ident = ?"

    override fun hentAlle(ident: String): List<Søknad> {
        return session.run(queryOf(hentAlle, ident)
            .map { row ->
                row.toSøknad()
            }.asList)
    }

    private fun slettAlle(ident: String): Unit {
        // TODO: later når vi har skrevet noen tester
//        session.run(
//
//        )
    }

    override fun lagre(ident: String, søknader: List<Søknad>): Int {
        slettAlle(ident)
        return 0
    }

    private fun Row.toSøknad(): Søknad {
        val id = uuid("id")
        val fornavn = string("fornavn")
        val etternavn = string("etternavn")
        val ident = string("ident")
        val deltarKvp = boolean("deltar_kvp")
        val deltarIntroduksjonsprogrammet = boolean("deltar_introduksjon")
        val oppholdInstitusjon = boolean("institusjon_opphold")
        val typeInstitusjon = string("institusjon_type")
        val tiltaksArrangør = string("tiltaks_arrangoer")
        val tiltaksType = string("tiltaks_type")
        val opprettet = localDateTime("opprettet")
        val brukerRegistrertStartDato = localDate("bruker_reg_startdato")
        val brukerRegistrertSluttDato = localDate("bruker_reg_sluttdato")
        val systemRegistrertStartDato = localDate("system_reg_startdato")
        val systemRegistrertSluttDato = localDate("system_reg_sluttdato")
        val barnetillegg = emptyList<Barnetillegg>() // TODO kalle barnetillegg.hentAlle(ident)
        val innhentet = localDateTime("innhentet")

        return Søknad(
            id = id,
            fornavn = fornavn,
            etternavn = etternavn,
            ident = ident,
            deltarKvp = deltarKvp,
            deltarIntroduksjonsprogrammet = deltarIntroduksjonsprogrammet,
            oppholdInstitusjon = oppholdInstitusjon,
            typeInstitusjon = typeInstitusjon,
            opprettet = opprettet,
            barnetillegg = barnetillegg,
            innhentet = innhentet,
            arenaTiltak = null,
            brukerregistrertTiltak = null,
            trygdOgPensjon = null,
            fritekst = null,
        )
    }
}
