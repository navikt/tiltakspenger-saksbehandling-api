package no.nav.tiltakspenger.saksbehandling.person.infra.repo

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.RammebehandlingFakeRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.PersonRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.MeldekortbehandlingFakeRepo
import no.nav.tiltakspenger.saksbehandling.sak.infra.repo.SakFakeRepo
import no.nav.tiltakspenger.saksbehandling.søknad.infra.repo.SøknadFakeRepo

class PersonFakeRepo(
    private val sakFakeRepo: SakFakeRepo,
    private val søknadFakeRepo: SøknadFakeRepo,
    private val meldekortbehandlingFakeRepo: MeldekortbehandlingFakeRepo,
    private val behandlingFakeRepo: RammebehandlingFakeRepo,
) : PersonRepo {

    override fun hentFnrForSakId(sakId: SakId): Fnr {
        return sakFakeRepo.data.get()[sakId]!!.fnr
    }

    override fun hentFnrForRammebehandlingId(behandlingId: RammebehandlingId): Fnr {
        return behandlingFakeRepo.hent(behandlingId).fnr
    }

    override fun hentFnrForSaksnummer(saksnummer: Saksnummer): Fnr? {
        return sakFakeRepo.hentFnrForSaksnummer(saksnummer)
    }

    override fun hentFnrForMeldekortId(meldekortId: MeldekortId): Fnr? {
        return meldekortbehandlingFakeRepo.hentFnrForMeldekortId(meldekortId)
    }

    override fun hentFnrForSøknadId(søknadId: SøknadId): Fnr {
        return søknadFakeRepo.hentForSøknadId(søknadId).fnr
    }
}
