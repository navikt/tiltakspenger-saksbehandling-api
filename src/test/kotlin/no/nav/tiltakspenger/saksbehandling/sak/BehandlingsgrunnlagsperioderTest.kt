package no.nav.tiltakspenger.saksbehandling.sak

import arrow.core.nonEmptyListOf
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.periodisering.TomPeriodisering
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.avbrytRammebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.hentEllerOpprettSakForSystembruker
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettOmgjøringInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettOmgjøringOpphør
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgStartRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterSøknadsbehandlingAvslag
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadPåSakId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendSøknadsbehandlingTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startBehandlingAvManueltRegistrertSøknad
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taRammebehandlinger
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseIntern
import org.junit.jupiter.api.Test

/**
 * Tilstanden bygges gjennom prodstien mot Postgres, og saken leses tilbake fra databasen før periodene sjekkes.
 * Hver type søknad, behandling og vedtak er dekket én gang, ikke alle kombinasjonene.
 */
class BehandlingsgrunnlagsperioderTest {
    private val januar = 1.januar(2025) til 31.januar(2025)
    private val februar = 1.februar(2025) til 28.februar(2025)
    private val mars = 1.mars(2025) til 31.mars(2025)
    private val mai = 1.mai(2025) til 31.mai(2025)
    private val januarTilMars = 1.januar(2025) til 31.mars(2025)
    private val søknadsperiode = 1.april(2025) til 10.april(2025)

