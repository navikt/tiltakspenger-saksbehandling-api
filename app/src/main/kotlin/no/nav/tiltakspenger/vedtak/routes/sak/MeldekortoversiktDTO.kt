package no.nav.tiltakspenger.vedtak.routes.sak

import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.saksbehandling.domene.sak.Sak
import no.nav.tiltakspenger.vedtak.routes.meldekort.dto.MeldekortstatusDTO
import no.nav.tiltakspenger.vedtak.routes.meldekort.dto.toMeldekortstatusDTO

data class MeldekortoversiktDTO(
    val meldekortId: String,
    val periode: PeriodeDTO,
    val status: MeldekortstatusDTO,
    val saksbehandler: String?,
    val beslutter: String?,
)

fun Sak.toMeldekortoversiktDTO(): List<MeldekortoversiktDTO> {
    return this.meldeperiodeKjeder.meldeperioder
        .map { meldeperiode ->
            this.meldekortBehandlinger.hentMeldekortForKjedeId(meldeperiode.id)?.let { return@map it.toOversiktDTO() }
            this.brukersMeldekort.find { it.meldeperiodeId == meldeperiode.id }?.let { return@map it.toOversiktDTO() }
            meldeperiode.toOversiktDTO()
        }
}

fun MeldekortBehandling.toOversiktDTO(): MeldekortoversiktDTO {
    return MeldekortoversiktDTO(
        meldekortId = id.toString(),
        periode = periode.toDTO(),
        status = this.toMeldekortstatusDTO(),
        saksbehandler = saksbehandler,
        beslutter = beslutter,
    )
}

fun BrukersMeldekort.toOversiktDTO(): MeldekortoversiktDTO {
    return MeldekortoversiktDTO(
        meldekortId = id.toString(),
        periode = periode.toDTO(),
        status = MeldekortstatusDTO.UTFYLT,
        saksbehandler = null,
        beslutter = null,
    )
}

fun Meldeperiode.toOversiktDTO(): MeldekortoversiktDTO {
    return MeldekortoversiktDTO(
        meldekortId = id.toString(),
        periode = periode.toDTO(),
        // Kommentar jah: Dersom vi genererer flere meldeperioder enn de som kan utfylles, så må meldeperioden ta stilling til om den kan fylles ut eller ikke.
        status = MeldekortstatusDTO.KLAR_TIL_UTFYLLING,
        saksbehandler = null,
        beslutter = null,
    )
}
