package no.nav.tiltakspenger.saksbehandling.felles.exceptions

import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.Saksbehandlerrolle
import no.nav.tiltakspenger.libs.common.Saksbehandlerroller

class TilgangException(
    message: String,
) : RuntimeException(message) {
    constructor(
        saksbehandler: Saksbehandler,
        krevRolle: Saksbehandlerrolle,
    ) : this(
        "Saksbehandler ${saksbehandler.navIdent} mangler rollen $krevRolle. Saksbehandlers roller: ${saksbehandler.roller}",
    )

    constructor(
        saksbehandler: Saksbehandler,
        vararg krevEnAvRollene: Saksbehandlerrolle,
    ) : this(saksbehandler, krevEnAvRollene.toList())

    constructor(
        saksbehandler: Saksbehandler,
        krevEnAvRollene: Saksbehandlerroller,
    ) : this(saksbehandler, krevEnAvRollene.toList())

    constructor(
        saksbehandler: Saksbehandler,
        krevEnAvRollene: List<Saksbehandlerrolle>,
    ) : this(
        "Saksbehandler ${saksbehandler.navIdent} mangler en av rollene $krevEnAvRollene. Saksbehandlers roller: ${saksbehandler.roller}",
    )
}
