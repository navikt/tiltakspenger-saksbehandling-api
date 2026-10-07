package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb

import arrow.core.nonEmptyListOf
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.iverksett.iverksett
import no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdater.oppdater
import no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdater.oppdaterInnvilgelse
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ta.taBehandling
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.tilBeslutning
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjørRammevedtak
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseIntern
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
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
import no.nav.tiltakspenger.saksbehandling.vedtak.opprettRammevedtak
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.LocalDate

class SakFinnEndringerMotGjeldendeVedtakTest {
    private val clock = fixedClock
    private val iDag = LocalDate.now(clock)
    private val deltakelse = ObjectMother.tiltaksdeltakelse(
        fom = iDag.minusMonths(2),
        tom = iDag.plusMonths(1),
        dagerPrUke = 2F,
        prosent = 50F,
    )

    @Test
    fun `uendret deltakelse gir ingen endringer`() {
        sakMedInnvilgelse().finnEndringer(deltakelse).shouldBeNull()
    }

    @Test
    fun `deltakelse som ikke er innvilget gir ingen endringer`() {
        sakMedInnvilgelse().finnEndringerMotGjeldendeVedtak(
            tiltaksdeltakerId = TiltaksdeltakerId.random(),
            oppdatertDeltakelse = deltakelse.copy(deltakelseProsent = 60F).tilLibsDeltakelse(),
            clock = clock,
        ).shouldBeNull()
    }

    @Test
    fun `endret deltakelsesmengde sammenlignes med gjeldende vedtak`() {
        sakMedInnvilgelse().finnEndringer(deltakelse.copy(deltakelseProsent = 60F, antallDagerPerUke = 3F)) shouldBe
            AndreEndringer(endretDeltakelsesmengde = EndretDeltakelsesmengde(60F, 3F))
    }

    @Test
    fun `forlenget deltakelse gir forlengelse`() {
        val nySluttdato = deltakelse.deltakelseTilOgMed!!.plusMonths(1)

        sakMedInnvilgelse().finnEndringer(deltakelse.copy(deltakelseTilOgMed = nySluttdato)) shouldBe Forlengelse(nySluttdato)
    }

    @Test
    fun `deltakelse som er stanset i hele perioden gir ingen endringer`() {
        val sak = sakMedInnvilgelse().medStans(stansFraOgMed = deltakelse.deltakelseFraOgMed!!)

        sak.finnEndringer(deltakelse.copy(deltakelseProsent = 60F)).shouldBeNull()
        sak.finnEndringer(deltakelse.copy(deltakelseTilOgMed = iDag.minusDays(1))).shouldBeNull()
    }

    @Test
    fun `avkortet sluttdato er bare avbrudd dersom den berører de gjenværende innvilgede periodene etter stans`() {
        val sak = sakMedInnvilgelse().medStans(stansFraOgMed = iDag)

        sak.finnEndringer(deltakelse.copy(deltakelseTilOgMed = iDag.minusWeeks(1))) shouldBe AvbruttDeltakelse
        sak.finnEndringer(deltakelse.copy(deltakelseTilOgMed = iDag)).shouldBeNull()
    }

    @Test
    fun `avkortet sluttdato er bare relevant dersom den berører de innvilgede periodene`() {
        val sak = sakMedInnvilgelse(innvilgelsesperiode = deltakelse.deltakelseFraOgMed!! til iDag.minusMonths(1))

        sak.finnEndringer(deltakelse.copy(deltakelseTilOgMed = iDag.minusWeeks(1))).shouldBeNull()
        sak.finnEndringer(deltakelse.copy(deltakelseTilOgMed = iDag.minusWeeks(6))) shouldBe AvbruttDeltakelse
    }

    @Test
    fun `avkortet sluttdato frem i tid innenfor de innvilgede periodene gir endret sluttdato`() {
        val nySluttdato = iDag.plusWeeks(1)

        sakMedInnvilgelse().finnEndringer(deltakelse.copy(deltakelseTilOgMed = nySluttdato)) shouldBe
            AndreEndringer(endretSluttdato = EndretSluttdato(nySluttdato))
    }

