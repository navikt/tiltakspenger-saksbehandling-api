package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import arrow.core.nonEmptyListOf
import arrow.core.nonEmptySetOf
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.SøknadsbehandlingsresultatType
import no.nav.tiltakspenger.saksbehandling.behandling.shouldBeRevurderingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.shouldBeSøknadsbehandlingDTO
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningerVedtatt
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.gyldigFnr
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperiodeKommando
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.nyInnvilgbarSøknad
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.nyOpprettetRevurderingOmgjøring
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.nyOpprettetSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.nyRammevedtakInnvilgelse
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.nyVedtattRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.nyVedtattRevurderingStans
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.nyVedtattSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksopplysninger
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.søknadstiltak
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.vedtak.Vedtaksliste
import org.junit.jupiter.api.Test

class RammebehandlingDTOTest {
    private val behandlingId = RammebehandlingId.fromString("beh_01K8R3V8S9X8KGR8HDXXDXN9P3")
    private val sakId = SakId.fromString("sak_01K8QWMR1KZZB728K0F4RQG184")
    private val saksnummer = Saksnummer("202510291001")
    private val vedtakId = VedtakId.fromString("vedtak_01J94XH6CKY0SZ5FBEE6YZG8S6")
    private val søknadId = SøknadId.fromString("soknad_01K8QWMR32C2X5Y5T4N945BF9V")
    private val søknadTiltakId = "06872f2f-5ca4-453a-8d41-8e91e1f777a3"
    private val eksternTiltaksdeltakelseId = "f02e50df-d2ee-47f6-9afa-db66bd842bfd"
    private val eksternTiltaksgjennomføringsId = "68f04dee-11a9-4d69-84fd-1096a4264492"
    private val internTiltaksdeltakelseId = TiltaksdeltakerId.fromString("tiltaksdeltaker_01KEF73CZJX0MKYG4NK27BV7HG")
    private val soknadstiltakInternTiltaksdeltakelseId = TiltaksdeltakerId.fromString("tiltaksdeltaker_01KEXQCG2FZV0629GX7QM4W1DV")
    private val fnr = gyldigFnr()

    private val vedtaksperiode = Periode(
        fraOgMed = 1.januar(2025),
        tilOgMed = 31.mars(2025),
    )
    private val beregninger =
        MeldeperiodeBeregningerVedtatt.fraVedtaksliste(Vedtaksliste.empty())

    @Test
    fun `Rammebehandling DTO fra søknadsbehandling med innvilgelse`() {
        val clock = fixedClock

        val behandling = nyVedtattSøknadsbehandling(
            clock = clock,
            id = behandlingId,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            resultat = SøknadsbehandlingsresultatType.INNVILGELSE,
            saksopplysningsperiode = vedtaksperiode,
            søknad = nyInnvilgbarSøknad(
                id = søknadId,
                søknadstiltak = søknadstiltak(
                    deltakelseFom = vedtaksperiode.fraOgMed,
                    deltakelseTom = vedtaksperiode.tilOgMed,
                    id = eksternTiltaksdeltakelseId,
                    tiltaksdeltakerId = internTiltaksdeltakelseId,
                ),
            ),
            saksopplysninger = saksopplysninger(
                fom = vedtaksperiode.fraOgMed,
                tom = vedtaksperiode.tilOgMed,
                tiltaksdeltakelse = listOf(
                    tiltaksdeltakelse(
                        fom = vedtaksperiode.fraOgMed,
                        tom = vedtaksperiode.tilOgMed,
                        eksternTiltaksdeltakelseId = eksternTiltaksdeltakelseId,
                        eksternTiltaksgjennomføringsId = eksternTiltaksgjennomføringsId,
                        internDeltakelseId = internTiltaksdeltakelseId,
                    ),
                ),
                clock = clock,
            ),
        )

        val behandlingJson = serialize(
            behandling.tilSøknadsbehandlingDTO(
                utbetalingsstatus = null,
                beregninger = beregninger,
                tilbakekrevingId = null,
                rammevedtakId = vedtakId,
            ),
        )

        behandlingJson.shouldBeSøknadsbehandlingDTO(
            sakId = sakId,
            saksnummer = saksnummer,
            behandlingId = behandlingId,
            rammevedtakId = vedtakId,
            søknadId = søknadId,
            vedtaksperiode = """{"fraOgMed": "2025-01-01","tilOgMed": "2025-03-31"}""",
            attesteringer = listOf("dummy"),
            saksbehandler = "Z12345",
            fritekstTilVedtaksbrev = "nyBehandlingUnderBeslutning()",
            begrunnelseVilkårsvurdering = "nyBehandlingUnderBeslutning()",
            internDeltakelseId = internTiltaksdeltakelseId.toString(),
            eksternDeltagelseId = eksternTiltaksdeltakelseId,
            søknadTiltakId = eksternTiltaksdeltakelseId,
            periodeFraOgMed = "2025-01-01",
            periodeTilOgMed = "2025-03-31",
            journalpostId = "journalpostId",
            eksternTiltaksgjennomføringsId = eksternTiltaksgjennomføringsId,
            søknadTiltakTypeNavn = "Gruppe AMO",
            deltakelseProsent = "100.0",
            antallDagerPerUke = "5.0",
        )
    }

