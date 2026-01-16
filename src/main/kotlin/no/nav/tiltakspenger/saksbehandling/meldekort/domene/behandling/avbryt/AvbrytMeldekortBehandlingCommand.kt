package no.nav.tiltakspenger.saksbehandling.meldekort.domene.behandling.avbryt

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.NonBlankString
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.felles.ServiceCommand

data class AvbrytMeldekortBehandlingCommand(
    val sakId: SakId,
    val meldekortId: MeldekortId,
    val begrunnelse: NonBlankString,
    override val saksbehandler: Saksbehandler,
    override val correlationId: CorrelationId,
) : ServiceCommand