    @Test
    fun `ny status eller forlengelse etter stans fra i dag gir ingen endringer`() {
        val sak = sakMedInnvilgelse().medStans(stansFraOgMed = iDag)

        sak.finnEndringer(
            deltakelse.copy(deltakelseTilOgMed = iDag.minusDays(1), deltakelseStatus = TiltakDeltakerstatus.Avbrutt),
        ).shouldBeNull()
        sak.finnEndringer(deltakelse.copy(deltakelseStatus = TiltakDeltakerstatus.IkkeAktuell)).shouldBeNull()
        sak.finnEndringer(deltakelse.copy(deltakelseTilOgMed = deltakelse.deltakelseTilOgMed!!.plusMonths(1))).shouldBeNull()
    }

    @Test
    fun `endret deltakelsesmengde etter stans fra i dag gir ingen endringer`() {
        sakMedInnvilgelse().medStans(stansFraOgMed = iDag)
            .finnEndringer(deltakelse.copy(deltakelseProsent = 60F))
            .shouldBeNull()
    }

    @Test
    fun `avsluttet status med passert sluttdato gir avsluttet som forventet`() {
        val opprinnelig = deltakelse.copy(deltakelseTilOgMed = iDag.minusDays(1))
        val sak = sakMedInnvilgelse(opprinnelig = opprinnelig)

        sak.finnEndringer(opprinnelig.copy(deltakelseStatus = TiltakDeltakerstatus.HarSluttet)) shouldBe AvsluttetSomForventet
        sak.finnEndringer(opprinnelig.copy(deltakelseStatus = TiltakDeltakerstatus.Fullført)) shouldBe AvsluttetSomForventet
    }

    @Test
    fun `avsluttet status med sluttdato frem i tid gir endret status`() {
        sakMedInnvilgelse().finnEndringer(deltakelse.copy(deltakelseStatus = TiltakDeltakerstatus.HarSluttet)) shouldBe
            AndreEndringer(endretStatus = EndretStatus(TiltakDeltakerstatus.HarSluttet))
    }

    @Test
    fun `avbrutt og ikke aktuell status gir egne utfall`() {
        val sak = sakMedInnvilgelse()

        sak.finnEndringer(deltakelse.copy(deltakelseStatus = TiltakDeltakerstatus.Avbrutt)) shouldBe AvbruttDeltakelse
        sak.finnEndringer(deltakelse.copy(deltakelseStatus = TiltakDeltakerstatus.IkkeAktuell)) shouldBe IkkeAktuellDeltakelse
    }

    @Test
    fun `senere startdato er bare relevant dersom den berører de innvilgede periodene`() {
        val nyStartdato = deltakelse.deltakelseFraOgMed!!.plusWeeks(1)

        sakMedInnvilgelse().finnEndringer(deltakelse.copy(deltakelseFraOgMed = nyStartdato)) shouldBe
            AndreEndringer(endretStartdato = EndretStartdato(nyStartdato))
        sakMedInnvilgelse(innvilgelsesperiode = deltakelse.deltakelseFraOgMed.plusMonths(1) til deltakelse.deltakelseTilOgMed!!)
            .finnEndringer(deltakelse.copy(deltakelseFraOgMed = nyStartdato))
            .shouldBeNull()
    }

    @Test
    fun `tidligere startdato gir endret startdato`() {
        val nyStartdato = deltakelse.deltakelseFraOgMed!!.minusWeeks(1)

        sakMedInnvilgelse().finnEndringer(deltakelse.copy(deltakelseFraOgMed = nyStartdato)) shouldBe
            AndreEndringer(endretStartdato = EndretStartdato(nyStartdato))
    }

    @Test
    fun `tidligere startdato samtidig med forlengelse gir andre endringer`() {
        val nyStartdato = deltakelse.deltakelseFraOgMed!!.minusWeeks(1)
        val nySluttdato = deltakelse.deltakelseTilOgMed!!.plusMonths(1)

        sakMedInnvilgelse().finnEndringer(
            deltakelse.copy(deltakelseFraOgMed = nyStartdato, deltakelseTilOgMed = nySluttdato),
        ) shouldBe AndreEndringer(
            endretStartdato = EndretStartdato(nyStartdato),
            endretSluttdato = EndretSluttdato(nySluttdato),
        )
    }

    @Test
    fun `åpen sluttdato gir endret sluttdato`() {
        sakMedInnvilgelse().finnEndringer(deltakelse.copy(deltakelseTilOgMed = null)) shouldBe
            AndreEndringer(endretSluttdato = EndretSluttdato(null))
    }

