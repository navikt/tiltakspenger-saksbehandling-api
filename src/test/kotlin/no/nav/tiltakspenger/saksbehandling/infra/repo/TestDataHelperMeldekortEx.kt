package no.nav.tiltakspenger.saksbehandling.infra.repo

import arrow.core.left
import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.beregning.beregnMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandletAutomatiskStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandletManuelt
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingBegrunnelse
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldekortvedtak
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.SendMeldekortTilBeslutterKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.oppdaterMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.opprettManuellMeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.opprettVedtak
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.saksbehandlerFyllerUtMeldeperiodeDager
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeSimulere
import java.time.Clock
import java.time.LocalDateTime

internal fun TestDataHelper.persisterBrukersMeldekort(
    sak: Sak? = null,
    // Første kall kan være null - alle påfølgende kall må ha med meldeperiode
    meldeperiode: Meldeperiode? = null,
    periode: Periode = Periode(
        2.januar(2023),
        15.januar(2023),
    ),
    clock: Clock = this.clock,
    genererSak: (Sak?) -> Sak = { s ->
        s ?: this.persisterIverksattSøknadsbehandling(
            deltakelseFom = periode.fraOgMed,
            deltakelseTom = periode.tilOgMed,
            clock = clock,
        ).first
    },
    behandlesAutomatisk: Boolean = false,
    behandletAutomatiskStatus: MeldekortBehandletAutomatiskStatus =
        if (behandlesAutomatisk) {
            MeldekortBehandletAutomatiskStatus.VENTER_BEHANDLING
        } else {
            MeldekortBehandletAutomatiskStatus.SKAL_IKKE_BEHANDLES_AUTOMATISK
        },
): Pair<Sak, BrukersMeldekort> {
    val generertSak = genererSak(sak)

    val valgtMeldeperiode = meldeperiode ?: generertSak.meldeperiodeKjeder.first().hentSisteMeldeperiode()

    val brukersMeldekort = ObjectMother.brukersMeldekort(
        id = MeldekortId.random(),
        mottatt = LocalDateTime.now(clock),
        sakId = generertSak.id,
        meldeperiode = valgtMeldeperiode,
        behandlesAutomatisk = behandlesAutomatisk,
        behandletAutomatiskStatus = behandletAutomatiskStatus,
    )

    this.meldekortBrukerRepo.lagre(
        brukersMeldekort = brukersMeldekort,
        sessionContext = null,
    )

    return this.sakRepo.hentForSakId(generertSak.id)!! to brukersMeldekort
}

internal fun TestDataHelper.persisterKlarTilBehandlingManuellMeldekortBehandling(
    sak: Sak? = null,
    saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    kjedeId: MeldeperiodeKjedeId? = null,
    periode: Periode = Periode(
        2.januar(2023),
        15.januar(2023),
    ),
    navkontor: Navkontor = Navkontor("0012", "Kontor TestDataHelper.persisterManuellMeldekortBehandling"),
    clock: Clock = this.clock,
    genererSak: (Sak?) -> Sak = { s ->
        s ?: this.persisterIverksattSøknadsbehandling(
            deltakelseFom = periode.fraOgMed,
            deltakelseTom = periode.tilOgMed,
            clock = clock,
        ).first
    },
): Pair<Sak, MeldekortUnderBehandling> {
    val generertSak = genererSak(sak)

    val (_, manuellMeldekortBehandling) = generertSak.opprettManuellMeldekortBehandling(
        kjedeId = kjedeId ?: generertSak.meldeperiodeKjeder.first().kjedeId,
        navkontor = navkontor,
        saksbehandler = saksbehandler,
        clock = clock,
    )

    this.meldekortRepo.lagre(manuellMeldekortBehandling, null)

    return this.sakRepo.hentForSakId(manuellMeldekortBehandling.sakId)!! to manuellMeldekortBehandling
}

/**
 * OBS: Prøver å avslutte siste meldekortbehandling i [sak], hvis den er i tilstanden [MeldekortUnderBehandling].
 * (enklere å begynne med :) )
 */
internal fun TestDataHelper.persisterAvsluttetMeldekortBehandling(
    sak: Sak? = null,
    saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    periode: Periode = Periode(
        2.januar(2023),
        15.januar(2023),
    ),
    begrunnelse: String = "TestDataHelper.persisterAvsluttetMeldekortBehandling",
    clock: Clock = this.clock,
    genererSak: (Sak?) -> Sak = { s ->
        s ?: this.persisterKlarTilBehandlingManuellMeldekortBehandling(
            s,
            periode = periode,
            clock = clock,
        ).first
    },
): Pair<Sak, MeldekortBehandling> {
    val generertSak = genererSak(sak)
    val meldekortBehandling = generertSak.meldekortbehandlinger
        .filterIsInstance<MeldekortUnderBehandling>()
        .last { it.meldeperiode.periode == periode }

    val avbruttMeldekortbehandling = meldekortBehandling.avbryt(
        avbruttAv = saksbehandler,
        begrunnelse = begrunnelse,
        tidspunkt = LocalDateTime.now(clock),
    ).getOrFail()

    this.meldekortRepo.oppdater(avbruttMeldekortbehandling, null, null)

    return this.sakRepo.hentForSakId(avbruttMeldekortbehandling.sakId)!! to avbruttMeldekortbehandling
}

