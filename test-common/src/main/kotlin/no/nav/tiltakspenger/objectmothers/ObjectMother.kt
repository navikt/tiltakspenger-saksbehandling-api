package no.nav.tiltakspenger.objectmothers

import no.nav.tiltakspenger.felles.Navkontor
import no.nav.tiltakspenger.saksbehandling.domene.personopplysninger.Navn

object ObjectMother :
    SaksbehandlerMother,
    SystembrukerMother,
    SøknadMother,
    BehandlingMother,
    SakMother,
    VilkårMother,
    PersonMother,
    MeldekortMother,
    UtbetalingsvedtakMother,
    TiltakMother {
    fun navn() = Navn("Fornavn", "Mellomnavn", "Etternavn")
    fun navkontor() = Navkontor("0220")
}