    @Test
    fun `digital søknad gir tiltaksdeltakelsen det er søkt tiltakspenger for`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak) = opprettSakOgSøknad(tac, tiltaksdeltakelse = tac.tiltaksdeltakelse(januar))

            tac.behandlingsgrunnlagsperioder(sak.id) shouldBe Behandlingsgrunnlagsperioder(nonEmptyListOf(januar))
        }
    }

    @Test
    fun `tilstøtende perioder slås sammen, og hull mellom periodene beholdes`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak) = opprettSakOgSøknad(tac, tiltaksdeltakelse = tac.tiltaksdeltakelse(mai))
            opprettSøknadPåSakId(tac, sak.id, tiltaksdeltakelse = tac.tiltaksdeltakelse(februar))
            opprettSøknadPåSakId(tac, sak.id, tiltaksdeltakelse = tac.tiltaksdeltakelse(januar))

            tac.behandlingsgrunnlagsperioder(sak.id) shouldBe Behandlingsgrunnlagsperioder(
                nonEmptyListOf(1.januar(2025) til 28.februar(2025), mai),
            )
        }
    }

    @Test
    fun `sak uten søknad og papirsøknad uten tiltak gir ingen perioder, mens papirsøknad med tiltak gir tiltaksdeltakelsen`() {
        withTestApplicationContextAndPostgres { tac ->
            val fnr = ObjectMother.gyldigFnr()
            val saksnummer = hentEllerOpprettSakForSystembruker(tac, fnr)
            val deltakelse = tac.tiltaksdeltakelse(februar)
            tac.leggTilPerson(fnr, ObjectMother.personopplysningKjedeligFyr(fnr), deltakelse)
            val sakId = tac.sakContext.sakRepo.hentForSaksnummer(saksnummer)!!.id
            tac.behandlingsgrunnlagsperioder(sakId) shouldBe null

            startBehandlingAvManueltRegistrertSøknad(tac, saksnummer, journalpostId = "papirsøknad-uten-tiltak")
            tac.behandlingsgrunnlagsperioder(sakId) shouldBe null

            startBehandlingAvManueltRegistrertSøknad(
                tac = tac,
                saksnummer = saksnummer,
                journalpostId = "papirsøknad-med-tiltak",
                tiltakJson = deltakelse.somSøknadstiltakJson(),
            )
            tac.behandlingsgrunnlagsperioder(sakId) shouldBe Behandlingsgrunnlagsperioder(nonEmptyListOf(februar))
        }
    }

    @Test
    fun `avbrutt søknad gir fortsatt tiltaksdeltakelsen`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac, tiltaksdeltakelse = tac.tiltaksdeltakelse(januar))

            val (_, avbruttSøknad) = avbrytRammebehandling(tac, sak.saksnummer, sak.id, behandling.id)!!

            avbruttSøknad.erAvbrutt shouldBe true
            tac.behandlingsgrunnlagsperioder(sak.id) shouldBe Behandlingsgrunnlagsperioder(nonEmptyListOf(januar))
        }
    }

    /** Papirsøknaden uten tiltak gir ingen periode, men avslaget får den manuelt satte søknadsperioden som vedtaksperiode. */
    @Test
    fun `avslag på papirsøknad uten tiltak gir vedtaksperioden`() {
        withTestApplicationContextAndPostgres { tac ->
            val fnr = ObjectMother.gyldigFnr()
            val saksnummer = hentEllerOpprettSakForSystembruker(tac, fnr)
            tac.leggTilPerson(fnr, ObjectMother.personopplysningKjedeligFyr(fnr), tac.tiltaksdeltakelse())
            startBehandlingAvManueltRegistrertSøknad(
                tac = tac,
                saksnummer = saksnummer,
                manueltSattSøknadsperiodeJson = """{"fraOgMed": "2025-03-01", "tilOgMed": "2025-03-31"}""",
            )
            val sak = tac.sakContext.sakRepo.hentForSaksnummer(saksnummer)!!
            val behandling = sak.rammebehandlinger.single()
            sak.behandlingsgrunnlagsperioder shouldBe null

            oppdaterSøknadsbehandlingAvslag(tac, sak.id, behandling.id)
            sendSøknadsbehandlingTilBeslutningForBehandlingId(tac, sak.id, behandling.id)
            taRammebehandlinger(tac, behandlinger = listOf(sak.id to behandling.id), saksbehandler = ObjectMother.beslutter())
            val (_, avslag) = iverksettForBehandlingId(tac, sak.id, behandling.id)!!

            avslag.periode shouldBe mars
            tac.behandlingsgrunnlagsperioder(sak.id) shouldBe Behandlingsgrunnlagsperioder(nonEmptyListOf(mars))
        }
    }

    @Test
    fun `revurdering til innvilgelse gir tiltaksdeltakelsen utover vedtaksperioden`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakelse = tac.tiltaksdeltakelse(søknadsperiode)
            val forlenget = deltakelse.copy(deltakelseTilOgMed = 24.april(2025))

            val (sak, _, _, revurderingsvedtak) = iverksettSøknadsbehandlingOgRevurderingInnvilgelse(
                tac = tac,
                søknadsbehandlingInnvilgelsesperioder = innvilgelsesperioder(søknadsperiode, deltakelse),
                revurderingInnvilgelsesperioder = innvilgelsesperioder(1.april(2025) til 17.april(2025), forlenget),
            )

            revurderingsvedtak.periode shouldBe (1.april(2025) til 17.april(2025))
            tac.behandlingsgrunnlagsperioder(sak.id) shouldBe Behandlingsgrunnlagsperioder(
                nonEmptyListOf(1.april(2025) til 24.april(2025)),
            )
        }
    }

    @Test
    fun `revurdering som ikke er vedtatt, utvider ikke periodene`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakelse = tac.tiltaksdeltakelse(søknadsperiode)

            val (sak, _, _, revurdering) = iverksettSøknadsbehandlingOgStartRevurderingInnvilgelse(
                tac = tac,
                søknadsbehandlingInnvilgelsesperioder = innvilgelsesperioder(søknadsperiode, deltakelse),
                oppdatertTiltaksdeltakelse = deltakelse.copy(deltakelseTilOgMed = 24.april(2025)),
            )

            revurdering.saksopplysninger.tiltaksdeltakelser.perioder shouldBe listOf(1.april(2025) til 24.april(2025))
            tac.behandlingsgrunnlagsperioder(sak.id) shouldBe Behandlingsgrunnlagsperioder(nonEmptyListOf(søknadsperiode))
        }
    }

    @Test
    fun `stans krymper ikke periodene`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak) = iverksettSøknadsbehandlingOgRevurderingStans(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(januarTilMars, tac.tiltaksdeltakelse(januarTilMars)),
            )

            val sakMedStans = tac.sakContext.sakRepo.hentForSakId(sak.id)!!
            sakMedStans.rammevedtaksliste.innvilgetTidslinje shouldBe TomPeriodisering.instance()
            sakMedStans.behandlingsgrunnlagsperioder shouldBe Behandlingsgrunnlagsperioder(nonEmptyListOf(januarTilMars))
        }
    }

    @Test
    fun `omgjøring gir tiltaksdeltakelsen utover vedtaksperioden`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakelse = tac.tiltaksdeltakelse(søknadsperiode)
            val (sak, _, søknadsvedtak) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(søknadsperiode, deltakelse),
            )
            val forlenget = deltakelse.copy(deltakelseTilOgMed = 20.april(2025))
            tac.oppdaterTiltaksdeltakelse(sak.fnr, forlenget)

            val (_, omgjøringsvedtak) = iverksettOmgjøringInnvilgelse(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = søknadsvedtak.id,
                innvilgelsesperioder = innvilgelsesperioder(søknadsperiode, forlenget),
            )

            omgjøringsvedtak.periode shouldBe søknadsperiode
            tac.behandlingsgrunnlagsperioder(sak.id) shouldBe Behandlingsgrunnlagsperioder(
                nonEmptyListOf(1.april(2025) til 20.april(2025)),
            )
        }
    }

    @Test
    fun `opphør krymper ikke periodene`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, søknadsvedtak) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(januarTilMars, tac.tiltaksdeltakelse(januarTilMars)),
            )

            iverksettOmgjøringOpphør(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = søknadsvedtak.id,
                vedtaksperiode = januarTilMars,
            )

            val sakMedOpphør = tac.sakContext.sakRepo.hentForSakId(sak.id)!!
            sakMedOpphør.rammevedtaksliste.innvilgetTidslinje shouldBe TomPeriodisering.instance()
            sakMedOpphør.behandlingsgrunnlagsperioder shouldBe Behandlingsgrunnlagsperioder(nonEmptyListOf(januarTilMars))
        }
    }

    @Test
    fun `periodene må være sortert og uten overlapp`() {
        shouldThrow<IllegalArgumentException> { Behandlingsgrunnlagsperioder(nonEmptyListOf(februar, januar)) }
        shouldThrow<IllegalArgumentException> { Behandlingsgrunnlagsperioder(nonEmptyListOf(januar, 15.januar(2025) til 15.februar(2025))) }
    }

    private fun TestApplicationContext.behandlingsgrunnlagsperioder(sakId: SakId): Behandlingsgrunnlagsperioder? =
        sakContext.sakRepo.hentForSakId(sakId)!!.behandlingsgrunnlagsperioder

    private fun TiltaksdeltakelseIntern.somSøknadstiltakJson(): String =
        """
            {
              "eksternDeltakelseId": "$eksternDeltakelseId",
              "deltakelseFraOgMed": "$deltakelseFraOgMed",
              "deltakelseTilOgMed": "$deltakelseTilOgMed",
              "typeKode": "${typeKode.name}",
              "typeNavn": "$typeNavn"
            }
        """.trimIndent()
}
