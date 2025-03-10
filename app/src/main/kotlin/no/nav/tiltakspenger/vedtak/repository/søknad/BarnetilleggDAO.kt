package no.nav.tiltakspenger.vedtak.repository.søknad

import kotliquery.Row
import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.common.UlidBase.Companion.random
import no.nav.tiltakspenger.vedtak.saksbehandling.domene.behandling.BarnetilleggFraSøknad
import org.intellij.lang.annotations.Language

internal object BarnetilleggDAO {

    private const val ULID_PREFIX_BARNETILLEGG = "btil"

    fun lagre(
        søknadId: SøknadId,
        barnetillegg: List<BarnetilleggFraSøknad>,
        session: TransactionalSession,
    ) {
        slettBarnetillegg(søknadId, session)
        barnetillegg.forEach {
            lagreBarnetillegg(søknadId, it, session)
        }
    }

    fun hentBarnetilleggListe(
        søknadId: SøknadId,
        session: Session,
    ): List<BarnetilleggFraSøknad> =
        session.run(
            queryOf(hentBarnetillegg, søknadId.toString())
                .map { row -> row.toBarnetillegg() }
                .asList,
        )

    private fun lagreBarnetillegg(
        søknadId: SøknadId,
        barnetillegg: BarnetilleggFraSøknad,
        txSession: TransactionalSession,
    ) {
        val paramMap =
            when (barnetillegg) {
                is BarnetilleggFraSøknad.FraPdl ->
                    mapOf(
                        "type" to "PDL",
                        "id" to random(ULID_PREFIX_BARNETILLEGG).toString(),
                        "soknadId" to søknadId.toString(),
                        "fodselsdato" to barnetillegg.fødselsdato,
                        "fornavn" to barnetillegg.fornavn,
                        "mellomnavn" to barnetillegg.mellomnavn,
                        "etternavn" to barnetillegg.etternavn,
                        "opphold_i_eos_type" to lagreJaNeiSpmType(barnetillegg.oppholderSegIEØS),
                    )

                is BarnetilleggFraSøknad.Manuell ->
                    mapOf(
                        "type" to "MANUELL",
                        "id" to random(ULID_PREFIX_BARNETILLEGG).toString(),
                        "soknadId" to søknadId.toString(),
                        "fodselsdato" to barnetillegg.fødselsdato,
                        "fornavn" to barnetillegg.fornavn,
                        "mellomnavn" to barnetillegg.mellomnavn,
                        "etternavn" to barnetillegg.etternavn,
                        "opphold_i_eos_type" to lagreJaNeiSpmType(barnetillegg.oppholderSegIEØS),
                    )
            }
        txSession.run(
            queryOf(lagreBarnetillegg, paramMap).asUpdate,
        )
    }

    private fun slettBarnetillegg(
        søknadId: SøknadId,
        txSession: TransactionalSession,
    ) {
        txSession.run(
            queryOf(slettBarnetillegg, søknadId.toString()).asUpdate,
        )
    }

    private fun Row.toBarnetillegg(): BarnetilleggFraSøknad {
        val type = string("type")
        val fødselsdato = localDate("fodselsdato")
        val fornavn = stringOrNull("fornavn")
        val mellomnavn = stringOrNull("mellomnavn")
        val etternavn = stringOrNull("etternavn")
        val oppholderSegIEØS = jaNeiSpm("opphold_i_eos")
        return if (type == "PDL") {
            BarnetilleggFraSøknad.FraPdl(
                oppholderSegIEØS = oppholderSegIEØS,
                fornavn = fornavn,
                mellomnavn = mellomnavn,
                etternavn = etternavn,
                fødselsdato = fødselsdato,
            )
        } else {
            checkNotNull(fornavn) { "Fornavn kan ikke være null for barnetillegg, manuelle barn " }
            checkNotNull(etternavn) { "Etternavn kan ikke være null for barnetillegg, manuelle barn " }
            BarnetilleggFraSøknad.Manuell(
                oppholderSegIEØS = oppholderSegIEØS,
                fornavn = fornavn,
                mellomnavn = mellomnavn,
                etternavn = etternavn,
                fødselsdato = fødselsdato,
            )
        }
    }

    @Language("SQL")
    private val lagreBarnetillegg =
        """
        insert into søknad_barnetillegg (
            id,
            søknad_id,
            type,
            fodselsdato,
            fornavn,
            mellomnavn,
            etternavn,
            opphold_i_eos_type
        ) values (
            :id,
            :soknadId,
            :type,
            :fodselsdato,
            :fornavn,
            :mellomnavn,
            :etternavn,
            :opphold_i_eos_type
        )
        """.trimIndent()

    @Language("SQL")
    private val slettBarnetillegg = "delete from søknad_barnetillegg where søknad_id = ?"

    @Language("SQL")
    private val hentBarnetillegg = "select * from søknad_barnetillegg where søknad_id = ?"
}
