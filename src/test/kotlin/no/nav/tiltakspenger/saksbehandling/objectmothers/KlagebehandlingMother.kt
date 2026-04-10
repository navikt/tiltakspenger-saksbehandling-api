package no.nav.tiltakspenger.saksbehandling.objectmothers

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import arrow.core.nonEmptySetOf
import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Hjemmel
import no.nav.tiltakspenger.saksbehandling.distribusjon.DistribusjonId
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.journalpost.DokumentInfoId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagehjemler
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klageinstanshendelser
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.Brevtekster
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.KlagebehandlingBrevKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.TittelOgTekst
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.oppdaterBrevtekst
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.KlageInnsendingskilde
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.KlagefristUnntakSvarord
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.KlagehendelseId
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.Klageinstanshendelse
import no.nav.tiltakspenger.saksbehandling.klage.domene.opprett.OpprettKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.opprett.opprett
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppretthold.OpprettholdKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppretthold.oppretthold
import no.nav.tiltakspenger.saksbehandling.klage.domene.vurder.KlageOmgjøringsårsak
import no.nav.tiltakspenger.saksbehandling.klage.domene.vurder.VurderOpprettholdKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.vurder.vurder
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

interface KlagebehandlingMother : MotherOfAllMothers {
    fun opprettKlagebehandling(
        clock: Clock = this.clock,
        id: KlagebehandlingId = KlagebehandlingId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(1.januar(2024), "1234"),
        fnr: Fnr = Fnr.random(),
        saksbehandler: Saksbehandler = saksbehandler(),
        opprettet: LocalDateTime = LocalDateTime.now(clock),
        journalpostOpprettet: LocalDateTime = LocalDateTime.now(clock),
        journalpostId: JournalpostId = JournalpostId("journalpostId"),
        vedtakDetKlagesPå: VedtakId? = null,
        behandlingDetKlagesPå: BehandlingId? = null,
        erKlagerPartISaken: Boolean = true,
        klagesDetPåKonkreteElementerIVedtaket: Boolean = true,
        erKlagefristenOverholdt: Boolean = true,
        erUnntakForKlagefrist: KlagefristUnntakSvarord? = null,
        erKlagenSignert: Boolean = true,
        innsendingsdato: LocalDate = 16.februar(2026),
        innsendingskilde: KlageInnsendingskilde = KlageInnsendingskilde.DIGITAL,
        correlationId: CorrelationId = CorrelationId.generate(),
    ): Klagebehandling {
        if (behandlingDetKlagesPå != null || vedtakDetKlagesPå != null) {
            require(vedtakDetKlagesPå != null && behandlingDetKlagesPå != null) {
                "vedtakDetKlagesPå og behandlingDetKlagesPå må begge være null eller satt. sakId: $sakId, klagebehandlingId: $id"
            }
        }
        return runBlocking {
            Klagebehandling.opprett(
                id = id,
                saksnummer = saksnummer,
                fnr = fnr,
                opprettet = opprettet,
                journalpostOpprettet = journalpostOpprettet,
                kommando = OpprettKlagebehandlingKommando(
                    sakId = sakId,
                    saksbehandler = saksbehandler,
                    journalpostId = journalpostId,
                    vedtakDetKlagesPå = vedtakDetKlagesPå,
                    erKlagerPartISaken = erKlagerPartISaken,
                    klagesDetPåKonkreteElementerIVedtaket = klagesDetPåKonkreteElementerIVedtaket,
                    erKlagefristenOverholdt = erKlagefristenOverholdt,
                    erUnntakForKlagefrist = erUnntakForKlagefrist,
                    erKlagenSignert = erKlagenSignert,
                    innsendingsdato = innsendingsdato,
                    innsendingskilde = innsendingskilde,
                    correlationId = correlationId,
                ),
                behandlingDetKlagesPå = behandlingDetKlagesPå,
            )
        }
    }

