package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.utbetalingsoversikt

import arrow.core.left
import arrow.core.right
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.common.personopplysning.Organisasjonsnummer
import no.nav.tiltakspenger.libs.common.personopplysning.Samhandlerident
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.august
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Aktør
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.UtbetalingsoversiktMappingfeil
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.utbetalingsoversikt.UtbetalingsoversiktDto.AktoerDto
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.utbetalingsoversikt.UtbetalingsoversiktDto.UtbetalingsperiodeDto
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.utbetalingsoversikt.UtbetalingsoversiktDto.YtelseDto
import org.junit.jupiter.api.Test

class UtbetalingsoversiktMapperTest {
    private val fnr = Fnr.random()
    private val person = AktoerDto(aktoertype = "PERSON", ident = fnr.verdi, navn = "Fornavn Etternavn")

    private fun utbetaling(
        utbetaltTil: AktoerDto? = person,
        ytelser: List<YtelseDto> = emptyList(),
    ) = UtbetalingsoversiktDto(
        ytelseListe = ytelser,
        utbetaltTil = utbetaltTil,
        utbetalingsmetode = "Til konto",
        utbetalingsstatus = "Utbetalt",
        posteringsdato = 24.august(2025),
    )

    private fun ytelse(
        periode: UtbetalingsperiodeDto = UtbetalingsperiodeDto(fom = 1.august(2025), tom = 14.august(2025)),
        rettighetshaver: AktoerDto? = person,
        refundertForOrg: AktoerDto? = null,
    ) = YtelseDto(
        ytelsestype = "Tiltakspenger",
        ytelsesperiode = periode,
        ytelseNettobeloep = "900.1".toBigDecimal(),
        rettighetshaver = rettighetshaver,
        skattsum = "0".toBigDecimal(),
        trekksum = "0".toBigDecimal(),
        ytelseskomponentersum = "900.1".toBigDecimal(),
        refundertForOrg = refundertForOrg,
    )

    private fun utbetaltTil(aktør: AktoerDto?) = listOf(utbetaling(utbetaltTil = aktør)).tilRegistrerteUtbetalinger().map { it.single().utbetaltTil }

    @Test
    fun `tom liste gir tom liste`() {
        emptyList<UtbetalingsoversiktDto>().tilRegistrerteUtbetalinger() shouldBe emptyList<Nothing>().right()
    }

    @Test
    fun `aktørtypen avgjør hvilken aktør det blir`() {
        utbetaltTil(person) shouldBe Aktør.Person(fnr).right()
        utbetaltTil(AktoerDto(aktoertype = "ORGANISASJON", ident = "999111222")) shouldBe
            Aktør.Organisasjon(Organisasjonsnummer("999111222")).right()
        utbetaltTil(AktoerDto(aktoertype = "SAMHANDLER", ident = "80912345678")) shouldBe
            Aktør.Samhandler(Samhandlerident("80912345678")).right()
    }

    @Test
    fun `aktørtype og ident trimmes, og aktørtypen kan ha små bokstaver`() {
        utbetaltTil(AktoerDto(aktoertype = " person ", ident = " ${fnr.verdi} ")) shouldBe Aktør.Person(fnr).right()
    }

    @Test
    fun `manglende eller blank aktørtype og ident gir PåkrevdFeltMangler`() {
        utbetaltTil(null) shouldBe UtbetalingsoversiktMappingfeil.PåkrevdFeltMangler("utbetaltTil").left()
        listOf(null, " ").forEach { blank ->
            utbetaltTil(AktoerDto(aktoertype = blank, ident = fnr.verdi)) shouldBe
                UtbetalingsoversiktMappingfeil.PåkrevdFeltMangler("utbetaltTil.aktoertype").left()
            utbetaltTil(AktoerDto(aktoertype = "PERSON", ident = blank)) shouldBe
                UtbetalingsoversiktMappingfeil.PåkrevdFeltMangler("utbetaltTil.ident").left()
        }
    }

