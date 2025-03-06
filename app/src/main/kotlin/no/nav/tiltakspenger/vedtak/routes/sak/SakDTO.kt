package no.nav.tiltakspenger.vedtak.routes.sak

import no.nav.tiltakspenger.saksbehandling.domene.sak.Sak
import no.nav.tiltakspenger.vedtak.routes.meldekort.dto.MeldeperiodeKjedeDTO
import no.nav.tiltakspenger.vedtak.routes.meldekort.dto.toMeldeperiodeKjederDTO
import java.time.LocalDate

/**
 * @property førsteLovligeStansdato Dersom vi ikke har vedtak vil denne være null. Hvis vi ikke har utbetalt, vil den være første dag i saksperioden. Dersom vi har utbetalt, vil den være dagen etter siste utbetalte dag.
 */
data class SakDTO(
    val saksnummer: String,
    val sakId: String,
    val fnr: String,
    val behandlingsoversikt: List<SaksoversiktDTO>,
    val meldeperiodeKjeder: List<MeldeperiodeKjedeDTO>,
    val førsteLovligeStansdato: LocalDate?,
    val sisteDagSomGirRett: LocalDate?,
)

fun Sak.toDTO() = SakDTO(
    saksnummer = saksnummer.verdi,
    sakId = id.toString(),
    fnr = fnr.verdi,
    behandlingsoversikt = (
        behandlinger.toSaksoversiktDTO() +
            this.soknader.filter { soknad -> behandlinger.none { it.søknad?.id == soknad.id } }
                .toSaksoversiktDTO()
        ).sortedBy { it.opprettet },
    meldeperiodeKjeder = toMeldeperiodeKjederDTO(),
    førsteLovligeStansdato = førsteLovligeStansdato(),
    sisteDagSomGirRett = sisteDagSomGirRett,
)
