package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse

import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO

/**
 * Knytter vår interne [TiltaksdeltakerId] til iden deltakelsen har hos kilden.
 * Se [Tiltaksdeltakelse.internDeltakelseId] for hvorfor vi trenger en egen intern id.
 *
 * @param eksternId iden deltakelsen har hos kilden nå.
 * @param utdatertEksternId forrige eksterne id, satt når en deltakelse flyttes ut av Arena og får ny id hos den nye kilden.
 */
data class Tiltaksdeltaker(
    val id: TiltaksdeltakerId,
    val eksternId: String,
    val tiltakstype: TiltakResponsDTO.TiltakTypeDTO,
    val utdatertEksternId: String?,
)
