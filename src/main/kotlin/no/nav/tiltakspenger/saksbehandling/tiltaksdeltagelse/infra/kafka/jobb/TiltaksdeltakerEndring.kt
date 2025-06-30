package no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.kafka.jobb

enum class TiltaksdeltakerEndring(val beskrivelse: String) {
    FORLENGELSE("Deltakelsen har blitt forlenget"),
    AVBRUTT_DELTAKELSE("Deltakelsen er avbrutt"),
    ENDRET_SLUTTDATO("Endret sluttdato"),
    ENDRET_STARTDATO("Endret startdato"),
    ENDRET_DELTAKELSESMENGDE("Endret deltakelsesmengde"),
    ENDRET_STATUS("Endret status"),
}

fun List<TiltaksdeltakerEndring>.getOppgaveTilleggstekst(): String? {
    if (this.isEmpty()) {
        return null
    }
    if (this.size == 1) {
        return "${this.first().beskrivelse}."
    }

    return this.joinToString("\n") { "- ${it.beskrivelse}" }
}
