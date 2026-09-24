package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO
import java.time.LocalDateTime

/**
 * Knytter vår interne [TiltaksdeltakerId] til iden deltakelsen har hos kilden.
 * Se [TiltaksdeltakelseIntern.internDeltakelseId] for hvorfor vi trenger en egen intern id.
 *
 * @param eksternId iden deltakelsen har hos kilden nå.
 * @param utdatertEksternId forrige eksterne id, satt når en deltakelse flyttes ut av Arena og får ny id hos den nye kilden.
 * @param sakId saken deltakeren er knyttet til.
 * @param sisteUbehandletEndringTidspunkt tidspunktet for siste hendelse som ikke er behandlet av [no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.OppdatertTiltaksdeltakelseJobb].
 * Null betyr at det ikke finnes en ubehandlet endring.
 */
data class Tiltaksdeltaker(
    val id: TiltaksdeltakerId,
    val eksternId: String,
    val tiltakstype: TiltakResponsDTO.TiltakTypeDTO,
    val utdatertEksternId: String?,
    val sakId: SakId,
    val sisteUbehandletEndringTidspunkt: LocalDateTime?,
)
