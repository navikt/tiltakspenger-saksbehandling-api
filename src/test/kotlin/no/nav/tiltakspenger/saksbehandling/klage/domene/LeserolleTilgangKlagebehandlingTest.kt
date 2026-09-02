package no.nav.tiltakspenger.saksbehandling.klage.domene

import arrow.core.nonEmptySetOf
import io.kotest.assertions.throwables.shouldThrow
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Hjemmel
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.TilgangException
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.avbryt.AvbruttKlagebehandlingStatus
import no.nav.tiltakspenger.saksbehandling.klage.domene.avbryt.AvbrytKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.avbryt.avbryt
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.Brevtekster
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.KlagebehandlingBrevKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.oppdaterBrevtekst
import no.nav.tiltakspenger.saksbehandling.klage.domene.ferdigstill.FerdigstillKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.ferdigstill.ferdigstill
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.KlageInnsendingskilde
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.OppdaterKlagebehandlingFormkravKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.oppdaterFormkrav
import no.nav.tiltakspenger.saksbehandling.klage.domene.gjenoppta.GjenopptaKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.gjenoppta.gjenopptaKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.iverksett.IverksettAvvisningKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.iverksett.iverksettAvvisning
import no.nav.tiltakspenger.saksbehandling.klage.domene.leggTilbake.LeggTilbakeKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.leggTilbake.leggTilbake
import no.nav.tiltakspenger.saksbehandling.klage.domene.opprett.OpprettKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.opprett.opprett
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppretthold.OpprettholdKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppretthold.oppretthold
import no.nav.tiltakspenger.saksbehandling.klage.domene.overta.OvertaKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.overta.overta
import no.nav.tiltakspenger.saksbehandling.klage.domene.settPåVent.SettKlagebehandlingPåVentKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.settPåVent.settPåVent
import no.nav.tiltakspenger.saksbehandling.klage.domene.settPåVent.settPåVentOgNullstillSaksbehandler
import no.nav.tiltakspenger.saksbehandling.klage.domene.ta.TaKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.ta.ta
import no.nav.tiltakspenger.saksbehandling.klage.domene.vurder.VurderOpprettholdKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.vurder.vurder
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Leseroller (veileder, utvikler) skal ikke kunne mutere en klagebehandling, uavhengig av behandlingens tilstand.
 * Domenet håndhever dette selv, slik at route-guardene ikke er eneste barriere.
 */
class LeserolleTilgangKlagebehandlingTest {

    private val leseroller
        get() = listOf(
            ObjectMother.saksbehandlerUtenTilgang(),
            ObjectMother.veileder(),
            ObjectMother.utvikler(),
        )

    private val klagebehandling get() = ObjectMother.opprettKlagebehandling()

    @Test
    fun `leserolle kan ikke opprette en klagebehandling`() {
        leseroller.forEach { bruker ->
            shouldThrow<TilgangException> {
                Klagebehandling.opprett(
                    saksnummer = ObjectMother.nesteSaksnummer(),
                    fnr = Fnr.random(),
                    opprettet = nå(ObjectMother.clock),
                    journalpostOpprettet = nå(ObjectMother.clock),
                    kommando = OpprettKlagebehandlingKommando(
                        sakId = SakId.random(),
                        saksbehandler = bruker,
                        journalpostId = JournalpostId("journalpostId"),
                        vedtakDetKlagesPå = null,
                        erKlagerPartISaken = true,
                        klagesDetPåKonkreteElementerIVedtaket = true,
                        erKlagefristenOverholdt = true,
                        erUnntakForKlagefrist = null,
                        erKlagenSignert = true,
                        innsendingsdato = LocalDate.of(2026, 2, 16),
                        innsendingskilde = KlageInnsendingskilde.DIGITAL,
                        correlationId = CorrelationId.generate(),
                    ),
                    behandlingDetKlagesPå = null,
                )
            }
        }
    }

    @Test
    fun `leserolle kan ikke ta en klagebehandling`() {
        leseroller.forEach { bruker ->
            shouldThrow<TilgangException> {
                klagebehandling.ta(
                    kommando = TaKlagebehandlingKommando(SakId.random(), KlagebehandlingId.random(), bruker),
                    tilknyttetBehandlingsstatus = null,
                    sistEndret = nå(ObjectMother.clock),
                )
            }
        }
    }

    @Test
    fun `leserolle kan ikke overta en klagebehandling`() {
        leseroller.forEach { bruker ->
            shouldThrow<TilgangException> {
                klagebehandling.overta(
                    kommando = OvertaKlagebehandlingKommando(
                        SakId.random(),
                        KlagebehandlingId.random(),
                        bruker,
                        overtarFra = "Z12345",
                        correlationId = CorrelationId.generate(),
                    ),
                    tilknyttetBehandlingsstatus = null,
                    clock = ObjectMother.clock,
                )
            }
        }
    }

    @Test
    fun `leserolle kan ikke legge tilbake en klagebehandling`() {
        leseroller.forEach { bruker ->
            shouldThrow<TilgangException> {
                klagebehandling.leggTilbake(
                    kommando = LeggTilbakeKlagebehandlingKommando(SakId.random(), KlagebehandlingId.random(), bruker),
                    tilknyttetBehandlingsstatus = null,
                    clock = ObjectMother.clock,
                )
            }
        }
    }

