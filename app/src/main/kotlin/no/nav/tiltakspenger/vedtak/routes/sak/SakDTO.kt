package no.nav.tiltakspenger.vedtak.routes.sak

import no.nav.tiltakspenger.saksbehandling.domene.sak.Sak
import no.nav.tiltakspenger.vedtak.routes.meldekort.dto.BrukersMeldekortDTO
import no.nav.tiltakspenger.vedtak.routes.meldekort.dto.MeldekortBehandlingDTO
import no.nav.tiltakspenger.vedtak.routes.meldekort.dto.MeldeperiodeDto
import no.nav.tiltakspenger.vedtak.routes.meldekort.dto.toDTO
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
    val meldeperioder: List<MeldeperiodeDto>,
    val brukersMeldekort: List<BrukersMeldekortDTO>,
    val meldekortbehandlinger: List<MeldekortBehandlingDTO>,
    val førsteLovligeStansdato: LocalDate?,
)

fun Sak.toDTO() = SakDTO(
    saksnummer = saksnummer.verdi,
    sakId = id.toString(),
    fnr = fnr.verdi,
    behandlingsoversikt = behandlinger.toSaksoversiktDTO(),
    meldekortoversikt = meldekortBehandlinger.verdi.toMeldekortoversiktDTO(),
    meldeperioder = meldeperiodeKjeder.toDTO(),
    // TODO Anders og John: Legg til brukers meldekort i saken
    brukersMeldekort = emptyList(),
    meldekortbehandlinger = meldekortBehandlinger.toDTO(
        vedtaksPeriode = this.vedtaksperiode!!,
        tiltaksnavn = this.hentTiltaksnavn()!!,
        antallDager = this.hentAntallDager()!!,
        forrigeNavkontor = { this.forrigeNavkontor(it) },
    ),
    førsteLovligeStansdato = førsteLovligeStansdato(),
)
