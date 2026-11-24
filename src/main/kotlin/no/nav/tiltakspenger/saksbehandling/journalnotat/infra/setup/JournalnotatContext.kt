package no.nav.tiltakspenger.saksbehandling.journalnotat.infra.setup

import no.nav.tiltakspenger.saksbehandling.behandling.domene.RammevedtakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.infra.setup.MdcKorrelasjon
import no.nav.tiltakspenger.saksbehandling.journalnotat.GenererJournalnotatKlient
import no.nav.tiltakspenger.saksbehandling.journalnotat.JournalførJournalnotatKlient
import no.nav.tiltakspenger.saksbehandling.journalnotat.infra.jobb.JournalførJournalnotatJobb
import no.nav.tiltakspenger.saksbehandling.saksbehandler.NavIdentClient
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.MeldekortvedtakRepo
import java.time.Clock

open class JournalnotatContext(
    rammevedtakRepo: RammevedtakRepo,
    meldekortvedtakRepo: MeldekortvedtakRepo,
    genererJournalnotatKlient: GenererJournalnotatKlient,
    journalførJournalnotatKlient: JournalførJournalnotatKlient,
    personService: PersonService,
    navIdentClient: NavIdentClient,
    clock: Clock,
) {
    val journalførJournalnotatJobb by lazy {
        JournalførJournalnotatJobb(
            rammevedtakRepo = rammevedtakRepo,
            meldekortvedtakRepo = meldekortvedtakRepo,
            genererJournalnotatKlient = genererJournalnotatKlient,
            journalførJournalnotatKlient = journalførJournalnotatKlient,
            personService = personService,
            navIdentClient = navIdentClient,
            clock = clock,
            korrelasjon = MdcKorrelasjon(),
        )
    }
}