    @Test
    fun `Rammebehandling DTO fra søknadsbehandling med avslag`() {
        val clock = fixedClock

        val behandling = nyVedtattSøknadsbehandling(
            clock = clock,
            id = behandlingId,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            resultat = SøknadsbehandlingsresultatType.AVSLAG,
            avslagsgrunner = nonEmptySetOf(Avslagsgrunnlag.DeltarIkkePåArbeidsmarkedstiltak),
            saksopplysningsperiode = vedtaksperiode,
            søknad = nyInnvilgbarSøknad(
                id = søknadId,
                søknadstiltak = søknadstiltak(
                    deltakelseFom = vedtaksperiode.fraOgMed,
                    deltakelseTom = vedtaksperiode.tilOgMed,
                    id = søknadTiltakId,
                    tiltaksdeltakerId = soknadstiltakInternTiltaksdeltakelseId,
                ),
            ),
            saksopplysninger = saksopplysninger(
                fom = vedtaksperiode.fraOgMed,
                tom = vedtaksperiode.tilOgMed,
                tiltaksdeltakelse = listOf(
                    tiltaksdeltakelse(
                        fom = vedtaksperiode.fraOgMed,
                        tom = vedtaksperiode.tilOgMed,
                        eksternTiltaksdeltakelseId = eksternTiltaksdeltakelseId,
                        eksternTiltaksgjennomføringsId = eksternTiltaksgjennomføringsId,
                        internDeltakelseId = internTiltaksdeltakelseId,
                    ),
                ),
                clock = clock,
            ),
        )

        val behandlingJson = serialize(
            behandling.tilSøknadsbehandlingDTO(
                utbetalingsstatus = null,
                beregninger = beregninger,
                tilbakekrevingId = null,
                rammevedtakId = vedtakId,
            ),
        )

        behandlingJson.shouldBeSøknadsbehandlingDTO(
            sakId = sakId,
            saksnummer = saksnummer,
            behandlingId = behandlingId,
            rammevedtakId = vedtakId,
            søknadId = søknadId,
            vedtaksperiode = """{"fraOgMed": "2025-01-01","tilOgMed": "2025-03-31"}""",
            attesteringer = listOf("dummy"),
            saksbehandler = "Z12345",
            fritekstTilVedtaksbrev = "nyBehandlingUnderBeslutning()",
            begrunnelseVilkårsvurdering = "nyBehandlingUnderBeslutning()",
            resultat = RammebehandlingResultatTypeDTO.AVSLAG,
            kanInnvilges = false,
            avslagsgrunner = listOf("DeltarIkkePåArbeidsmarkedstiltak"),
            barnetillegg = false,
            innvilgelsesperiode = false,
            internDeltakelseId = internTiltaksdeltakelseId.toString(),
            eksternDeltagelseId = eksternTiltaksdeltakelseId,
            søknadTiltakId = søknadTiltakId,
            periodeFraOgMed = "2025-01-01",
            periodeTilOgMed = "2025-03-31",
            journalpostId = "journalpostId",
            eksternTiltaksgjennomføringsId = eksternTiltaksgjennomføringsId,
            søknadTiltakTypeNavn = "Gruppe AMO",
            deltakelseProsent = "100.0",
            antallDagerPerUke = "5.0",
        )
    }

