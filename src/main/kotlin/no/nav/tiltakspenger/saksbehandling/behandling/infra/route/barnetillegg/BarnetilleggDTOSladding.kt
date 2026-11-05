package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg

import no.nav.tiltakspenger.saksbehandling.infra.route.SladdetVerdi

/**
 * Sladding av [BarnetilleggDTO].
 * Begrunnelsen er saksbehandlers fritekst og kan inneholde opplysninger om barna.
 * Personopplysningene om barna sladdes som i søknaden.
 * Perioder, antall barn og kilde er ikke identifiserende og beholdes.
 */

fun BarnetilleggDTO.sladdet(): BarnetilleggDTO = this.copy(
    begrunnelse = SladdetVerdi,
    barn = barn?.map { it.sladdet() },
)

private fun BarnMedBarnetilleggDTO.sladdet(): BarnMedBarnetilleggDTO = this.copy(
    fnr = SladdetVerdi,
    fornavn = SladdetVerdi,
    mellomnavn = SladdetVerdi,
    etternavn = SladdetVerdi,
    fødselsdato = SladdetVerdi,
)
