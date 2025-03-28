package no.nav.tiltakspenger.saksbehandling.søknad.infra.repo

import kotliquery.Row
import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.common.UlidBase.Companion.random
import no.nav.tiltakspenger.saksbehandling.søknad.Søknadstiltak
import org.intellij.lang.annotations.Language

internal object SøknadTiltakDAO {

    private const val ULID_PREFIX_TILTAK = "tilt"

    fun hent(
        søknadId: SøknadId,
        session: Session,
    ): Søknadstiltak = hentTiltak(søknadId, session)!!

    private fun hentTiltak(
        søknadId: SøknadId,
        session: Session,
    ): Søknadstiltak? =
        session.run(
            queryOf(hentTiltak, søknadId.toString()).map { row -> row.toTiltak() }.asSingle,
        )

    fun lagre(
        søknadId: SøknadId,
        søknadstiltak: Søknadstiltak,
        txSession: TransactionalSession,
    ) {
        slettTiltak(søknadId, txSession)
        lagreTiltak(søknadId, søknadstiltak, txSession)
    }

    private fun lagreTiltak(
        søknadId: SøknadId,
        søknadstiltak: Søknadstiltak,
        session: Session,
    ) {
        session.run(
            queryOf(
                lagreTiltak,
                mapOf(
                    "id" to random(ULID_PREFIX_TILTAK).toString(),
                    "soknad_id" to søknadId.toString(),
                    "ekstern_id" to søknadstiltak.id,
                    "typekode" to søknadstiltak.typeKode,
                    "typenavn" to søknadstiltak.typeNavn,
                    "deltakelse_fra_og_med" to søknadstiltak.deltakelseFom,
                    "deltakelse_til_og_med" to søknadstiltak.deltakelseTom,
                ),
            ).asUpdate,
        )
    }

    private fun slettTiltak(
        søknadId: SøknadId,
        session: Session,
    ) {
        session.run(queryOf(slettTiltak, søknadId.toString()).asUpdate)
    }

    private fun Row.toTiltak(): Søknadstiltak {
        val eksternId = string("ekstern_id")
        val typekode = string("typekode")
        val typenavn = string("typenavn")
        val deltakelseFom = localDate("deltakelse_fra_og_med")
        val deltakelseTom = localDate("deltakelse_til_og_med")
        return Søknadstiltak(
            id = eksternId,
            deltakelseFom = deltakelseFom,
            deltakelseTom = deltakelseTom,
            typeKode = typekode,
            typeNavn = typenavn,
        )
    }

    @Language("SQL")
    private val hentTiltak = "select * from søknadstiltak where søknad_id = ?"

    @Language("SQL")
    private val slettTiltak = "delete from søknadstiltak where søknad_id = ?"

    @Language("SQL")
    private val lagreTiltak =
        """
        insert into søknadstiltak (
            id,
            søknad_id,
            ekstern_id,
            typekode,
            typenavn,
            deltakelse_fra_og_med,
            deltakelse_til_og_med
        ) values (
            :id,
            :soknad_id,
            :ekstern_id,
            :typekode,
            :typenavn,
            :deltakelse_fra_og_med,
            :deltakelse_til_og_med
        )
        """.trimIndent()
}
