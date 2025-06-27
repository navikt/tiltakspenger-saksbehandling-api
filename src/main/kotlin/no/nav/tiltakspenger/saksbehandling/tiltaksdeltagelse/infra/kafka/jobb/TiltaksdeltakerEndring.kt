package no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.kafka.jobb

enum class TiltaksdeltakerEndring {
    FORLENGELSE,
    AVBRUTT_DELTAKELSE,
    ENDRET_SLUTTDATO,
    ENDRET_STARTDATO,
    ENDRET_DELTAKELSESMENGDE,
    ENDRET_STATUS,
}
