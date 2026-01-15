package no.nav.tiltakspenger.saksbehandling.klage.infra.repo

import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus

enum class KlagebehandlingsstatusDbEnum {
    KLAR_TIL_BEHANDLING,
    UNDER_BEHANDLING,
    AVBRUTT,
    ;

    fun toDomain(): Klagebehandlingsstatus {
        return when (this) {
            KLAR_TIL_BEHANDLING -> Klagebehandlingsstatus.KLAR_TIL_BEHANDLING
            UNDER_BEHANDLING -> Klagebehandlingsstatus.UNDER_BEHANDLING
            AVBRUTT -> Klagebehandlingsstatus.AVBRUTT
        }
    }
}

fun Klagebehandlingsstatus.toDbEnum(): KlagebehandlingsstatusDbEnum {
    return when (this) {
        Klagebehandlingsstatus.KLAR_TIL_BEHANDLING -> KlagebehandlingsstatusDbEnum.KLAR_TIL_BEHANDLING
        Klagebehandlingsstatus.UNDER_BEHANDLING -> KlagebehandlingsstatusDbEnum.UNDER_BEHANDLING
        Klagebehandlingsstatus.AVBRUTT -> KlagebehandlingsstatusDbEnum.AVBRUTT
    }
}

fun String.toKlagebehandlingsstatus(): Klagebehandlingsstatus {
    return KlagebehandlingsstatusDbEnum.valueOf(this).toDomain()
}