    @Test
    fun `forlengelse med endret deltakelsesmengde gir forlengelse med mengde`() {
        val nySluttdato = deltakelse.deltakelseTilOgMed!!.plusMonths(1)

        sakMedInnvilgelse().finnEndringer(deltakelse.copy(deltakelseTilOgMed = nySluttdato, deltakelseProsent = 60F)) shouldBe
            Forlengelse(nySluttdato, EndretDeltakelsesmengde(60F, 2F))
    }

    @Test
    fun `endret deltakelsesmengde etter at innvilgelsen er utløpt sammenlignes med siste innvilgede periode`() {
        val opprinnelig = deltakelse.copy(deltakelseTilOgMed = iDag.minusWeeks(1))

        sakMedInnvilgelse(opprinnelig = opprinnelig).finnEndringer(opprinnelig.copy(deltakelseProsent = 60F)) shouldBe
            AndreEndringer(endretDeltakelsesmengde = EndretDeltakelsesmengde(60F, 2F))
    }

    @Test
    fun `deltakelsesmengden sammenlignes med vedtakene som gjelder fra og med i dag`() {
        val sak = sakMedInnvilgelse().medInnvilgelse(
            innvilgelsesperiode = iDag til deltakelse.deltakelseTilOgMed!!,
            innvilgetDeltakelse = deltakelse.copy(deltakelseProsent = 60F),
        )

        sak.finnEndringer(deltakelse.copy(deltakelseProsent = 60F)).shouldBeNull()
        sak.finnEndringer(deltakelse) shouldBe AndreEndringer(endretDeltakelsesmengde = EndretDeltakelsesmengde(50F, 2F))
    }

    @Test
    fun `sluttdatoen sammenlignes med vedtaket som innvilger slutten av deltakelsen`() {
        val forlengetSluttdato = deltakelse.deltakelseTilOgMed!!.plusMonths(1)
        val sak = sakMedInnvilgelse().medInnvilgelse(
            innvilgelsesperiode = iDag til forlengetSluttdato,
            innvilgetDeltakelse = deltakelse.copy(deltakelseTilOgMed = forlengetSluttdato),
        )

        sak.finnEndringer(deltakelse.copy(deltakelseTilOgMed = forlengetSluttdato)).shouldBeNull()
        sak.finnEndringer(deltakelse.copy(deltakelseTilOgMed = forlengetSluttdato.plusMonths(1))) shouldBe
            Forlengelse(forlengetSluttdato.plusMonths(1))
        sak.finnEndringer(deltakelse) shouldBe
            AndreEndringer(endretSluttdato = EndretSluttdato(deltakelse.deltakelseTilOgMed))
    }

    @Test
    fun `startdatoen sammenlignes med vedtaket som innvilger starten av deltakelsen`() {
        val tidligereStartdato = deltakelse.deltakelseFraOgMed!!.minusWeeks(2)
        val sak = sakMedInnvilgelse().medInnvilgelse(
            innvilgelsesperiode = tidligereStartdato til iDag.minusMonths(1),
            innvilgetDeltakelse = deltakelse.copy(deltakelseFraOgMed = tidligereStartdato),
        )

        sak.finnEndringer(deltakelse.copy(deltakelseFraOgMed = tidligereStartdato)).shouldBeNull()
        sak.finnEndringer(deltakelse) shouldBe
            AndreEndringer(endretStartdato = EndretStartdato(deltakelse.deltakelseFraOgMed))
    }

    @Test
    fun `endret deltakelsesmengde gir omgjøring av vedtaket`() {
        val sak = sakMedInnvilgelse()

        sak.vurder(deltakelse.copy(deltakelseProsent = 60F)).automatiskRevurdering shouldBe
            AutomatiskRevurderingAvEndring.Omgjøring(sak.rammevedtaksliste.verdi.single().id)
    }

    @Test
    fun `forlengelse etter siste dag med rett gir innvilgelse`() {
        sakMedInnvilgelse()
            .vurder(deltakelse.copy(deltakelseTilOgMed = deltakelse.deltakelseTilOgMed!!.plusMonths(1)))
            .automatiskRevurdering shouldBe AutomatiskRevurderingAvEndring.Innvilgelse
    }

    @Test
    fun `avbrudd gir stans`() {
        val sak = sakMedInnvilgelse()

        sak.vurder(deltakelse.copy(deltakelseStatus = TiltakDeltakerstatus.Avbrutt)).automatiskRevurdering shouldBe
            AutomatiskRevurderingAvEndring.Stans
        sak.vurder(deltakelse.copy(deltakelseTilOgMed = iDag.minusWeeks(1))).automatiskRevurdering shouldBe
            AutomatiskRevurderingAvEndring.Stans
    }

