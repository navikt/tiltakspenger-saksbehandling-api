package no.nav.tiltakspenger.saksbehandling.klage.infra.route

import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat

enum class KlageresultatstypeDto {
    AVVIST,
    OMGJØR,
    OPPRETTHOLDT,
    ;

    companion object {
        fun Klagebehandlingsresultat.toKlageresultatstypDto(): KlageresultatstypeDto = when (this) {
            is Klagebehandlingsresultat.Avvist -> AVVIST
            is Klagebehandlingsresultat.Omgjør -> OMGJØR
            is Klagebehandlingsresultat.Opprettholdt -> OPPRETTHOLDT
        }
    }
}
