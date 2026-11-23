package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.saksbehandling.felles.Loggbar
import no.nav.tiltakspenger.saksbehandling.felles.Loggkontekst
import no.nav.tiltakspenger.saksbehandling.felles.loggkontekst
import no.nav.tiltakspenger.saksbehandling.felles.sikkerloggkontekst

private const val MIGRERING_HOS_HEL_VED = "migrering til oppdragsversjon 3 hos Hel Ved"

/**
 * Grunner til at en sak ikke kan migreres fra [Oppdragsversjon.V2] til [Oppdragsversjon.V3].
 * Saken står fortsatt på [Oppdragsversjon.V2], og migreringen kan kjøres på nytt.
 */
sealed interface KanIkkeMigrereTilOppdragsversjon3 : Loggbar {
    /** Migreringen er avslått i prod. */
    data object FunksjonenErIkkePåslått : KanIkkeMigrereTilOppdragsversjon3 {
        override val loggkontekst = Loggkontekst("Migrering til oppdragsversjon 3 er ikke påslått i dette miljøet")
    }

    /**
     * Hel Ved kan bare videreføre kjeder der alle utbetalingene er sendt og kvittert OK.
     * Saken er ikke sperret.
     */
    data class HarUtbetalingSomIkkeErKvittert(val utbetalingIder: List<UtbetalingId>) : KanIkkeMigrereTilOppdragsversjon3 {
        override val loggkontekst = Loggkontekst("Kan ikke migrere saken til oppdragsversjon 3 fordi utbetalingene $utbetalingIder ikke er kvittert OK")
    }

    /**
     * Hel Ved svarte 423 fordi siste iverksetting på saken ikke er kvittert OK hos dem.
     * Saken står med utbetalingssperre.
     */
    data class HelvedHarIkkeKvittertSisteIverksetting(val feil: HttpKlientError) : KanIkkeMigrereTilOppdragsversjon3 {
        override val loggkontekst = feil.loggkontekst(MIGRERING_HOS_HEL_VED).let { it.copy(melding = "${it.melding}. Saken står sperret på oppdragsversjon 2") }
        override val sikkerloggkontekst = feil.sikkerloggkontekst(MIGRERING_HOS_HEL_VED)
    }

    /** Saken står med utbetalingssperre. */
    data class HelvedFeilet(val feil: HttpKlientError) : KanIkkeMigrereTilOppdragsversjon3 {
        override val loggkontekst = feil.loggkontekst(MIGRERING_HOS_HEL_VED).let { it.copy(melding = "${it.melding}. Saken står sperret på oppdragsversjon 2") }
        override val sikkerloggkontekst = feil.sikkerloggkontekst(MIGRERING_HOS_HEL_VED)
    }
}