    fun opprettholdtKlagebehandlingKlarForOversendelse(
        innstillingsbrevJournalpostId: JournalpostId = JournalpostId("journalpostId"),
        dokumentInfoIder: NonEmptyList<DokumentInfoId> = nonEmptyListOf(DokumentInfoId("dokumentInfoId")),
    ): Klagebehandling {
        val clock = TikkendeKlokke()
        val correlationId: CorrelationId = CorrelationId.generate()
        val opprettetKlagebehandling = opprettKlagebehandling(
            vedtakDetKlagesPå = VedtakId.random(),
            behandlingDetKlagesPå = BehandlingId.random(),
            clock = clock,
            correlationId = correlationId,
        )
        val vurdertKlagebehandling = opprettetKlagebehandling.vurder(
            kommando = VurderOpprettholdKlagebehandlingKommando(
                sakId = opprettetKlagebehandling.sakId,
                klagebehandlingId = opprettetKlagebehandling.id,
                saksbehandler = saksbehandler(opprettetKlagebehandling.saksbehandler!!),
                correlationId = correlationId,
                hjemler = Klagehjemler(nonEmptySetOf(Hjemmel.TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_3)),
            ),
            rammebehandlingsstatus = null,
            clock = clock,
        ).getOrFail()

        val medBrev = vurdertKlagebehandling.oppdaterBrevtekst(
            kommando = KlagebehandlingBrevKommando(
                sakId = opprettetKlagebehandling.sakId,
                klagebehandlingId = opprettetKlagebehandling.id,
                saksbehandler = saksbehandler(opprettetKlagebehandling.saksbehandler),
                correlationId = correlationId,
                brevtekster = Brevtekster(listOf(TittelOgTekst("Tittel", "Tekst"))),
            ),
            clock = clock,
        ).getOrFail()
        val nå = nå(clock)
        val iverksattOpprettholdt = medBrev.oppretthold(
            OpprettholdKlagebehandlingKommando(
                sakId = opprettetKlagebehandling.sakId,
                klagebehandlingId = opprettetKlagebehandling.id,
                saksbehandler = saksbehandler(opprettetKlagebehandling.saksbehandler),
                correlationId = correlationId,
                tidspunkt = nå,
            ),
        ).getOrFail()
        return iverksattOpprettholdt
            .oppdaterInnstillingsbrevJournalpost(
                LocalDate.now(fixedClock),
                innstillingsbrevJournalpostId,
                dokumentInfoIder,
                nå,
            )
            .oppdaterInnstillingsbrevDistribusjon(DistribusjonId("distribusjonId"), nå)
    }

    fun klagebehandlingresultatAvvist(
        brevtekster: Brevtekster? = null,
    ): Klagebehandlingsresultat.Avvist {
        return Klagebehandlingsresultat.Avvist(brevtekst = brevtekster)
    }

    fun klagebehandlingresultatOmgjør(
        årsak: KlageOmgjøringsårsak = KlageOmgjøringsårsak.FEIL_LOVANVENDELSE,
        begrunnelse: Begrunnelse = Begrunnelse.create("klagebehandlingresultatOmgjørt")!!,
        rammebehandlingId: List<BehandlingId> = emptyList(),
        åpenRammebehandlingId: BehandlingId? = null,
    ): Klagebehandlingsresultat.Omgjør {
        return Klagebehandlingsresultat.Omgjør(
            årsak = KlageOmgjøringsårsak.FEIL_LOVANVENDELSE,
            begrunnelse = begrunnelse,
            rammebehandlingId = rammebehandlingId,
            ferdigstiltTidspunkt = null,
            begrunnelseFerdigstilling = null,
            åpenRammebehandlingId = åpenRammebehandlingId,
        )
    }

    fun klagebehandlingresultatOpprettholdt(
        hjemler: Klagehjemler = Klagehjemler(nonEmptySetOf(Hjemmel.TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_3)),
        brevtekst: Brevtekster? = null,
        iverksattOpprettholdelseTidspunkt: LocalDateTime? = null,
        brevdato: LocalDate? = null,
        journalpostIdInnstillingsbrev: JournalpostId? = null,
        dokumentInfoIder: List<DokumentInfoId> = emptyList(),
        journalføringstidspunktInnstillingsbrev: LocalDateTime? = null,
        distribusjonIdInnstillingsbrev: DistribusjonId? = null,
        distribusjonstidspunktInnstillingsbrev: LocalDateTime? = null,
        oversendtKlageinstansenTidspunkt: LocalDateTime? = null,
        klageinstanshendelser: Klageinstanshendelser = Klageinstanshendelser(listOf(klageinstanshendelseAvsluttet())),
        ferdigstiltTidspunkt: LocalDateTime? = null,
        begrunnelseFerdigstilling: Begrunnelse? = null,
        rammebehandlingId: List<BehandlingId> = emptyList(),
        åpenRammebehandlingId: BehandlingId? = null,
    ): Klagebehandlingsresultat.Opprettholdt {
        return Klagebehandlingsresultat.Opprettholdt(
            hjemler = hjemler,
            brevtekst = brevtekst,
            iverksattOpprettholdelseTidspunkt = iverksattOpprettholdelseTidspunkt,
            brevdato = brevdato,
            journalpostIdInnstillingsbrev = journalpostIdInnstillingsbrev,
            journalføringstidspunktInnstillingsbrev = journalføringstidspunktInnstillingsbrev,
            dokumentInfoIder = dokumentInfoIder,
            distribusjonIdInnstillingsbrev = distribusjonIdInnstillingsbrev,
            distribusjonstidspunktInnstillingsbrev = distribusjonstidspunktInnstillingsbrev,
            oversendtKlageinstansenTidspunkt = oversendtKlageinstansenTidspunkt,
            klageinstanshendelser = klageinstanshendelser,
            ferdigstiltTidspunkt = ferdigstiltTidspunkt,
            rammebehandlingId = rammebehandlingId,
            begrunnelseFerdigstilling = begrunnelseFerdigstilling,
            åpenRammebehandlingId = åpenRammebehandlingId,
        )
    }

