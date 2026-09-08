package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.saksbehandling.felles.Loggbar
import no.nav.tiltakspenger.saksbehandling.felles.Loggkontekst

/**
 * Forventede feil ved start av en revurdering.
 * Alle gjelder omgjøring i dag, siden det bare er der saksbehandler peker på et bestemt vedtak.
 */
sealed interface KanIkkeStarteRevurdering : Loggbar {

    /**
     * Vedtaket er ikke lenger gjeldende, eller det er et avslagsvedtak.
     * Tilsvarer at [no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak.gyldigOmgjøringskommando] er null.
     */
    data class VedtaketKanIkkeOmgjøres(
        val vedtakId: VedtakId,
        val saksnummer: Saksnummer,
    ) : KanIkkeStarteRevurdering {
        override val loggkontekst get() = Loggkontekst("vedtaket $vedtakId på saksnummer $saksnummer er ikke gjeldende eller er et avslagsvedtak")
    }
}
