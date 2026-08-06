package no.nav.tiltakspenger.saksbehandling.søknad.infra.repo

import kotliquery.Row
import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.common.UlidBase.Companion.random
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.periode
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.tilDbPeriode
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadstiltak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import org.intellij.lang.annotations.Language

object SøknadTiltakDAO {

    private const val ULID_PREFIX_TILTAK = "tilt"

    fun hentTiltak(
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
                    "typekode" to søknadstiltak.typeKode.name,
                    "typenavn" to søknadstiltak.typeNavn,
                    "deltakelse" to tilDbPeriode(søknadstiltak.deltakelseFom, søknadstiltak.deltakelseTom),
                    "tiltaksdeltaker_id" to søknadstiltak.tiltaksdeltakerId.toString(),
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
        val deltakelse = periode("deltakelse")
        val tiltaksdeltakerId = string("tiltaksdeltaker_id")
        return Søknadstiltak(
            id = eksternId,
            deltakelseFom = deltakelse.fraOgMed,
            deltakelseTom = deltakelse.tilOgMed,
            typeKode = TiltakResponsDTO.TiltakTypeDTO.valueOf(typekode),
            typeNavn = typenavn,
            tiltaksdeltakerId = TiltaksdeltakerId.fromString(tiltaksdeltakerId),
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
            deltakelse,
            tiltaksdeltaker_id
        ) values (
            :id,
            :soknad_id,
            :ekstern_id,
            :typekode,
            :typenavn,
            :deltakelse::periode,
            :tiltaksdeltaker_id
        )
        """.trimIndent()
}
