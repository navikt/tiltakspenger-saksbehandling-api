package no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.Ulid

fun String.tilBehandlingIdFraTilbakekreving(): Either<String, Ulid> {
    if (this.startsWith(RammebehandlingId.PREFIX)) {
        return RammebehandlingId.fromString(this).right()
    }

    if (this.startsWith(MeldekortId.PREFIX)) {
        return MeldekortId.fromString(this).right()
    }

    // For bakoverkompatibilitet - treffer denne for behandlinger der vi har satt behandlingId = kravgrunnlagReferanse i infosvaret
    // Tilsvarer "behandlingId" som vi sender til utbetaling, som er uuid-delen av utbetalings-id'en
    return this.left()
}