    @Test
    fun `ukjent aktørtype gir UkjentAktørtype`() {
        utbetaltTil(AktoerDto(aktoertype = "ROBOT", ident = fnr.verdi)) shouldBe
            UtbetalingsoversiktMappingfeil.UkjentAktørtype("utbetaltTil").left()
    }

    @Test
    fun `PERSON med ident som ikke er et fødselsnummer gir UgyldigPersonident`() {
        utbetaltTil(AktoerDto(aktoertype = "PERSON", ident = "999111222")) shouldBe
            UtbetalingsoversiktMappingfeil.UgyldigPersonident("utbetaltTil").left()
    }

    @Test
    fun `feilen navngir feltet aktøren sto i`() {
        val ukjent = AktoerDto(aktoertype = "ROBOT", ident = fnr.verdi)

        listOf(utbetaling(ytelser = listOf(ytelse(rettighetshaver = ukjent)))).tilRegistrerteUtbetalinger() shouldBe
            UtbetalingsoversiktMappingfeil.UkjentAktørtype("rettighetshaver").left()
        listOf(utbetaling(ytelser = listOf(ytelse(refundertForOrg = ukjent)))).tilRegistrerteUtbetalinger() shouldBe
            UtbetalingsoversiktMappingfeil.UkjentAktørtype("refundertForOrg").left()
    }

    @Test
    fun `refundertForOrg er valgfri og mappes når den finnes`() {
        val organisasjon = AktoerDto(aktoertype = "ORGANISASJON", ident = "999111222")

        fun refundertFor(aktør: AktoerDto?) =
            listOf(utbetaling(ytelser = listOf(ytelse(refundertForOrg = aktør)))).tilRegistrerteUtbetalinger().getOrFail().single().ytelser.single().refundertFor

        refundertFor(null) shouldBe null
        refundertFor(organisasjon) shouldBe Aktør.Organisasjon(Organisasjonsnummer("999111222"))
    }

    @Test
    fun `manglende påkrevd felt gir PåkrevdFeltMangler med feltnavnet fra json-en`() {
        mapOf(
            "utbetalingsmetode" to utbetaling().copy(utbetalingsmetode = null),
            "utbetalingsstatus" to utbetaling().copy(utbetalingsstatus = null),
            "posteringsdato" to utbetaling().copy(posteringsdato = null),
            "ytelseNettobeloep" to utbetaling(ytelser = listOf(ytelse().copy(ytelseNettobeloep = null))),
            "rettighetshaver" to utbetaling(ytelser = listOf(ytelse(rettighetshaver = null))),
            "skattsum" to utbetaling(ytelser = listOf(ytelse().copy(skattsum = null))),
            "trekksum" to utbetaling(ytelser = listOf(ytelse().copy(trekksum = null))),
            "ytelseskomponentersum" to utbetaling(ytelser = listOf(ytelse().copy(ytelseskomponentersum = null))),
        ).forEach { (felt, dto) ->
            withClue(felt) {
                listOf(dto).tilRegistrerteUtbetalinger() shouldBe UtbetalingsoversiktMappingfeil.PåkrevdFeltMangler(felt).left()
            }
        }
    }

    @Test
    fun `ytelsesperiode med fom etter tom gir UgyldigPeriode`() {
        val snudd = UtbetalingsperiodeDto(fom = 14.august(2025), tom = 1.august(2025))

        listOf(utbetaling(ytelser = listOf(ytelse(periode = snudd)))).tilRegistrerteUtbetalinger() shouldBe
            UtbetalingsoversiktMappingfeil.UgyldigPeriode("ytelsesperiode").left()
    }

    @Test
    fun `periode på én dag er gyldig`() {
        val enDag = UtbetalingsperiodeDto(fom = 1.august(2025), tom = 1.august(2025))

        listOf(utbetaling(ytelser = listOf(ytelse(periode = enDag)))).tilRegistrerteUtbetalinger().isRight() shouldBe true
    }
}
