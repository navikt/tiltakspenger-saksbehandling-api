package no.nav.tiltakspenger.vedtak.kafka.tiltaksdeltakelser.arena

import com.fasterxml.jackson.module.kotlin.readValue
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.TiltakDeltakerstatus
import no.nav.tiltakspenger.vedtak.kafka.tiltaksdeltakelser.repository.TiltaksdeltakerKafkaDb
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class ArenaDeltakerMapper {
    private val log = KotlinLogging.logger { }

    fun mapArenaDeltaker(
        eksternId: String,
        melding: String,
        sakId: SakId,
    ): TiltaksdeltakerKafkaDb? {
        val arenaKafkaMessage = objectMapper.readValue<ArenaKafkaMessage>(melding)
        arenaKafkaMessage.after?.let { return it.toTiltaksdeltakerKafkaDb(eksternId, sakId) }

        if (arenaKafkaMessage.opType == OperationType.D) {
            log.warn { "Deltakelse med id $eksternId er slettet fra Arena" }
            return null
        } else {
            log.error { "Deltakelse med id $eksternId er ikke slettet, men mangler likevel deltakerinfo" }
            throw IllegalArgumentException()
        }
    }

    private fun ArenaDeltakerKafka.toTiltaksdeltakerKafkaDb(eksternId: String, sakId: SakId): TiltaksdeltakerKafkaDb {
        val deltakelseFraOgMed = DATO_FRA?.asValidatedLocalDate()
        return TiltaksdeltakerKafkaDb(
            id = eksternId,
            deltakelseFraOgMed = deltakelseFraOgMed,
            deltakelseTilOgMed = DATO_TIL?.asValidatedLocalDate(),
            dagerPerUke = ANTALL_DAGER_PR_UKE,
            deltakelsesprosent = PROSENT_DELTID,
            deltakerstatus = DELTAKERSTATUSKODE.toTiltakDeltakerstatus(deltakelseFraOgMed),
            sakId = sakId,
            oppgaveId = null,
        )
    }

    private fun DeltakerStatusKodeArena.toTiltakDeltakerstatus(deltakelseFraOgMed: LocalDate?): TiltakDeltakerstatus {
        val startdatoFremITid = deltakelseFraOgMed == null || deltakelseFraOgMed.isAfter(LocalDate.now())
        return when (this) {
            DeltakerStatusKodeArena.DELAVB,
            DeltakerStatusKodeArena.GJENN_AVB,
            DeltakerStatusKodeArena.IKKEM,
            -> TiltakDeltakerstatus.Avbrutt

            DeltakerStatusKodeArena.GJENN_AVL,
            DeltakerStatusKodeArena.IKKAKTUELL,
            DeltakerStatusKodeArena.AVSLAG,
            DeltakerStatusKodeArena.NEITAKK,
            -> TiltakDeltakerstatus.IkkeAktuell

            DeltakerStatusKodeArena.GJENN,
            DeltakerStatusKodeArena.TILBUD,
            -> if (startdatoFremITid) TiltakDeltakerstatus.VenterPåOppstart else TiltakDeltakerstatus.Deltar

            DeltakerStatusKodeArena.VENTELISTE,
            DeltakerStatusKodeArena.INFOMOETE,
            -> TiltakDeltakerstatus.Venteliste

            DeltakerStatusKodeArena.AKTUELL -> TiltakDeltakerstatus.SøktInn
            DeltakerStatusKodeArena.JATAKK -> TiltakDeltakerstatus.Deltar
            DeltakerStatusKodeArena.FULLF -> TiltakDeltakerstatus.Fullført
            DeltakerStatusKodeArena.FEILREG -> TiltakDeltakerstatus.Feilregistrert
        }
    }

    private fun String.asValidatedLocalDate(): LocalDate {
        try {
            return this.asLocalDate()
        } catch (e: DateTimeParseException) {
            throw IllegalArgumentException("$this kan ikke parses til LocalDate")
        }
    }

    private fun String.asLocalDate(): LocalDate {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        return LocalDate.parse(this, formatter)
    }
}
