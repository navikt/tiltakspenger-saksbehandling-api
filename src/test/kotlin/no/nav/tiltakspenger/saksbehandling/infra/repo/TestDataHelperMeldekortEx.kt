package no.nav.tiltakspenger.saksbehandling.infra.repo

import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.right
import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.NonBlankString.Companion.toNonBlankString
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.brukersmeldekort.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortBehandletAutomatiskStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingManuell
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdater.OppdaterMeldekortbehandlingKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdater.oppdaterMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.opprettManuellMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.tilBeslutter.SendMeldekortbehandlingTilBeslutterKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortvedtak.Meldekortvedtak
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortvedtak.opprettVedtak
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.Meldeperiode
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksbehandler
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

internal fun TestDataHelper.persisterKlarTilBehandlingManuellMeldekortbehandling(
    sak: Sak? = null,
    saksbehandler: Saksbehandler = saksbehandler(),
    kjedeId: MeldeperiodeKjedeId? = null,
    periode: Periode = Periode(
        2.januar(2023),
        15.januar(2023),
    ),
    navkontor: Navkontor = Navkontor("0012", "Kontor TestDataHelper.persisterManuellMeldekortbehandling"),
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

    val (_, manuellMeldekortbehandling) = generertSak.opprettManuellMeldekortbehandling(
        kjedeId = kjedeId ?: generertSak.meldeperiodeKjeder.first().kjedeId,
        navkontor = navkontor,
        saksbehandler = saksbehandler,
        clock = clock,
    ).getOrFail()

    this.meldekortRepo.lagre(manuellMeldekortbehandling, null)

    return this.sakRepo.hentForSakId(manuellMeldekortbehandling.sakId)!! to manuellMeldekortbehandling
}

/**
 * OBS: Prøver å avslutte siste meldekortbehandling i [sak], hvis den er i tilstanden [MeldekortUnderBehandling].
 * (enklere å begynne med :) )
 */
internal fun TestDataHelper.persisterAvsluttetMeldekortbehandling(
    sak: Sak? = null,
    saksbehandler: Saksbehandler = saksbehandler(),
    periode: Periode = Periode(
        2.januar(2023),
        15.januar(2023),
    ),
    begrunnelse: String = "TestDataHelper.persisterAvsluttetMeldekortbehandling",
    clock: Clock = this.clock,
    genererSak: (Sak?) -> Sak = { s ->
        s ?: this.persisterKlarTilBehandlingManuellMeldekortbehandling(
            s,
            periode = periode,
            clock = clock,
        ).first
    },
): Pair<Sak, Meldekortbehandling> {
    val generertSak = genererSak(sak)
    val meldekortbehandling = generertSak.meldekortbehandlinger
        .filterIsInstance<MeldekortUnderBehandling>()
        .last { it.meldeperiode.periode == periode }

    val avbruttMeldekortbehandling = meldekortbehandling.avbryt(
        avbruttAv = saksbehandler,
        begrunnelse = begrunnelse.toNonBlankString(),
        tidspunkt = LocalDateTime.now(clock),
    ).getOrFail()

    this.meldekortRepo.oppdater(avbruttMeldekortbehandling, null, null)

    return this.sakRepo.hentForSakId(avbruttMeldekortbehandling.sakId)!! to avbruttMeldekortbehandling
}

