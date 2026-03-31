package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.underkjenn

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.felles.ServiceCommand

data class UnderkjennMeldekortbehandlingKommando(
    val sakId: SakId,
    val meldekortId: MeldekortId,
    val begrunnelse: String,
    override val saksbehandler: Saksbehandler,
    override val correlationId: CorrelationId,
) : ServiceCommand
