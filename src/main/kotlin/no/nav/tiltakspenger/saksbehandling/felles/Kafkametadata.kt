package no.nav.tiltakspenger.saksbehandling.felles

import java.time.Instant

// TODO - denne og ConsumerRecord.tilKafkametadata() burde flyttes til libs, slik at alle consumerne kan lagre dem.

/** Hvor en melding lå på Kafka; brukes til deduplisering og offset-rekkefølge. */
data class Kafkametadata(
    val topic: String,
    val partisjon: Int,
    val offset: Long,
    /** Kafkas tidsstempel; [tidspunktstype] sier om det er satt av produsenten (CreateTime) eller av Kafka (LogAppendTime). */
    val tidspunkt: Instant,
    val tidspunktstype: String,
    val headere: Map<String, String?>,
)