    @Test
    fun `avbrudd i en innvilgelse som er passert følges opp manuelt`() {
        val sak = sakMedInnvilgelse(innvilgelsesperiode = deltakelse.deltakelseFraOgMed!! til iDag.minusMonths(1))

        sak.vurder(deltakelse.copy(deltakelseTilOgMed = iDag.minusWeeks(6))) shouldBe
            VurdertTiltaksdeltakerEndring(AvbruttDeltakelse, automatiskRevurdering = null)
    }

    @Test
    fun `ikke aktuell, avsluttet som forventet og ren statusendring følges opp manuelt`() {
        val opprinnelig = deltakelse.copy(deltakelseTilOgMed = iDag.minusDays(1))

        sakMedInnvilgelse().vurder(deltakelse.copy(deltakelseStatus = TiltakDeltakerstatus.IkkeAktuell)).automatiskRevurdering
            .shouldBeNull()
        sakMedInnvilgelse().vurder(deltakelse.copy(deltakelseStatus = TiltakDeltakerstatus.HarSluttet)).automatiskRevurdering
            .shouldBeNull()
        sakMedInnvilgelse(opprinnelig = opprinnelig)
            .vurder(opprinnelig.copy(deltakelseStatus = TiltakDeltakerstatus.Fullført))
            .automatiskRevurdering.shouldBeNull()
    }

    @Test
    fun `oppretter ikke revurdering når saken har en åpen behandling`() {
        val sak = sakMedInnvilgelse()
        val åpenRevurdering = ObjectMother.nyOpprettetRevurderingInnvilgelse(
            sakId = sak.id,
            saksnummer = sak.saksnummer,
            fnr = sak.fnr,
            clock = Clock.offset(clock, Duration.ofSeconds(30)),
        )

        sak.leggTilRevurdering(åpenRevurdering).vurder(deltakelse.copy(deltakelseProsent = 60F)) shouldBe
            VurdertTiltaksdeltakerEndring(
                endring = AndreEndringer(endretDeltakelsesmengde = EndretDeltakelsesmengde(60F, 2F)),
                automatiskRevurdering = null,
            )
    }

    @Test
    fun `omgjøring gis for vedtaket endringen berører når deltakelsen er innvilget i flere vedtak`() {
        val sak = sakMedInnvilgelse().medInnvilgelse(
            innvilgelsesperiode = iDag til deltakelse.deltakelseTilOgMed!!,
            innvilgetDeltakelse = deltakelse,
        )
        val (førsteVedtak, andreVedtak) = sak.rammevedtaksliste.verdi
        val senereStartdato = deltakelse.deltakelseFraOgMed!!.plusWeeks(1)

        sak.vurder(deltakelse.copy(deltakelseProsent = 60F)).automatiskRevurdering shouldBe
            AutomatiskRevurderingAvEndring.Omgjøring(andreVedtak.id)
        sak.vurder(deltakelse.copy(deltakelseTilOgMed = iDag.plusWeeks(1))).automatiskRevurdering shouldBe
            AutomatiskRevurderingAvEndring.Omgjøring(andreVedtak.id)
        sak.vurder(deltakelse.copy(deltakelseFraOgMed = senereStartdato)).automatiskRevurdering shouldBe
            AutomatiskRevurderingAvEndring.Omgjøring(førsteVedtak.id)
        sak.vurder(deltakelse.copy(deltakelseFraOgMed = senereStartdato, deltakelseProsent = 60F)).let {
            it.endring shouldBe AndreEndringer(
                endretDeltakelsesmengde = EndretDeltakelsesmengde(60F, 2F),
                endretStartdato = EndretStartdato(senereStartdato),
            )
            it.automatiskRevurdering.shouldBeNull()
        }
    }

    @Test
    fun `avbrudd når en annen deltakelse er innvilget etterpå gir omgjøring i stedet for stans`() {
        val sluttdato = deltakelse.deltakelseTilOgMed!!
        val annenDeltakelse = ObjectMother.tiltaksdeltakelse(fom = sluttdato.plusDays(1), tom = sluttdato.plusMonths(1))
        val sak = sakMedInnvilgelse().medInnvilgelse(
            innvilgelsesperiode = annenDeltakelse.periode!!,
            innvilgetDeltakelse = annenDeltakelse,
        )
        val førsteVedtak = sak.rammevedtaksliste.verdi.first()

        sak.vurder(deltakelse.copy(deltakelseStatus = TiltakDeltakerstatus.Avbrutt)).automatiskRevurdering shouldBe
            AutomatiskRevurderingAvEndring.Omgjøring(førsteVedtak.id)
    }

