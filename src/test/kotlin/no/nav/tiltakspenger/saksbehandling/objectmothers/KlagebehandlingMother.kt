package no.nav.tiltakspenger.saksbehandling.objectmothers

import arrow.core.nonEmptySetOf
import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Hjemmel
import no.nav.tiltakspenger.saksbehandling.distribusjon.DistribusjonId
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagehjemler
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.Brevtekster
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.KlagebehandlingBrevKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.TittelOgTekst
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.oppdaterBrevtekst
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.KlageInnsendingskilde
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.KlagefristUnntakSvarord
import no.nav.tiltakspenger.saksbehandling.klage.domene.opprett.OpprettKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.opprett.opprett
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppretthold.OpprettholdKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppretthold.oppretthold
import no.nav.tiltakspenger.saksbehandling.klage.domene.vurder.VurderOpprettholdKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.vurder.vurder
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

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
        erKlagerPartISaken: Boolean = true,
        klagesDetPåKonkreteElementerIVedtaket: Boolean = true,
        erKlagefristenOverholdt: Boolean = true,
        erUnntakForKlagefrist: KlagefristUnntakSvarord? = null,
        erKlagenSignert: Boolean = true,
        innsendingsdato: LocalDate = 16.februar(2026),
        innsendingskilde: KlageInnsendingskilde = KlageInnsendingskilde.DIGITAL,
        correlationId: CorrelationId = CorrelationId.generate(),
    ): Klagebehandling {
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
            )
        }
    }

    fun opprettholdtKlagebehandlingKlarForOversendelse(): Klagebehandling {
        val clock = TikkendeKlokke()
        val correlationId: CorrelationId = CorrelationId.generate()
        val opprettetKlagebehandling = opprettKlagebehandling(vedtakDetKlagesPå = VedtakId.random(), clock = clock, correlationId = correlationId)
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
            .oppdaterInnstillingsbrevJournalpost(JournalpostId("journalpostId"), nå)
            .oppdaterInnstillingsbrevDistribusjon(DistribusjonId("distribusjonId"), nå)
    }
}
