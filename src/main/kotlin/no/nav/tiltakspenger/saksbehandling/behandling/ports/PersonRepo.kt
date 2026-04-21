package no.nav.tiltakspenger.saksbehandling.behandling.ports

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.SøknadId

/*
    Dette repoet brukes av auditloggeren
 */
interface PersonRepo {
    fun hentFnrForSakId(sakId: SakId): Fnr?
    fun hentFnrForRammebehandlingId(behandlingId: RammebehandlingId): Fnr
    fun hentFnrForSaksnummer(saksnummer: Saksnummer): Fnr?
    fun hentFnrForMeldekortId(meldekortId: MeldekortId): Fnr?
    fun hentFnrForSøknadId(søknadId: SøknadId): Fnr?
}
