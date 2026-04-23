package no.nav.tiltakspenger.saksbehandling.klage.infra.route.opprettbehandling

import arrow.core.nonEmptyListOf
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
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
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOpprettBehandlingForKlage
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterOmgjøringInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettBehandlingForKlage
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgFerdigstillOppretholdtKlagebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendRevurderingTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taBehandling
import org.junit.jupiter.api.Test

class OpprettBehandlingFraKlageRouteTest {
    @Test
    fun `kan opprette søknadsbehandling for klagebehandling`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, rammebehandlingMedKlagebehandling, json) = iverksettSøknadsbehandlingOgOpprettBehandlingForKlage(
                tac = tac,
            )!!
            val klagebehandling = rammebehandlingMedKlagebehandling.klagebehandling!!
            rammebehandlingMedKlagebehandling as Søknadsbehandling
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
                    tilknyttetBehandlingId = nonEmptyListOf(rammebehandlingMedKlagebehandling.id),
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

            json.toString().shouldEqualJson(
                """{
                    "behandlingId": "${rammebehandlingMedKlagebehandling.id}",
                    "sakId": "${sak.id}",
                    "type": "${OpprettetBehandlingFraKlageDto.Companion.Type.RAMMEBEHANDLING}"
                }
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `kan opprette revurdering innvilgelse for klagebehandling`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, rammebehandlingMedKlagebehandling, json) = iverksettSøknadsbehandlingOgOpprettBehandlingForKlage(
                tac = tac,
                type = "REVURDERING_INNVILGELSE",
            )!!
            val klagebehandling = rammebehandlingMedKlagebehandling.klagebehandling!!
            rammebehandlingMedKlagebehandling as Revurdering
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
                    tilknyttetBehandlingId = nonEmptyListOf(rammebehandlingMedKlagebehandling.id),
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

            json.toString().shouldEqualJson(
                """{
                  "behandlingId": "${rammebehandlingMedKlagebehandling.id}",   
                    "sakId": "${sak.id}",
                    "type": "${OpprettetBehandlingFraKlageDto.Companion.Type.RAMMEBEHANDLING}"
                }
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `kan opprette omgjøring for klagebehandling`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, rammebehandlingMedKlagebehandling, json) = iverksettSøknadsbehandlingOgOpprettBehandlingForKlage(
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
                    tilknyttetBehandlingId = nonEmptyListOf(rammebehandlingMedKlagebehandling.id),
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

            json.toString().shouldEqualJson(
                """{
                  "behandlingId": "${rammebehandlingMedKlagebehandling.id}",   
                    "sakId": "${sak.id}",
                    "type": "${OpprettetBehandlingFraKlageDto.Companion.Type.RAMMEBEHANDLING}"
                }
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `kan opprette meldekortbehandling for klagebehandling`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, meldekortbehandlingMedKlagebheandling, opprettetBehandlingJson) = iverksettSøknadsbehandlingOgOpprettBehandlingForKlage(
                tac = tac,
                type = "MELDEKORTBEHANDLING",
            )!!

            val klagebehandling = meldekortbehandlingMedKlagebheandling.klagebehandling!!
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
                    tilknyttetBehandlingId = nonEmptyListOf(meldekortbehandlingMedKlagebheandling.id),
                    ferdigstiltTidspunkt = null,
                    begrunnelseFerdigstilling = null,
                    åpenBehandlingId = meldekortbehandlingMedKlagebheandling.id,
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

            opprettetBehandlingJson.toString().shouldEqualJson(
                """{
                  "behandlingId": "${meldekortbehandlingMedKlagebheandling.id}",   
                    "sakId": "${sak.id}",
                    "type": "${OpprettetBehandlingFraKlageDto.Companion.Type.MELDEKORTBEHANDLING}"
                }
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `kan ikke ha 2 åpne rammebehandlinger knyttet til samme klagebehandling`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, rammebehandlingMedKlagebehandling, _) = iverksettSøknadsbehandlingOgOpprettBehandlingForKlage(
                tac = tac,
            )!!
            val klagebehandling = rammebehandlingMedKlagebehandling.klagebehandling!!
            val søknad = (rammebehandlingMedKlagebehandling as Søknadsbehandling).søknad
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
                          "melding": "Det finnes allerede en åpen rammebehandling ${rammebehandlingMedKlagebehandling.id} for denne klagebehandlingen.",
                          "kode": "finnes_åpen_rammebehandling"
                        }
                    """.trimIndent()
                },
            )
        }
    }

    @Test
    fun `kan opprette, og vedta rammebehandling for en ferdigstilt klagebehandling `() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, ferdigstiltKlagebehandling, _) = opprettSakOgFerdigstillOppretholdtKlagebehandling(
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
            val (sak, ferdigstiltKlagebehandling, _) = opprettSakOgFerdigstillOppretholdtKlagebehandling(
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
                          "melding": "Det finnes allerede en åpen rammebehandling ${opprettetRammebehandling.id} for denne klagebehandlingen.",
                          "kode": "finnes_åpen_rammebehandling"
                        }
                    """.trimIndent()
                },
            )
            val opprinneligvedtaksperiode =
                sak.vedtaksliste.rammevedtaksliste.single { it.id == ferdigstiltKlagebehandling.formkrav.vedtakDetKlagesPå }
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
}
