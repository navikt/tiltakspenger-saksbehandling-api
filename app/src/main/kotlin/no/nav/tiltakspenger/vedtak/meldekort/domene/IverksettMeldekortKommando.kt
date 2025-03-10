package no.nav.tiltakspenger.vedtak.meldekort.domene

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler

data class IverksettMeldekortKommando(
    val sakId: SakId,
    val meldekortId: MeldekortId,
    val beslutter: Saksbehandler,
    val correlationId: CorrelationId,
)
