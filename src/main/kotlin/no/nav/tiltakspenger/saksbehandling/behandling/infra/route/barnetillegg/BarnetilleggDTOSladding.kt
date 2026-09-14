package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg

import no.nav.tiltakspenger.saksbehandling.infra.route.SLADDET_TEKST

/**
 * Sladding av [BarnetilleggDTO].
 * Begrunnelsen er saksbehandlers fritekst og kan inneholde opplysninger om barna.
 * Perioder og antall barn er ikke identifiserende og beholdes.
 */

fun BarnetilleggDTO.sladdet(): BarnetilleggDTO = this.copy(
    begrunnelse = begrunnelse?.let { SLADDET_TEKST },
)
