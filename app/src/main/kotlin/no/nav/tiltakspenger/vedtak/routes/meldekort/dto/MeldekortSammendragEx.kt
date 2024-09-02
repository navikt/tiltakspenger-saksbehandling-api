package no.nav.tiltakspenger.vedtak.routes.meldekort.dto

import no.nav.tiltakspenger.meldekort.domene.MeldekortSammendrag
import no.nav.tiltakspenger.vedtak.db.serialize
import no.nav.tiltakspenger.vedtak.routes.dto.PeriodeDTO
import no.nav.tiltakspenger.vedtak.routes.dto.toDTO

private data class MeldekortSammendragDTO(
    val meldekortId: String,
    val periode: PeriodeDTO,
    val erUtfylt: Boolean,
)

fun List<MeldekortSammendrag>.toDTO(): String = serialize(map { it.toDTO() })

private fun MeldekortSammendrag.toDTO(): MeldekortSammendragDTO =
    MeldekortSammendragDTO(
        meldekortId = meldekortId.toString(),
        periode = periode.toDTO(),
        erUtfylt = erUtfylt,
    )