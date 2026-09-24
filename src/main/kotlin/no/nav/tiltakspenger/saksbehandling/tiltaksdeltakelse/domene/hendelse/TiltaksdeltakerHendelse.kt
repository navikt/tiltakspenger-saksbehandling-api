package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import java.time.LocalDate

/**
 *  [id] Vår interne id for hendelsen
 *  [internDeltakerId] Vår interne id for deltakelsen
 *  [eksternDeltakerId] Id for deltakelsen fra arena/tiltak/komet
 * */
data class TiltaksdeltakerHendelse(
    val id: TiltaksdeltakerHendelseId,
    val internDeltakerId: TiltaksdeltakerId,
    val eksternDeltakerId: String,
    val deltakelseFraOgMed: LocalDate?,
    val deltakelseTilOgMed: LocalDate?,
    val dagerPerUke: Float?,
    val deltakelsesprosent: Float?,
    val deltakerstatus: TiltakDeltakerstatus,
    val sakId: SakId,
)