    @Test
    fun `Rammebehandling DTO fra søknadsbehandling uten valgt resultat`() {
        val clock = fixedClock

        val behandling = nyOpprettetSøknadsbehandling(
            clock = clock,
            id = behandlingId,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            søknad = nyInnvilgbarSøknad(
                id = søknadId,
                søknadstiltak = søknadstiltak(
                    deltakelseFom = vedtaksperiode.fraOgMed,
                    deltakelseTom = vedtaksperiode.tilOgMed,
                    id = eksternTiltaksdeltakelseId,
                    tiltaksdeltakerId = internTiltaksdeltakelseId,
                ),
            ),
            hentSaksopplysninger = { _, _, _, _, _ ->
                saksopplysninger(
                    fom = vedtaksperiode.fraOgMed,
                    tom = vedtaksperiode.tilOgMed,
                    tiltaksdeltakelse = listOf(
                        tiltaksdeltakelse(
                            fom = vedtaksperiode.fraOgMed,
                            tom = vedtaksperiode.tilOgMed,
                            eksternTiltaksdeltakelseId = eksternTiltaksdeltakelseId,
                            eksternTiltaksgjennomføringsId = eksternTiltaksgjennomføringsId,
                            internDeltakelseId = internTiltaksdeltakelseId,
                        ),
                    ),
                    clock = clock,
                )
            },
        )

        val behandlingJson = serialize(
            behandling.tilSøknadsbehandlingDTO(
                utbetalingsstatus = null,
                beregninger = beregninger,
                tilbakekrevingId = null,
                rammevedtakId = vedtakId,
            ),
        )

        behandlingJson.shouldBeSøknadsbehandlingDTO(
            sakId = sakId,
            saksnummer = saksnummer,
            behandlingId = behandlingId,
            rammevedtakId = vedtakId,
            søknadId = søknadId,
            vedtaksperiode = null,
            iverksattTidspunkt = null,
            attesteringer = emptyList(),
            saksbehandler = "Z12345",
            beslutter = null,
            fritekstTilVedtaksbrev = null,
            begrunnelseVilkårsvurdering = null,
            resultat = RammebehandlingResultatTypeDTO.IKKE_VALGT,
            barnetillegg = false,
            innvilgelsesperiode = false,
            status = "UNDER_BEHANDLING",
            internDeltakelseId = internTiltaksdeltakelseId.toString(),
            eksternDeltagelseId = eksternTiltaksdeltakelseId,
            søknadTiltakId = eksternTiltaksdeltakelseId,
            periodeFraOgMed = "2025-01-01",
            periodeTilOgMed = "2025-03-31",
            journalpostId = "journalpostId",
            eksternTiltaksgjennomføringsId = eksternTiltaksgjennomføringsId,
            søknadTiltakTypeNavn = "Gruppe AMO",
            deltakelseProsent = "100.0",
            antallDagerPerUke = "5.0",
        )
    }

