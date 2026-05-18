package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.repo

import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingVenter
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingVenter.TilbakekrevingVentegrunn
import java.time.LocalDate

data class TilbakekrevingVenterDb(
    val grunn: TilbakekrevingVentegrunnDb,
    val gjenopptas: LocalDate,
) {
    enum class TilbakekrevingVentegrunnDb {
        AVVENTER_BRUKERUTTALELSE,
        ;

        fun tilDomene(): TilbakekrevingVentegrunn = when (this) {
            AVVENTER_BRUKERUTTALELSE -> TilbakekrevingVentegrunn.AVVENTER_BRUKERUTTALELSE
        }
    }

    fun tilDomene(): TilbakekrevingVenter = TilbakekrevingVenter(
        grunn = grunn.tilDomene(),
        gjenopptas = gjenopptas,
    )
}

fun TilbakekrevingVenter.tilDb(): TilbakekrevingVenterDb = TilbakekrevingVenterDb(
    grunn = when (grunn) {
        TilbakekrevingVentegrunn.AVVENTER_BRUKERUTTALELSE -> TilbakekrevingVenterDb.TilbakekrevingVentegrunnDb.AVVENTER_BRUKERUTTALELSE
    },
    gjenopptas = gjenopptas,
)

fun TilbakekrevingVenter.toDbJson(): String = serialize(this.tilDb())

fun String.tilTilbakekrevingVenter(): TilbakekrevingVenter =
    deserialize<TilbakekrevingVenterDb>(this).tilDomene()
