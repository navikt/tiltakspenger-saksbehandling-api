package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.tilBeslutter

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler

/**
 * Representerer en saksbehandler som fyller ut hele meldekortet, begrunnelsen og sender til beslutter.
 * Vi gjør ingen validering i denne klassen, det gjøres heller av [no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingManuell]
 *
 */
class SendMeldekortbehandlingTilBeslutterKommando(
    val sakId: SakId,
    val meldekortId: MeldekortId,
    val saksbehandler: Saksbehandler,
    val correlationId: CorrelationId,
)
