package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse

import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakerEndring

/**
 * Lagrer endringer i tiltaksdeltakelse, kun for sporbarhet til feilsøking og etterlevelse.
 * Endringene leses aldri tilbake til domeneklasser.
 * Behandling av endringer styres av markøren på tiltaksdeltakeren, ikke av det som lagres her.
 */
interface TiltaksdeltakerHendelseRepo {

    /** Lagrer en mottatt hendelse med meldingen ordrett som verdi. */
    fun lagre(
        tiltaksdeltakerHendelse: TiltaksdeltakerHendelse,
        melding: String,
        kilde: TiltaksdeltakerHendelseKilde,
        sessionContext: SessionContext,
    )

    /**
     * Lagrer en behandlet endring med nå-tilstanden fra tiltakshistorikk som verdi.
     * [nåtilstand] er null når deltakelsen mangler i tiltakshistorikken eller ikke kan leses.
     * [endring] er null når det ikke finnes noen relevant endring.
     */
    fun lagreBehandletEndring(
        tiltaksdeltakerHendelse: TiltaksdeltakerHendelse,
        nåtilstand: Tiltaksdeltakelse.GirRett?,
        endring: TiltaksdeltakerEndring?,
        behandlingId: RammebehandlingId?,
        oppgaveId: OppgaveId?,
        sessionContext: SessionContext,
    )
}
