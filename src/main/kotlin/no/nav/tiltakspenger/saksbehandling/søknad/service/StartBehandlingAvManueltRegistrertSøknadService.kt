package no.nav.tiltakspenger.saksbehandling.søknad.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RammebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadRepo
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.HentSaksopplysingerService
import no.nav.tiltakspenger.saksbehandling.behandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.journalpost.ValiderJournalpostService
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService
import no.nav.tiltakspenger.saksbehandling.søknad.domene.KanIkkeStarteBehandlingenAvManueltRegistrertSøknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.søknad.infra.route.StartBehandlingAvManueltRegistrertSøknadCommand
import java.time.Clock

class StartBehandlingAvManueltRegistrertSøknadService(
    private val clock: Clock,
    private val sakService: SakService,
    private val rammebehandlingRepo: RammebehandlingRepo,
    private val hentSaksopplysingerService: HentSaksopplysingerService,
    private val søknadRepo: SøknadRepo,
    private val statistikkService: StatistikkService,
    private val journalpostService: ValiderJournalpostService,
    private val personService: PersonService,
    private val sessionFactory: SessionFactory,
) {
    val logger = KotlinLogging.logger { }

    suspend fun startBehandlingAvManueltRegistrertSøknad(
        kommando: StartBehandlingAvManueltRegistrertSøknadCommand,
        saksnummer: Saksnummer,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Either<KanIkkeStarteBehandlingenAvManueltRegistrertSøknad, Pair<Sak, Søknadsbehandling>> {
        if (kommando.manueltSattSøknadMottattDato == null && kommando.behandlingsarsak == null) {
            return KanIkkeStarteBehandlingenAvManueltRegistrertSøknad.EnManuellDatoKrevesNårDetIkkeErMigreringFraArena.left()
        }

        val sak = sakService.hentForSaksnummer(saksnummer)
        val journalpostValidering = journalpostService.hentOgValiderJournalpost(sak.fnr, kommando.journalpostId)

        if (!journalpostValidering.journalpostFinnes) {
            return KanIkkeStarteBehandlingenAvManueltRegistrertSøknad.JournalpostFinnesIkke(kommando.journalpostId).left()
        }

        if (journalpostValidering.datoOpprettet == null) {
            return KanIkkeStarteBehandlingenAvManueltRegistrertSøknad.JournalpostManglerOpprettsdato(kommando.journalpostId).left()
        }

        if (journalpostValidering.gjelderInnsendtFnr == false) {
            return KanIkkeStarteBehandlingenAvManueltRegistrertSøknad.JournalpostTilhørerEnAnnenBruker(kommando.journalpostId).left()
        }

        val personopplysninger = personService.hentPersonopplysninger(sak.fnr)

        val manueltRegistrertSøknad = Søknad.opprett(
            sak = sak,
            journalpostId = kommando.journalpostId.toString(),
            opprettet = nå(clock),
            mottatt = kommando.manueltSattSøknadMottattDato?.atStartOfDay() ?: journalpostValidering.datoOpprettet,
            personopplysninger = Søknad.Personopplysninger(
                fnr = personopplysninger.fnr,
                fornavn = personopplysninger.fornavn,
                etternavn = personopplysninger.etternavn,
            ),
            søknadstiltak = kommando.søknadstiltak,
            barnetillegg = kommando.barnetillegg,
            harSøktPåTiltak = kommando.harSøktPåTiltak,
            harSøktOmBarnetillegg = kommando.harSøktOmBarnetillegg,
            kvp = kommando.kvp,
            intro = kommando.intro,
            institusjon = kommando.institusjon,
            etterlønn = kommando.etterlønn,
            gjenlevendepensjon = kommando.gjenlevendepensjon,
            alderspensjon = kommando.alderspensjon,
            sykepenger = kommando.sykepenger,
            supplerendeStønadAlder = kommando.supplerendeStønadAlder,
            supplerendeStønadFlyktning = kommando.supplerendeStønadFlyktning,
            jobbsjansen = kommando.jobbsjansen,
            trygdOgPensjon = kommando.trygdOgPensjon,
            antallVedlegg = kommando.antallVedlegg,
            manueltSattSøknadsperiode = kommando.manueltSattSøknadsperiode,
            manueltSattTiltak = kommando.manueltSattTiltak,
            søknadstype = kommando.søknadstype,
            behandlingsarsak = kommando.behandlingsarsak,
            manueltRegistrert = true,
        )

        // Legg søknaden inn i sak før vi oppretter behandlingen eventuelt tiltak inkluderes i saksopplysningene
        val sakMedSøknad = sak.leggTilSøknad(manueltRegistrertSøknad)

        val (oppdatertSak, søknadsbehandling, statistikkhendelser) = Søknadsbehandling.opprett(
            sak = sakMedSøknad,
            søknad = manueltRegistrertSøknad,
            saksbehandler = saksbehandler,
            hentSaksopplysninger = hentSaksopplysingerService::hentSaksopplysningerFraRegistre,
            correlationId = correlationId,
            clock = clock,
            klagebehandling = null,
        )

        val statistikkDTO = statistikkService.generer(statistikkhendelser)

        sessionFactory.withTransactionContext { tx ->
            søknadRepo.lagre(manueltRegistrertSøknad, tx)
            rammebehandlingRepo.lagre(søknadsbehandling, tx)
            statistikkService.lagre(statistikkDTO, tx)
            sakService.markerSkalSendesTilMeldekortApi(
                sakId = sak.id,
                sessionContext = tx,
            )
        }
        return (oppdatertSak to søknadsbehandling).right()
    }
}
