package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlinger
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseIntern
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakerEndring.AvbruttDeltakelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakerEndring.EndretDeltakelsesmengde
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakerEndring.EndretSluttdato
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakerEndring.EndretStartdato
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakerEndring.EndretStatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakerEndring.Forlengelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakerEndring.IkkeAktuellDeltakelse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import java.time.Clock
import java.time.Duration
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SakFinnEndringerTest {
    private val clock = fixedClock
    private val iDag = LocalDate.now(clock)
    private val kjentTilstand = ObjectMother.tiltaksdeltakelse(
        fom = iDag.minusMonths(2),
        tom = iDag.plusMonths(1),
        dagerPrUke = 2F,
        prosent = 50F,
    )

    @Test
    fun `uendret deltakelse gir ingen endringer`() {
        finnEndringerPåSak(kjentTilstand, kjentTilstand).shouldBeNull()
    }

    @Test
    fun `ukjent deltakelse gir ingen endringer selv om verdiene er endret`() {
        val sak = sakMedKjentTilstand(kjentTilstand)

        sak.finnEndringer(
            tiltaksdeltakerId = TiltaksdeltakerId.random(),
            nåtilstand = kjentTilstand.copy(deltakelseProsent = 60F),
            clock = clock,
        ).shouldBeNull()
    }

    @Test
    fun `nyere åpen manuell behandling brukes som sammenligningsgrunnlag fremfor eldre vedtak`() {
        val (sak, _, vedtattBehandling) = ObjectMother.nySakMedVedtak(
            vedtaksperiode = kjentTilstand.periode!!,
            clock = clock,
        )
        val vedtattTilstand = vedtattBehandling.saksopplysninger.tiltaksdeltakelser.single()
        val nyereTilstand = vedtattTilstand.copy(deltakelseProsent = 60F, antallDagerPerUke = 1F)
        val oppdatertSak = sak.medNyereÅpenBehandling(nyereTilstand)

        oppdatertSak.finnEndringer(vedtattTilstand.internDeltakelseId, nyereTilstand, clock).shouldBeNull()
        oppdatertSak.finnEndringer(vedtattTilstand.internDeltakelseId, vedtattTilstand, clock)?.toList() shouldBe
            listOf(EndretDeltakelsesmengde(vedtattTilstand.deltakelseProsent, vedtattTilstand.antallDagerPerUke))
    }

    @Test
    fun `nyere automatisk behandling overstyrer ikke sammenligningsgrunnlaget fra vedtak`() {
        val (sak, _, vedtattBehandling) = ObjectMother.nySakMedVedtak(
            vedtaksperiode = kjentTilstand.periode!!,
            clock = clock,
        )
        val vedtattTilstand = vedtattBehandling.saksopplysninger.tiltaksdeltakelser.single()
        val nyereTilstand = vedtattTilstand.copy(deltakelseProsent = 60F, antallDagerPerUke = 1F)
        val oppdatertSak = sak.medNyereÅpenBehandling(nyereTilstand, automatisk = true)

        oppdatertSak.rammebehandlinger.last().erUnderAutomatiskBehandling shouldBe true
        oppdatertSak.finnEndringer(vedtattTilstand.internDeltakelseId, vedtattTilstand, clock).shouldBeNull()
        oppdatertSak.finnEndringer(vedtattTilstand.internDeltakelseId, nyereTilstand, clock)?.toList() shouldBe
            listOf(EndretDeltakelsesmengde(60F, 1F))
    }

    @Test
    fun `nyere manuell behandling for en annen deltakelse ignoreres ved valg av sammenligningsgrunnlag`() {
        val (sak, _, vedtattBehandling) = ObjectMother.nySakMedVedtak(
            vedtaksperiode = kjentTilstand.periode!!,
            clock = clock,
        )
        val vedtattTilstand = vedtattBehandling.saksopplysninger.tiltaksdeltakelser.single()
        val annenDeltakelse = ObjectMother.tiltaksdeltakelse(
            periode = kjentTilstand.periode!!,
            prosent = 60F,
            dagerPrUke = 1F,
        )
        val oppdatertSak = sak.medNyereÅpenBehandling(annenDeltakelse)
        val nåtilstand = vedtattTilstand.copy(deltakelseProsent = 60F, antallDagerPerUke = 1F)

        oppdatertSak.finnEndringer(vedtattTilstand.internDeltakelseId, vedtattTilstand, clock).shouldBeNull()
        oppdatertSak.finnEndringer(vedtattTilstand.internDeltakelseId, nåtilstand, clock)?.toList() shouldBe
            listOf(EndretDeltakelsesmengde(60F, 1F))
    }

    @ParameterizedTest
    @CsvSource(
        "50, 1",
        "60, 2",
        "60, 1",
    )
    fun `endret prosent eller dager gir nøyaktig den nye deltakelsesmengden`(
        prosent: Float,
        dager: Float,
    ) {
        val nåtilstand = kjentTilstand.copy(deltakelseProsent = prosent, antallDagerPerUke = dager)

        finnEndringerPåSak(kjentTilstand, nåtilstand) shouldBe listOf(EndretDeltakelsesmengde(prosent, dager))
    }

    @ParameterizedTest
    @CsvSource(
        "50, 1",
        "60, 2",
        "60, 1",
    )
    fun `endret prosent eller dager kommer før forlengelse`(
        prosent: Float,
        dager: Float,
    ) {
        val nySluttdato = kjentTilstand.deltakelseTilOgMed!!.plusMonths(1)
        val nåtilstand = kjentTilstand.copy(
            deltakelseProsent = prosent,
            antallDagerPerUke = dager,
            deltakelseTilOgMed = nySluttdato,
        )

        finnEndringerPåSak(kjentTilstand, nåtilstand) shouldBe listOf(
            EndretDeltakelsesmengde(prosent, dager),
            Forlengelse(nySluttdato),
        )
    }

    @ParameterizedTest
    @CsvSource(
        "null, 2, 0, 2",
        "0, 2, null, 2",
        "null, 2, null, 2",
        "50, null, 50, 0",
        "50, 0, 50, null",
        "50, null, 50, null",
        "null, null, null, null",
        "null, null, 0, 0",
        "0, 0, null, null",
        "0, 0, 0, 0",
        nullValues = ["null"],
    )
    fun `null og 0 regnes som samme deltakelsesmengde i begge retninger og når begge mangler`(
        gammelProsent: Float?,
        gamleDager: Float?,
        nyProsent: Float?,
        nyeDager: Float?,
    ) {
        val kjent = kjentTilstand.copy(deltakelseProsent = gammelProsent, antallDagerPerUke = gamleDager)
        val nåtilstand = kjent.copy(deltakelseProsent = nyProsent, antallDagerPerUke = nyeDager)

        finnEndringerPåSak(kjent, nåtilstand).shouldBeNull()
    }

    @ParameterizedTest
    @CsvSource(
        "null, 2, 50, 2",
        "50, 2, null, 2",
        "50, null, 50, 2",
        "50, 2, 50, null",
        "null, null, 50, 2",
        "50, 2, null, null",
        nullValues = ["null"],
    )
    fun `null er forskjellig fra en deltakelsesmengde større enn 0`(
        gammelProsent: Float?,
        gamleDager: Float?,
        nyProsent: Float?,
        nyeDager: Float?,
    ) {
        val kjent = kjentTilstand.copy(deltakelseProsent = gammelProsent, antallDagerPerUke = gamleDager)
        val nåtilstand = kjent.copy(deltakelseProsent = nyProsent, antallDagerPerUke = nyeDager)

        finnEndringerPåSak(kjent, nåtilstand) shouldBe listOf(EndretDeltakelsesmengde(nyProsent, nyeDager))
    }

    @Test
    fun `senere sluttdato med samme startdato er en forlengelse`() {
        val nySluttdato = kjentTilstand.deltakelseTilOgMed!!.plusDays(1)

        finnEndringerPåSak(
            kjentTilstand,
            kjentTilstand.copy(deltakelseTilOgMed = nySluttdato),
        ) shouldBe listOf(Forlengelse(nySluttdato))
    }

    @Test
    fun `forlengelse fra i dag med statusendring til deltar gir bare forlengelse`() {
        val kjent = kjentTilstand.copy(
            deltakelseFraOgMed = iDag,
            deltakelseStatus = TiltakDeltakerstatus.VenterPåOppstart,
        )
        val nySluttdato = kjent.deltakelseTilOgMed!!.plusDays(1)
        val nåtilstand = kjent.copy(
            deltakelseTilOgMed = nySluttdato,
            deltakelseStatus = TiltakDeltakerstatus.Deltar,
        )

        finnEndringerPåSak(kjent, nåtilstand) shouldBe listOf(Forlengelse(nySluttdato))
    }

    @ParameterizedTest
    @ValueSource(longs = [-1, 0])
    fun `avkorting til før eller på dagens dato er avbrutt selv uten statusendring`(dagerFraIDag: Long) {
        val nåtilstand = kjentTilstand.copy(deltakelseTilOgMed = iDag.plusDays(dagerFraIDag))

        finnEndringerPåSak(kjentTilstand, nåtilstand) shouldBe listOf(AvbruttDeltakelse)
    }

    @Test
    fun `avkorting til fremtiden gir endret sluttdato`() {
        val nySluttdato = iDag.plusDays(1)

        finnEndringerPåSak(
            kjentTilstand,
            kjentTilstand.copy(deltakelseTilOgMed = nySluttdato),
        ) shouldBe listOf(EndretSluttdato(nySluttdato))
    }

    @ParameterizedTest
    @ValueSource(longs = [-1, 0])
    fun `åpen deltakelse får sluttdato før eller på dagens dato og blir avbrutt`(dagerFraIDag: Long) {
        val kjent = kjentTilstand.copy(deltakelseTilOgMed = null)
        val nåtilstand = kjent.copy(deltakelseTilOgMed = iDag.plusDays(dagerFraIDag))

        finnEndringerPåSak(kjent, nåtilstand) shouldBe listOf(AvbruttDeltakelse)
    }

    @Test
    fun `åpen deltakelse får sluttdato i fremtiden og er ikke en forlengelse`() {
        val kjent = kjentTilstand.copy(deltakelseTilOgMed = null)
        val nySluttdato = iDag.plusDays(1)

        finnEndringerPåSak(kjent, kjent.copy(deltakelseTilOgMed = nySluttdato)) shouldBe
            listOf(EndretSluttdato(nySluttdato))
    }

    @Test
    fun `fjernet sluttdato gir endret sluttdato med null`() {
        finnEndringerPåSak(
            kjentTilstand,
            kjentTilstand.copy(deltakelseTilOgMed = null),
        ) shouldBe listOf(EndretSluttdato(null))
    }

    @Test
    fun `begge mangler sluttdato og gir ingen endringer`() {
        val kjent = kjentTilstand.copy(deltakelseTilOgMed = null)

        finnEndringerPåSak(kjent, kjent).shouldBeNull()
    }

    @Test
    fun `forlengelse av en utløpt deltakelse er ikke avbrutt selv om ny sluttdato også er i fortiden`() {
        val kjent = kjentTilstand.copy(deltakelseTilOgMed = iDag.minusDays(2))
        val nySluttdato = iDag.minusDays(1)

        finnEndringerPåSak(kjent, kjent.copy(deltakelseTilOgMed = nySluttdato)) shouldBe
            listOf(Forlengelse(nySluttdato))
    }

    @ParameterizedTest
    @ValueSource(longs = [-1, 1])
    fun `endret startdato gir den nye startdatoen`(dagerFraGammelStart: Long) {
        val nyStartdato = kjentTilstand.deltakelseFraOgMed!!.plusDays(dagerFraGammelStart)

        finnEndringerPåSak(
            kjentTilstand,
            kjentTilstand.copy(deltakelseFraOgMed = nyStartdato),
        ) shouldBe listOf(EndretStartdato(nyStartdato))
    }

    @Test
    fun `fjernet startdato gir endret startdato med null`() {
        finnEndringerPåSak(
            kjentTilstand,
            kjentTilstand.copy(deltakelseFraOgMed = null),
        ) shouldBe listOf(EndretStartdato(null))
    }

    @Test
    fun `manglende startdato erstattes med en dato`() {
        val kjent = kjentTilstand.copy(deltakelseFraOgMed = null)

        finnEndringerPåSak(kjent, kjent.copy(deltakelseFraOgMed = iDag)) shouldBe listOf(EndretStartdato(iDag))
    }

    @Test
    fun `begge mangler startdato og gir ingen endringer`() {
        val kjent = kjentTilstand.copy(deltakelseFraOgMed = null)

        finnEndringerPåSak(kjent, kjent).shouldBeNull()
    }

    @Test
    fun `senere sluttdato og endret startdato er to datoendringer og ikke forlengelse`() {
        val nyStartdato = kjentTilstand.deltakelseFraOgMed!!.plusDays(1)
        val nySluttdato = kjentTilstand.deltakelseTilOgMed!!.plusDays(1)
        val nåtilstand = kjentTilstand.copy(
            deltakelseFraOgMed = nyStartdato,
            deltakelseTilOgMed = nySluttdato,
        )

        finnEndringerPåSak(kjentTilstand, nåtilstand) shouldBe listOf(
            EndretStartdato(nyStartdato),
            EndretSluttdato(nySluttdato),
        )
    }

    @ParameterizedTest
    @CsvSource(
        "Fullført, -1",
        "Fullført, 0",
        "HarSluttet, -1",
        "HarSluttet, 0",
    )
    fun `forventet avslutning før eller på dagens dato gir ingen endringer`(
        nyStatus: TiltakDeltakerstatus,
        dagerFraIDag: Long,
    ) {
        val kjent = kjentTilstand.copy(deltakelseTilOgMed = iDag.plusDays(dagerFraIDag))

        finnEndringerPåSak(kjent, kjent.copy(deltakelseStatus = nyStatus)).shouldBeNull()
    }

    @ParameterizedTest
    @EnumSource(TiltakDeltakerstatus::class, names = ["Fullført", "HarSluttet"])
    fun `avsluttet status før sluttdato gir endret status`(nyStatus: TiltakDeltakerstatus) {
        val kjent = kjentTilstand.copy(deltakelseTilOgMed = iDag.plusDays(1))

        finnEndringerPåSak(kjent, kjent.copy(deltakelseStatus = nyStatus)) shouldBe listOf(EndretStatus(nyStatus))
    }

    @ParameterizedTest
    @EnumSource(TiltakDeltakerstatus::class, names = ["Fullført", "HarSluttet"])
    fun `avsluttet status uten sluttdato gir endret status`(nyStatus: TiltakDeltakerstatus) {
        val kjent = kjentTilstand.copy(deltakelseTilOgMed = null)

        finnEndringerPåSak(kjent, kjent.copy(deltakelseStatus = nyStatus)) shouldBe listOf(EndretStatus(nyStatus))
    }

    @ParameterizedTest
    @EnumSource(TiltakDeltakerstatus::class, names = ["Fullført", "HarSluttet"])
    fun `forventet avslutning skjuler ikke samtidig endret deltakelsesmengde`(nyStatus: TiltakDeltakerstatus) {
        val kjent = kjentTilstand.copy(deltakelseTilOgMed = iDag)
        val nåtilstand = kjent.copy(deltakelseStatus = nyStatus, antallDagerPerUke = 1F)

        finnEndringerPåSak(kjent, nåtilstand) shouldBe listOf(
            EndretDeltakelsesmengde(50F, 1F),
            EndretStatus(nyStatus),
        )
    }

    @Test
    fun `kun statusendring fra venter på oppstart til deltar gir endret status`() {
        val kjent = kjentTilstand.copy(
            deltakelseFraOgMed = iDag.minusDays(1),
            deltakelseStatus = TiltakDeltakerstatus.VenterPåOppstart,
        )

        finnEndringerPåSak(kjent, kjent.copy(deltakelseStatus = TiltakDeltakerstatus.Deltar)) shouldBe
            listOf(EndretStatus(TiltakDeltakerstatus.Deltar))
    }

    @Test
    fun `kun statusendring til avbrutt gir avbrutt deltakelse`() {
        finnEndringerPåSak(
            kjentTilstand,
            kjentTilstand.copy(deltakelseStatus = TiltakDeltakerstatus.Avbrutt),
        ) shouldBe listOf(AvbruttDeltakelse)
    }

    @Test
    fun `kun statusendring til ikke aktuell gir ikke aktuell deltakelse`() {
        finnEndringerPåSak(
            kjentTilstand,
            kjentTilstand.copy(deltakelseStatus = TiltakDeltakerstatus.IkkeAktuell),
        ) shouldBe listOf(IkkeAktuellDeltakelse)
    }

    @ParameterizedTest
    @EnumSource(TiltakDeltakerstatus::class, names = ["Avbrutt", "IkkeAktuell"])
    fun `uendret avslutningsstatus overstyrer ikke en endret deltakelsesmengde`(status: TiltakDeltakerstatus) {
        val kjent = kjentTilstand.copy(deltakelseStatus = status)

        finnEndringerPåSak(kjent, kjent.copy(antallDagerPerUke = 1F)) shouldBe
            listOf(EndretDeltakelsesmengde(50F, 1F))
    }

    @Test
    fun `flere samtidige endringer returneres i rekkefølgen mengde startdato sluttdato status`() {
        val nyStartdato = kjentTilstand.deltakelseFraOgMed!!.plusDays(1)
        val nySluttdato = iDag.plusDays(1)
        val nåtilstand = kjentTilstand.copy(
            deltakelseProsent = 60F,
            antallDagerPerUke = 1F,
            deltakelseFraOgMed = nyStartdato,
            deltakelseTilOgMed = nySluttdato,
            deltakelseStatus = TiltakDeltakerstatus.HarSluttet,
        )

        finnEndringerPåSak(kjentTilstand, nåtilstand) shouldBe listOf(
            EndretDeltakelsesmengde(60F, 1F),
            EndretStartdato(nyStartdato),
            EndretSluttdato(nySluttdato),
            EndretStatus(TiltakDeltakerstatus.HarSluttet),
        )
    }

    @Test
    fun `statusendring til avbrutt prioriteres foran mengde og datoendringer`() {
        val nåtilstand = kjentTilstand.copy(
            deltakelseProsent = 60F,
            antallDagerPerUke = 1F,
            deltakelseFraOgMed = null,
            deltakelseTilOgMed = kjentTilstand.deltakelseTilOgMed!!.plusDays(1),
            deltakelseStatus = TiltakDeltakerstatus.Avbrutt,
        )

        finnEndringerPåSak(kjentTilstand, nåtilstand) shouldBe listOf(AvbruttDeltakelse)
    }

    @Test
    fun `ikke aktuell prioriteres foran fjernede datoer og endret mengde`() {
        val kjent = kjentTilstand.copy(
            deltakelseFraOgMed = iDag.plusDays(1),
            deltakelseStatus = TiltakDeltakerstatus.VenterPåOppstart,
        )
        val nåtilstand = kjent.copy(
            deltakelseProsent = null,
            antallDagerPerUke = null,
            deltakelseFraOgMed = null,
            deltakelseTilOgMed = null,
            deltakelseStatus = TiltakDeltakerstatus.IkkeAktuell,
        )

        finnEndringerPåSak(kjent, nåtilstand) shouldBe listOf(IkkeAktuellDeltakelse)
    }

    @ParameterizedTest
    @EnumSource(TiltakDeltakerstatus::class, names = ["Fullført", "HarSluttet", "IkkeAktuell"])
    fun `avkorting til fortiden prioriteres foran ny status og andre endringer`(nyStatus: TiltakDeltakerstatus) {
        val nåtilstand = kjentTilstand.copy(
            deltakelseProsent = 60F,
            antallDagerPerUke = 1F,
            deltakelseFraOgMed = kjentTilstand.deltakelseFraOgMed!!.plusDays(1),
            deltakelseTilOgMed = iDag.minusDays(1),
            deltakelseStatus = nyStatus,
        )

        finnEndringerPåSak(kjentTilstand, nåtilstand) shouldBe listOf(AvbruttDeltakelse)
    }

    private fun finnEndringerPåSak(
        kjent: TiltaksdeltakelseIntern,
        nåtilstand: TiltaksdeltakelseIntern,
    ): List<TiltaksdeltakerEndring>? =
        sakMedKjentTilstand(kjent).finnEndringer(kjent.internDeltakelseId, nåtilstand, clock)?.toList()

    private fun Sak.medNyereÅpenBehandling(
        tilstand: TiltaksdeltakelseIntern,
        automatisk: Boolean = false,
    ): Sak {
        val nyereClock = Clock.offset(clock, Duration.ofSeconds(1))
        val søknad = ObjectMother.nyInnvilgbarSøknad(
            sakId = id,
            fnr = fnr,
            saksnummer = saksnummer,
            periode = tilstand.periode!!,
            søknadstiltak = ObjectMother.søknadstiltak(
                id = tilstand.eksternDeltakelseId,
                tiltaksdeltakerId = tilstand.internDeltakelseId,
                deltakelseFom = tilstand.deltakelseFraOgMed!!,
                deltakelseTom = tilstand.deltakelseTilOgMed!!,
            ),
            clock = nyereClock,
        )
        val saksopplysninger = ObjectMother.saksopplysninger(
            tiltaksdeltakelse = listOf(tilstand),
            oppslagsperiode = tilstand.periode!!,
            clock = nyereClock,
        )
        val sakMedSøknad = leggTilSøknad(søknad)
        val behandling = if (automatisk) {
            ObjectMother.sakMedOpprettetAutomatiskBehandling(
                sakId = id,
                fnr = fnr,
                saksnummer = saksnummer,
                søknad = søknad,
                saksopplysninger = saksopplysninger,
                sak = sakMedSøknad,
                clock = nyereClock,
            ).second
        } else {
            ObjectMother.nyOpprettetSøknadsbehandling(
                sakId = id,
                fnr = fnr,
                saksnummer = saksnummer,
                søknad = søknad,
                hentSaksopplysninger = { _, _, _, _, _, _ -> saksopplysninger },
                sak = sakMedSøknad,
                clock = nyereClock,
            )
        }
        behandling.sistEndret.isAfter(rammebehandlinger.last().sistEndret) shouldBe true
        return sakMedSøknad.leggTilSøknadsbehandling(behandling)
    }

    private fun sakMedKjentTilstand(kjent: TiltaksdeltakelseIntern): Sak {
        val behandling = ObjectMother.nyOpprettetSøknadsbehandling(
            hentSaksopplysninger = { _, _, _, _, _, _ ->
                ObjectMother.saksopplysninger(tiltaksdeltakelse = listOf(kjent), clock = clock)
            },
            clock = clock,
        )
        return ObjectMother.nySak(
            sakId = behandling.sakId,
            fnr = behandling.fnr,
            saksnummer = behandling.saksnummer,
            søknader = listOf(behandling.søknad),
            behandlinger = Rammebehandlinger(behandling),
            clock = clock,
        )
    }
}