    @Test
    fun `forlengelse inn i perioden til en annen deltakelse følges opp manuelt med mindre deltakelsesmengden er endret`() {
        val sluttdato = deltakelse.deltakelseTilOgMed!!
        val annenDeltakelse = ObjectMother.tiltaksdeltakelse(fom = sluttdato.plusDays(1), tom = sluttdato.plusMonths(1))
        val sak = sakMedInnvilgelse().medInnvilgelse(
            innvilgelsesperiode = annenDeltakelse.periode!!,
            innvilgetDeltakelse = annenDeltakelse,
        )
        val nySluttdato = sluttdato.plusWeeks(2)

        sak.vurder(deltakelse.copy(deltakelseTilOgMed = nySluttdato)) shouldBe
            VurdertTiltaksdeltakerEndring(Forlengelse(nySluttdato), automatiskRevurdering = null)
        sak.vurder(deltakelse.copy(deltakelseTilOgMed = nySluttdato, deltakelseProsent = 60F)).automatiskRevurdering shouldBe
            AutomatiskRevurderingAvEndring.Omgjøring(sak.rammevedtaksliste.verdi.first().id)
        sak.vurder(deltakelse.copy(deltakelseTilOgMed = sluttdato.plusMonths(2))).automatiskRevurdering shouldBe
            AutomatiskRevurderingAvEndring.Innvilgelse
    }

    @Test
    fun `åpen manuell behandling som allerede kjenner nå-tilstanden gir ingen endringer`() {
        val nåtilstand = deltakelse.copy(deltakelseTilOgMed = deltakelse.deltakelseTilOgMed!!.plusMonths(1))
        val sak = sakMedInnvilgelse()
        val åpenRevurdering = ObjectMother.nyOpprettetRevurderingInnvilgelse(
            sakId = sak.id,
            saksnummer = sak.saksnummer,
            fnr = sak.fnr,
            clock = Clock.offset(clock, Duration.ofSeconds(30)),
            saksopplysningsperiode = nåtilstand.periode!!,
            hentSaksopplysninger = { oppslagsperiode ->
                ObjectMother.saksopplysninger(tiltaksdeltakelse = listOf(nåtilstand), oppslagsperiode = oppslagsperiode, clock = clock)
            },
        )

        sak.finnEndringer(nåtilstand) shouldBe Forlengelse(nåtilstand.deltakelseTilOgMed!!)
        sak.leggTilRevurdering(åpenRevurdering).finnEndringer(nåtilstand).shouldBeNull()
        sak.leggTilRevurdering(åpenRevurdering).vurder(nåtilstand.copy(deltakelseProsent = 60F)) shouldBe
            VurdertTiltaksdeltakerEndring(
                endring = Forlengelse(nåtilstand.deltakelseTilOgMed, EndretDeltakelsesmengde(60F, 2F)),
                automatiskRevurdering = null,
            )
    }

    @Test
    fun `uten vedtak sammenlignes nå-tilstanden med den åpne manuelle behandlingen`() {
        val sak = sakMedÅpenSøknadsbehandling()
        val nySluttdato = deltakelse.deltakelseTilOgMed!!.plusMonths(1)

        sak.finnEndringer(deltakelse).shouldBeNull()
        sak.vurder(deltakelse.copy(deltakelseTilOgMed = nySluttdato)) shouldBe
            VurdertTiltaksdeltakerEndring(endring = Forlengelse(nySluttdato), automatiskRevurdering = null)
        sak.finnEndringer(deltakelse.copy(deltakelseFraOgMed = deltakelse.deltakelseFraOgMed!!.plusDays(1), antallDagerPerUke = 3F)) shouldBe
            AndreEndringer(
                endretDeltakelsesmengde = EndretDeltakelsesmengde(50F, 3F),
                endretStartdato = EndretStartdato(deltakelse.deltakelseFraOgMed.plusDays(1)),
            )
    }

    private fun Sak.vurder(nåtilstand: TiltaksdeltakelseIntern): VurdertTiltaksdeltakerEndring =
        finnEndringerMotGjeldendeVedtak(deltakelse.internDeltakelseId, nåtilstand.tilLibsDeltakelse(), clock).shouldNotBeNull()

