package no.nav.tiltakspenger.saksbehandling.klage.infra.route

import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagevedtak

enum class KlageresultatstypeDto {
    AVVIST,
    OMGJØR,
    ;

    companion object {
        fun Klagebehandlingsresultat.toKlageresultatstypDto(): KlageresultatstypeDto = when (this) {
            is Klagebehandlingsresultat.Avvist -> AVVIST
            is Klagebehandlingsresultat.Omgjør -> OMGJØR
        }

        fun Klagebehandling.toKlageResultat(): KlageresultatstypeDto? = this.resultat?.toKlageresultatstypDto()

        fun Klagevedtak.toKlageResultat(): KlageresultatstypeDto = this.resultat.toKlageresultatstypDto()
    }
}
