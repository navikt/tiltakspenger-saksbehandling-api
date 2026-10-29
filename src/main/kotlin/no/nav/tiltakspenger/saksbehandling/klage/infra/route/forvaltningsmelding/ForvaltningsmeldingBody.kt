package no.nav.tiltakspenger.saksbehandling.klage.infra.route.forvaltningsmelding

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.NonBlankString
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.Brevtekster
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.TittelOgTekst
import no.nav.tiltakspenger.saksbehandling.klage.domene.forvaltningsmelding.ForvaltningsmeldingKommando

data class ForvaltningsmeldingBody(
    val tekster: List<TittelOgTekstBody> = emptyList(),
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
    ) = ForvaltningsmeldingKommando(
        sakId = sakId,
        klagebehandlingId = klagebehandlingId,
        saksbehandler = saksbehandler,
        correlationId = correlationId,
        brevtekster = Brevtekster(
            tekster.map {
                TittelOgTekst(
                    tittel = NonBlankString.create(it.tittel),
                    tekst = NonBlankString.create(it.tekst),
                )
            },
        ),
    )
}