    private fun Sak.finnEndringer(nåtilstand: TiltaksdeltakelseIntern): TiltaksdeltakerEndring? =
        finnEndringerMotGjeldendeVedtak(deltakelse.internDeltakelseId, nåtilstand.tilLibsDeltakelse(), clock)?.endring

    private fun sakMedInnvilgelse(
        opprinnelig: TiltaksdeltakelseIntern = deltakelse,
        innvilgelsesperiode: Periode = opprinnelig.periode!!,
    ): Sak {
        val sakId = SakId.random()
        val fnr = Fnr.random()
        val saksnummer = ObjectMother.nesteSaksnummer()
        val saksbehandler = ObjectMother.saksbehandler()
        val beslutter = ObjectMother.beslutter()
        val søknad = ObjectMother.nyInnvilgbarSøknad(
            sakId = sakId,
            fnr = fnr,
            saksnummer = saksnummer,
            periode = opprinnelig.periode!!,
            søknadstiltak = ObjectMother.søknadstiltak(
                id = opprinnelig.eksternDeltakelseId,
                tiltaksdeltakerId = opprinnelig.internDeltakelseId,
                deltakelseFom = opprinnelig.deltakelseFraOgMed!!,
                deltakelseTom = opprinnelig.deltakelseTilOgMed!!,
            ),
            clock = clock,
        )
        val sak = ObjectMother.nySak(sakId = sakId, fnr = fnr, saksnummer = saksnummer, søknader = listOf(søknad), clock = clock)
        val iverksatt = ObjectMother.nyOpprettetSøknadsbehandling(
            sakId = sakId,
            fnr = fnr,
            saksnummer = saksnummer,
            saksbehandler = saksbehandler,
            søknad = søknad,
            hentSaksopplysninger = { _, _, _, _, _, _ ->
                ObjectMother.saksopplysninger(
                    tiltaksdeltakelse = listOf(opprinnelig),
                    oppslagsperiode = opprinnelig.periode!!,
                    clock = clock,
                )
            },
            sak = sak,
            clock = clock,
        ).oppdater(
            ObjectMother.oppdaterSøknadsbehandlingInnvilgelseKommando(
                sakId = sakId,
                saksbehandler = saksbehandler,
                innvilgelsesperioder = listOf(
                    ObjectMother.innvilgelsesperiodeKommando(
                        innvilgelsesperiode = innvilgelsesperiode,
                        tiltaksdeltakelse = opprinnelig,
                    ),
                ),
                barnetillegg = Barnetillegg.utenBarnetillegg(nonEmptyListOf(innvilgelsesperiode)),
            ),
            clock = clock,
            utbetaling = null,
            omgjørRammevedtak = OmgjørRammevedtak.empty,
        ).getOrFail()
            .tilBeslutning(saksbehandler = saksbehandler, clock = clock)
            .taBehandling(beslutter, clock).getOrFail().first
            .iverksett(
                utøvendeBeslutter = beslutter,
                attestering = ObjectMother.godkjentAttestering(beslutter),
                correlationId = CorrelationId.generate(),
                clock = clock,
            ).getOrFail().first

        return sak.leggTilSøknadsbehandling(iverksatt as Søknadsbehandling)
            .opprettRammevedtak(iverksatt, clock)
            .getOrFail().first
    }

    private fun sakMedÅpenSøknadsbehandling(): Sak {
        val sakId = SakId.random()
        val fnr = Fnr.random()
        val saksnummer = ObjectMother.nesteSaksnummer()
        val søknad = ObjectMother.nyInnvilgbarSøknad(
            sakId = sakId,
            fnr = fnr,
            saksnummer = saksnummer,
            periode = deltakelse.periode!!,
            søknadstiltak = ObjectMother.søknadstiltak(
                id = deltakelse.eksternDeltakelseId,
                tiltaksdeltakerId = deltakelse.internDeltakelseId,
                deltakelseFom = deltakelse.deltakelseFraOgMed!!,
                deltakelseTom = deltakelse.deltakelseTilOgMed!!,
            ),
            clock = clock,
        )
        val sak = ObjectMother.nySak(sakId = sakId, fnr = fnr, saksnummer = saksnummer, søknader = listOf(søknad), clock = clock)
        val behandling = ObjectMother.nyOpprettetSøknadsbehandling(
            sakId = sakId,
            fnr = fnr,
            saksnummer = saksnummer,
            saksbehandler = ObjectMother.saksbehandler(),
            søknad = søknad,
            hentSaksopplysninger = { _, _, _, _, _, _ ->
                ObjectMother.saksopplysninger(
                    tiltaksdeltakelse = listOf(deltakelse),
                    oppslagsperiode = deltakelse.periode!!,
                    clock = clock,
                )
            },
            sak = sak,
            clock = clock,
        )
        return sak.leggTilSøknadsbehandling(behandling)
    }

