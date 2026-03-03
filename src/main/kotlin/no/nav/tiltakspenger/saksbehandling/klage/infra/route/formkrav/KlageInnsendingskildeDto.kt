package no.nav.tiltakspenger.saksbehandling.klage.infra.route.formkrav

import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.KlageInnsendingskilde

enum class KlageInnsendingskildeDto {
    DIGITAL,
    PAPIR_SKJEMA,
    PAPIR_FRIHAND,
    MODIA,
    ANNET,
    ;

    fun toDomain(): KlageInnsendingskilde = when (this) {
        DIGITAL -> KlageInnsendingskilde.DIGITAL
        PAPIR_SKJEMA -> KlageInnsendingskilde.PAPIR_SKJEMA
        PAPIR_FRIHAND -> KlageInnsendingskilde.PAPIR_FRIHAND
        MODIA -> KlageInnsendingskilde.MODIA
        ANNET -> KlageInnsendingskilde.ANNET
    }

    companion object {
        fun KlageInnsendingskilde.toDto(): KlageInnsendingskildeDto = when (this) {
            KlageInnsendingskilde.DIGITAL -> DIGITAL
            KlageInnsendingskilde.PAPIR_SKJEMA -> PAPIR_SKJEMA
            KlageInnsendingskilde.PAPIR_FRIHAND -> PAPIR_FRIHAND
            KlageInnsendingskilde.MODIA -> MODIA
            KlageInnsendingskilde.ANNET -> ANNET
        }
    }
}
