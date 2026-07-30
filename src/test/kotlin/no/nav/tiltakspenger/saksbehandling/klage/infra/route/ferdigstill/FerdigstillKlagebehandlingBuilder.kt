package no.nav.tiltakspenger.saksbehandling.klage.infra.route.ferdigstill

import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.KlagebehandlingDTOJson
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.Klageinstanshendelse
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.infra.kafka.GenerererKlageinstanshendelse
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.klageinstanshendelse.mottaHendelseFraKlageinstansen
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterKlagebehandlingBrevtekstForSakId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettKlagebehandlingForSakId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgOpprettholdKlagebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettholdKlagebehandlingForSakId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.vurderKlagebehandlingOpprettholdelseForSakId
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.util.UUID

interface FerdigstillKlagebehandlingBuilder {
    /** 1. Oppretter ny sak
     *  2. Starter klagebehandling til opprettholdelse
     *  3. Oppdaterer brevtekst
     *  4. Opprettholder (emulerer journalføring, distribuering av vedtaksbrev, oversendelse til klageinstansen, og utfall fra klageinstansen)
     *  5. Ferdigstiller klagebehandling
     */
    suspend fun ApplicationTestBuilder.ferdigstiltOpprettholdtKlagebehandling(
        tac: TestApplicationContext,
        fnr: Fnr = ObjectMother.gyldigFnr(),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
        utførJobber: Boolean = true,
        hendelseGenerering: (
            sak: Sak,
            klagebehandling: Klagebehandling,
        ) -> String = { sak, klagebehandling ->
            GenerererKlageinstanshendelse.avsluttetJson(
                eventId = UUID.randomUUID().toString(),
                kildeReferanse = klagebehandling.id.toString(),
                kabalReferanse = UUID.randomUUID().toString(),
                avsluttetTidspunkt = nå(tac.clock).toString(),
                utfall = Klageinstanshendelse.KlagebehandlingAvsluttet.KlagehendelseKlagebehandlingAvsluttetUtfall.STADFESTELSE,
                journalpostReferanser = emptyList(),
            )
        },
    ): Triple<Sak, Klagebehandling, KlagebehandlingDTOJson>? {
        val (sak, klagebehandling) = this.opprettSakOgOpprettholdKlagebehandling(
            tac = tac,
            saksbehandler = saksbehandler,
            fnr = fnr,
        ) ?: return null

        if (utførJobber) {
            tac.mottaHendelseFraKlageinstansen(hendelseGenerering(sak, klagebehandling))
            tac.klagebehandlingContext.knyttKlageinstansHendelseTilKlagebehandlingJobb.knyttHendelser()
        }

        return ferdigstillKlagebehandlingForSakId(
            tac = tac,
            sakId = sak.id,
            klagebehandlingId = klagebehandling.id,
            saksbehandler = saksbehandler,
            forventet = forventet,
        )
    }

    /**
     * Oppretter en klagebehandling på siste vedtaket som finnes på saken som sendes inn
     */
    suspend fun ApplicationTestBuilder.ferdigstiltOpprettholdtKlagebehandlingForSak(
        tac: TestApplicationContext,
        fnr: Fnr = ObjectMother.gyldigFnr(),
        sak: Sak,
        vedtakDetKlagesPå: VedtakId = sak.vedtaksliste.alle.last().id,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
        utførJobber: Boolean = true,
        hendelseGenerering: (
            sak: Sak,
            klagebehandling: Klagebehandling,
        ) -> String = { sak, klagebehandling ->
            GenerererKlageinstanshendelse.avsluttetJson(
                eventId = UUID.randomUUID().toString(),
                kildeReferanse = klagebehandling.id.toString(),
                kabalReferanse = UUID.randomUUID().toString(),
                avsluttetTidspunkt = nå(tac.clock).toString(),
                utfall = Klageinstanshendelse.KlagebehandlingAvsluttet.KlagehendelseKlagebehandlingAvsluttetUtfall.STADFESTELSE,
                journalpostReferanser = emptyList(),
            )
        },
    ): Triple<Sak, Klagebehandling, KlagebehandlingDTOJson>? {
        val (_, opprettetklagebehandling, _) = this.opprettKlagebehandlingForSakId(
            tac = tac,
            sakId = sak.id,
            vedtakDetKlagesPå = vedtakDetKlagesPå,
        )!!
        this.vurderKlagebehandlingOpprettholdelseForSakId(
            tac = tac,
            sakId = sak.id,
            klagebehandlingId = opprettetklagebehandling.id,
        )
        this.oppdaterKlagebehandlingBrevtekstForSakId(
            tac = tac,
            sakId = sak.id,
            klagebehandlingId = opprettetklagebehandling.id,
        )
        this.opprettholdKlagebehandlingForSakId(
            tac = tac,
            sakId = sak.id,
            klagebehandlingId = opprettetklagebehandling.id,
        )

        if (utførJobber) {
            tac.mottaHendelseFraKlageinstansen(hendelseGenerering(sak, opprettetklagebehandling))
            tac.klagebehandlingContext.knyttKlageinstansHendelseTilKlagebehandlingJobb.knyttHendelser()
        }

        return ferdigstillKlagebehandlingForSakId(
            tac = tac,
            sakId = sak.id,
            klagebehandlingId = opprettetklagebehandling.id,
            saksbehandler = saksbehandler,
            forventet = forventet,
        )
    }

    suspend fun ApplicationTestBuilder.ferdigstillKlagebehandlingForSakId(
        tac: TestApplicationContext,
        sakId: SakId,
        klagebehandlingId: KlagebehandlingId,
        begrunnelse: String? = null,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Triple<Sak, Klagebehandling, KlagebehandlingDTOJson>? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler)
        tac.leggTilBruker(jwt, saksbehandler)
        defaultRequestWithAssertions(
            HttpMethod.PATCH,
            "/sak/$sakId/klage/$klagebehandlingId/ferdigstill",
            jwt = jwt,
            forventet = forventet,
            //language=json
            body = """{"begrunnelse": ${begrunnelse?.let { "\"$it\"" }}}""".trimIndent(),
        ).apply {
            val bodyAsText = this.body

            if (statusCode != 200) return null
            val jsonObject: KlagebehandlingDTOJson = objectMapper.readTree(bodyAsText)
            val klagebehandlingId = KlagebehandlingId.fromString(jsonObject.get("id").asString())
            val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sakId)!!

            return Triple(
                oppdatertSak,
                oppdatertSak.hentKlagebehandling(klagebehandlingId),
                jsonObject,
            )
        }
    }
}
