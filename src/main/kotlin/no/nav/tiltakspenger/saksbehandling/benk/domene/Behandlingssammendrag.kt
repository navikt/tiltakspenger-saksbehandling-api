package no.nav.tiltakspenger.saksbehandling.benk.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import java.time.LocalDateTime

data class Behandlingssammendrag(
    val fnr: Fnr,
    val saksnummer: Saksnummer,
    val startet: LocalDateTime,
    val behandlingstype: BehandlingssammendragType,
    val status: Behandlingsstatus?,
    val saksbehandler: String?,
    val beslutter: String?,
)
