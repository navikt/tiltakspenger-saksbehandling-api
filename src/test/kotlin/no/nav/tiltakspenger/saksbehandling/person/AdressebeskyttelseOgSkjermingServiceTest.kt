package no.nav.tiltakspenger.saksbehandling.person

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.left
import arrow.core.right
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.personklient.pdl.FellesSkjermingError
import no.nav.tiltakspenger.libs.personklient.pdl.dto.ForelderBarnRelasjon
import no.nav.tiltakspenger.libs.personklient.skjerming.FellesSkjermingsklient
import no.nav.tiltakspenger.saksbehandling.felles.Loggkontekst
import no.nav.tiltakspenger.saksbehandling.felles.uventetStatus
import org.junit.jupiter.api.Test
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class AdressebeskyttelseOgSkjermingServiceTest {

    private val ubeskyttet = Fnr.random()
    private val fortrolig = Fnr.random()
    private val strengtFortrolig = Fnr.random()
    private val strengtFortroligUtland = Fnr.random()
    private val skjermet = Fnr.random()
    private val alle = listOf(ubeskyttet, fortrolig, strengtFortrolig, strengtFortroligUtland, skjermet)
    private val forventet = mapOf(
        ubeskyttet to AdressebeskyttelseOgSkjerming(Adressebeskyttelse.UGRADERT, skjermet = false),
        fortrolig to AdressebeskyttelseOgSkjerming(Adressebeskyttelse.FORTROLIG, skjermet = false),
        strengtFortrolig to AdressebeskyttelseOgSkjerming(Adressebeskyttelse.STRENGT_FORTROLIG, skjermet = false),
        strengtFortroligUtland to AdressebeskyttelseOgSkjerming(Adressebeskyttelse.STRENGT_FORTROLIG_UTLAND, skjermet = false),
        skjermet to AdressebeskyttelseOgSkjerming(Adressebeskyttelse.UGRADERT, skjermet = true),
    )

    @Test
    fun `gir adressebeskyttelsen og skjermingen til hver person`() = runTest {
        val service = AdressebeskyttelseOgSkjermingService(TellendePersonKlient(), TellendeSkjermingsklient())

        service.hent(alle, CorrelationId.generate()) shouldBe forventet.right()
    }

    @Test
    fun `en person PDL ikke gir avklart adressebeskyttelse for, regnes som ugradert`() = runTest {
        val service = AdressebeskyttelseOgSkjermingService(TellendePersonKlient(finnes = false), TellendeSkjermingsklient())

        service.hent(listOf(fortrolig), CorrelationId.generate()) shouldBe mapOf(
            fortrolig to AdressebeskyttelseOgSkjerming(Adressebeskyttelse.UGRADERT, skjermet = false),
        ).right()
    }

    @Test
    fun `slår bare opp personene som ikke ligger i cachen`() = runTest {
        val personKlient = TellendePersonKlient()
        val skjermingsklient = TellendeSkjermingsklient()
        val service = AdressebeskyttelseOgSkjermingService(personKlient, skjermingsklient)

        service.hent(listOf(ubeskyttet, fortrolig), CorrelationId.generate())
        service.hent(alle, CorrelationId.generate()) shouldBe forventet.right()
        // Rent cachetreff: ingen nye oppslag, og samme svar.
        service.hent(alle, CorrelationId.generate()) shouldBe forventet.right()

        personKlient.oppslag shouldBe listOf(listOf(ubeskyttet, fortrolig), listOf(strengtFortrolig, strengtFortroligUtland, skjermet))
        skjermingsklient.oppslag shouldBe personKlient.oppslag
    }

    @Test
    fun `deler opp i bolker på maks 1000 og slår opp hver person én gang`() = runTest {
        val personKlient = TellendePersonKlient()
        val skjermingsklient = TellendeSkjermingsklient()
        val service = AdressebeskyttelseOgSkjermingService(personKlient, skjermingsklient)
        val mange = List(1001) { Fnr.random() }.distinct()

        service.hent(mange + mange.take(10), CorrelationId.generate()).getOrNull()?.size shouldBe mange.size

        personKlient.oppslag.map { it.size }.sorted() shouldBe listOf(mange.size - 1000, 1000)
        skjermingsklient.oppslag.toSet() shouldBe personKlient.oppslag.toSet()
    }

    @Test
    fun `slår opp bolkene og de to registrene samtidig`() = runTest {
        val service = AdressebeskyttelseOgSkjermingService(
            TellendePersonKlient(forsinkelse = 100.milliseconds),
            TellendeSkjermingsklient(forsinkelse = 100.milliseconds),
        )

        service.hent(List(1001) { Fnr.random() }.distinct(), CorrelationId.generate())

        // To bolker med to kall hver ville tatt 400 ms etter hverandre.
        testScheduler.currentTime shouldBe 100
    }

    @Test
    fun `gjør ingen oppslag når lista med fødselsnumre er tom`() = runTest {
        val personKlient = TellendePersonKlient()
        val service = AdressebeskyttelseOgSkjermingService(personKlient, TellendeSkjermingsklient())

        service.hent(emptyList(), CorrelationId.generate()) shouldBe emptyMap<Fnr, AdressebeskyttelseOgSkjerming>().right()
        personKlient.oppslag shouldBe emptyList()
    }

    @Test
    fun `feil mot skjermingsregisteret gir feil og caches ikke`() = runTest {
        val skjermingsklient = TellendeSkjermingsklient(feiler = true)
        val service = AdressebeskyttelseOgSkjermingService(TellendePersonKlient(), skjermingsklient)

        service.hent(alle, CorrelationId.generate()).leftOrNull()
            .shouldBeInstanceOf<KunneIkkeHenteAdressebeskyttelseEllerSkjerming.FeilVedKallMotSkjerming>()
        service.hent(alle, CorrelationId.generate())
        skjermingsklient.oppslag.size shouldBe 2
    }

    /** Feilen logges av route-laget, så konteksten den bærer er det som havner i loggen. */
    @Test
    fun `fødselsnumrene i en feilet request havner bare i sikkerloggkonteksten`() = runTest {
        val service = AdressebeskyttelseOgSkjermingService(TellendePersonKlient(), TellendeSkjermingsklient(feiler = true))

        val feil = service.hent(listOf(skjermet), CorrelationId.generate()).leftOrNull()!!

        feil.loggkontekst.melding shouldContain "Uventet HTTP-status 500 mot"
        feil.loggkontekst.melding shouldContain "1 personer"
        feil.loggkontekst.melding shouldNotContain skjermet.verdi
        feil.loggkontekst.underliggendeFeil shouldBe null
        feil.sikkerloggkontekst!!.melding shouldContain skjermet.verdi
    }

    @Test
    fun `feil mot PDL gir feil`() = runTest {
        val service = AdressebeskyttelseOgSkjermingService(TellendePersonKlient(feiler = true), TellendeSkjermingsklient())

        service.hent(alle, CorrelationId.generate()).leftOrNull()
            .shouldBeInstanceOf<KunneIkkeHenteAdressebeskyttelseEllerSkjerming.FeilVedKallMotPdl>()
    }

    private inner class TellendePersonKlient(
        private val feiler: Boolean = false,
        private val finnes: Boolean = true,
        private val forsinkelse: Duration = Duration.ZERO,
    ) : PersonKlient {
        val oppslag = CopyOnWriteArrayList<List<Fnr>>()

        override suspend fun hentAdressebeskyttelse(
            fnrs: List<Fnr>,
        ): Either<KunneIkkeHenteAdressebeskyttelseEllerSkjerming.FeilVedKallMotPdl, Map<Fnr, Adressebeskyttelse>> {
            oppslag.add(fnrs)
            delay(forsinkelse)
            if (feiler) {
                return KunneIkkeHenteAdressebeskyttelseEllerSkjerming.FeilVedKallMotPdl(
                    loggkontekst = Loggkontekst("PDL svarte ikke"),
                    sikkerloggkontekst = null,
                ).left()
            }
            if (!finnes) return emptyMap<Fnr, Adressebeskyttelse>().right()
            return fnrs.associateWith {
                when (it) {
                    fortrolig -> Adressebeskyttelse.FORTROLIG
                    strengtFortrolig -> Adressebeskyttelse.STRENGT_FORTROLIG
                    strengtFortroligUtland -> Adressebeskyttelse.STRENGT_FORTROLIG_UTLAND
                    else -> Adressebeskyttelse.UGRADERT
                }
            }.right()
        }

        override suspend fun hentPersonBolk(fnrs: List<Fnr>): List<EnkelPerson> = throw NotImplementedError()

        override suspend fun hentEnkelPerson(fnr: Fnr): EnkelPerson = throw NotImplementedError()

        override suspend fun hentPersonSineForelderBarnRelasjoner(fnr: Fnr): List<ForelderBarnRelasjon> = throw NotImplementedError()

        override suspend fun hentIdenter(aktorId: String): List<Personident> = throw NotImplementedError()
    }

    private inner class TellendeSkjermingsklient(
        private val feiler: Boolean = false,
        private val forsinkelse: Duration = Duration.ZERO,
    ) : FellesSkjermingsklient {
        val oppslag = CopyOnWriteArrayList<List<Fnr>>()

        override suspend fun erSkjermetPersoner(
            fnrListe: NonEmptyList<Fnr>,
            correlationId: CorrelationId,
        ): Either<FellesSkjermingError, Map<Fnr, Boolean>> {
            oppslag.add(fnrListe.toList())
            delay(forsinkelse)
            if (feiler) {
                return FellesSkjermingError.Ikke2xx(
                    uventetStatus(
                        uri = "http://skjerming.test/skjermetBulk",
                        rawRequestString = fnrListe.joinToString(prefix = "{\"personidenter\":[", postfix = "]}") { "\"${it.verdi}\"" },
                    ),
                ).left()
            }
            return fnrListe.associateWith { it == skjermet }.right()
        }

        override suspend fun erSkjermetPerson(fnr: Fnr, correlationId: CorrelationId): Either<FellesSkjermingError, Boolean> =
            throw NotImplementedError()
    }
}
