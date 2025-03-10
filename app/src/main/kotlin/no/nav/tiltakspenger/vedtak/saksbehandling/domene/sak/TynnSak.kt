package no.nav.tiltakspenger.vedtak.saksbehandling.domene.sak

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId

data class TynnSak(
    val id: SakId,
    val fnr: Fnr,
    val saksnummer: Saksnummer,
)
