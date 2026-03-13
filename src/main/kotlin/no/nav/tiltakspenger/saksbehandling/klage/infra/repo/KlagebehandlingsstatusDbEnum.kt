package no.nav.tiltakspenger.saksbehandling.klage.infra.repo

import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus

private enum class KlagebehandlingsstatusDbEnum {
    KLAR_TIL_BEHANDLING,
    UNDER_BEHANDLING,
    AVBRUTT,
    VEDTATT,
    OPPRETTHOLDT,
    OVERSENDT,
    FERDIGSTILT,
    MOTTATT_FRA_KLAGEINSTANS,
    OMGJØRING_ETTER_KLAGEINSTANS,
    ;

    fun toDomain(): Klagebehandlingsstatus {
        return when (this) {
            KLAR_TIL_BEHANDLING -> Klagebehandlingsstatus.KLAR_TIL_BEHANDLING
            UNDER_BEHANDLING -> Klagebehandlingsstatus.UNDER_BEHANDLING
            AVBRUTT -> Klagebehandlingsstatus.AVBRUTT
            VEDTATT -> Klagebehandlingsstatus.VEDTATT
            OPPRETTHOLDT -> Klagebehandlingsstatus.OPPRETTHOLDT
            OVERSENDT -> Klagebehandlingsstatus.OVERSENDT
            FERDIGSTILT -> Klagebehandlingsstatus.FERDIGSTILT
            MOTTATT_FRA_KLAGEINSTANS -> Klagebehandlingsstatus.MOTTATT_FRA_KLAGEINSTANS
            OMGJØRING_ETTER_KLAGEINSTANS -> Klagebehandlingsstatus.OMGJØRING_ETTER_KLAGEINSTANS
        }
    }
}

fun Klagebehandlingsstatus.toDbEnum(): String {
    return when (this) {
        Klagebehandlingsstatus.KLAR_TIL_BEHANDLING -> KlagebehandlingsstatusDbEnum.KLAR_TIL_BEHANDLING
        Klagebehandlingsstatus.UNDER_BEHANDLING -> KlagebehandlingsstatusDbEnum.UNDER_BEHANDLING
        Klagebehandlingsstatus.AVBRUTT -> KlagebehandlingsstatusDbEnum.AVBRUTT
        Klagebehandlingsstatus.VEDTATT -> KlagebehandlingsstatusDbEnum.VEDTATT
        Klagebehandlingsstatus.OVERSENDT -> KlagebehandlingsstatusDbEnum.OVERSENDT
        Klagebehandlingsstatus.OPPRETTHOLDT -> KlagebehandlingsstatusDbEnum.OPPRETTHOLDT
        Klagebehandlingsstatus.MOTTATT_FRA_KLAGEINSTANS -> KlagebehandlingsstatusDbEnum.MOTTATT_FRA_KLAGEINSTANS
        Klagebehandlingsstatus.OMGJØRING_ETTER_KLAGEINSTANS -> KlagebehandlingsstatusDbEnum.OMGJØRING_ETTER_KLAGEINSTANS
        Klagebehandlingsstatus.FERDIGSTILT -> KlagebehandlingsstatusDbEnum.FERDIGSTILT
    }.name
}

fun String.toKlagebehandlingsstatus(): Klagebehandlingsstatus {
    return KlagebehandlingsstatusDbEnum.valueOf(this).toDomain()
}
