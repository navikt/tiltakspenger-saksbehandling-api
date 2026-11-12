package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.start

import arrow.core.Tuple5
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.StartRevurderingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.RevurderingsresultatType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.SøknadsbehandlingsresultatType
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContextMedPostgres
import no.nav.tiltakspenger.saksbehandling.infra.route.RammebehandlingDTOJson
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.tilStartRevurderingTypeDTO
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseIntern
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.AutomatiskOpprettetRevurderingGrunn
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http.TiltaksdeltakelseFakeTestKlient
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakerEndring
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.medPeriode
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import org.json.JSONObject

interface StartRevurderingBuilder {

    /**
     * Iverksetter en innvilgelse og lar jobben for endringer i tiltaksdeltakelse opprette en stans når deltakelsen avbrytes.
     * Den automatiske saksbehandleren fyller deretter ut stansen og sender den til beslutning.
     * Krever postgres, fordi jobben for endringer i tiltaksdeltakelse ikke har fakes.
     */
    suspend fun ApplicationTestBuilder.opprettAutomatiskStansKlarTilBeslutning(
        tac: TestApplicationContextMedPostgres,
    ): Pair<Sak, Revurdering> {
        val deltakelse = tac.tiltaksdeltakelse(5.januar(2025) til 5.mai(2025))
        val (sak) = iverksettSøknadsbehandling(
            tac = tac,
            tiltaksdeltakelse = deltakelse,
            innvilgelsesperioder = innvilgelsesperioder(deltakelse.lukketPeriode!!, deltakelse),
        )
        tac.oppdaterTiltaksdeltakelse(
            sak.fnr,
            deltakelse.medPeriode(tilOgMed = 20.april(2025)).copy(deltakelseStatus = TiltakDeltakerstatus.Avbrutt),
        )
        tac.tiltakContext.tiltaksdeltakerRepo.registrerUbehandletEndring(
            id = deltakelse.internDeltakelseId,
            sakId = sak.id,
            tidspunkt = nå(tac.clock).minusMinutes(20),
        )
        tac.oppdatertTiltaksdeltakelseJobb.håndterUbehandledeEndringer()
        val revurderingId = tac.sakContext.sakRepo.hentForSakId(sak.id)!!.rammebehandlinger.last().id
        tac.delautomatiskRevurderingJobb.automatiskBehandleRevurdering(revurderingId)
        val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sak.id)!!
        val revurdering = oppdatertSak.hentRammebehandling(revurderingId).shouldBeInstanceOf<Revurdering>()
        revurdering.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
        revurdering.automatiskSaksbehandlet shouldBe true
        return oppdatertSak to revurdering
    }

    /**
     * Iverksetter en innvilgelse og lar systemet opprette en ubestemt revurdering på saken.
     * Ubestemte revurderinger har ingen route, så de opprettes gjennom servicen slik systemet gjør det.
     */
    suspend fun ApplicationTestBuilder.iverksettSøknadsbehandlingOgOpprettUbestemtRevurdering(
        tac: TestApplicationContext,
        endring: TiltaksdeltakerEndring = TiltaksdeltakerEndring.AvbruttDeltakelse,
    ): Pair<Sak, Revurdering> {
        val (sak) = iverksettSøknadsbehandling(tac)
        val tiltaksdeltakerId = sak.tiltaksdeltakelserDetErSøktTiltakspengerFor.single().søknadstiltak.tiltaksdeltakerId
        return tac.behandlingContext.startRevurderingService.startRevurdering(
            StartRevurderingKommando.Ubestemt(
                sakId = sak.id,
                correlationId = CorrelationId.generate(),
                automatiskOpprettetGrunn = AutomatiskOpprettetRevurderingGrunn(endring, tiltaksdeltakerId),
            ),
        ).getOrNull()!!
    }

    /** Oppretter ny sak, søknad, innvilget søknadsbehandling og revurdering. */
    suspend fun ApplicationTestBuilder.iverksettSøknadsbehandlingOgStartRevurderingStans(
        tac: TestApplicationContext,
        sakId: SakId? = null,
        fnr: Fnr = Fnr.random(),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        beslutter: Saksbehandler = ObjectMother.beslutter(),
        innvilgelsesperioder: Innvilgelsesperioder = innvilgelsesperioder(),
        barnetillegg: Barnetillegg = Barnetillegg.utenBarnetillegg(innvilgelsesperioder.perioder),
        // Utledes fra innvilgelsesperiodene slik at flyten registrerer samme deltakelse som innvilges; en frisk deltakelse ville fått ny id.
        tiltaksdeltakelse: TiltaksdeltakelseIntern = innvilgelsesperioder.valgteTiltaksdeltagelser.verdier.flatten().distinct().single(),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Tuple5<Sak, Søknad, Rammevedtak, Revurdering, RammebehandlingDTOJson> {
        val (sak, søknad, søknadsbehandling) = iverksettSøknadsbehandling(
            tac = tac,
            innvilgelsesperioder = innvilgelsesperioder,
            barnetillegg = barnetillegg,
            tiltaksdeltakelse = tiltaksdeltakelse,
            fnr = fnr,
            sakId = sakId,
            saksbehandler = saksbehandler,
            beslutter = beslutter,
        )
        val (oppdatertSak, revurdering, jsonResponse) = startRevurderingForSakId(
            tac = tac,
            sakId = sak.id,
            type = RevurderingsresultatType.STANS,
            saksbehandler = saksbehandler,
            forventet = forventet,
        )!!
        return Tuple5(
            oppdatertSak,
            søknad,
            søknadsbehandling,
            revurdering,
            jsonResponse,
        )
    }

    /** Oppretter ny sak, søknad, innvilget søknadsbehandling og revurdering. */
    suspend fun ApplicationTestBuilder.startRevurderingStans(
        tac: TestApplicationContext,
        sakId: SakId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Triple<Sak, Revurdering, RammebehandlingDTOJson>? {
        return startRevurderingForSakId(
            tac = tac,
            sakId = sakId,
            type = RevurderingsresultatType.STANS,
            saksbehandler = saksbehandler,
            forventet = forventet,
        )
    }

    /** Oppretter ny sak, søknad, innvilget søknadsbehandling og revurdering. */
    suspend fun ApplicationTestBuilder.iverksettSøknadsbehandlingOgStartRevurderingInnvilgelse(
        tac: TestApplicationContext,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        beslutter: Saksbehandler = ObjectMother.beslutter(),
        søknadsbehandlingInnvilgelsesperioder: Innvilgelsesperioder = innvilgelsesperioder(),
        // Default: revurderingen gjelder samme deltakelse som innvilgelsen.
        // Send inn en kopi med endret periode for å teste endret deltakelse.
        oppdatertTiltaksdeltakelse: TiltaksdeltakelseIntern? = søknadsbehandlingInnvilgelsesperioder.valgteTiltaksdeltagelser.verdier.flatten().distinct().single(),
        fnr: Fnr = Fnr.random(),
        sakId: SakId? = null,
    ): Tuple5<Sak, Søknad, Rammevedtak, Revurdering, RammebehandlingDTOJson> {
        val (sak, søknad, rammevedtakSøknadsbehandling) = iverksettSøknadsbehandling(
            tac,
            fnr = fnr,
            beslutter = beslutter,
            resultat = SøknadsbehandlingsresultatType.INNVILGELSE,
            sakId = sakId,
            innvilgelsesperioder = søknadsbehandlingInnvilgelsesperioder,
        )

        val tiltaksdeltakelseFakeKlient = tac.tiltakContext.tiltaksdeltakelseKlient as TiltaksdeltakelseFakeTestKlient

        tiltaksdeltakelseFakeKlient.lagre(
            sak.fnr,
            oppdatertTiltaksdeltakelse,
        )

        val (oppdatertSak, revurdering, jsonResponse) = startRevurderingForSakId(
            tac,
            sak.id,
            RevurderingsresultatType.INNVILGELSE,
        )!!

        return Tuple5(
            oppdatertSak,
            søknad,
            rammevedtakSøknadsbehandling,
            revurdering,
            jsonResponse,
        )
    }

    /**
     * Starter en ny revurdering til innvilgelse på [sakId]
     * Merk at denne ikke oppretter sak, søknad eller søknadsbehandling.
     * */
    suspend fun ApplicationTestBuilder.startRevurderingInnvilgelse(
        tac: TestApplicationContext,
        sakId: SakId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Triple<Sak, Revurdering, RammebehandlingDTOJson>? {
        return startRevurderingForSakId(
            tac = tac,
            sakId = sakId,
            type = RevurderingsresultatType.INNVILGELSE,
            saksbehandler = saksbehandler,
            rammevedtakIdSomOmgjøres = null,
            forventet = forventet,
        )
    }

    /**
     * Oppretter ny sak, søknad, innvilget søknadsbehandling og revurdering til omgjøring.
     * Default: Tiltaksdeltakelsen har endret seg fra 1. til 3. april.
     * @param [oppdatertTiltaksdeltakelse] Dersom null, fjernes den for dette fødselsnummeret.
     * */
    suspend fun ApplicationTestBuilder.iverksettSøknadsbehandlingOgStartRevurderingOmgjøring(
        tac: TestApplicationContext,
        sakId: SakId? = null,
        fnr: Fnr = Fnr.random(),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        beslutter: Saksbehandler = ObjectMother.beslutter(),
        søknadsbehandlingInnvilgelsesperioder: Innvilgelsesperioder = innvilgelsesperioder(),
        oppdatertTiltaksdeltakelse: TiltaksdeltakelseIntern? = søknadsbehandlingInnvilgelsesperioder.periodisering.first().verdi.valgteTiltaksdeltakelser.single(),
        forventetForStartRevurdering: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Tuple5<Sak, Søknad, Rammevedtak, Revurdering, RammebehandlingDTOJson>? {
        val (sak, søknad, rammevedtakSøknadsbehandling) = iverksettSøknadsbehandling(
            tac = tac,
            beslutter = beslutter,
            saksbehandler = saksbehandler,
            sakId = sakId,
            fnr = fnr,
            innvilgelsesperioder = søknadsbehandlingInnvilgelsesperioder,
        )

        tac.oppdaterTiltaksdeltakelse(sak.fnr, oppdatertTiltaksdeltakelse)

        val (oppdatertSak, revurdering, jsonResponse) = startRevurderingForSakId(
            tac = tac,
            sakId = sak.id,
            type = RevurderingsresultatType.OMGJØRING_INNVILGELSE,
            rammevedtakIdSomOmgjøres = sak.rammevedtaksliste.single().id,
            forventet = forventetForStartRevurdering,
        ) ?: return null

        return Tuple5(
            oppdatertSak,
            søknad,
            rammevedtakSøknadsbehandling,
            revurdering,
            jsonResponse,
        )
    }

    /**
     * Starter en ny revurdering til omgjøring på [sakId]
     * Merk at denne ikke oppretter sak, søknad eller søknadsbehandling.
     * */
    suspend fun ApplicationTestBuilder.startRevurderingOmgjøring(
        tac: TestApplicationContext,
        sakId: SakId,
        rammevedtakIdSomOmgjøres: VedtakId?,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Triple<Sak, Revurdering, RammebehandlingDTOJson>? {
        return startRevurderingForSakId(
            tac = tac,
            sakId = sakId,
            type = RevurderingsresultatType.OMGJØRING_INNVILGELSE,
            rammevedtakIdSomOmgjøres = rammevedtakIdSomOmgjøres,
            saksbehandler = saksbehandler,
            forventet = forventet,
        )
    }

    /** Starter en forlengelse av [rammevedtakIdSomForlenges] på [sakId]. */
    suspend fun ApplicationTestBuilder.startForlengelse(
        tac: TestApplicationContext,
        sakId: SakId,
        rammevedtakIdSomForlenges: VedtakId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Triple<Sak, Revurdering, RammebehandlingDTOJson>? {
        return startRevurderingForSakId(
            tac = tac,
            sakId = sakId,
            type = RevurderingsresultatType.FORLENGELSE,
            saksbehandler = saksbehandler,
            rammevedtakIdSomForlenges = rammevedtakIdSomForlenges,
            forventet = forventet,
        )
    }

    /** Forventer at det allerede finnes en sak og søknad. */
    suspend fun ApplicationTestBuilder.startRevurderingForSakId(
        tac: TestApplicationContext,
        sakId: SakId,
        type: RevurderingsresultatType,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        rammevedtakIdSomOmgjøres: VedtakId? = null,
        rammevedtakIdSomForlenges: VedtakId? = null,
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Triple<Sak, Revurdering, RammebehandlingDTOJson>? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler)
        tac.leggTilBruker(jwt, saksbehandler)
        defaultRequestWithAssertions(
            HttpMethod.POST,
            "/sak/$sakId/revurdering/start",
            jwt = jwt,
            forventet = forventet,
            body =
            """
                {
                "revurderingType": "${type.tilStartRevurderingTypeDTO()}", 
                "rammevedtakIdSomOmgjøres": ${if (rammevedtakIdSomOmgjøres != null) """"$rammevedtakIdSomOmgjøres"""" else null},
                "rammevedtakIdSomForlenges": ${if (rammevedtakIdSomForlenges != null) """"$rammevedtakIdSomForlenges"""" else null}
                }
            """.trimIndent(),
        )
            .apply {
                val bodyAsText = this.body

                if (statusCode != 200) return null
                val jsonObject: RammebehandlingDTOJson = JSONObject(bodyAsText)
                val revurderingId = RammebehandlingId.fromString(jsonObject.getString("id"))
                val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sakId)!!
                return Triple(
                    oppdatertSak,
                    tac.behandlingContext.rammebehandlingRepo.hent(revurderingId) as Revurdering,
                    jsonObject,
                )
            }
    }
}