internal fun TestDataHelper.persisterManuellMeldekortbehandlingTilBeslutning(
    sak: Sak? = null,
    saksbehandler: Saksbehandler = saksbehandler(),
    periode: Periode = Periode(
        2.januar(2023),
        15.januar(2023),
    ),
    clock: Clock = this.clock,
    genererSak: (Sak?) -> Pair<Sak, MeldekortUnderBehandling> = { s ->
        this.persisterKlarTilBehandlingManuellMeldekortbehandling(
            s,
            periode = periode,
            clock = clock,
        )
    },
): Pair<Sak, Meldekortbehandling> {
    val (sakMedOpprettetMeldekortbehandling, opprettetMeldekortbehandling) = genererSak(sak)
    val dager = saksbehandlerFyllerUtMeldeperiodeDager(opprettetMeldekortbehandling.meldeperiode)
    val begrunnelse = Begrunnelse.create("TestDataHelper.persisterManuellMeldekortbehandlingTilBeslutning")

    return runBlocking {
        val (sakMedOppdatertMeldekortbehandling, meldekortbehandling, simuleringMedMetadata) = sakMedOpprettetMeldekortbehandling.oppdaterMeldekort(
            kommando = ObjectMother.oppdaterMeldekortKommando(
                sakId = sakMedOpprettetMeldekortbehandling.id,
                meldekortId = opprettetMeldekortbehandling.id,
                saksbehandler = saksbehandler,
                begrunnelse = begrunnelse,
                correlationId = CorrelationId.generate(),
                dager = dager,
            ),
            simuler = { KunneIkkeSimulere.Stengt.left() },
            clock = clock,
        ).getOrFail()

        meldekortRepo.oppdater(meldekortbehandling, simuleringMedMetadata)

        val meldekortbehandlingTilBeslutning = meldekortbehandling.sendTilBeslutter(
            kommando = SendMeldekortbehandlingTilBeslutterKommando(
                sakId = sakMedOppdatertMeldekortbehandling.id,
                meldekortId = meldekortbehandling.id,
                saksbehandler = saksbehandler,
                correlationId = CorrelationId.generate(),
            ),
            clock = clock,
        ).getOrFail()

        meldekortRepo.oppdater(meldekortbehandlingTilBeslutning)

        sakRepo.hentForSakId(sakMedOpprettetMeldekortbehandling.id)!! to meldekortbehandlingTilBeslutning
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
    genererSak: (Sak?) -> Pair<Sak, Meldekortbehandling> = { s ->
        this.persisterManuellMeldekortbehandlingTilBeslutning(
            s,
            periode = periode,
            clock = clock,
        )
    },
): Pair<Sak, Meldekortvedtak> {
    val (sakMedMeldekortbehandlingTilBeslutning, meldekortbehandlingTilBeslutning) = genererSak(sak)

    val iverksattMeldekortbehandling =
        (meldekortbehandlingTilBeslutning.taMeldekortbehandling(beslutter, clock) as MeldekortbehandlingManuell)
            .iverksettMeldekort(beslutter, clock).getOrFail()

    val meldekortvedtak = iverksattMeldekortbehandling.opprettVedtak(
        forrigeUtbetaling = sakMedMeldekortbehandlingTilBeslutning.utbetalinger.lastOrNull(),
        clock = clock,
    )

    meldekortRepo.oppdater(iverksattMeldekortbehandling)
    meldekortvedtakRepo.lagre(meldekortvedtak)

    return sakRepo.hentForSakId(sakMedMeldekortbehandlingTilBeslutning.id)!! to meldekortvedtak
}

internal fun TestDataHelper.persisterOppdatertMeldekortbehandling(
    id: MeldekortId? = null,
    behandling: Meldekortbehandling? = id?.let { meldekortRepo.hent(it) },
    dager: OppdaterMeldekortbehandlingKommando.OppdatertMeldeperiode? = null,
    begrunnelse: Begrunnelse? = null,
    fritekstTilVedtaksbrev: FritekstTilVedtaksbrev? = null,
    skalSendeVedtaksbrev: Boolean = true,
): Pair<Sak, Meldekortbehandling> {
    requireNotNull(behandling) {
        "Meldekortbehandling eller gyldig meldekortbehandling id må spesifiseres"
    }

    val sakId = behandling.sakId
    val sak = sakRepo.hentForSakId(sakId)!!

    val (_, oppdatertBehandling) = runBlocking {
        sak.oppdaterMeldekort(
            OppdaterMeldekortbehandlingKommando(
                sakId = sakId,
                meldekortId = behandling.id,
                saksbehandler = saksbehandler(navIdent = behandling.saksbehandler!!),
                meldeperioder = nonEmptyListOf(dager ?: saksbehandlerFyllerUtMeldeperiodeDager(behandling.meldeperiode)),
                begrunnelse = begrunnelse,
                fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
                correlationId = CorrelationId.generate(),
                skalSendeVedtaksbrev = skalSendeVedtaksbrev,
            ),
            simuler = { ObjectMother.simuleringMedMetadata().right() },
            clock = clock,
        ).getOrFail()
    }

    meldekortRepo.oppdater(oppdatertBehandling)

    return sakRepo.hentForSakId(sakId)!! to oppdatertBehandling
}
