package no.nav.tiltakspenger.saksbehandling.klage.infra.route.brev

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.NonBlankString
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.Brevtekster
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.KlagebehandlingBrevKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.TittelOgTekst

data class KlagebehandlingTeksterTilBrevBody(
    val tekstTilVedtaksbrev: List<TittelOgTekstBody> = emptyList(),
) {
    data class TittelOgTekstBody(
        val tittel: String,
        val tekst: String,
    )

    fun tilKommando(
        sakId: SakId,
        klagebehandlingId: KlagebehandlingId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ) = KlagebehandlingBrevKommando(
        sakId = sakId,
        klagebehandlingId = klagebehandlingId,
        saksbehandler = saksbehandler,
        correlationId = correlationId,
        brevtekster = Brevtekster(
            tekstTilVedtaksbrev.map {
                TittelOgTekst(
                    tittel = NonBlankString.create(it.tittel),
                    tekst = NonBlankString.create(it.tekst),
                )
            },
        ),
    )
}
