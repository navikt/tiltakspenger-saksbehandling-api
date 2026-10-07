package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseIntern
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakerEndring.AndreEndringer
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakerEndring.AvbruttDeltakelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakerEndring.AvsluttetSomForventet
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakerEndring.EndretDeltakelsesmengde
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakerEndring.EndretSluttdato
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakerEndring.EndretStartdato
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakerEndring.EndretStatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakerEndring.Forlengelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakerEndring.IkkeAktuellDeltakelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.tilLibsDeltakelse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DeltakelsestilstandFinnEndringerTest {
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
        finnEndringer(kjentTilstand, kjentTilstand).shouldBeNull()
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

        finnEndringer(kjentTilstand, nåtilstand) shouldBe AndreEndringer(endretDeltakelsesmengde = EndretDeltakelsesmengde(prosent, dager))
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

        finnEndringer(kjentTilstand, nåtilstand) shouldBe Forlengelse(
            nySluttdato = nySluttdato,
            endretDeltakelsesmengde = EndretDeltakelsesmengde(prosent, dager),
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

        finnEndringer(kjent, nåtilstand).shouldBeNull()
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

        finnEndringer(kjent, nåtilstand) shouldBe AndreEndringer(endretDeltakelsesmengde = EndretDeltakelsesmengde(nyProsent, nyeDager))
    }

    @Test
    fun `senere sluttdato med samme startdato er en forlengelse`() {
        val nySluttdato = kjentTilstand.deltakelseTilOgMed!!.plusDays(1)

        finnEndringer(
            kjentTilstand,
            kjentTilstand.copy(deltakelseTilOgMed = nySluttdato),
        ) shouldBe Forlengelse(nySluttdato)
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

        finnEndringer(kjent, nåtilstand) shouldBe Forlengelse(nySluttdato)
    }

    @ParameterizedTest
    @ValueSource(longs = [-1, 0])
    fun `avkorting til før eller på dagens dato er avbrutt selv uten statusendring`(dagerFraIDag: Long) {
        val nåtilstand = kjentTilstand.copy(deltakelseTilOgMed = iDag.plusDays(dagerFraIDag))

        finnEndringer(kjentTilstand, nåtilstand) shouldBe AvbruttDeltakelse
    }

    @Test
    fun `avkorting til fremtiden gir endret sluttdato`() {
        val nySluttdato = iDag.plusDays(1)

        finnEndringer(
            kjentTilstand,
            kjentTilstand.copy(deltakelseTilOgMed = nySluttdato),
        ) shouldBe AndreEndringer(endretSluttdato = EndretSluttdato(nySluttdato))
    }

    @ParameterizedTest
    @ValueSource(longs = [-1, 0])
    fun `åpen deltakelse får sluttdato før eller på dagens dato og blir avbrutt`(dagerFraIDag: Long) {
        val kjent = kjentTilstand.copy(deltakelseTilOgMed = null)
        val nåtilstand = kjent.copy(deltakelseTilOgMed = iDag.plusDays(dagerFraIDag))

        finnEndringer(kjent, nåtilstand) shouldBe AvbruttDeltakelse
    }

    @Test
    fun `åpen deltakelse får sluttdato i fremtiden og er ikke en forlengelse`() {
        val kjent = kjentTilstand.copy(deltakelseTilOgMed = null)
        val nySluttdato = iDag.plusDays(1)

        finnEndringer(kjent, kjent.copy(deltakelseTilOgMed = nySluttdato)) shouldBe
            AndreEndringer(endretSluttdato = EndretSluttdato(nySluttdato))
    }

    @Test
    fun `fjernet sluttdato gir endret sluttdato med null`() {
        finnEndringer(
            kjentTilstand,
            kjentTilstand.copy(deltakelseTilOgMed = null),
        ) shouldBe AndreEndringer(endretSluttdato = EndretSluttdato(null))
    }

    @Test
    fun `begge mangler sluttdato og gir ingen endringer`() {
        val kjent = kjentTilstand.copy(deltakelseTilOgMed = null)

        finnEndringer(kjent, kjent).shouldBeNull()
    }

    @Test
    fun `forlengelse av en utløpt deltakelse er ikke avbrutt selv om ny sluttdato også er i fortiden`() {
        val kjent = kjentTilstand.copy(deltakelseTilOgMed = iDag.minusDays(2))
        val nySluttdato = iDag.minusDays(1)

        finnEndringer(kjent, kjent.copy(deltakelseTilOgMed = nySluttdato)) shouldBe
            Forlengelse(nySluttdato)
    }

    @ParameterizedTest
    @ValueSource(longs = [-1, 1])
    fun `endret startdato gir den nye startdatoen`(dagerFraGammelStart: Long) {
        val nyStartdato = kjentTilstand.deltakelseFraOgMed!!.plusDays(dagerFraGammelStart)

        finnEndringer(
            kjentTilstand,
            kjentTilstand.copy(deltakelseFraOgMed = nyStartdato),
        ) shouldBe AndreEndringer(endretStartdato = EndretStartdato(nyStartdato))
    }

    @Test
    fun `fjernet startdato gir endret startdato med null`() {
        finnEndringer(
            kjentTilstand,
            kjentTilstand.copy(deltakelseFraOgMed = null),
        ) shouldBe AndreEndringer(endretStartdato = EndretStartdato(null))
    }

    @Test
    fun `manglende startdato erstattes med en dato`() {
        val kjent = kjentTilstand.copy(deltakelseFraOgMed = null)

        finnEndringer(kjent, kjent.copy(deltakelseFraOgMed = iDag)) shouldBe AndreEndringer(endretStartdato = EndretStartdato(iDag))
    }

    @Test
    fun `begge mangler startdato og gir ingen endringer`() {
        val kjent = kjentTilstand.copy(deltakelseFraOgMed = null)

        finnEndringer(kjent, kjent).shouldBeNull()
    }

    @Test
    fun `senere sluttdato og endret startdato er to datoendringer og ikke forlengelse`() {
        val nyStartdato = kjentTilstand.deltakelseFraOgMed!!.plusDays(1)
        val nySluttdato = kjentTilstand.deltakelseTilOgMed!!.plusDays(1)
        val nåtilstand = kjentTilstand.copy(
            deltakelseFraOgMed = nyStartdato,
            deltakelseTilOgMed = nySluttdato,
        )

        finnEndringer(kjentTilstand, nåtilstand) shouldBe AndreEndringer(
            endretStartdato = EndretStartdato(nyStartdato),
            endretSluttdato = EndretSluttdato(nySluttdato),
        )
    }

    @ParameterizedTest
    @CsvSource(
        "Fullført, -1",
        "Fullført, 0",
        "HarSluttet, -1",
        "HarSluttet, 0",
    )
    fun `forventet avslutning før eller på dagens dato gir avsluttet som forventet`(
        nyStatus: TiltakDeltakerstatus,
        dagerFraIDag: Long,
    ) {
        val kjent = kjentTilstand.copy(deltakelseTilOgMed = iDag.plusDays(dagerFraIDag))

        finnEndringer(kjent, kjent.copy(deltakelseStatus = nyStatus)) shouldBe AvsluttetSomForventet
    }

    @ParameterizedTest
    @EnumSource(TiltakDeltakerstatus::class, names = ["Fullført", "HarSluttet"])
    fun `avsluttet status før sluttdato gir endret status`(nyStatus: TiltakDeltakerstatus) {
        val kjent = kjentTilstand.copy(deltakelseTilOgMed = iDag.plusDays(1))

        finnEndringer(kjent, kjent.copy(deltakelseStatus = nyStatus)) shouldBe AndreEndringer(endretStatus = EndretStatus(nyStatus))
    }

    @ParameterizedTest
    @EnumSource(TiltakDeltakerstatus::class, names = ["Fullført", "HarSluttet"])
    fun `avsluttet status uten sluttdato gir endret status`(nyStatus: TiltakDeltakerstatus) {
        val kjent = kjentTilstand.copy(deltakelseTilOgMed = null)

        finnEndringer(kjent, kjent.copy(deltakelseStatus = nyStatus)) shouldBe AndreEndringer(endretStatus = EndretStatus(nyStatus))
    }

    @ParameterizedTest
    @EnumSource(TiltakDeltakerstatus::class, names = ["Fullført", "HarSluttet"])
    fun `forventet avslutning skjuler ikke samtidig endret deltakelsesmengde`(nyStatus: TiltakDeltakerstatus) {
        val kjent = kjentTilstand.copy(deltakelseTilOgMed = iDag)
        val nåtilstand = kjent.copy(deltakelseStatus = nyStatus, antallDagerPerUke = 1F)

        finnEndringer(kjent, nåtilstand) shouldBe AndreEndringer(
            endretDeltakelsesmengde = EndretDeltakelsesmengde(50F, 1F),
            endretStatus = EndretStatus(nyStatus),
        )
    }

    @Test
    fun `kun statusendring fra venter på oppstart til deltar gir endret status`() {
        val kjent = kjentTilstand.copy(
            deltakelseFraOgMed = iDag.minusDays(1),
            deltakelseStatus = TiltakDeltakerstatus.VenterPåOppstart,
        )

        finnEndringer(kjent, kjent.copy(deltakelseStatus = TiltakDeltakerstatus.Deltar)) shouldBe
            AndreEndringer(endretStatus = EndretStatus(TiltakDeltakerstatus.Deltar))
    }

    @Test
    fun `kun statusendring til avbrutt gir avbrutt deltakelse`() {
        finnEndringer(
            kjentTilstand,
            kjentTilstand.copy(deltakelseStatus = TiltakDeltakerstatus.Avbrutt),
        ) shouldBe AvbruttDeltakelse
    }

    @Test
    fun `kun statusendring til ikke aktuell gir ikke aktuell deltakelse`() {
        finnEndringer(
            kjentTilstand,
            kjentTilstand.copy(deltakelseStatus = TiltakDeltakerstatus.IkkeAktuell),
        ) shouldBe IkkeAktuellDeltakelse
    }

    @ParameterizedTest
    @EnumSource(TiltakDeltakerstatus::class, names = ["Avbrutt", "IkkeAktuell"])
    fun `uendret avslutningsstatus overstyrer ikke en endret deltakelsesmengde`(status: TiltakDeltakerstatus) {
        val kjent = kjentTilstand.copy(deltakelseStatus = status)

        finnEndringer(kjent, kjent.copy(antallDagerPerUke = 1F)) shouldBe
            AndreEndringer(endretDeltakelsesmengde = EndretDeltakelsesmengde(50F, 1F))
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

        finnEndringer(kjentTilstand, nåtilstand) shouldBe AndreEndringer(
            endretDeltakelsesmengde = EndretDeltakelsesmengde(60F, 1F),
            endretStartdato = EndretStartdato(nyStartdato),
            endretSluttdato = EndretSluttdato(nySluttdato),
            endretStatus = EndretStatus(TiltakDeltakerstatus.HarSluttet),
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

        finnEndringer(kjentTilstand, nåtilstand) shouldBe AvbruttDeltakelse
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

        finnEndringer(kjent, nåtilstand) shouldBe IkkeAktuellDeltakelse
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

        finnEndringer(kjentTilstand, nåtilstand) shouldBe AvbruttDeltakelse
    }

    private fun finnEndringer(
        kjent: TiltaksdeltakelseIntern,
        nåtilstand: TiltaksdeltakelseIntern,
    ): TiltaksdeltakerEndring? =
        kjent.tilDeltakelsestilstand().finnEndringer(nåtilstand.tilLibsDeltakelse().tilDeltakelsestilstand(clock), clock)
}