    @Test
    fun `leserolle kan ikke sette en klagebehandling på vent`() {
        leseroller.forEach { bruker ->
            val kommando = SettKlagebehandlingPåVentKommando(
                SakId.random(),
                KlagebehandlingId.random(),
                bruker,
                begrunnelse = "begrunnelse",
                frist = null,
            )
            shouldThrow<TilgangException> { klagebehandling.settPåVent(kommando, ObjectMother.clock) }
            shouldThrow<TilgangException> { klagebehandling.settPåVentOgNullstillSaksbehandler(kommando, ObjectMother.clock) }
        }
    }

    @Test
    fun `leserolle kan ikke gjenoppta en klagebehandling`() {
        leseroller.forEach { bruker ->
            shouldThrow<TilgangException> {
                klagebehandling.gjenopptaKlagebehandling(
                    kommando = GjenopptaKlagebehandlingKommando(
                        SakId.random(),
                        KlagebehandlingId.random(),
                        bruker,
                        correlationId = CorrelationId.generate(),
                    ),
                    clock = ObjectMother.clock,
                )
            }
        }
    }

    @Test
    fun `leserolle kan ikke avbryte en klagebehandling`() {
        leseroller.forEach { bruker ->
            shouldThrow<TilgangException> {
                klagebehandling.avbryt(
                    kommando = AvbrytKlagebehandlingKommando(
                        sakId = SakId.random(),
                        klagebehandlingId = KlagebehandlingId.random(),
                        status = AvbruttKlagebehandlingStatus.KLAGE_TRUKKET,
                        begrunnelse = null,
                        saksbehandler = bruker,
                        correlationId = CorrelationId.generate(),
                    ),
                    clock = ObjectMother.clock,
                )
            }
        }
    }

    @Test
    fun `leserolle kan ikke vurdere en klagebehandling`() {
        leseroller.forEach { bruker ->
            shouldThrow<TilgangException> {
                klagebehandling.vurder(
                    kommando = VurderOpprettholdKlagebehandlingKommando(
                        sakId = SakId.random(),
                        klagebehandlingId = KlagebehandlingId.random(),
                        saksbehandler = bruker,
                        correlationId = CorrelationId.generate(),
                        hjemler = Klagehjemler(nonEmptySetOf(Hjemmel.TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_3)),
                    ),
                    tilknyttetBehandlingsstatus = null,
                    clock = ObjectMother.clock,
                )
            }
        }
    }

    @Test
    fun `leserolle kan ikke oppdatere formkrav på en klagebehandling`() {
        leseroller.forEach { bruker ->
            shouldThrow<TilgangException> {
                klagebehandling.oppdaterFormkrav(
                    kommando = OppdaterKlagebehandlingFormkravKommando(
                        sakId = SakId.random(),
                        klagebehandlingId = KlagebehandlingId.random(),
                        saksbehandler = bruker,
                        journalpostId = JournalpostId("journalpostId"),
                        vedtakDetKlagesPå = null,
                        erKlagerPartISaken = true,
                        klagesDetPåKonkreteElementerIVedtaket = true,
                        erKlagefristenOverholdt = true,
                        erUnntakForKlagefrist = null,
                        erKlagenSignert = true,
                        innsendingsdato = LocalDate.of(2026, 2, 16),
                        innsendingskilde = KlageInnsendingskilde.DIGITAL,
                        correlationId = CorrelationId.generate(),
                    ),
                    journalpostOpprettet = nå(ObjectMother.clock),
                    clock = ObjectMother.clock,
                    behandlingDetKlagesPå = null,
                )
            }
        }
    }

    @Test
    fun `leserolle kan ikke oppdatere brevtekst på en klagebehandling`() {
        leseroller.forEach { bruker ->
            shouldThrow<TilgangException> {
                klagebehandling.oppdaterBrevtekst(
                    kommando = KlagebehandlingBrevKommando(
                        sakId = SakId.random(),
                        klagebehandlingId = KlagebehandlingId.random(),
                        saksbehandler = bruker,
                        correlationId = CorrelationId.generate(),
                        brevtekster = Brevtekster.empty,
                    ),
                    clock = ObjectMother.clock,
                )
            }
        }
    }

    @Test
    fun `leserolle kan ikke opprettholde en klagebehandling`() {
        leseroller.forEach { bruker ->
            shouldThrow<TilgangException> {
                klagebehandling.oppretthold(
                    kommando = OpprettholdKlagebehandlingKommando(
                        sakId = SakId.random(),
                        klagebehandlingId = KlagebehandlingId.random(),
                        tidspunkt = nå(ObjectMother.clock),
                        saksbehandler = bruker,
                        correlationId = CorrelationId.generate(),
                    ),
                )
            }
        }
    }

    @Test
    fun `leserolle kan ikke ferdigstille en klagebehandling`() {
        leseroller.forEach { bruker ->
            shouldThrow<TilgangException> {
                klagebehandling.ferdigstill(
                    kommando = FerdigstillKlagebehandlingKommando(
                        sakId = SakId.random(),
                        klagebehandlingId = KlagebehandlingId.random(),
                        saksbehandler = bruker,
                        begrunnelse = null,
                        correlationId = CorrelationId.generate(),
                    ),
                    tilknyttedeBehandlinger = emptyList(),
                    clock = ObjectMother.clock,
                )
            }
        }
    }

    @Test
    fun `leserolle kan ikke iverksette en avvist klagebehandling`() {
        leseroller.forEach { bruker ->
            shouldThrow<TilgangException> {
                klagebehandling.iverksettAvvisning(
                    kommando = IverksettAvvisningKommando(
                        sakId = SakId.random(),
                        klagebehandlingId = KlagebehandlingId.random(),
                        iverksattTidspunkt = nå(ObjectMother.clock),
                        saksbehandler = bruker,
                        correlationId = CorrelationId.generate(),
                    ),
                )
            }
        }
    }
}
