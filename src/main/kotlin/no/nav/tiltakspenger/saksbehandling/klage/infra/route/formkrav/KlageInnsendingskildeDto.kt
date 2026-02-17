package no.nav.tiltakspenger.saksbehandling.klage.infra.route.formkrav

import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.KlageInnsendingskilde

enum class KlageInnsendingskildeDto {
    DIGITAL,
    PAPIR,
    MODIA,
    ANNET,
    ;

    fun toDomain(): KlageInnsendingskilde = when (this) {
        DIGITAL -> KlageInnsendingskilde.DIGITAL
        PAPIR -> KlageInnsendingskilde.PAPIR
        MODIA -> KlageInnsendingskilde.MODIA
        ANNET -> KlageInnsendingskilde.ANNET
    }

    companion object {
        fun KlageInnsendingskilde.toDto(): KlageInnsendingskildeDto = when (this) {
            KlageInnsendingskilde.DIGITAL -> DIGITAL
            KlageInnsendingskilde.PAPIR -> PAPIR
            KlageInnsendingskilde.MODIA -> MODIA
            KlageInnsendingskilde.ANNET -> ANNET
        }
    }
}
