package no.nav.tiltakspenger.saksbehandling.behandling.domene

@JvmInline
value class AntallDagerForMeldeperiode(val value: Int) {
    init {
        // TODO: Dersom du ønsker å bruke denne for stans eller tidslinje, så kan du endre den til >= 0
        require(value > 0 && value <= 14)
    }
}
