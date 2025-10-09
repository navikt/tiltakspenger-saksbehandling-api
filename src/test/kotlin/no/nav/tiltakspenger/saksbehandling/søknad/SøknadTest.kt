package no.nav.tiltakspenger.saksbehandling.søknad

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tiltakspenger.libs.common.førsteNovember24
import no.nav.tiltakspenger.saksbehandling.felles.Avbrutt
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.fraOgMedDatoNei
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.nei
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.periodeNei
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.søknadstiltak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.IkkeInnvilgbarSøknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.InnvilgbarSøknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadstype
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

class SøknadTest {
    @Nested
    inner class Opprett {
        @Test
        fun `oppretter en innvilgbar søknad`() {
            val sak = ObjectMother.nySak()
            val opprettetSøknad = Søknad.opprett(
                søknadstiltak = søknadstiltak(),
                journalpostId = "99999",
                kravtidspunkt = LocalDateTime.now(),
                personopplysninger = ObjectMother.personSøknad(fnr = sak.fnr),
                barnetillegg = emptyList(),
                kvp = periodeNei(),
                intro = periodeNei(),
                institusjon = periodeNei(),
                etterlønn = nei(),
                gjenlevendepensjon = periodeNei(),
                alderspensjon = fraOgMedDatoNei(),
                sykepenger = periodeNei(),
                supplerendeStønadAlder = periodeNei(),
                supplerendeStønadFlyktning = periodeNei(),
                jobbsjansen = periodeNei(),
                trygdOgPensjon = periodeNei(),
                antallVedlegg = 1,
                manueltSattSøknadsperiode = null,
                søknadstype = Søknadstype.PAPIR,
                sak = sak,
            )

            opprettetSøknad.shouldBeInstanceOf<InnvilgbarSøknad>()
            opprettetSøknad.tiltak shouldNotBe null
        }

        @Test
        fun `oppretter en ikke innvilgbar søknad`() {
            val sak = ObjectMother.nySak()
            val opprettetSøknad = Søknad.opprett(
                søknadstiltak = null,
                journalpostId = "99999",
                kravtidspunkt = LocalDateTime.now(),
                personopplysninger = ObjectMother.personSøknad(fnr = sak.fnr),
                barnetillegg = emptyList(),
                kvp = periodeNei(),
                intro = periodeNei(),
                institusjon = periodeNei(),
                etterlønn = nei(),
                gjenlevendepensjon = periodeNei(),
                alderspensjon = fraOgMedDatoNei(),
                sykepenger = periodeNei(),
                supplerendeStønadAlder = periodeNei(),
                supplerendeStønadFlyktning = periodeNei(),
                jobbsjansen = periodeNei(),
                trygdOgPensjon = periodeNei(),
                antallVedlegg = 1,
                manueltSattSøknadsperiode = null,
                søknadstype = Søknadstype.PAPIR,
                sak = sak,
            )

            opprettetSøknad.shouldBeInstanceOf<IkkeInnvilgbarSøknad>()
            opprettetSøknad.tiltak shouldBe null
        }
    }

    @Test
    fun `avbryter en søknad`() {
        val søknad = ObjectMother.nyInnvilgbarSøknad()
        val avbruttSøknad = søknad.avbryt(ObjectMother.saksbehandler(), "jeg avbryter søknad", førsteNovember24)

        avbruttSøknad.erAvbrutt shouldBe true
        avbruttSøknad.avbrutt.let {
            it shouldNotBe null
            it!!.saksbehandler shouldBe ObjectMother.saksbehandler().navIdent
            it.begrunnelse shouldBe "jeg avbryter søknad"
            it.tidspunkt shouldBe førsteNovember24
        }
    }

    @Test
    fun `kaster exception dersom man prøver å avbryte en avbrutt søknad`() {
        val avbruttSøknad = ObjectMother.nyInnvilgbarSøknad(
            avbrutt = Avbrutt(
                tidspunkt = førsteNovember24,
                saksbehandler = "navident",
                begrunnelse = "skal få exception",
            ),
        )

        assertThrows<IllegalStateException> {
            avbruttSøknad.avbryt(ObjectMother.saksbehandler(), "jeg avbryter søknad", førsteNovember24)
        }
    }
}
