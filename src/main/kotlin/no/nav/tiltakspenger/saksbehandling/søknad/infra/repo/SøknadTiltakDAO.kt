package no.nav.tiltakspenger.saksbehandling.søknad.infra.repo

import kotliquery.Row
import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.common.UlidBase.Companion.random
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadstiltak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import org.intellij.lang.annotations.Language

internal object SøknadTiltakDAO {

    private const val ULID_PREFIX_TILTAK = "tilt"

    fun hentTiltak(
        søknadId: SøknadId,
        session: Session,
    ): Søknadstiltak? =
        session.run(
            queryOf(hentTiltak, søknadId.toString()).map { row -> row.toTiltak() }.asSingle,
        )

    fun hentTiltakUtenInternId(
        limit: Int,
        session: Session,
    ): List<Søknadstiltak> =
        session.run(
            sqlQuery(
                """
                    select *
                    from søknadstiltak
                    where tiltaksdeltaker_id is null
                    order by id
                    limit :limit
                """.trimIndent(),
                "limit" to limit,
            ).map { row: Row ->
                row.toTiltak()
            }.asList,
        )

    fun oppdaterInternId(
        eksternId: String,
        internId: TiltaksdeltakerId,
        session: Session,
    ) {
        session.run(
            queryOf(
                """
                        update søknadstiltak
                        set tiltaksdeltaker_id = :tiltaksdeltaker_id
                        where tiltaksdeltaker_id is null
                          and ekstern_id = :ekstern_id
                """.trimIndent(),
                mapOf(
                    "tiltaksdeltaker_id" to internId.toString(),
                    "ekstern_id" to eksternId,
                ),
            ).asUpdate,
        )
    }

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
                    "typekode" to søknadstiltak.typeKode.name,
                    "typenavn" to søknadstiltak.typeNavn,
                    "deltakelse_fra_og_med" to søknadstiltak.deltakelseFom,
                    "deltakelse_til_og_med" to søknadstiltak.deltakelseTom,
                    "tiltaksdeltaker_id" to søknadstiltak.tiltaksdeltakerId?.toString(),
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
        val tiltaksdeltakerId = stringOrNull("tiltaksdeltaker_id")
        return Søknadstiltak(
            id = eksternId,
            deltakelseFom = deltakelseFom,
            deltakelseTom = deltakelseTom,
            typeKode = TiltakResponsDTO.TiltakType.valueOf(typekode),
            typeNavn = typenavn,
            tiltaksdeltakerId = tiltaksdeltakerId?.let { TiltaksdeltakerId.fromString(it) },
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
            deltakelse_til_og_med,
            tiltaksdeltaker_id
        ) values (
            :id,
            :soknad_id,
            :ekstern_id,
            :typekode,
            :typenavn,
            :deltakelse_fra_og_med,
            :deltakelse_til_og_med,
            :tiltaksdeltaker_id
        )
        """.trimIndent()
}