    private fun Sak.medStans(stansFraOgMed: LocalDate): Sak {
        val stansClock = Clock.offset(clock, Duration.ofSeconds(10))
        val sisteDagSomGirRett = rammevedtaksliste.sisteDagSomGirRett!!
        val stansperiode = stansFraOgMed til sisteDagSomGirRett
        val stans = ObjectMother.nyVedtattRevurderingStans(
            clock = stansClock,
            sakId = id,
            saksnummer = saksnummer,
            fnr = fnr,
            vedtaksperiode = stansperiode,
            saksopplysninger = ObjectMother.saksopplysninger(
                tiltaksdeltakelse = listOf(deltakelse),
                oppslagsperiode = deltakelse.periode!!,
                clock = stansClock,
            ),
            stansFraOgMed = stansFraOgMed,
            førsteDagSomGirRett = rammevedtaksliste.førsteDagSomGirRett!!,
            sisteDagSomGirRett = sisteDagSomGirRett,
            omgjørRammevedtak = rammevedtaksliste.finnVedtakSomOmgjøres(stansperiode),
        )
        return leggTilRevurdering(stans).opprettRammevedtak(stans, stansClock).getOrFail().first
    }

    /** Innvilger deltakelsen på nytt i en revurdering, med tilstanden [innvilgetDeltakelse] og omgjøring av vedtakene i [innvilgelsesperiode]. */
    private fun Sak.medInnvilgelse(innvilgelsesperiode: Periode, innvilgetDeltakelse: TiltaksdeltakelseIntern): Sak {
        val innvilgelseClock = Clock.offset(clock, Duration.ofSeconds(20))
        val behandlingId = RammebehandlingId.random()
        val saksbehandler = ObjectMother.saksbehandler()
        val beslutter = ObjectMother.beslutter()
        val iverksatt = ObjectMother.nyOpprettetRevurderingInnvilgelse(
            clock = innvilgelseClock,
            id = behandlingId,
            sakId = id,
            saksnummer = saksnummer,
            fnr = fnr,
            saksbehandler = saksbehandler,
            saksopplysningsperiode = innvilgetDeltakelse.periode!!,
            hentSaksopplysninger = { oppslagsperiode ->
                ObjectMother.saksopplysninger(
                    tiltaksdeltakelse = listOf(innvilgetDeltakelse),
                    oppslagsperiode = oppslagsperiode,
                    clock = innvilgelseClock,
                )
            },
        ).oppdaterInnvilgelse(
            kommando = ObjectMother.oppdaterRevurderingInnvilgelseKommando(
                sakId = id,
                behandlingId = behandlingId,
                saksbehandler = saksbehandler,
                innvilgelsesperioder = listOf(
                    ObjectMother.innvilgelsesperiodeKommando(
                        innvilgelsesperiode = innvilgelsesperiode,
                        tiltaksdeltakelse = innvilgetDeltakelse,
                    ),
                ),
                barnetillegg = Barnetillegg.utenBarnetillegg(nonEmptyListOf(innvilgelsesperiode)),
            ),
            utbetaling = null,
            omgjørRammevedtak = rammevedtaksliste.finnVedtakSomOmgjøres(innvilgelsesperiode),
            clock = innvilgelseClock,
        ).getOrFail()
            .tilBeslutning(saksbehandler = saksbehandler, clock = innvilgelseClock)
            .taBehandling(beslutter, innvilgelseClock).getOrFail().first
            .iverksett(
                utøvendeBeslutter = beslutter,
                attestering = ObjectMother.godkjentAttestering(beslutter),
                correlationId = CorrelationId.generate(),
                clock = innvilgelseClock,
            ).getOrFail().first as Revurdering

        return leggTilRevurdering(iverksatt).opprettRammevedtak(iverksatt, innvilgelseClock).getOrFail().first
    }
}
