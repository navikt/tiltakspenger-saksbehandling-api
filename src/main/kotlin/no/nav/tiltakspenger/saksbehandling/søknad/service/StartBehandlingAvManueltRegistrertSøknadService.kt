package no.nav.tiltakspenger.saksbehandling.søknad.service

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.ports.RammebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SøknadRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.HentSaksopplysingerService
import no.nav.tiltakspenger.saksbehandling.behandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.infra.metrikker.MetricRegister
import no.nav.tiltakspenger.saksbehandling.journalpost.ValiderJournalpostService
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakService
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.søknad.infra.route.StartBehandlingAvManueltRegistrertSøknadCommand
import java.time.Clock

class StartBehandlingAvManueltRegistrertSøknadService(
    private val clock: Clock,
    private val sakService: SakService,
    private val rammebehandlingRepo: RammebehandlingRepo,
    private val hentSaksopplysingerService: HentSaksopplysingerService,
    private val søknadRepo: SøknadRepo,
    private val statistikkSakService: StatistikkSakService,
    private val statistikkSakRepo: StatistikkSakRepo,
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
    ): Pair<Sak, Søknadsbehandling> {
        val sak = sakService.hentForSaksnummer(saksnummer)
        val journalpostValidering = journalpostService.hentOgValiderJournalpost(sak.fnr, kommando.journalpostId)

        if (!journalpostValidering.journalpostFinnes) {
            throw IllegalArgumentException("Journalpost ${kommando.journalpostId} finnes ikke")
        }

        if (journalpostValidering.datoOpprettet == null) {
            throw IllegalArgumentException("Journalpost ${kommando.journalpostId} mangler datoOpprettet")
        }

        val personopplysninger = personService.hentPersonopplysninger(sak.fnr)

        val manueltRegistrertSøknad = Søknad.opprett(
            sak = sak,
            journalpostId = kommando.journalpostId.toString(),
            opprettet = journalpostValidering.datoOpprettet,
            tidsstempelHosOss = nå(clock),
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
        )

        // Legg søknaden inn i sak før vi oppretter behandlingen eventuelt tiltak inkluderes i saksopplysningene
        val sakMedSøknad = sak.copy(søknader = sak.søknader + manueltRegistrertSøknad)

        val (oppdatertSak, søknadsbehandling) = Søknadsbehandling.opprett(
            sak = sakMedSøknad,
            søknad = manueltRegistrertSøknad,
            saksbehandler = saksbehandler,
            hentSaksopplysninger = hentSaksopplysingerService::hentSaksopplysningerFraRegistre,
            correlationId = correlationId,
            clock = clock,
            klagebehandling = null,
        )

        val opprettetBehandlingStatistikk = statistikkSakService.genererStatistikkForSøknadsbehandling(
            behandling = søknadsbehandling,
        )

        sessionFactory.withTransactionContext { tx ->
            søknadRepo.lagre(manueltRegistrertSøknad, tx)
            rammebehandlingRepo.lagre(søknadsbehandling, tx)
            statistikkSakRepo.lagre(opprettetBehandlingStatistikk, tx)
            sakService.oppdaterSkalSendesTilMeldekortApi(
                sakId = sak.id,
                skalSendesTilMeldekortApi = true,
                sessionContext = tx,
            )
            // TODO jah: Å gjøre om withTransactionContext til suspend function er målet, men krever noen dagers arbeid
            @Suppress("RunBlockingInSuspendFunction")
            runBlocking {
                tx.onSuccess {
                    MetricRegister.STARTET_BEHANDLING.inc()
                    MetricRegister.MOTTATT_MANUELT_REGISTRERT_SOKNAD.inc()
                }
            }
        }
        return (oppdatertSak to søknadsbehandling)
    }
}
