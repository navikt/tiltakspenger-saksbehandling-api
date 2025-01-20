package no.nav.tiltakspenger.vedtak.routes.sak

import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandlingStatus
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.saksbehandling.domene.sak.Sak
import no.nav.tiltakspenger.vedtak.routes.dto.PeriodeDTO
import no.nav.tiltakspenger.vedtak.routes.dto.toDTO
import no.nav.tiltakspenger.vedtak.routes.meldekort.dto.MeldekortstatusDTO
import no.nav.tiltakspenger.vedtak.routes.meldekort.dto.toMeldekortstatusDTO

data class MeldekortoversiktDTO(
    val meldekortId: String,
    val periode: PeriodeDTO,
    val status: MeldekortstatusDTO,
    val saksbehandler: String?,
    val beslutter: String?,
)

enum class MeldeperiodeStatusDTO {
    IKKE_RETT_TIL_TILTAKSPENGER,
    IKKE_KLAR_TIL_UTFYLLING,
    VENTER_PÅ_UTFYLLING,
    KLAR_TIL_BEHANDLING,
    KLAR_TIL_BESLUTNING,
    GODKJENT,
}

data class MeldeperiodeSammendragDTO(
    val meldeperiodeId: String,
    val hendelseId: String,
    val hendelseVersjon: Int,
    val periode: PeriodeDTO,
    val saksbehandler: String?,
    val beslutter: String?,
    val status: MeldeperiodeStatusDTO,
)

fun Sak.toMeldeperiodeoversiktDTO(): List<MeldeperiodeSammendragDTO> {
    return this.meldeperiodeKjeder.meldeperioder.map { meldeperiode ->
        val meldekortBehandling = this.meldekortBehandlinger.find { meldekortBehandling ->
            meldekortBehandling.meldeperiode.hendelseId == meldeperiode.hendelseId
        }
        val brukersMeldekort = this.brukersMeldekort.findLast { brukersMeldekort ->
            brukersMeldekort.meldeperiode.hendelseId == meldeperiode.hendelseId
        }

        MeldeperiodeSammendragDTO(
            meldeperiodeId = meldeperiode.id.toString(),
            hendelseId = meldeperiode.hendelseId.toString(),
            hendelseVersjon = meldeperiode.versjon.value,
            periode = meldeperiode.periode.toDTO(),
            saksbehandler = meldekortBehandling?.saksbehandler,
            beslutter = meldekortBehandling?.beslutter,
            status = when (meldekortBehandling?.status) {
                MeldekortBehandlingStatus.GODKJENT -> MeldeperiodeStatusDTO.GODKJENT
                MeldekortBehandlingStatus.KLAR_TIL_BESLUTNING -> MeldeperiodeStatusDTO.KLAR_TIL_BESLUTNING
                MeldekortBehandlingStatus.IKKE_BEHANDLET -> MeldeperiodeStatusDTO.KLAR_TIL_BEHANDLING
                MeldekortBehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER -> MeldeperiodeStatusDTO.IKKE_RETT_TIL_TILTAKSPENGER
                null -> when {
                    meldeperiode.periode.fraOgMed > nå().toLocalDate() -> MeldeperiodeStatusDTO.IKKE_KLAR_TIL_UTFYLLING
                    brukersMeldekort == null -> MeldeperiodeStatusDTO.VENTER_PÅ_UTFYLLING
                    else -> MeldeperiodeStatusDTO.KLAR_TIL_BEHANDLING
                }
            },
        )
    }
}

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
