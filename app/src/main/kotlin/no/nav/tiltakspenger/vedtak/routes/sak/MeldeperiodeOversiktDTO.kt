package no.nav.tiltakspenger.vedtak.routes.sak

import no.nav.tiltakspenger.saksbehandling.domene.sak.Sak
import no.nav.tiltakspenger.vedtak.routes.dto.PeriodeDTO
import no.nav.tiltakspenger.vedtak.routes.dto.toDTO
import no.nav.tiltakspenger.vedtak.routes.meldekort.dto.MeldeperiodeStatusDTO
import no.nav.tiltakspenger.vedtak.routes.meldekort.dto.toMeldeperiodeStatusDTO

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

        MeldeperiodeSammendragDTO(
            meldeperiodeId = meldeperiode.id.toString(),
            hendelseId = meldeperiode.hendelseId.toString(),
            hendelseVersjon = meldeperiode.versjon.value,
            periode = meldeperiode.periode.toDTO(),
            saksbehandler = meldekortBehandling?.saksbehandler,
            beslutter = meldekortBehandling?.beslutter,
            status = toMeldeperiodeStatusDTO(meldeperiode),
        )
    }
}
