package no.nav.tiltakspenger.saksbehandling.infra.repo

import arrow.core.nonEmptySetOf
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Hjemmel.TiltakspengeforskriftenHjemmel
import no.nav.tiltakspenger.saksbehandling.distribusjon.DistribusjonId
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagehjemler
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klageinstanshendelser
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.Brevtekster
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.KlagebehandlingBrevKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.TittelOgTekst
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.oppdaterBrevtekst
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.KlagehendelseId
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.Klageinstanshendelse
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppretthold.OpprettholdKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppretthold.oppretthold
import no.nav.tiltakspenger.saksbehandling.klage.domene.settPåVent.SettKlagebehandlingPåVentKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.settPåVent.settPåVent
import no.nav.tiltakspenger.saksbehandling.klage.domene.vurder.VurderOpprettholdKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.vurder.vurder
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer

/**
 * Enkelt case der man kun trenger en sak for å opprette en klagebehandling til avvisning (knyttes ikke til et vedtak).
 * Merk at det ikke persisteres søknad eller søknadsbehandling.
 */
internal fun TestDataHelper.persisterOpprettetKlagebehandlingTilAvvisning(
    sakId: SakId = SakId.random(),
    klagebehandlingId: KlagebehandlingId = KlagebehandlingId.random(),
    saksnummer: Saksnummer = this.saksnummerGenerator.neste(),
    fnr: Fnr = Fnr.random(),
    sak: Sak = ObjectMother.nySak(
        sakId = sakId,
        fnr = fnr,
        saksnummer = saksnummer,
    ),
    erKlagerPartISaken: Boolean = true,
    klagesDetPåKonkreteElementerIVedtaket: Boolean = true,
    erKlagefristenOverholdt: Boolean = true,
    erKlagenSignert: Boolean = true,
): Pair<Sak, Klagebehandling> {
    this.persisterSak(sak = sak)
    val klagebehandling = ObjectMother.opprettKlagebehandling(
        id = klagebehandlingId,
        sakId = sak.id,
        fnr = sak.fnr,
        saksnummer = sak.saksnummer,
        erKlagerPartISaken = erKlagerPartISaken,
        klagesDetPåKonkreteElementerIVedtaket = klagesDetPåKonkreteElementerIVedtaket,
        erKlagefristenOverholdt = erKlagefristenOverholdt,
        erKlagenSignert = erKlagenSignert,
    )
    this.klagebehandlingRepo.lagreKlagebehandling(klagebehandling)

    val oppdatertSak = sakRepo.hentForSakId(sakId)!!
    oppdatertSak.behandlinger.klagebehandlinger.single() shouldBe klagebehandling
    return Pair(
        oppdatertSak,
        klagebehandling,
    )
}

internal fun TestDataHelper.persisterOpprettetKlagebehandlingTilVurdering(
    sakId: SakId = SakId.random(),
    klagebehandlingId: KlagebehandlingId = KlagebehandlingId.random(),
    saksnummer: Saksnummer = this.saksnummerGenerator.neste(),
    fnr: Fnr = Fnr.random(),
    sak: Sak = ObjectMother.nySak(
        sakId = sakId,
        fnr = fnr,
        saksnummer = saksnummer,
    ),
    settPåVent: Boolean = false,
): Pair<Sak, Klagebehandling> {
    val (_, vedtak) = this.persisterIverksattSøknadsbehandling(sak = sak)
    val klagebehandling = ObjectMother.opprettKlagebehandling(
        id = klagebehandlingId,
        sakId = sak.id,
        fnr = sak.fnr,
        saksnummer = sak.saksnummer,
        vedtakDetKlagesPå = vedtak.id,
        erKlagerPartISaken = true,
        klagesDetPåKonkreteElementerIVedtaket = true,
        erKlagefristenOverholdt = true,
        erKlagenSignert = true,
    )
    this.klagebehandlingRepo.lagreKlagebehandling(klagebehandling)

    if (settPåVent) {
        val sattPåVent = klagebehandling.settPåVent(
            kommando = SettKlagebehandlingPåVentKommando(
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                saksbehandler = ObjectMother.saksbehandler(),
                begrunnelse = "persisterOpprettetKlagebehandlingTilVurdering",
                frist = 13.februar(2026),
            ),
            clock = this.clock,
        ).getOrFail()
        this.klagebehandlingRepo.lagreKlagebehandling(sattPåVent)

        val oppdatertSak = sakRepo.hentForSakId(sakId)!!
        oppdatertSak.behandlinger.klagebehandlinger.single() shouldBe sattPåVent

        return Pair(oppdatertSak, sattPåVent)
    }
    val oppdatertSak = sakRepo.hentForSakId(sakId)!!
    oppdatertSak.behandlinger.klagebehandlinger.single() shouldBe klagebehandling

    return Pair(oppdatertSak, klagebehandling)
}

