package no.nav.tiltakspenger.saksbehandling.behandling.domene.konverter

import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.felles.Loggbar
import no.nav.tiltakspenger.saksbehandling.felles.Loggkontekst

sealed interface KanIkkeKonvertereRevurdering : Loggbar {

    data object ErIkkeRevurdering : KanIkkeKonvertereRevurdering {
        override val loggkontekst = Loggkontekst(melding = "Bare revurderinger kan konverteres")
    }

    data object MåVæreSaksbehandler : KanIkkeKonvertereRevurdering {
        override val loggkontekst = Loggkontekst(melding = "Utøvende saksbehandler mangler saksbehandlerrollen")
    }

    data class MåVæreUnderBehandling(val status: Rammebehandlingsstatus) : KanIkkeKonvertereRevurdering {
        override val loggkontekst = Loggkontekst(melding = "Revurderingen må være under behandling. Status er $status")
    }

    data class EiesAvAnnenSaksbehandler(val eiesAvSaksbehandler: String?) : KanIkkeKonvertereRevurdering {
        override val loggkontekst = Loggkontekst(melding = "Revurderingen er tildelt $eiesAvSaksbehandler, og kan bare konverteres av den tildelte saksbehandleren")
    }

    /** Klagebehandlingen er knyttet til revurderingen slik den ble startet, så den kan ikke bytte type. */
    data object HarKlagebehandling : KanIkkeKonvertereRevurdering {
        override val loggkontekst = Loggkontekst(melding = "Revurderingen er knyttet til en klagebehandling")
    }

    data object HarAlleredeTypen : KanIkkeKonvertereRevurdering {
        override val loggkontekst = Loggkontekst(melding = "Revurderingen er allerede av typen den skal konverteres til")
    }

    /** Tilsvarer at [no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtaksliste.gyldigForlengelseskommando] er null. */
    data class VedtaketKanIkkeForlenges(val vedtakId: VedtakId) : KanIkkeKonvertereRevurdering {
        override val loggkontekst = Loggkontekst(melding = "Vedtaket $vedtakId finnes ikke på saken eller kan ikke forlenges")
    }

    /** Forlengelse er foreløpig sperret i prod. */
    data object ForlengelseErIkkePåslått : KanIkkeKonvertereRevurdering {
        override val loggkontekst = Loggkontekst(melding = "Forlengelse er ikke påslått i dette miljøet")
    }
}
