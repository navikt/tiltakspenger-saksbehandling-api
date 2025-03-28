package no.nav.tiltakspenger.saksbehandling.felles

import no.nav.tiltakspenger.libs.common.NonBlankString
import java.time.LocalDateTime

data class Attestering(
    val id: AttesteringId = AttesteringId.random(),
    val status: Attesteringsstatus,
    val begrunnelse: NonBlankString?,
    val beslutter: String,
    val tidspunkt: LocalDateTime,
) {
    fun isGodkjent() = status == Attesteringsstatus.GODKJENT

    fun isUnderkjent() = status == Attesteringsstatus.SENDT_TILBAKE
}

enum class Attesteringsstatus {
    GODKJENT,
    SENDT_TILBAKE,
}

// TODO - test
data class Attesteringer(
    private val attesteringer: List<Attestering>,
) : List<Attestering> by attesteringer {
    fun erGodkjent(): Boolean = this.lastOrNull()?.isGodkjent() ?: false
    fun erUnderkjent(): Boolean = this.lastOrNull()?.isUnderkjent() ?: false

    fun leggTil(attestering: Attestering): Attesteringer {
        require(this.lastOrNull()?.tidspunkt?.isBefore(attestering.tidspunkt) ?: true) {
            "Den nye attesteringen må være etter den siste attesteringen"
        }

        return Attesteringer(attesteringer + attestering)
    }

    init {
        require(attesteringer.sortedBy { it.tidspunkt } == attesteringer) {
            "Attesteringer må være sortert etter tidspunkt"
        }
        require(attesteringer.none { it.isGodkjent() } || attesteringer.filter { it.isGodkjent() }.size == 1) {
            "Kan ikke ha flere enn en godkjent attestering"
        }
    }

    companion object {
        fun empty(): Attesteringer = Attesteringer(emptyList())
    }
}

fun List<Attestering>.toAttesteringer(): Attesteringer = Attesteringer(this.sortedBy { it.tidspunkt })