/**
 * Oppdaterer også brevtekst
 */
internal fun TestDataHelper.persisterOpprettholdtKlagebehandling(
    sakId: SakId = SakId.random(),
    klagebehandlingId: KlagebehandlingId = KlagebehandlingId.random(),
    saksnummer: Saksnummer = this.saksnummerGenerator.neste(),
    fnr: Fnr = Fnr.random(),
    saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    sak: Sak = ObjectMother.nySak(
        sakId = sakId,
        fnr = fnr,
        saksnummer = saksnummer,
    ),
    hjemler: Klagehjemler = Klagehjemler(nonEmptySetOf(TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_2)),
): Pair<Sak, Klagebehandling> {
    val (_, vedtak) = this.persisterIverksattSøknadsbehandling(sak = sak)
    val klagebehandling = ObjectMother.opprettKlagebehandling(
        id = klagebehandlingId,
        sakId = sak.id,
        fnr = sak.fnr,
        saksnummer = sak.saksnummer,
        vedtakDetKlagesPå = vedtak.id,
        erKlagerPartISaken = true,
        klagesDetPåKonkreteElementerIVedtaket = true,
        erKlagefristenOverholdt = true,
        erKlagenSignert = true,
        saksbehandler = saksbehandler,
    )
    this.klagebehandlingRepo.lagreKlagebehandling(klagebehandling)

    val opprettholdtKlageMedBrevtekst = klagebehandling.vurder(
        kommando = VurderOpprettholdKlagebehandlingKommando(
            sakId = sakId,
            klagebehandlingId = klagebehandling.id,
            saksbehandler = saksbehandler,
            correlationId = CorrelationId.generate(),
            hjemler = hjemler,
        ),
        rammebehandlingsstatus = null,
        clock = clock,
    ).getOrFail()
        .oppdaterBrevtekst(
            kommando = KlagebehandlingBrevKommando(
                sakId = klagebehandling.sakId,
                klagebehandlingId = klagebehandling.id,
                saksbehandler = saksbehandler,
                correlationId = CorrelationId.generate(),
                brevtekster = Brevtekster(listOf(TittelOgTekst("Brev tittel", "Brev tekst"))),
            ),
            clock = clock,
        ).getOrFail()

    this.klagebehandlingRepo.lagreKlagebehandling(opprettholdtKlageMedBrevtekst)

    val oppdatertSak = sakRepo.hentForSakId(sakId)!!
    oppdatertSak.behandlinger.klagebehandlinger.single() shouldBe opprettholdtKlageMedBrevtekst

    return Pair(oppdatertSak, opprettholdtKlageMedBrevtekst)
}

