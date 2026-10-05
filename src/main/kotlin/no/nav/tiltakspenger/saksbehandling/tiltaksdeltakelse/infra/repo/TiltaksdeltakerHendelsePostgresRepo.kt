package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo

import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse.TiltaksdeltakerHendelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse.TiltaksdeltakerHendelseKilde
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse.TiltaksdeltakerHendelseRepo
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http.TiltaksdeltakelseFraRegister
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakerEndring
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
        lagre(
            tiltaksdeltakerHendelse = tiltaksdeltakerHendelse,
            kilde = kilde,
            verdi = melding,
            endring = null,
            behandlingId = null,
            oppgaveId = null,
            erBehandlet = false,
            sessionContext = sessionContext,
        )
    }

    override fun lagreBehandletEndring(
        tiltaksdeltakerHendelse: TiltaksdeltakerHendelse,
        nåtilstand: TiltaksdeltakelseFraRegister?,
        endring: TiltaksdeltakerEndring?,
        behandlingId: RammebehandlingId?,
        oppgaveId: OppgaveId?,
        sessionContext: SessionContext,
    ) {
        lagre(
            tiltaksdeltakerHendelse = tiltaksdeltakerHendelse,
            kilde = TiltaksdeltakerHendelseKilde.Tiltakshistorikk,
            verdi = nåtilstand?.let { serialize(it.toDbJson()) },
            endring = endring?.toDbJson(),
            behandlingId = behandlingId,
            oppgaveId = oppgaveId,
            erBehandlet = true,
            sessionContext = sessionContext,
        )
    }

    private fun lagre(
        tiltaksdeltakerHendelse: TiltaksdeltakerHendelse,
        kilde: TiltaksdeltakerHendelseKilde,
        verdi: String?,
        endring: String?,
        behandlingId: RammebehandlingId?,
        oppgaveId: OppgaveId?,
        erBehandlet: Boolean,
        sessionContext: SessionContext,
    ) {
        val nå = nå(clock)
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
                            endring,
                            behandling_id,
                            oppgave_id,
                            opprettet,
                            behandlet_tidspunkt
                        ) values (
                            :id,
                            :ekstern_deltaker_id,
                            :tiltaksdeltaker_id,
                            :sak_id,
                            :kilde,
                            :verdi,
                            :endring::jsonb,
                            :behandling_id,
                            :oppgave_id,
                            :opprettet,
                            :behandlet_tidspunkt
                        )
                    """.trimIndent(),
                    "id" to tiltaksdeltakerHendelse.id.toString(),
                    "ekstern_deltaker_id" to tiltaksdeltakerHendelse.eksternDeltakerId,
                    "tiltaksdeltaker_id" to tiltaksdeltakerHendelse.internDeltakerId.toString(),
                    "sak_id" to tiltaksdeltakerHendelse.sakId.toString(),
                    "kilde" to kilde.name,
                    "verdi" to verdi,
                    "endring" to endring,
                    "behandling_id" to behandlingId?.toString(),
                    "oppgave_id" to oppgaveId?.toString(),
                    "opprettet" to nå,
                    "behandlet_tidspunkt" to nå.takeIf { erBehandlet },
                ).asUpdate,
            )
        }
    }
}

/**
 * Nå-tilstanden fra tiltakshistorikk slik den lagres for sporbarhet.
 * Feltene settes eksplisitt, slik at formatet i databasen ikke endrer seg stille når domeneklassen gjør det.
 */
private data class TiltaksdeltakelseFraRegisterDbJson(
    val eksternDeltakelseId: String,
    val gjennomføringId: String?,
    val typeNavn: String,
    val typeKode: String,
    val rettPåTiltakspenger: Boolean,
    val deltakelseFraOgMed: String?,
    val deltakelseTilOgMed: String?,
    val deltakelseStatus: String,
    val deltakelseProsent: Float?,
    val antallDagerPerUke: Float?,
    val kilde: String,
    val deltidsprosentGjennomforing: Double?,
)

private fun TiltaksdeltakelseFraRegister.toDbJson() = TiltaksdeltakelseFraRegisterDbJson(
    eksternDeltakelseId = eksternDeltakelseId,
    gjennomføringId = gjennomføringId,
    typeNavn = typeNavn,
    typeKode = typeKode.name,
    rettPåTiltakspenger = rettPåTiltakspenger,
    deltakelseFraOgMed = deltakelseFraOgMed?.toString(),
    deltakelseTilOgMed = deltakelseTilOgMed?.toString(),
    deltakelseStatus = deltakelseStatus.name,
    deltakelseProsent = deltakelseProsent,
    antallDagerPerUke = antallDagerPerUke,
    kilde = kilde.name,
    deltidsprosentGjennomforing = deltidsprosentGjennomforing,
)
