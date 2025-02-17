package no.nav.tiltakspenger.vedtak.kafka.tiltaksdeltakelser.repository

import no.nav.tiltakspenger.felles.OppgaveId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.TiltakDeltakerstatus
import java.time.LocalDate

data class TiltaksdeltakerKafkaDb(
    val id: String,
    val deltakelseFraOgMed: LocalDate?,
    val deltakelseTilOgMed: LocalDate?,
    val dagerPerUke: Float?,
    val deltakelsesprosent: Float?,
    val deltakerstatus: TiltakDeltakerstatus,
    val sakId: SakId,
    val oppgaveId: OppgaveId?,
)
