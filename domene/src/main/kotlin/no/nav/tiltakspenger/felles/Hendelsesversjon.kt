package no.nav.tiltakspenger.felles

@Suppress("MemberVisibilityCanBePrivate")
@JvmInline
value class Hendelsesversjon(val value: Long) : Comparable<Hendelsesversjon> {

    init {
        require(value > 0L) { "Versjonen må være større enn 0L" }
    }

    override fun compareTo(other: Hendelsesversjon): Int {
        return this.value.compareTo(other.value)
    }

    operator fun inc() = Hendelsesversjon(this.value + 1)
    operator fun inc(value: Int) = Hendelsesversjon(this.value + value)

    override fun toString() = value.toString()

    companion object {
        /**
         * [ny] er ment å brukes direkte.
         * Det vil si at man skal opprette en ny versjon, for å så gjøre en [inc] på den nye hendelsen
         */
        fun ny(): Hendelsesversjon = Hendelsesversjon(1)

        fun max(first: Hendelsesversjon, second: Hendelsesversjon): Hendelsesversjon =
            if (first > second) first else second

        fun max(first: Hendelsesversjon?, second: Hendelsesversjon): Hendelsesversjon = if (first == null) second else max(first, second)

        fun max(first: Hendelsesversjon, second: Hendelsesversjon?): Hendelsesversjon = if (second == null) first else max(first, second)
    }
}
