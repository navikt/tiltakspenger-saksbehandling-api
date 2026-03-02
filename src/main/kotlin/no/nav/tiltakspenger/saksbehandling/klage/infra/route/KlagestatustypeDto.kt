package no.nav.tiltakspenger.saksbehandling.klage.infra.route

import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus

enum class KlagestatustypeDto {
    KLAR_TIL_BEHANDLING,
    UNDER_BEHANDLING,
    AVBRUTT,
    VEDTATT,
    OPPRETTHOLDT,
    OVERSENDT,
    FERDIGSTILT,
    MOTTATT_FRA_KLAGEINSTANS,
    ;

    companion object {
        fun Klagebehandlingsstatus.toKlagestatustypeDto(): KlagestatustypeDto = when (this) {
            Klagebehandlingsstatus.KLAR_TIL_BEHANDLING -> KLAR_TIL_BEHANDLING
            Klagebehandlingsstatus.UNDER_BEHANDLING -> UNDER_BEHANDLING
            Klagebehandlingsstatus.AVBRUTT -> AVBRUTT
            Klagebehandlingsstatus.VEDTATT -> VEDTATT
            Klagebehandlingsstatus.OPPRETTHOLDT -> OPPRETTHOLDT
            Klagebehandlingsstatus.OVERSENDT -> OVERSENDT
            Klagebehandlingsstatus.FERDIGSTILT -> FERDIGSTILT
            Klagebehandlingsstatus.MOTTATT_FRA_KLAGEINSTANS -> MOTTATT_FRA_KLAGEINSTANS
        }
    }
}
