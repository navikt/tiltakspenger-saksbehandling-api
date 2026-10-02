package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonUnwrapped
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse.TiltaksdeltakerHendelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse.TiltaksdeltakerHendelseKilde
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http.TiltaksdeltakelseFraRegister
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakerEndring
import java.time.Clock

/**
 * Lagrer endringer i tiltaksdeltakelse i `tiltaksdeltaker_endring`, kun for sporbarhet til feilsøking og etterlevelse.
 * Radene leses aldri tilbake til domeneklasser.
 * Behandling av endringer styres av markøren på tiltaksdeltakeren, ikke av radene her.
 */
// TODO: Klassen står i whitelisten til RepoKonvensjonKonsistTest fordi den ikke har et `Repo`-grensesnitt.
//  Unntaket er ikke målet: et repo som nås fra en service skal nås gjennom en port i domenet.
//  Innfør en port for lagring av endringshistorikken i domenet for å fjerne unntaket.
class TiltaksdeltakerHendelsePostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
    private val clock: Clock,
) {

    /** Lagrer en mottatt hendelse med meldingen ordrett som verdi. */
    fun lagre(
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

    /**
     * Lagrer en behandlet endring med nå-tilstanden fra tiltakshistorikk som verdi.
     * [nåtilstand] er null når deltakelsen mangler i tiltakshistorikken eller ikke kan leses.
     * [endring] er null når det ikke finnes noen relevant endring.
     */
    fun lagreBehandletEndring(
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
            verdi = nåtilstand?.let { serialize(TiltaksdeltakelseFraRegisterDbJson(it)) },
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
 * Verdien serialiseres slik den er, og formatet følger klassen — det er greit at det endrer seg over tid.
 * Avledede getter-verdier fra TiltaksdeltakelseLegacy er ikke data, og Periode kan ikke serialiseres.
 */
private class TiltaksdeltakelseFraRegisterDbJson(
    @get:JsonUnwrapped
    @get:JsonIgnoreProperties("kanInnvilges", "periode")
    val verdi: TiltaksdeltakelseFraRegister,
)
