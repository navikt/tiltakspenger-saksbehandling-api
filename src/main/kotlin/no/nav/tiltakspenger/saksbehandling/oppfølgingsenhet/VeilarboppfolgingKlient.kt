package no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet

import no.nav.tiltakspenger.libs.common.Fnr

interface VeilarboppfolgingKlient {
    suspend fun hentOppfolgingsenhet(fnr: Fnr): Navkontor
}
