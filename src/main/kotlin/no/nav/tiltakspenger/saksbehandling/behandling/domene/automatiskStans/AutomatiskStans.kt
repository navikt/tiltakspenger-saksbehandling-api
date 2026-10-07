package no.nav.tiltakspenger.saksbehandling.behandling.domene.automatiskStans

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForStans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdater.OppdaterRevurderingKommando.Stans.ValgtStansFraOgMed
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.felles.Loggbar
import no.nav.tiltakspenger.saksbehandling.felles.Loggkontekst
import no.nav.tiltakspenger.saksbehandling.sak.Sak

/**
 * Det systemet har utledet for å kunne fylle ut en automatisk opprettet stans-revurdering uten saksbehandler.
 */
data class AutomatiskStans(
    val hjemmel: HjemmelForStans,
    val stansFraOgMed: ValgtStansFraOgMed,
    val begrunnelse: Begrunnelse,
)

sealed interface KanIkkeStanseAutomatisk : Loggbar {
    data object ManglerUtfylling : KanIkkeStanseAutomatisk {
        override val loggkontekst = Loggkontekst("verdiene for å fylle ut stansen kunne ikke utledes med stor nok sikkerhet")
    }

    data object ErIkkeAutomatiskOpprettetStans : KanIkkeStanseAutomatisk {
        override val loggkontekst = Loggkontekst("revurderingen er ikke en automatisk opprettet stans")
    }

    data class BehandlingenErIkkeKlarTilAutomatiskBehandling(val status: Rammebehandlingsstatus) : KanIkkeStanseAutomatisk {
        override val loggkontekst get() = Loggkontekst("revurderingen har status $status, er tildelt en saksbehandler, står på vent eller er knyttet til en klage")
    }

    data object HarAndreÅpneBehandlinger : KanIkkeStanseAutomatisk {
        override val loggkontekst = Loggkontekst("saken har andre åpne behandlinger")
    }

    /**
     * Vurderingen gikk gjennom, men et av stegene for å ta, fylle ut eller sende revurderingen til beslutning feilet.
     * [årsak] skal være PII-fri, f.eks. klassenavnet til den underliggende feilen.
     */
    data class FeilVedAutomatiskBehandling(val steg: String, val årsak: String) : KanIkkeStanseAutomatisk {
        override val loggkontekst get() = Loggkontekst("feil ved $steg: $årsak")
    }
}

/**
 * Sjekker at revurderingen er en automatisk opprettet stans som fortsatt kan fylles ut uten saksbehandler.
 * Verdiene for utfyllingen utledes når stansen opprettes, så her sjekkes kun at ingenting har endret seg på saken siden.
 */
fun Sak.kanStanseAutomatisk(revurdering: Revurdering): Either<KanIkkeStanseAutomatisk, Unit> {
    if (revurdering.resultat !is Revurderingsresultat.Stans || revurdering.automatiskOpprettetGrunn == null) {
        return KanIkkeStanseAutomatisk.ErIkkeAutomatiskOpprettetStans.left()
    }

    if (revurdering.status != Rammebehandlingsstatus.KLAR_TIL_BEHANDLING ||
        revurdering.saksbehandler != null ||
        revurdering.ventestatus.erSattPåVent ||
        revurdering.klagebehandling != null
    ) {
        return KanIkkeStanseAutomatisk.BehandlingenErIkkeKlarTilAutomatiskBehandling(revurdering.status).left()
    }

    if (rammebehandlinger.åpneBehandlinger.any { it.id != revurdering.id }) {
        return KanIkkeStanseAutomatisk.HarAndreÅpneBehandlinger.left()
    }

    return Unit.right()
}
