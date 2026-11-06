package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo

import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infra.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infra.sqlQuery
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse.TiltaksdeltakerHendelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse.TiltaksdeltakerHendelseKilde
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse.TiltaksdeltakerHendelseRepo
import java.time.Clock

/** Lagrer i `tiltaksdeltaker_endring`. */
class TiltaksdeltakerHendelsePostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
    private val clock: Clock,
) : TiltaksdeltakerHendelseRepo {

    override fun lagre(
        tiltaksdeltakerHendelse: TiltaksdeltakerHendelse,
        melding: String,
        kilde: TiltaksdeltakerHendelseKilde,
        sessionContext: SessionContext,
    ) {
        sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                        insert into tiltaksdeltaker_endring (
                            id,
                            ekstern_deltaker_id,
                            tiltaksdeltaker_id,
                            sak_id,
                            kilde,
                            verdi,
                            opprettet
                        ) values (
                            :id,
                            :ekstern_deltaker_id,
                            :tiltaksdeltaker_id,
                            :sak_id,
                            :kilde,
                            :verdi,
                            :opprettet
                        )
                    """.trimIndent(),
                    "id" to tiltaksdeltakerHendelse.id.toString(),
                    "ekstern_deltaker_id" to tiltaksdeltakerHendelse.eksternDeltakerId,
                    "tiltaksdeltaker_id" to tiltaksdeltakerHendelse.internDeltakerId.toString(),
                    "sak_id" to tiltaksdeltakerHendelse.sakId.toString(),
                    "kilde" to kilde.name,
                    "verdi" to melding,
                    "opprettet" to nå(clock),
                ).asUpdate,
            )
        }
    }
}
