package no.nav.tiltakspenger.vedtak.routes.sak

import no.nav.tiltakspenger.saksbehandling.domene.sak.Sak
import java.time.LocalDate

/**
 * @property førsteLovligeStansdato Dersom vi ikke har vedtak vil denne være null. Hvis vi ikke har utbetalt, vil den være første dag i saksperioden. Dersom vi har utbetalt, vil den være dagen etter siste utbetalte dag.
 */
data class SakDTO(
    val saksnummer: String,
    val sakId: String,
    val fnr: String,
    val behandlingsoversikt: List<SaksoversiktDTO>,
    val meldekortoversikt: List<MeldekortoversiktDTO>,
    val førsteLovligeStansdato: LocalDate?,
)

fun Sak.toDTO() =
    SakDTO(
        saksnummer = saksnummer.verdi,
        sakId = id.toString(),
        fnr = fnr.verdi,
        behandlingsoversikt = behandlinger.toSaksoversiktDTO(),
        meldekortoversikt = meldekortBehandlinger.verdi.toMeldekortoversiktDTO(),
        førsteLovligeStansdato = førsteLovligeStansdato(),
    )
