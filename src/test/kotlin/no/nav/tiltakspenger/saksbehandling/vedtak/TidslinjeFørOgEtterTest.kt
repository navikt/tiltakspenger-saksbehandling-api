package no.nav.tiltakspenger.saksbehandling.vedtak

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.juni
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.barnetillegg
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettOmgjøringInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettOmgjøringOpphør
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOmgjøringInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingOgAvbryt
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandlingMedAvslag
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import org.junit.jupiter.api.Test

class TidslinjeFørOgEtterTest {

    private val tiDager = AntallDagerForMeldeperiode(10)
    private val ingenBarn = AntallBarn(0)

    @Test
    fun `søknadsbehandling under behandling uten tidligere vedtak gir tom før-tidslinje`() {
        withTestApplicationContext { tac ->
            val periode = 1.januar(2025) til 31.mars(2025)
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(periode),
            )

            val tidslinje = sak.rammevedtaksliste.tidslinjeFørOgEtter(behandling).shouldNotBeNull()

            tidslinje.periode shouldBe periode
            tidslinje.før shouldBe Periodisering.empty()
            tidslinje.etter.tilPair() shouldBe listOf(
                periode to Tidslinjeverdi(
                    rammebehandlingId = behandling.id,
                    rammevedtakId = null,
                    resultat = Tidslinjeresultat.SØKNADSBEHANDLING_INNVILGELSE,
                    antallDagerPerMeldeperiode = tiDager,
                    antallBarn = ingenBarn,
                ),
            )
            tidslinje.harEndring shouldBe true
        }
    }

    @Test
    fun `vedtatt stans bruker kun tidligere vedtak i før-tidslinjen`() {
        withTestApplicationContext { tac ->
            val innvilgelsesperiode = 1.januar(2025) til 31.mars(2025)
            val stansperiode = 1.februar(2025) til 31.mars(2025)
            val (sak, _, søknadsvedtak, stansvedtak, json) = iverksettSøknadsbehandlingOgRevurderingStans(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(innvilgelsesperiode),
                stansFraOgMed = stansperiode.fraOgMed,
            )

            val tidslinje = sak.rammevedtaksliste.tidslinjeFørOgEtter(stansvedtak.rammebehandling).shouldNotBeNull()

            tidslinje.periode shouldBe stansperiode
            tidslinje.før.tilPair() shouldBe listOf(
                stansperiode to Tidslinjeverdi(
                    rammebehandlingId = søknadsvedtak.rammebehandling.id,
                    rammevedtakId = søknadsvedtak.id,
                    resultat = Tidslinjeresultat.SØKNADSBEHANDLING_INNVILGELSE,
                    antallDagerPerMeldeperiode = tiDager,
                    antallBarn = ingenBarn,
                ),
            )
            tidslinje.etter.tilPair() shouldBe listOf(
                stansperiode to Tidslinjeverdi(
                    rammebehandlingId = stansvedtak.rammebehandling.id,
                    rammevedtakId = stansvedtak.id,
                    resultat = Tidslinjeresultat.STANS,
                    antallDagerPerMeldeperiode = null,
                    antallBarn = null,
                ),
            )
            tidslinje.harEndring shouldBe true

            json.getJSONObject("tidslinjeFørOgEtter").toString().shouldEqualJson(
                """
                {
                  "periode": { "fraOgMed": "2025-02-01", "tilOgMed": "2025-03-31" },
                  "før": [
                    {
                      "periode": { "fraOgMed": "2025-02-01", "tilOgMed": "2025-03-31" },
                      "tidslinjeResultat": "SØKNADSBEHANDLING_INNVILGELSE",
                      "behandlingId": "${søknadsvedtak.rammebehandling.id}",
                      "rammevedtakId": "${søknadsvedtak.id}",
                      "antallDagerPerMeldeperiode": 10,
                      "antallBarn": 0
                    }
                  ],
                  "etter": [
                    {
                      "periode": { "fraOgMed": "2025-02-01", "tilOgMed": "2025-03-31" },
                      "tidslinjeResultat": "STANS",
                      "behandlingId": "${stansvedtak.rammebehandling.id}",
                      "rammevedtakId": "${stansvedtak.id}",
                      "antallDagerPerMeldeperiode": null,
                      "antallBarn": null
                    }
                  ],
                  "harEndring": true
                }
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `stans over hull i innvilgelsen beholder hullet i før-tidslinjen`() {
        withTestApplicationContext { tac ->
            val januar = 1.januar(2025) til 31.januar(2025)
            val mars = 1.mars(2025) til 31.mars(2025)
            val (sak, _, søknadsvedtak, stansvedtak) = iverksettSøknadsbehandlingOgRevurderingStans(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(januar, mars),
                stansFraOgMed = januar.fraOgMed,
            )

            val tidslinje = sak.rammevedtaksliste.tidslinjeFørOgEtter(stansvedtak.rammebehandling).shouldNotBeNull()

            tidslinje.periode shouldBe (januar.fraOgMed til mars.tilOgMed)
            tidslinje.før.tilPair().map { (periode, verdi) -> periode to verdi.rammevedtakId } shouldBe listOf(
                januar to søknadsvedtak.id,
                mars to søknadsvedtak.id,
            )
            tidslinje.etter.tilPair().map { (periode, verdi) -> periode to verdi.resultat } shouldBe listOf(
                tidslinje.periode to Tidslinjeresultat.STANS,
            )
            tidslinje.harEndring shouldBe true
        }
    }

    @Test
    fun `omgjøring med kortere innvilgelse opphører resten av vedtaksperioden`() {
        withTestApplicationContext { tac ->
            val vedtaksperiode = 1.januar(2025) til 31.mars(2025)
            val omgjortInnvilgelsesperiode = 1.februar(2025) til 31.mars(2025)
            val deltakelse = tiltaksdeltakelse(fom = vedtaksperiode.fraOgMed, tom = vedtaksperiode.tilOgMed)
            val (sak, _, søknadsvedtak, omgjøringsvedtak) = iverksettSøknadsbehandlingOgOmgjøringInnvilgelse(
                tac = tac,
                søknadsbehandlingInnvilgelsesperioder = innvilgelsesperioder(vedtaksperiode, deltakelse),
                omgjøringInnvilgelsesperioder = innvilgelsesperioder(omgjortInnvilgelsesperiode, deltakelse),
                omgjøringVedtaksperiode = vedtaksperiode,
            ).shouldNotBeNull()

            val tidslinje = sak.rammevedtaksliste.tidslinjeFørOgEtter(omgjøringsvedtak.rammebehandling).shouldNotBeNull()

            tidslinje.periode shouldBe vedtaksperiode
            tidslinje.før.tilPair().map { (periode, verdi) -> periode to verdi.rammevedtakId } shouldBe listOf(
                vedtaksperiode to søknadsvedtak.id,
            )
            tidslinje.etter.tilPair().map { (periode, verdi) -> periode to verdi.resultat } shouldBe listOf(
                (1.januar(2025) til 31.januar(2025)) to Tidslinjeresultat.OMGJØRING_OPPHØR,
                omgjortInnvilgelsesperiode to Tidslinjeresultat.OMGJØRING_INNVILGELSE,
            )
            tidslinje.harEndring shouldBe true
        }
    }

    @Test
    fun `omgjøring som ikke endrer retten har ingen endring`() {
        withTestApplicationContext { tac ->
            val vedtaksperiode = 1.januar(2025) til 31.mars(2025)
            val (sak, _, _, omgjøringsvedtak) = iverksettSøknadsbehandlingOgOmgjøringInnvilgelse(
                tac = tac,
                søknadsbehandlingInnvilgelsesperioder = innvilgelsesperioder(vedtaksperiode),
            ).shouldNotBeNull()

            val tidslinje = sak.rammevedtaksliste.tidslinjeFørOgEtter(omgjøringsvedtak.rammebehandling).shouldNotBeNull()

            tidslinje.etter.tilPair().map { (periode, verdi) -> periode to verdi.resultat } shouldBe listOf(
                vedtaksperiode to Tidslinjeresultat.OMGJØRING_INNVILGELSE,
            )
            tidslinje.harEndring shouldBe false
        }
    }

    @Test
    fun `avbrutt behandling gir ingen tidslinje`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingOgAvbryt(tac = tac).shouldNotBeNull()

            sak.rammevedtaksliste.tidslinjeFørOgEtter(behandling.shouldNotBeNull()) shouldBe null
        }
    }

    @Test
    fun `behandling uten valgt resultat gir ingen tidslinje`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac = tac)

            sak.rammevedtaksliste.tidslinjeFørOgEtter(behandling) shouldBe null
        }
    }

    @Test
    fun `avslag endrer ikke tidslinjen`() {
        withTestApplicationContext { tac ->
            val periode = 1.januar(2025) til 31.mars(2025)
            val deltakelse = tiltaksdeltakelse(fom = periode.fraOgMed, tom = periode.tilOgMed)
            val (sak, _, søknadsvedtak) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(periode, deltakelse),
            )
            val (oppdatertSak, _, avslag) = opprettSøknadsbehandlingUnderBehandlingMedAvslag(
                tac = tac,
                sakId = sak.id,
                fnr = sak.fnr,
                tiltaksdeltakelse = deltakelse,
            )

            val tidslinje = oppdatertSak.rammevedtaksliste.tidslinjeFørOgEtter(avslag).shouldNotBeNull()

            tidslinje.etter shouldBe tidslinje.før
            tidslinje.før.verdier.map { it.rammevedtakId }.distinct() shouldBe listOf(søknadsvedtak.id)
            tidslinje.harEndring shouldBe false
        }
    }

    @Test
    fun `flere delvis overlappende vedtak og omgjøringer gir riktige tidslinjer for alle behandlinger, også tilbake i tid`() {
        withTestApplicationContext { tac ->
            val deltakelse = tiltaksdeltakelse(fom = 1.januar(2025), tom = 30.juni(2025))
            val åtteDager = AntallDagerForMeldeperiode(8)
            val seksDager = AntallDagerForMeldeperiode(6)
            val ettBarn = AntallBarn(1)

            // 1: Søknadsvedtak for januar–mars.
            val søknadsperiode = 1.januar(2025) til 31.mars(2025)
            val (sakEtterVedtak1, _, vedtak1) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(søknadsperiode, deltakelse),
            )
            val sakId = sakEtterVedtak1.id

            // 2: Revurdering som overlapper søknadsvedtaket delvis og forlenger til og med april.
            val revurderingsperiode = 1.februar(2025) til 30.april(2025)
            val (sakEtterVedtak2, vedtak2) = iverksettRevurderingInnvilgelse(
                tac = tac,
                sakId = sakId,
                innvilgelsesperioder = innvilgelsesperioder(revurderingsperiode, deltakelse, åtteDager),
                barnetilleggRevurdering = barnetillegg(periode = revurderingsperiode, antallBarn = ettBarn),
            )

            // 3: Omgjøring av en del av søknadsvedtaket, der bare slutten av omgjøringsperioden innvilges.
            val omgjøringsperiode = 10.januar(2025) til 20.januar(2025)
            val omgjortInnvilgelse = 15.januar(2025) til 20.januar(2025)
            val (sakEtterVedtak3, vedtak3) = iverksettOmgjøringInnvilgelse(
                tac = tac,
                sakId = sakId,
                rammevedtakIdSomOmgjøres = vedtak1.id,
                innvilgelsesperioder = innvilgelsesperioder(omgjortInnvilgelse, deltakelse, seksDager),
                vedtaksperiode = omgjøringsperiode,
            )

            // 4: Opphør av hele revurderingsvedtaket.
            val (sakEtterVedtak4, vedtak4) = iverksettOmgjøringOpphør(
                tac = tac,
                sakId = sakId,
                rammevedtakIdSomOmgjøres = vedtak2.id,
                vedtaksperiode = revurderingsperiode,
            )

            // 5: Revurdering under behandling som overlapper alle vedtakene.
            val nyPeriode = 1.januar(2025) til 30.juni(2025)
            val (_, startetRevurdering) = startRevurderingInnvilgelse(tac = tac, sakId = sakId).shouldNotBeNull()
            val (sakNå, revurdering) = oppdaterRevurderingInnvilgelse(
                tac = tac,
                sakId = sakId,
                behandlingId = startetRevurdering.id,
                innvilgelsesperioder = innvilgelsesperioder(nyPeriode, deltakelse),
            )

            val forventetVedtak1 = Forventet(
                periode = søknadsperiode,
                før = emptyList(),
                etter = listOf(
                    søknadsperiode to vedtak1.tidslinjeverdi(Tidslinjeresultat.SØKNADSBEHANDLING_INNVILGELSE, tiDager, ingenBarn),
                ),
            )
            val forventetVedtak2 = Forventet(
                periode = revurderingsperiode,
                før = listOf(
                    (1.februar(2025) til 31.mars(2025)) to vedtak1.tidslinjeverdi(Tidslinjeresultat.SØKNADSBEHANDLING_INNVILGELSE, tiDager, ingenBarn),
                ),
                etter = listOf(
                    revurderingsperiode to vedtak2.tidslinjeverdi(Tidslinjeresultat.REVURDERING_INNVILGELSE, åtteDager, ettBarn),
                ),
            )
            val forventetVedtak3 = Forventet(
                periode = omgjøringsperiode,
                før = listOf(
                    omgjøringsperiode to vedtak1.tidslinjeverdi(Tidslinjeresultat.SØKNADSBEHANDLING_INNVILGELSE, tiDager, ingenBarn),
                ),
                etter = listOf(
                    (10.januar(2025) til 14.januar(2025)) to vedtak3.tidslinjeverdi(Tidslinjeresultat.OMGJØRING_OPPHØR),
                    omgjortInnvilgelse to vedtak3.tidslinjeverdi(Tidslinjeresultat.OMGJØRING_INNVILGELSE, seksDager, ingenBarn),
                ),
            )
            val forventetVedtak4 = Forventet(
                periode = revurderingsperiode,
                før = listOf(
                    revurderingsperiode to vedtak2.tidslinjeverdi(Tidslinjeresultat.REVURDERING_INNVILGELSE, åtteDager, ettBarn),
                ),
                etter = listOf(
                    revurderingsperiode to vedtak4.tidslinjeverdi(Tidslinjeresultat.OMGJØRING_OPPHØR),
                ),
            )
            val forventetRevurdering = Forventet(
                periode = nyPeriode,
                før = listOf(
                    (1.januar(2025) til 9.januar(2025)) to vedtak1.tidslinjeverdi(Tidslinjeresultat.SØKNADSBEHANDLING_INNVILGELSE, tiDager, ingenBarn),
                    (10.januar(2025) til 14.januar(2025)) to vedtak3.tidslinjeverdi(Tidslinjeresultat.OMGJØRING_OPPHØR),
                    omgjortInnvilgelse to vedtak3.tidslinjeverdi(Tidslinjeresultat.OMGJØRING_INNVILGELSE, seksDager, ingenBarn),
                    (21.januar(2025) til 31.januar(2025)) to vedtak1.tidslinjeverdi(Tidslinjeresultat.SØKNADSBEHANDLING_INNVILGELSE, tiDager, ingenBarn),
                    revurderingsperiode to vedtak4.tidslinjeverdi(Tidslinjeresultat.OMGJØRING_OPPHØR),
                ),
                etter = listOf(
                    nyPeriode to Tidslinjeverdi(
                        rammebehandlingId = revurdering.id,
                        rammevedtakId = null,
                        resultat = Tidslinjeresultat.REVURDERING_INNVILGELSE,
                        antallDagerPerMeldeperiode = tiDager,
                        antallBarn = ingenBarn,
                    ),
                ),
            )

            // Tidslinjene slik de var rett etter hver iverksettelse.
            sakEtterVedtak1.skalHaTidslinje(vedtak1.rammebehandling, forventetVedtak1)
            sakEtterVedtak2.skalHaTidslinje(vedtak2.rammebehandling, forventetVedtak2)
            sakEtterVedtak3.skalHaTidslinje(vedtak3.rammebehandling, forventetVedtak3)
            sakEtterVedtak4.skalHaTidslinje(vedtak4.rammebehandling, forventetVedtak4)

            // Senere vedtak skal ikke endre tidslinjene til tidligere vedtak.
            sakNå.skalHaTidslinje(vedtak1.rammebehandling, forventetVedtak1)
            sakNå.skalHaTidslinje(vedtak2.rammebehandling, forventetVedtak2)
            sakNå.skalHaTidslinje(vedtak3.rammebehandling, forventetVedtak3)
            sakNå.skalHaTidslinje(vedtak4.rammebehandling, forventetVedtak4)
            sakNå.skalHaTidslinje(revurdering, forventetRevurdering)
        }
    }

    @Test
    fun `vedtak som overlapper hele tidligere vedtak gir riktige tidslinjer for alle behandlinger, også tilbake i tid`() {
        withTestApplicationContext { tac ->
            val periode = 1.januar(2025) til 31.mars(2025)
            val deltakelse = tiltaksdeltakelse(fom = periode.fraOgMed, tom = periode.tilOgMed)
            val toBarn = AntallBarn(2)

            // 1: Søknadsvedtak uten barn.
            val (sakEtterVedtak1, _, vedtak1) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(periode, deltakelse),
            )
            val sakId = sakEtterVedtak1.id

            // 2: Revurdering over hele søknadsvedtaket, med to barn.
            val (sakEtterVedtak2, vedtak2) = iverksettRevurderingInnvilgelse(
                tac = tac,
                sakId = sakId,
                innvilgelsesperioder = innvilgelsesperioder(periode, deltakelse),
                barnetilleggRevurdering = barnetillegg(periode = periode, antallBarn = toBarn),
            )

            // 3: Stans fra mars.
            val stansperiode = 1.mars(2025) til 31.mars(2025)
            val (sakEtterVedtak3, vedtak3) = iverksettRevurderingStans(
                tac = tac,
                sakId = sakId,
                stansFraOgMed = stansperiode.fraOgMed,
            )

            // 4: Omgjøring av hele den gjeldende delen av revurderingen, uten barn.
            val omgjøringsperiode = 1.januar(2025) til 28.februar(2025)
            val (sakEtterVedtak4, vedtak4) = iverksettOmgjøringInnvilgelse(
                tac = tac,
                sakId = sakId,
                rammevedtakIdSomOmgjøres = vedtak2.id,
                innvilgelsesperioder = innvilgelsesperioder(omgjøringsperiode, deltakelse),
                vedtaksperiode = omgjøringsperiode,
            )

            val forventetVedtak1 = Forventet(
                periode = periode,
                før = emptyList(),
                etter = listOf(
                    periode to vedtak1.tidslinjeverdi(Tidslinjeresultat.SØKNADSBEHANDLING_INNVILGELSE, tiDager, ingenBarn),
                ),
            )
            val forventetVedtak2 = Forventet(
                periode = periode,
                før = listOf(
                    periode to vedtak1.tidslinjeverdi(Tidslinjeresultat.SØKNADSBEHANDLING_INNVILGELSE, tiDager, ingenBarn),
                ),
                etter = listOf(
                    periode to vedtak2.tidslinjeverdi(Tidslinjeresultat.REVURDERING_INNVILGELSE, tiDager, toBarn),
                ),
            )
            val forventetVedtak3 = Forventet(
                periode = stansperiode,
                før = listOf(
                    stansperiode to vedtak2.tidslinjeverdi(Tidslinjeresultat.REVURDERING_INNVILGELSE, tiDager, toBarn),
                ),
                etter = listOf(
                    stansperiode to vedtak3.tidslinjeverdi(Tidslinjeresultat.STANS),
                ),
            )
            val forventetVedtak4 = Forventet(
                periode = omgjøringsperiode,
                før = listOf(
                    omgjøringsperiode to vedtak2.tidslinjeverdi(Tidslinjeresultat.REVURDERING_INNVILGELSE, tiDager, toBarn),
                ),
                etter = listOf(
                    omgjøringsperiode to vedtak4.tidslinjeverdi(Tidslinjeresultat.OMGJØRING_INNVILGELSE, tiDager, ingenBarn),
                ),
            )

            sakEtterVedtak1.skalHaTidslinje(vedtak1.rammebehandling, forventetVedtak1)
            sakEtterVedtak2.skalHaTidslinje(vedtak2.rammebehandling, forventetVedtak2)
            sakEtterVedtak3.skalHaTidslinje(vedtak3.rammebehandling, forventetVedtak3)
            sakEtterVedtak4.skalHaTidslinje(vedtak4.rammebehandling, forventetVedtak4)

            sakEtterVedtak4.skalHaTidslinje(vedtak1.rammebehandling, forventetVedtak1)
            sakEtterVedtak4.skalHaTidslinje(vedtak2.rammebehandling, forventetVedtak2)
            sakEtterVedtak4.skalHaTidslinje(vedtak3.rammebehandling, forventetVedtak3)
        }
    }

    private data class Forventet(
        val periode: Periode,
        val før: List<Pair<Periode, Tidslinjeverdi>>,
        val etter: List<Pair<Periode, Tidslinjeverdi>>,
        val harEndring: Boolean = true,
    )

    private fun Sak.skalHaTidslinje(rammebehandling: Rammebehandling, forventet: Forventet) {
        val tidslinje = rammevedtaksliste.tidslinjeFørOgEtter(rammebehandling).shouldNotBeNull()
        tidslinje.periode shouldBe forventet.periode
        tidslinje.før.tilPair() shouldBe forventet.før
        tidslinje.etter.tilPair() shouldBe forventet.etter
        tidslinje.harEndring shouldBe forventet.harEndring
    }

    private fun Rammevedtak.tidslinjeverdi(
        resultat: Tidslinjeresultat,
        antallDagerPerMeldeperiode: AntallDagerForMeldeperiode? = null,
        antallBarn: AntallBarn? = null,
    ) = Tidslinjeverdi(
        rammebehandlingId = rammebehandling.id,
        rammevedtakId = id,
        resultat = resultat,
        antallDagerPerMeldeperiode = antallDagerPerMeldeperiode,
        antallBarn = antallBarn,
    )
}
