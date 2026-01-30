package no.nav.tiltakspenger.saksbehandling.klage.infra.repo

import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus

private enum class KlagebehandlingsstatusDbEnum {
    KLAR_TIL_BEHANDLING,
    UNDER_BEHANDLING,
    AVBRUTT,

    // TODO jah: Lag en migrering fra IVERKSATT TIL VEDTATT samtidig som du endrer denne.
    IVERKSATT,
    ;

    fun toDomain(): Klagebehandlingsstatus {
        return when (this) {
            KLAR_TIL_BEHANDLING -> Klagebehandlingsstatus.KLAR_TIL_BEHANDLING
            UNDER_BEHANDLING -> Klagebehandlingsstatus.UNDER_BEHANDLING
            AVBRUTT -> Klagebehandlingsstatus.AVBRUTT
            IVERKSATT -> Klagebehandlingsstatus.VEDTATT
        }
    }
}

fun Klagebehandlingsstatus.toDbEnum(): String {
    return when (this) {
        Klagebehandlingsstatus.KLAR_TIL_BEHANDLING -> KlagebehandlingsstatusDbEnum.KLAR_TIL_BEHANDLING
        Klagebehandlingsstatus.UNDER_BEHANDLING -> KlagebehandlingsstatusDbEnum.UNDER_BEHANDLING
        Klagebehandlingsstatus.AVBRUTT -> KlagebehandlingsstatusDbEnum.AVBRUTT
        Klagebehandlingsstatus.VEDTATT -> KlagebehandlingsstatusDbEnum.IVERKSATT
    }.name
}

fun String.toKlagebehandlingsstatus(): Klagebehandlingsstatus {
    return KlagebehandlingsstatusDbEnum.valueOf(this).toDomain()
}
