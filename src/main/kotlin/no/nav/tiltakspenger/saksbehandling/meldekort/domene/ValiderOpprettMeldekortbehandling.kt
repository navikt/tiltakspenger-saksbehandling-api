package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.AUTOMATISK_BEHANDLET
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.GODKJENT
import no.nav.tiltakspenger.saksbehandling.sak.Sak

fun Sak.validerOpprettMeldekortBehandling(kjedeId: MeldeperiodeKjedeId): Either<KanIkkeOppretteMeldekortbehandling, Unit> {
    val meldeperiodekjede = this.meldeperiodeKjeder.hentMeldeperiodekjedeForKjedeId(kjedeId)!!
    val meldeperiode = meldeperiodekjede.hentSisteMeldeperiode()

    val åpenBehandling = this.meldekortbehandlinger.åpenMeldekortBehandling

    if (åpenBehandling != null && åpenBehandling.kjedeId != kjedeId) {
        return KanIkkeOppretteMeldekortbehandling.HAR_ÅPEN_BEHANDLING.left()
    }

    if (this.meldekortbehandlinger.isEmpty() &&
        meldeperiode != this.meldeperiodeKjeder.first()
            .hentSisteMeldeperiode()
    ) {
        return KanIkkeOppretteMeldekortbehandling.MÅ_BEHANDLE_FØRSTE_KJEDE.left()
    }

    this.meldeperiodeKjeder.hentForegåendeMeldeperiodekjedeMedRett(kjedeId)?.also { foregåendeMeldeperiodekjede ->
        this.meldekortbehandlinger.hentMeldekortBehandlingerForKjede(foregåendeMeldeperiodekjede.kjedeId)
            .also { behandlinger ->
                if (behandlinger.none { it.status == GODKJENT || it.status == AUTOMATISK_BEHANDLET }) {
                    return KanIkkeOppretteMeldekortbehandling.MÅ_BEHANDLE_NESTE_KJEDE.left()
                }
            }
    }

    if (meldeperiode.ingenDagerGirRett) {
        return KanIkkeOppretteMeldekortbehandling.INGEN_DAGER_GIR_RETT.left()
    }

    return Unit.right()
}

enum class KanIkkeOppretteMeldekortbehandling {
    HAR_ÅPEN_BEHANDLING,
    MÅ_BEHANDLE_FØRSTE_KJEDE,
    MÅ_BEHANDLE_NESTE_KJEDE,
    INGEN_DAGER_GIR_RETT,
}
