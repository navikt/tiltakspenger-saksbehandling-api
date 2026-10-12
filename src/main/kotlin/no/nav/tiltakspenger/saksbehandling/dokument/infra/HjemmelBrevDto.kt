package no.nav.tiltakspenger.saksbehandling.dokument.infra

import no.nav.tiltakspenger.saksbehandling.behandling.domene.Hjemmel
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rettskilde

/**
 * Én hjemmel slik brevene i tiltakspenger-pdfgenrs kjenner den.
 * Pdfgenrs slår sammen, sorterer og fjerner duplikater selv.
 */
data class HjemmelBrevDto(
    val rettskilde: RettskildeBrevDto,
    val paragraf: String,
    val ledd: Int?,
)

/** Kodene er kontrakten mot tiltakspenger-pdfgenrs, og navnene må ikke endres uten at malen endres samtidig. */
enum class RettskildeBrevDto {
    ARBEIDSMARKEDSLOVEN,
    FOLKETRYGDLOVEN,
    FORELDELSESLOVEN,
    FORVALTNINGSLOVEN,
    TILTAKSPENGEFORSKRIFTEN,
}

fun Hjemmel.tilHjemmelBrevDto(): HjemmelBrevDto = HjemmelBrevDto(
    rettskilde = rettskilde.tilRettskildeBrevDto(),
    paragraf = paragraf.verdi,
    ledd = ledd?.nummer,
)

fun Rettskilde.tilRettskildeBrevDto(): RettskildeBrevDto = when (this) {
    Rettskilde.Arbeidsmarkedsloven -> RettskildeBrevDto.ARBEIDSMARKEDSLOVEN
    Rettskilde.Folketrygdloven -> RettskildeBrevDto.FOLKETRYGDLOVEN
    Rettskilde.Foreldelsesloven -> RettskildeBrevDto.FORELDELSESLOVEN
    Rettskilde.Forvaltningsloven -> RettskildeBrevDto.FORVALTNINGSLOVEN
    Rettskilde.Tiltakspengeforskriften -> RettskildeBrevDto.TILTAKSPENGEFORSKRIFTEN
}
