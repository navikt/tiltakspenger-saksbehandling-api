package no.nav.tiltakspenger.vedtak.routes.meldekort.dto

import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.saksbehandling.domene.sak.Sak
import no.nav.tiltakspenger.vedtak.routes.dto.PeriodeDTO

data class MeldeperiodeKjedeDTO(
    val meldeperiodeId: String,
    val rammevedtakId: String,
    val periode: PeriodeDTO,
    val tiltaksnavn: TiltakstypeSomGirRettDTO,
    val vedtaksPeriode: PeriodeDTO,
    val meldeperioder: List<MeldeperiodeDTO>,
)

fun Sak.toMeldeperiodeKjedeDTO(meldeperiodeId: MeldeperiodeId): MeldeperiodeDTO {
    TODO()
}