internal fun TestDataHelper.persisterOversendtKlagebehandling(
    sakId: SakId = SakId.random(),
    klagebehandlingId: KlagebehandlingId = KlagebehandlingId.random(),
    saksnummer: Saksnummer = this.saksnummerGenerator.neste(),
    fnr: Fnr = Fnr.random(),
    saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    sak: Sak = ObjectMother.nySak(
        sakId = sakId,
        fnr = fnr,
        saksnummer = saksnummer,
    ),
): Pair<Sak, Klagebehandling> {
    val (_, klagebehandling) = this.persisterOpprettholdtKlagebehandling(
        sakId = sakId,
        klagebehandlingId = klagebehandlingId,
        saksnummer = saksnummer,
        fnr = fnr,
        saksbehandler = saksbehandler,
        sak = sak,
    )

    val oversendtKlagebehandling = klagebehandling.oppretthold(
        kommando = OpprettholdKlagebehandlingKommando(
            sakId = sakId,
            klagebehandlingId = klagebehandling.id,
            tidspunkt = nå(clock),
            saksbehandler = saksbehandler,
            correlationId = CorrelationId.generate(),
        ),
    ).getOrFail()
        .copy(
            status = Klagebehandlingsstatus.OVERSENDT,
            // simulerer at jobbene er kjørt
            resultat = (klagebehandling.resultat as Klagebehandlingsresultat.Opprettholdt).copy(
                iverksattOpprettholdelseTidspunkt = nå(clock),
                journalpostIdInnstillingsbrev = JournalpostId("journalpostIdInnstillingsbrev"),
                journalføringstidspunktInnstillingsbrev = nå(clock),
                distribusjonIdInnstillingsbrev = DistribusjonId("distribusjonIdInnstillingsbrev"),
                distribusjonstidspunktInnstillingsbrev = nå(clock),
                oversendtKlageinstansenTidspunkt = nå(clock),
            ),
        )

    this.klagebehandlingRepo.lagreKlagebehandling(oversendtKlagebehandling)

    val oppdatertSak = sakRepo.hentForSakId(sakId)!!
    oppdatertSak.behandlinger.klagebehandlinger.single() shouldBe oversendtKlagebehandling

    return Pair(oppdatertSak, oversendtKlagebehandling)
}

internal fun TestDataHelper.persisterOversendtKlagebehandlingMedSvarFraKA(
    sakId: SakId = SakId.random(),
    klagebehandlingId: KlagebehandlingId = KlagebehandlingId.random(),
    saksnummer: Saksnummer = this.saksnummerGenerator.neste(),
    fnr: Fnr = Fnr.random(),
    saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    sak: Sak = ObjectMother.nySak(
        sakId = sakId,
        fnr = fnr,
        saksnummer = saksnummer,
    ),
    klageinstanshendelser: Klageinstanshendelser = Klageinstanshendelser(
        listOf(
            Klageinstanshendelse.KlagebehandlingAvsluttet(
                klagehendelseId = KlagehendelseId.random(),
                klagebehandlingId = klagebehandlingId,
                opprettet = nå(clock),
                sistEndret = nå(clock),
                eksternKlagehendelseId = "eksternKlagehendelseId",
                avsluttetTidspunkt = nå(clock),
                utfall = Klageinstanshendelse.KlagebehandlingAvsluttet.KlagehendelseKlagebehandlingAvsluttetUtfall.STADFESTELSE,
                journalpostreferanser = listOf(),
            ),
        ),
    ),
): Pair<Sak, Klagebehandling> {
    val (_, klagebehandling) = this.persisterOversendtKlagebehandling(
        sakId = sakId,
        klagebehandlingId = klagebehandlingId,
        saksnummer = saksnummer,
        fnr = fnr,
        saksbehandler = saksbehandler,
        sak = sak,
    )

    val oversendtKlagebehandlingMedSvarFraKA = klagebehandling.copy(
        resultat = (klagebehandling.resultat as Klagebehandlingsresultat.Opprettholdt).copy(
            klageinstanshendelser = klageinstanshendelser,
        ),
    )

    this.klagebehandlingRepo.lagreKlagebehandling(oversendtKlagebehandlingMedSvarFraKA)

    val oppdatertSak = sakRepo.hentForSakId(sakId)!!
    oppdatertSak.behandlinger.klagebehandlinger.single() shouldBe oversendtKlagebehandlingMedSvarFraKA

    return Pair(oppdatertSak, oversendtKlagebehandlingMedSvarFraKA)
}
