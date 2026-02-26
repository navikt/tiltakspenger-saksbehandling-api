package no.nav.tiltakspenger.saksbehandling.klage.infra.route

import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus

enum class KlagestatustypeDto {
    KLAR_TIL_BEHANDLING,
    UNDER_BEHANDLING,
    AVBRUTT,
    IVERKSATT,
    OPPRETTHOLDT,
    OVERSENDT,
    FERDIGSTILT,
    ;

    companion object {
        fun Klagebehandlingsstatus.toKlagestatustypeDto(): KlagestatustypeDto = when (this) {
            Klagebehandlingsstatus.KLAR_TIL_BEHANDLING -> KLAR_TIL_BEHANDLING

            Klagebehandlingsstatus.UNDER_BEHANDLING -> UNDER_BEHANDLING

            Klagebehandlingsstatus.AVBRUTT -> AVBRUTT

            // TODO jah: Endre til VEDTATT her og frontend samtidig.
            Klagebehandlingsstatus.VEDTATT -> IVERKSATT

            Klagebehandlingsstatus.OPPRETTHOLDT -> OPPRETTHOLDT

            Klagebehandlingsstatus.OVERSENDT -> OVERSENDT

            Klagebehandlingsstatus.FERDIGSTILT -> FERDIGSTILT
        }
    }
}
