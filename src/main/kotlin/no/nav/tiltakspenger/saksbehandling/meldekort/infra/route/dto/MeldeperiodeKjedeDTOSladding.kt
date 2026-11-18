package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.sladdet

/**
 * Sladding av [MeldeperiodeKjedeDTO].
 * Begrunnelsen for prioriteringen sladdes på linje med prioriteringen i benken og på behandlingene, men det at kjeden er prioritert beholdes.
 * Meldeperiodene, beregningene og statusene er ikke identifiserende og beholdes.
 */
fun MeldeperiodeKjedeDTO.sladdet(): MeldeperiodeKjedeDTO = this.copy(
    prioritering = prioritering?.sladdet(),
)
