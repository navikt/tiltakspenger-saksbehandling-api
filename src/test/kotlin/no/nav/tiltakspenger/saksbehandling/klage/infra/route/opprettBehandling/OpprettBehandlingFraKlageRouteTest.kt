package no.nav.tiltakspenger.saksbehandling.klage.infra.route.opprettBehandling

import arrow.core.nonEmptyListOf
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.RammebehandlingResultatTypeDTO
import no.nav.tiltakspenger.saksbehandling.behandling.shouldBeRevurderingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.shouldBeSøknadsbehandlingDTO
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.felles.Ventestatus
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.KlageFormkrav
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.KlageInnsendingskilde
import no.nav.tiltakspenger.saksbehandling.klage.domene.vurder.KlageOmgjøringsårsak
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingType
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.ferdigstiltOpprettholdtKlagebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterOmgjøringInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettBehandlingForKlage
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettMeldekortbehandlingForKlage
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettetRevurderingForKlage
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettetSøknadsbehandlingForKlage
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendRevurderingTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taBehandling
import org.junit.jupiter.api.Test

class OpprettBehandlingFraKlageRouteTest {
    @Test
    fun `kan opprette søknadsbehandling for klagebehandling`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, rammebehandlingMedKlagebehandling, json) = opprettetSøknadsbehandlingForKlage(
                tac = tac,
            )!!
            val klagebehandling = rammebehandlingMedKlagebehandling.klagebehandling!!
            klagebehandling shouldBe Klagebehandling(
                id = klagebehandling.id,
                sakId = sak.id,
                opprettet = klagebehandling.opprettet,
                sistEndret = klagebehandling.sistEndret,
                saksnummer = sak.saksnummer,
                fnr = sak.fnr,
                status = Klagebehandlingsstatus.UNDER_BEHANDLING,
                klagensJournalpostId = JournalpostId("12345"),
                klagensJournalpostOpprettet = klagebehandling.klagensJournalpostOpprettet,
                saksbehandler = "saksbehandlerKlagebehandling",
                resultat = Klagebehandlingsresultat.Omgjør(
                    årsak = KlageOmgjøringsårsak.PROSESSUELL_FEIL,
                    begrunnelse = Begrunnelse.createOrThrow("Begrunnelse for omgjøring"),
                    behandlingId = nonEmptyListOf(rammebehandlingMedKlagebehandling.id),
                    ferdigstiltTidspunkt = null,
                    begrunnelseFerdigstilling = null,
                    åpenBehandlingId = rammebehandlingMedKlagebehandling.id,
                ),
                formkrav = KlageFormkrav(
                    erKlagerPartISaken = true,
                    klagesDetPåKonkreteElementerIVedtaket = true,
                    erKlagefristenOverholdt = true,
                    erKlagenSignert = true,
                    erUnntakForKlagefrist = null,
                    vedtakDetKlagesPå = sak.vedtaksliste.rammevedtaksliste.first().id,
                    behandlingDetKlagesPå = sak.vedtaksliste.rammevedtaksliste.first().rammebehandling.id,
                    innsendingsdato = 16.februar(2026),
                    innsendingskilde = KlageInnsendingskilde.DIGITAL,
                ),
                iverksattTidspunkt = null,
                avbrutt = null,
                ventestatus = Ventestatus(),
            )

            json.toString().`shouldBeSøknadsbehandlingDTO`(
                behandlingId = rammebehandlingMedKlagebehandling.id,
                sakId = sak.id,
                saksnummer = Saksnummer("202505011001"),
                klagebehandlingId = klagebehandling.id,
                søknadId = rammebehandlingMedKlagebehandling.søknad.id,
                rammevedtakId = null,
                status = "UNDER_BEHANDLING",
                iverksattTidspunkt = null,
                beslutter = null,
                resultat = RammebehandlingResultatTypeDTO.IKKE_VALGT,
                vedtaksperiode = null,
                innvilgelsesperiode = false,
                barnetillegg = false,
            )
        }
    }

    @Test
    fun `kan opprette revurdering innvilgelse for klagebehandling`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, rammebehandlingMedKlagebehandling, json) = opprettetRevurderingForKlage(
                tac = tac,
                type = "REVURDERING_INNVILGELSE",
            )!!
            val klagebehandling = rammebehandlingMedKlagebehandling.klagebehandling!!
            klagebehandling shouldBe Klagebehandling(
                id = klagebehandling.id,
                sakId = sak.id,
                opprettet = klagebehandling.opprettet,
                sistEndret = klagebehandling.sistEndret,
                saksnummer = sak.saksnummer,
                fnr = sak.fnr,
                status = Klagebehandlingsstatus.UNDER_BEHANDLING,
                klagensJournalpostId = JournalpostId("12345"),
                klagensJournalpostOpprettet = klagebehandling.klagensJournalpostOpprettet,
                saksbehandler = "saksbehandlerKlagebehandling",
                resultat = Klagebehandlingsresultat.Omgjør(
                    årsak = KlageOmgjøringsårsak.PROSESSUELL_FEIL,
                    begrunnelse = Begrunnelse.createOrThrow("Begrunnelse for omgjøring"),
                    behandlingId = nonEmptyListOf(rammebehandlingMedKlagebehandling.id),
                    ferdigstiltTidspunkt = null,
                    begrunnelseFerdigstilling = null,
                    åpenBehandlingId = rammebehandlingMedKlagebehandling.id,
                ),
                formkrav = KlageFormkrav(
                    erKlagerPartISaken = true,
                    klagesDetPåKonkreteElementerIVedtaket = true,
                    erKlagefristenOverholdt = true,
                    erKlagenSignert = true,
                    erUnntakForKlagefrist = null,
                    vedtakDetKlagesPå = sak.vedtaksliste.rammevedtaksliste.first().id,
                    behandlingDetKlagesPå = sak.vedtaksliste.rammevedtaksliste.first().rammebehandling.id,
                    innsendingsdato = 16.februar(2026),
                    innsendingskilde = KlageInnsendingskilde.DIGITAL,
                ),
                iverksattTidspunkt = null,
                avbrutt = null,
                ventestatus = Ventestatus(),
            )

            json.toString().shouldBeRevurderingDTO(
                behandlingId = rammebehandlingMedKlagebehandling.id,
                status = "UNDER_BEHANDLING",
                sakId = sak.id,
                saksnummer = Saksnummer("202505011001"),
                klagebehandlingId = klagebehandling.id,
                rammevedtakId = null,
                resultat = RammebehandlingResultatTypeDTO.REVURDERING_INNVILGELSE,
                attesteringer = emptyList(),
                iverksattTidspunkt = null,
                beslutter = null,
                vedtaksperiode = null,
                barnetillegg = false,
                innvilgelsesperiode = false,
            )
        }
    }

    @Test
    fun `kan opprette omgjøring for klagebehandling`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, rammebehandlingMedKlagebehandling, json) = opprettetRevurderingForKlage(
                tac = tac,
                type = "REVURDERING_OMGJØRING",
            )!!
            val klagebehandling = rammebehandlingMedKlagebehandling.klagebehandling!!
            klagebehandling shouldBe Klagebehandling(
                id = klagebehandling.id,
                sakId = sak.id,
                opprettet = klagebehandling.opprettet,
                sistEndret = klagebehandling.sistEndret,
                saksnummer = sak.saksnummer,
                fnr = sak.fnr,
                status = Klagebehandlingsstatus.UNDER_BEHANDLING,
                klagensJournalpostId = JournalpostId("12345"),
                klagensJournalpostOpprettet = klagebehandling.klagensJournalpostOpprettet,
                saksbehandler = "saksbehandlerKlagebehandling",
                resultat = Klagebehandlingsresultat.Omgjør(
                    årsak = KlageOmgjøringsårsak.PROSESSUELL_FEIL,
                    begrunnelse = Begrunnelse.createOrThrow("Begrunnelse for omgjøring"),
                    behandlingId = nonEmptyListOf(rammebehandlingMedKlagebehandling.id),
                    ferdigstiltTidspunkt = null,
                    begrunnelseFerdigstilling = null,
                    åpenBehandlingId = rammebehandlingMedKlagebehandling.id,
                ),
                formkrav = KlageFormkrav(
                    erKlagerPartISaken = true,
                    klagesDetPåKonkreteElementerIVedtaket = true,
                    erKlagefristenOverholdt = true,
                    erKlagenSignert = true,
                    erUnntakForKlagefrist = null,
                    vedtakDetKlagesPå = sak.vedtaksliste.rammevedtaksliste.first().id,
                    behandlingDetKlagesPå = sak.vedtaksliste.rammevedtaksliste.first().rammebehandling.id,
                    innsendingsdato = 16.februar(2026),
                    innsendingskilde = KlageInnsendingskilde.DIGITAL,
                ),
                iverksattTidspunkt = null,
                avbrutt = null,
                ventestatus = Ventestatus(),
            )

            json.toString().shouldBeRevurderingDTO(
                behandlingId = rammebehandlingMedKlagebehandling.id,
                status = "UNDER_BEHANDLING",
                sakId = rammebehandlingMedKlagebehandling.sakId,
                saksnummer = Saksnummer("202505011001"),
                klagebehandlingId = klagebehandling.id,
                rammevedtakId = null,
                resultat = RammebehandlingResultatTypeDTO.OMGJØRING_IKKE_VALGT,
                attesteringer = emptyList(),
                iverksattTidspunkt = null,
                beslutter = null,
                vedtaksperiode = null,
                barnetillegg = false,
                innvilgelsesperiode = false,
                omgjørVedtak = sak.vedtaksliste.rammevedtaksliste.first().id,
            )
        }
    }

    @Test
    fun `kan ikke ha 2 åpne rammebehandlinger knyttet til samme klagebehandling`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, rammebehandlingMedKlagebehandling, _) = opprettetSøknadsbehandlingForKlage(
                tac = tac,
            )!!
            val klagebehandling = rammebehandlingMedKlagebehandling.klagebehandling!!
            val søknad = rammebehandlingMedKlagebehandling.søknad
            opprettBehandlingForKlage(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                søknadId = søknad.id,
                vedtakIdSomOmgjøres = null,
                type = "SØKNADSBEHANDLING_INNVILGELSE",
                forventetStatus = HttpStatusCode.BadRequest,
                forventetJsonBody = {
                    """
                        {
                          "melding": "Det finnes allerede en åpen behandling ${rammebehandlingMedKlagebehandling.id} for denne klagebehandlingen.",
                          "kode": "finnes_åpen_behandling"
                        }
                    """.trimIndent()
                },
            )
        }
    }

    @Test
    fun `kan opprette, og vedta rammebehandling for en ferdigstilt klagebehandling `() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, ferdigstiltKlagebehandling, _) = ferdigstiltOpprettholdtKlagebehandling(
                tac = tac,
            )!!
            val saksbehandler = ObjectMother.saksbehandler(ferdigstiltKlagebehandling.saksbehandler!!)
            val (_, opprettetRammebehandling) = opprettBehandlingForKlage(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = ferdigstiltKlagebehandling.id,
                søknadId = null,
                vedtakIdSomOmgjøres = ferdigstiltKlagebehandling.formkrav.vedtakDetKlagesPå!!.toString(),
                type = "REVURDERING_OMGJØRING",
            )!!

            oppdaterOmgjøringInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = opprettetRammebehandling.id as RammebehandlingId,
                saksbehandler = saksbehandler,
                vedtaksperiode = ObjectMother.vedtaksperiode(),
            )
            sendRevurderingTilBeslutningForBehandlingId(
                tac = tac,
                sakId = sak.id,
                behandlingId = opprettetRammebehandling.id as RammebehandlingId,
                saksbehandler = saksbehandler,
            )
            val beslutter = ObjectMother.beslutter()
            taBehandling(
                tac = tac,
                sakId = sak.id,
                behandlingId = opprettetRammebehandling.id as RammebehandlingId,
                saksbehandler = beslutter,
            )
            iverksettForBehandlingId(
                tac = tac,
                sakId = sak.id,
                behandlingId = opprettetRammebehandling.id as RammebehandlingId,
                beslutter = beslutter,
            )!!

            /*
            formålet er å sjekke at hele flyten går ok uten noen exceptions, derfor ingen videre asserts
            Vi har lignende tester med asserts.
             */
        }
    }

    @Test
    fun `kan opprette N antall behandlinger på ferdigstilt klage, så lenge det kun er 1 åpen om gangen`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, ferdigstiltKlagebehandling, _) = ferdigstiltOpprettholdtKlagebehandling(
                tac = tac,
            )!!
            val saksbehandler = ObjectMother.saksbehandler(ferdigstiltKlagebehandling.saksbehandler!!)
            val (_, opprettetRammebehandling) = opprettBehandlingForKlage(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = ferdigstiltKlagebehandling.id,
                søknadId = null,
                vedtakIdSomOmgjøres = ferdigstiltKlagebehandling.formkrav.vedtakDetKlagesPå!!.toString(),
                type = "REVURDERING_OMGJØRING",
            )!!
            // bare en sanity check på at vi får feil
            opprettBehandlingForKlage(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = ferdigstiltKlagebehandling.id,
                søknadId = null,
                vedtakIdSomOmgjøres = ferdigstiltKlagebehandling.formkrav.vedtakDetKlagesPå.toString(),
                type = "REVURDERING_OMGJØRING",
                forventetStatus = HttpStatusCode.BadRequest,
                forventetJsonBody = {
                    """
                        {
                          "melding": "Det finnes allerede en åpen behandling ${opprettetRammebehandling.id} for denne klagebehandlingen.",
                          "kode": "finnes_åpen_behandling"
                        }
                    """.trimIndent()
                },
            )
            val opprinneligvedtaksperiode = sak.vedtaksliste.rammevedtaksliste.single { it.id == ferdigstiltKlagebehandling.formkrav.vedtakDetKlagesPå }
            oppdaterOmgjøringInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = opprettetRammebehandling.id as RammebehandlingId,
                saksbehandler = saksbehandler,
                vedtaksperiode = opprinneligvedtaksperiode.periode,
            )
            sendRevurderingTilBeslutningForBehandlingId(
                tac = tac,
                sakId = sak.id,
                behandlingId = opprettetRammebehandling.id as RammebehandlingId,
                saksbehandler = saksbehandler,
            )
            val beslutter = ObjectMother.beslutter()
            taBehandling(
                tac = tac,
                sakId = sak.id,
                behandlingId = opprettetRammebehandling.id as RammebehandlingId,
                saksbehandler = beslutter,
            )
            val (_, nyttRammevedtak) = iverksettForBehandlingId(
                tac = tac,
                sakId = sak.id,
                behandlingId = opprettetRammebehandling.id as RammebehandlingId,
                beslutter = beslutter,
            )!!

            // andre behandling på ferdigstile klagebehandlingen
            val (_, andreRammebehandling) = opprettBehandlingForKlage(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = ferdigstiltKlagebehandling.id,
                søknadId = null,
                vedtakIdSomOmgjøres = nyttRammevedtak.id.toString(),
                type = "REVURDERING_OMGJØRING",
            )!!

            oppdaterOmgjøringInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = andreRammebehandling.id as RammebehandlingId,
                saksbehandler = saksbehandler,
                vedtaksperiode = nyttRammevedtak.periode,
            )
            sendRevurderingTilBeslutningForBehandlingId(
                tac = tac,
                sakId = sak.id,
                behandlingId = andreRammebehandling.id as RammebehandlingId,
                saksbehandler = saksbehandler,
            )
            val andreRammebehandlingBeslutter = ObjectMother.beslutter()
            taBehandling(
                tac = tac,
                sakId = sak.id,
                behandlingId = andreRammebehandling.id as RammebehandlingId,
                saksbehandler = andreRammebehandlingBeslutter,
            )
            iverksettForBehandlingId(
                tac = tac,
                sakId = sak.id,
                behandlingId = andreRammebehandling.id as RammebehandlingId,
                beslutter = andreRammebehandlingBeslutter,
            )!!
        }
    }

    @Test
    fun `Kan opprette meldekortbehandling for klagebehandling`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, klagebehandling, meldekortbehandling) = opprettMeldekortbehandlingForKlage(
                tac = tac,
            )

            klagebehandling.status shouldBe Klagebehandlingsstatus.UNDER_BEHANDLING
            klagebehandling.saksbehandler shouldBe "saksbehandlerKlagebehandling"

            meldekortbehandling.status shouldBe MeldekortbehandlingStatus.UNDER_BEHANDLING
            meldekortbehandling.type shouldBe MeldekortbehandlingType.KORRIGERING
            meldekortbehandling.sakId shouldBe sak.id
            meldekortbehandling.klagebehandling?.id shouldBe klagebehandling.id
            meldekortbehandling.saksbehandler shouldBe klagebehandling.saksbehandler
        }
    }
}
