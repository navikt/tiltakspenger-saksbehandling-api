package no.nav.tiltakspenger.saksbehandling.meldekort.ports

import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.felles.Forsøkshistorikk
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandletAutomatiskStatus

interface BrukersMeldekortRepo {
    fun lagre(
        brukersMeldekort: BrukersMeldekort,
        sessionContext: SessionContext? = null,
    )

    fun hentForSakId(
        sakId: SakId,
        sessionContext: SessionContext? = null,
    ): List<BrukersMeldekort>

    fun hentForMeldekortId(
        meldekortId: MeldekortId,
        sessionContext: SessionContext? = null,
    ): BrukersMeldekort?

    fun hentForMeldeperiodeId(
        meldeperiodeId: MeldeperiodeId,
        sessionContext: SessionContext? = null,
    ): List<BrukersMeldekort>

    fun hentForKjedeId(
        kjedeId: MeldeperiodeKjedeId,
        sakId: SakId,
        sessionContext: SessionContext? = null,
    ): List<BrukersMeldekort>

    fun hentMeldekortSomSkalBehandlesAutomatisk(
        sessionContext: SessionContext? = null,
    ): List<BrukersMeldekort>

    fun oppdaterAutomatiskBehandletStatus(
        meldekortId: MeldekortId,
        status: MeldekortBehandletAutomatiskStatus,
        behandlesAutomatisk: Boolean,
        metadata: Forsøkshistorikk,
        sessionContext: SessionContext? = null,
    )
}