internal fun TestDataHelper.persisterManuellMeldekortBehandlingTilBeslutning(
    sak: Sak? = null,
    saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    periode: Periode = Periode(
        2.januar(2023),
        15.januar(2023),
    ),
    clock: Clock = this.clock,
    genererSak: (Sak?) -> Pair<Sak, MeldekortUnderBehandling> = { s ->
        this.persisterKlarTilBehandlingManuellMeldekortBehandling(
            s,
            periode = periode,
            clock = clock,
        )
    },
): Pair<Sak, MeldekortBehandling> {
    val (sakMedOpprettetMeldekortBehandling, opprettetMeldekortBehandling) = genererSak(sak)
    val dager = saksbehandlerFyllerUtMeldeperiodeDager(opprettetMeldekortBehandling.meldeperiode)
    val begrunnelse = MeldekortBehandlingBegrunnelse("TestDataHelper.persisterManuellMeldekortBehandlingTilBeslutning")

    return runBlocking {
        val (sakMedOppdatertMeldekortbehandling, meldekortBehandling, simuleringMedMetadata) = sakMedOpprettetMeldekortBehandling.oppdaterMeldekort(
            kommando = ObjectMother.oppdaterMeldekortKommando(
                sakId = sakMedOpprettetMeldekortBehandling.id,
                meldekortId = opprettetMeldekortBehandling.id,
                saksbehandler = saksbehandler,
                begrunnelse = begrunnelse,
                correlationId = CorrelationId.generate(),
                dager = dager,
            ),
            simuler = { KunneIkkeSimulere.Stengt.left() },
        ).getOrFail()

        meldekortRepo.oppdater(meldekortBehandling, simuleringMedMetadata)

        val (meldekortBehandlingTilBeslutning, andreSimuleringMedMetadata) = meldekortBehandling.sendTilBeslutter(
            kommando = SendMeldekortTilBeslutterKommando(
                sakId = sakMedOppdatertMeldekortbehandling.id,
                meldekortId = meldekortBehandling.id,
                saksbehandler = saksbehandler,
                dager = dager,
                begrunnelse = begrunnelse,
                correlationId = CorrelationId.generate(),
            ),
            beregn = {
                sakMedOppdatertMeldekortbehandling.beregnMeldekort(
                    meldekortIdSomBeregnes = meldekortBehandling.id,
                    meldeperiodeSomBeregnes = dager.tilMeldekortDager(meldekortBehandling.meldeperiode),
                )
            },
            simuler = { KunneIkkeSimulere.Stengt.left() },
            clock = clock,
        ).getOrFail()

        meldekortRepo.oppdater(meldekortBehandlingTilBeslutning, andreSimuleringMedMetadata)

        sakRepo.hentForSakId(sakMedOpprettetMeldekortBehandling.id)!! to meldekortBehandlingTilBeslutning
    }
}

internal fun TestDataHelper.persisterIverksattMeldekortbehandling(
    sak: Sak? = null,
    beslutter: Saksbehandler = ObjectMother.beslutter(),
    periode: Periode = Periode(
        2.januar(2023),
        15.januar(2023),
    ),
    clock: Clock = this.clock,
    genererSak: (Sak?) -> Pair<Sak, MeldekortBehandling> = { s ->
        this.persisterManuellMeldekortBehandlingTilBeslutning(
            s,
            periode = periode,
            clock = clock,
        )
    },
): Pair<Sak, Meldekortvedtak> {
    val (sakMedMeldekortbehandlingTilBeslutning, meldekortbehandlingTilBeslutning) = genererSak(sak)

    val iverksattMeldekortBehandling =
        (meldekortbehandlingTilBeslutning.taMeldekortBehandling(beslutter) as MeldekortBehandletManuelt)
            .iverksettMeldekort(beslutter, clock).getOrFail()

    val meldekortvedtak = iverksattMeldekortBehandling.opprettVedtak(
        forrigeUtbetaling = sakMedMeldekortbehandlingTilBeslutning.utbetalinger.lastOrNull(),
        clock = clock,
    )

    meldekortRepo.oppdater(iverksattMeldekortBehandling)
    meldekortvedtakRepo.lagre(meldekortvedtak)

    return sakRepo.hentForSakId(sakMedMeldekortbehandlingTilBeslutning.id)!! to meldekortvedtak
}