    @Test
    fun `Rammebehandling DTO fra revurdering med innvilgelse`() {
        val clock = fixedClock

        val behandling = nyVedtattRevurderingInnvilgelse(
            clock = clock,
            id = behandlingId,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            saksopplysningsperiode = vedtaksperiode,
            saksopplysninger = saksopplysninger(
                fom = vedtaksperiode.fraOgMed,
                tom = vedtaksperiode.tilOgMed,
                tiltaksdeltakelse = listOf(
                    tiltaksdeltakelse(
                        fom = vedtaksperiode.fraOgMed,
                        tom = vedtaksperiode.tilOgMed,
                        eksternTiltaksdeltakelseId = eksternTiltaksdeltakelseId,
                        eksternTiltaksgjennomføringsId = eksternTiltaksgjennomføringsId,
                        internDeltakelseId = internTiltaksdeltakelseId,
                    ),
                ),
                clock = clock,
            ),
        )

        val behandlingJson = serialize(
            behandling.tilRevurderingDTO(
                utbetalingsstatus = null,
                beregninger = beregninger,
                tilbakekrevingId = null,
                rammevedtakId = vedtakId,
            ),
        )

        behandlingJson.shouldBeRevurderingDTO(
            sakId = sakId,
            saksnummer = saksnummer,
            behandlingId = behandlingId,
            rammevedtakId = vedtakId,
            saksbehandler = "Z12345",
            fritekstTilVedtaksbrev = "nyRevurderingKlarTilBeslutning()",
            begrunnelseVilkårsvurdering = "nyRevurderingKlarTilBeslutning()",
            resultat = RammebehandlingResultatTypeDTO.REVURDERING_INNVILGELSE,
            internDeltakelseId = internTiltaksdeltakelseId.toString(),
            eksternDeltagelseId = eksternTiltaksdeltakelseId,
            vedtaksperiode = """{"fraOgMed": "2025-01-01","tilOgMed": "2025-03-31"}""",
            periodeFraOgMed = "2025-01-01",
            periodeTilOgMed = "2025-03-31",
            eksternTiltaksgjennomføringsId = eksternTiltaksgjennomføringsId,
            deltakelseProsent = "100.0",
            antallDagerPerUke = "5.0",
        )
    }

    @Test
    fun `Rammebehandling DTO fra revurdering med stans`() {
        val clock = fixedClock

        val behandling = nyVedtattRevurderingStans(
            clock = clock,
            id = behandlingId,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            vedtaksperiode = vedtaksperiode,
            stansFraOgMed = vedtaksperiode.fraOgMed.plusDays(7),
            førsteDagSomGirRett = vedtaksperiode.fraOgMed,
            sisteDagSomGirRett = vedtaksperiode.tilOgMed,
            saksopplysninger = saksopplysninger(
                fom = vedtaksperiode.fraOgMed,
                tom = vedtaksperiode.tilOgMed,
                tiltaksdeltakelse = listOf(
                    tiltaksdeltakelse(
                        fom = vedtaksperiode.fraOgMed,
                        tom = vedtaksperiode.tilOgMed,
                        eksternTiltaksdeltakelseId = eksternTiltaksdeltakelseId,
                        eksternTiltaksgjennomføringsId = eksternTiltaksgjennomføringsId,
                        internDeltakelseId = internTiltaksdeltakelseId,
                    ),
                ),
                clock = clock,
            ),
        )

        val behandlingJson = serialize(
            behandling.tilRevurderingDTO(
                utbetalingsstatus = null,
                beregninger = beregninger,
                tilbakekrevingId = null,
                rammevedtakId = vedtakId,
            ),
        )

        behandlingJson.shouldBeRevurderingDTO(
            sakId = sakId,
            saksnummer = saksnummer,
            behandlingId = behandlingId,
            rammevedtakId = vedtakId,
            saksbehandler = "Z12345",
            fritekstTilVedtaksbrev = "nyRevurderingKlarTilBeslutning()",
            begrunnelseVilkårsvurdering = "nyRevurderingKlarTilBeslutning()",
            resultat = RammebehandlingResultatTypeDTO.STANS,
            barnetillegg = false,
            innvilgelsesperiode = false,
            valgtHjemmelHarIkkeRettighet = listOf("DeltarIkkePåArbeidsmarkedstiltak"),
            harValgtStansFraFørsteDagSomGirRett = false,
            internDeltakelseId = internTiltaksdeltakelseId.toString(),
            eksternDeltagelseId = eksternTiltaksdeltakelseId,
            vedtaksperiode = """{"fraOgMed": "2025-01-08","tilOgMed": "2025-03-31"}""",
            periodeFraOgMed = "2025-01-01",
            periodeTilOgMed = "2025-03-31",
            eksternTiltaksgjennomføringsId = eksternTiltaksgjennomføringsId,
            deltakelseProsent = "100.0",
            antallDagerPerUke = "5.0",
        )
    }

