package no.nav.tiltakspenger.saksbehandling.objectmothers

import no.nav.tiltakspenger.saksbehandling.felles.Navkontor
import no.nav.tiltakspenger.saksbehandling.felles.OppgaveId
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.personopplysninger.Navn

object ObjectMother :
    SaksbehandlerMother,
    SystembrukerMother,
    SøknadMother,
    BehandlingMother,
    SakMother,
    PersonMother,
    MeldekortMother,
    UtbetalingsvedtakMother,
    TiltakMother,
    SaksopplysningerMother,
    BarnetilleggMother {
    fun navn() = Navn("Fornavn", "Mellomnavn", "Etternavn")
    fun navkontor() = Navkontor(kontornummer = "0220", kontornavn = "Nav Asker")
    fun oppgaveId(oppgaveId: OppgaveId = OppgaveId("100")) = oppgaveId
}