    fun klageinstanshendelseAvsluttet(
        clock: Clock = this.clock,
        klagehendelseId: KlagehendelseId = KlagehendelseId.random(),
        klagebehandlingId: KlagebehandlingId = KlagebehandlingId.random(),
        opprettet: LocalDateTime = LocalDateTime.now(clock),
        sistEndret: LocalDateTime = LocalDateTime.now(clock),
        eksternKlagehendelseId: String = UUID.randomUUID().toString(),
        avsluttetTidspunkt: LocalDateTime = LocalDateTime.now(clock),
        utfall: Klageinstanshendelse.KlagebehandlingAvsluttet.KlagehendelseKlagebehandlingAvsluttetUtfall = Klageinstanshendelse.KlagebehandlingAvsluttet.KlagehendelseKlagebehandlingAvsluttetUtfall.STADFESTELSE,
        journalpostreferanser: List<JournalpostId> = emptyList(),
    ): Klageinstanshendelse.KlagebehandlingAvsluttet {
        return Klageinstanshendelse.KlagebehandlingAvsluttet(
            klagehendelseId = klagehendelseId,
            klagebehandlingId = klagebehandlingId,
            opprettet = opprettet,
            sistEndret = sistEndret,
            eksternKlagehendelseId = eksternKlagehendelseId,
            avsluttetTidspunkt = avsluttetTidspunkt,
            utfall = utfall,
            journalpostreferanser = journalpostreferanser,
        )
    }

    fun klageinstanshendelseOmgjøringskrav(
        clock: Clock = this.clock,
        klagehendelseId: KlagehendelseId = KlagehendelseId.random(),
        klagebehandlingId: KlagebehandlingId = KlagebehandlingId.random(),
        opprettet: LocalDateTime = LocalDateTime.now(clock),
        sistEndret: LocalDateTime = LocalDateTime.now(clock),
        eksternKlagehendelseId: String = UUID.randomUUID().toString(),
        avsluttetTidspunkt: LocalDateTime = LocalDateTime.now(clock),
        utfall: Klageinstanshendelse.OmgjøringskravbehandlingAvsluttet.OmgjøringskravbehandlingAvsluttetUtfall = Klageinstanshendelse.OmgjøringskravbehandlingAvsluttet.OmgjøringskravbehandlingAvsluttetUtfall.MEDHOLD_ETTER_FVL_35,
        journalpostreferanser: List<JournalpostId> = emptyList(),
    ): Klageinstanshendelse.OmgjøringskravbehandlingAvsluttet {
        return Klageinstanshendelse.OmgjøringskravbehandlingAvsluttet(
            klagehendelseId = klagehendelseId,
            klagebehandlingId = klagebehandlingId,
            opprettet = opprettet,
            sistEndret = sistEndret,
            eksternKlagehendelseId = eksternKlagehendelseId,
            journalpostreferanser = journalpostreferanser,
            avsluttetTidspunkt = avsluttetTidspunkt,
            utfall = utfall,
        )
    }

    fun klageinstanshendelseFeilregistrert(
        clock: Clock = this.clock,
        klagehendelseId: KlagehendelseId = KlagehendelseId.random(),
        klagebehandlingId: KlagebehandlingId = KlagebehandlingId.random(),
        opprettet: LocalDateTime = LocalDateTime.now(clock),
        sistEndret: LocalDateTime = LocalDateTime.now(clock),
        eksternKlagehendelseId: String = UUID.randomUUID().toString(),
        feilregistrertTidspunkt: LocalDateTime = LocalDateTime.now(clock),
        årsak: String = "Feilregistrert årsak",
        navIdent: String = "Z123456",
        type: Klageinstanshendelse.BehandlingFeilregistrert.KlagehendelseFeilregistrertType = Klageinstanshendelse.BehandlingFeilregistrert.KlagehendelseFeilregistrertType.KLAGE,
    ): Klageinstanshendelse.BehandlingFeilregistrert {
        return Klageinstanshendelse.BehandlingFeilregistrert(
            klagehendelseId = klagehendelseId,
            klagebehandlingId = klagebehandlingId,
            opprettet = opprettet,
            sistEndret = sistEndret,
            eksternKlagehendelseId = eksternKlagehendelseId,
            feilregistrertTidspunkt = feilregistrertTidspunkt,
            årsak = årsak,
            navIdent = navIdent,
            type = type,
        )
    }
}