    @Test
    fun `Rammebehandling DTO fra revurdering med omgjøring uten valgt resultat`() {
        val nyClock = fixedClock

        val behandling = nyVedtattSøknadsbehandling(
            clock = nyClock,
            id = behandlingId,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            resultat = SøknadsbehandlingsresultatType.INNVILGELSE,
            saksopplysningsperiode = vedtaksperiode,
            søknad = nyInnvilgbarSøknad(
                id = søknadId,
                søknadstiltak = søknadstiltak(
                    deltakelseFom = vedtaksperiode.fraOgMed,
                    deltakelseTom = vedtaksperiode.tilOgMed,
                    id = søknadTiltakId,
                    tiltaksdeltakerId = soknadstiltakInternTiltaksdeltakelseId,
                ),
            ),
            saksopplysninger = saksopplysninger(
                fom = vedtaksperiode.fraOgMed,
                tom = vedtaksperiode.tilOgMed,
                tiltaksdeltakelse = listOf(
                    tiltaksdeltakelse(
                        fom = vedtaksperiode.fraOgMed,
                        tom = vedtaksperiode.tilOgMed,
                        eksternTiltaksdeltakelseId = eksternTiltaksdeltakelseId,
                        eksternTiltaksgjennomføringsId = eksternTiltaksgjennomføringsId,
                        internDeltakelseId = internTiltaksdeltakelseId,
                    ),
                ),
                clock = nyClock,
            ),
        )

        val omgjøringId = RammebehandlingId.fromString("beh_01K8R3V8S9X8KGR8HDXXDXN9P4")
        val omgjøringVedtakId = VedtakId.fromString("vedtak_01J94XH6CKY0SZ5FBEE6YZG8S7")

        val omgjøring = nyOpprettetRevurderingOmgjøring(
            clock = nyClock,
            id = omgjøringId,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            søknadsbehandlingInnvilgelsesperiode = vedtaksperiode.plusFraOgMed(1),
            vedtattInnvilgetSøknadsbehandling = nyRammevedtakInnvilgelse(
                id = vedtakId,
                sakId = sakId,
                innvilgelsesperioder = nonEmptyListOf(
                    innvilgelsesperiodeKommando(innvilgelsesperiode = ObjectMother.vedtaksperiode()),
                ),
                fnr = fnr,
                behandling = behandling,
            ),
            hentSaksopplysninger = {
                saksopplysninger(
                    fom = vedtaksperiode.fraOgMed,
                    tom = vedtaksperiode.tilOgMed,
                    tiltaksdeltakelse = listOf(
                        tiltaksdeltakelse(
                            fom = vedtaksperiode.fraOgMed,
                            tom = vedtaksperiode.tilOgMed,
                            eksternTiltaksdeltakelseId = eksternTiltaksdeltakelseId,
                            eksternTiltaksgjennomføringsId = eksternTiltaksgjennomføringsId,
                            internDeltakelseId = internTiltaksdeltakelseId,
                        ),
                    ),
                    clock = nyClock,
                )
            },
        )

        val behandlingJson = serialize(
            omgjøring.tilRevurderingDTO(
                utbetalingsstatus = null,
                beregninger = beregninger,
                tilbakekrevingId = null,
                rammevedtakId = omgjøringVedtakId,
            ),
        )

        behandlingJson.shouldBeRevurderingDTO(
            sakId = sakId,
            saksnummer = saksnummer,
            behandlingId = omgjøringId,
            rammevedtakId = omgjøringVedtakId,
            omgjørVedtak = vedtakId,
            saksbehandler = "Z12345",
            beslutter = null,
            attesteringer = emptyList(),
            iverksattTidspunkt = null,
            vedtaksperiode = null,
            fritekstTilVedtaksbrev = null,
            begrunnelseVilkårsvurdering = null,
            resultat = RammebehandlingResultatTypeDTO.OMGJØRING_IKKE_VALGT,
            status = "UNDER_BEHANDLING",
            barnetillegg = false,
            innvilgelsesperiode = false,
            internDeltakelseId = internTiltaksdeltakelseId.toString(),
            eksternDeltagelseId = eksternTiltaksdeltakelseId,
            periodeFraOgMed = "2025-01-01",
            periodeTilOgMed = "2025-03-31",
            eksternTiltaksgjennomføringsId = eksternTiltaksgjennomføringsId,
            deltakelseProsent = "100.0",
            antallDagerPerUke = "5.0",
        )
    }
}
