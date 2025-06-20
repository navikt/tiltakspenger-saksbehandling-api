package no.nav.tiltakspenger.saksbehandling.behandling.service.delautomatiskbehandling

import no.nav.tiltakspenger.libs.common.GenerellSystembrukerrolle
import no.nav.tiltakspenger.libs.common.GenerellSystembrukerroller
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.Saksbehandlerrolle
import no.nav.tiltakspenger.libs.common.Saksbehandlerroller
import no.nav.tiltakspenger.saksbehandling.felles.Systembrukerroller
import no.nav.tiltakspenger.saksbehandling.infra.setup.AUTOMATISK_SAKSBEHANDLER_ID

val AUTOMATISK_SAKSBEHANDLER = Saksbehandler(
    navIdent = AUTOMATISK_SAKSBEHANDLER_ID,
    brukernavn = AUTOMATISK_SAKSBEHANDLER_ID,
    epost = "",
    roller = Saksbehandlerroller(Saksbehandlerrolle.SAKSBEHANDLER),
    scopes = Systembrukerroller(
        emptySet(),
    ) as GenerellSystembrukerroller<GenerellSystembrukerrolle>,
    klientId = "",
    klientnavn = "",
)
