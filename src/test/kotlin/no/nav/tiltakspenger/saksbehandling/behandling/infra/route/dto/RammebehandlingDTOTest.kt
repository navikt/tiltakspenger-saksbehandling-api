package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import arrow.core.nonEmptySetOf
import io.kotest.assertions.json.shouldEqualJson
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingType
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningerVedtatt
import no.nav.tiltakspenger.saksbehandling.objectmothers.KlokkeMother.clock
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.gyldigFnr
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.nyInnvilgbarSøknad
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.nyOpprettetRevurderingOmgjøring
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.nyOpprettetSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.nyRammevedtakInnvilgelse
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.nyVedtattRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.nyVedtattRevurderingStans
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.nyVedtattSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksopplysninger
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.søknadstiltak
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.tiltaksdeltagelse
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.vedtak.Vedtaksliste
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class RammebehandlingDTOTest {
    private val behandlingId = BehandlingId.fromString("beh_01K8R3V8S9X8KGR8HDXXDXN9P3")
    private val sakId = SakId.fromString("sak_01K8QWMR1KZZB728K0F4RQG184")
    private val saksnummer = Saksnummer("202510291001")
    private val vedtakId = VedtakId.fromString("vedtak_01J94XH6CKY0SZ5FBEE6YZG8S6")
    private val søknadId = SøknadId.fromString("soknad_01K8QWMR32C2X5Y5T4N945BF9V")
    private val søknadTiltakId = "06872f2f-5ca4-453a-8d41-8e91e1f777a3"
    private val eksternTiltaksdeltagelseId = "f02e50df-d2ee-47f6-9afa-db66bd842bfd"
    private val eksternTiltaksgjennomføringsId = "68f04dee-11a9-4d69-84fd-1096a4264492"
    private val fnr = gyldigFnr()

    private val virkningsperiode = Periode(
        fraOgMed = 1.januar(2025),
        tilOgMed = 31.mars(2025),
    )
    private val beregninger =
        MeldeperiodeBeregningerVedtatt.fraVedtaksliste(Vedtaksliste.empty())

    @Test
    fun `Rammebehandling DTO fra søknadsbehandling med innvilgelse`() {
        val behandling = nyVedtattSøknadsbehandling(
            clock = clock.copy(),
            id = behandlingId,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            resultat = SøknadsbehandlingType.INNVILGELSE,
            virkningsperiode = virkningsperiode,
            søknad = nyInnvilgbarSøknad(
                id = søknadId,
                søknadstiltak = søknadstiltak(
                    deltakelseFom = virkningsperiode.fraOgMed,
                    deltakelseTom = virkningsperiode.tilOgMed,
                    id = søknadTiltakId,
                ),
            ),
            saksopplysninger = saksopplysninger(
                fom = virkningsperiode.fraOgMed,
                tom = virkningsperiode.tilOgMed,
                tiltaksdeltagelse = tiltaksdeltagelse(
                    fom = virkningsperiode.fraOgMed,
                    tom = virkningsperiode.tilOgMed,
                    eksternTiltaksdeltagelseId = eksternTiltaksdeltagelseId,
                    eksternTiltaksgjennomføringsId = eksternTiltaksgjennomføringsId,
                ),
            ),
        )

        val behandlingJson = serialize(
            behandling.tilSøknadsbehandlingDTO(
                utbetalingsstatus = null,
                beregninger = beregninger,
                rammevedtakId = vedtakId,
            ),
        )

        @Language("JSON")
        val expectedJson = """
            {
              "id" : "beh_01K8R3V8S9X8KGR8HDXXDXN9P3",
              "status" : "VEDTATT",
              "sakId" : "sak_01K8QWMR1KZZB728K0F4RQG184",
              "saksnummer" : "202510291001",
              "rammevedtakId" : "vedtak_01J94XH6CKY0SZ5FBEE6YZG8S6",
              "saksbehandler" : "Z12345",
              "beslutter" : "B12345",
              "saksopplysninger" : {
                "fødselsdato" : "2001-01-01",
                "tiltaksdeltagelse" : [ {
                  "eksternDeltagelseId" : "f02e50df-d2ee-47f6-9afa-db66bd842bfd",
                  "gjennomføringId" : "68f04dee-11a9-4d69-84fd-1096a4264492",
                  "typeNavn" : "Arbeidsmarkedsoppfølging gruppe",
                  "typeKode" : "GRUPPE_AMO",
                  "deltagelseFraOgMed" : "2025-01-01",
                  "deltagelseTilOgMed" : "2025-03-31",
                  "deltakelseStatus" : "Deltar",
                  "deltakelseProsent" : 100.0,
                  "antallDagerPerUke" : 5.0,
                  "kilde" : "Komet",
                  "deltakelseProsentFraGjennomforing" : false
                } ],
                "periode" : {
                  "fraOgMed" : "2025-01-01",
                  "tilOgMed" : "2025-03-31"
                },
                "ytelser" : [ ],
                "tiltakspengevedtakFraArena" : [ ]
              },
              "attesteringer" : [ {
                "endretAv" : "B12345",
                "status" : "GODKJENT",
                "begrunnelse" : null,
                "endretTidspunkt" : "2025-01-01T01:02:07.456789"
              } ],
              "virkningsperiode" : {
                "fraOgMed" : "2025-01-01",
                "tilOgMed" : "2025-03-31"
              },
              "fritekstTilVedtaksbrev" : "nyBehandlingUnderBeslutning()",
              "begrunnelseVilkårsvurdering" : "nyBehandlingUnderBeslutning()",
              "avbrutt" : null,
              "sistEndret" : "2025-01-01T01:02:05.456789",
              "iverksattTidspunkt" : "2025-01-01T01:02:08.456789",
              "ventestatus" : null,
              "utbetaling" : null,
              "innvilgelsesperiode" : {
                "fraOgMed" : "2025-01-01",
                "tilOgMed" : "2025-03-31"
              },
              "valgteTiltaksdeltakelser" : [ {
                "eksternDeltagelseId" : "f02e50df-d2ee-47f6-9afa-db66bd842bfd",
                "periode" : {
                  "fraOgMed" : "2025-01-01",
                  "tilOgMed" : "2025-03-31"
                }
              } ],
              "barnetillegg" : {
                "perioder" : [ {
                  "antallBarn" : 0,
                  "periode" : {
                    "fraOgMed" : "2025-01-01",
                    "tilOgMed" : "2025-03-31"
                  }
                } ],
                "begrunnelse" : null
              },
              "antallDagerPerMeldeperiode" : [ {
                "periode" : {
                  "fraOgMed" : "2025-01-01",
                  "tilOgMed" : "2025-03-31"
                },
                "antallDagerPerMeldeperiode" : 10
              } ],
              "resultat" : "INNVILGELSE",
              "søknad" : {
                "id" : "soknad_01K8QWMR32C2X5Y5T4N945BF9V",
                "journalpostId" : "journalpostId",
                "tiltak" : {
                  "id" : "06872f2f-5ca4-453a-8d41-8e91e1f777a3",
                  "fraOgMed" : "2025-01-01",
                  "tilOgMed" : "2025-03-31",
                  "typeKode" : "GRUPPEAMO",
                  "typeNavn" : "Gruppe AMO"
                },
                "barnetillegg" : [ ],
                "opprettet" : "2022-01-01T12:00:00",
                "tidsstempelHosOss" : "2022-01-01T12:00:00",
                "kvp" : null,
                "intro" : null,
                "institusjon" : null,
                "etterlønn" : false,
                "gjenlevendepensjon" : null,
                "alderspensjon" : null,
                "sykepenger" : null,
                "supplerendeStønadAlder" : null,
                "supplerendeStønadFlyktning" : null,
                "jobbsjansen" : null,
                "trygdOgPensjon" : null,
                "antallVedlegg" : 0,
                "avbrutt" : null,
                "kanInnvilges" : true
              },
              "automatiskSaksbehandlet" : false,
              "manueltBehandlesGrunner" : [ ],
              "type" : "SØKNADSBEHANDLING"
            }
        """.trimIndent()

        behandlingJson.shouldEqualJson(expectedJson)
    }

    @Test
    fun `Rammebehandling DTO fra søknadsbehandling med avslag`() {
        val behandling = nyVedtattSøknadsbehandling(
            clock = clock.copy(),
            id = behandlingId,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            resultat = SøknadsbehandlingType.AVSLAG,
            avslagsgrunner = nonEmptySetOf(Avslagsgrunnlag.DeltarIkkePåArbeidsmarkedstiltak),
            virkningsperiode = virkningsperiode,
            søknad = nyInnvilgbarSøknad(
                id = søknadId,
                søknadstiltak = søknadstiltak(
                    deltakelseFom = virkningsperiode.fraOgMed,
                    deltakelseTom = virkningsperiode.tilOgMed,
                    id = søknadTiltakId,
                ),
            ),
            saksopplysninger = saksopplysninger(
                fom = virkningsperiode.fraOgMed,
                tom = virkningsperiode.tilOgMed,
                tiltaksdeltagelse = tiltaksdeltagelse(
                    fom = virkningsperiode.fraOgMed,
                    tom = virkningsperiode.tilOgMed,
                    eksternTiltaksdeltagelseId = eksternTiltaksdeltagelseId,
                    eksternTiltaksgjennomføringsId = eksternTiltaksgjennomføringsId,
                ),
            ),
        )

        val behandlingJson = serialize(
            behandling.tilSøknadsbehandlingDTO(
                utbetalingsstatus = null,
                beregninger = beregninger,
                rammevedtakId = vedtakId,
            ),
        )

        @Language("JSON")
        val expectedJson = """
            {
              "id" : "beh_01K8R3V8S9X8KGR8HDXXDXN9P3",
              "status" : "VEDTATT",
              "sakId" : "sak_01K8QWMR1KZZB728K0F4RQG184",
              "saksnummer" : "202510291001",
              "rammevedtakId" : "vedtak_01J94XH6CKY0SZ5FBEE6YZG8S6",
              "saksbehandler" : "Z12345",
              "beslutter" : "B12345",
              "saksopplysninger" : {
                "fødselsdato" : "2001-01-01",
                "tiltaksdeltagelse" : [ {
                  "eksternDeltagelseId" : "f02e50df-d2ee-47f6-9afa-db66bd842bfd",
                  "gjennomføringId" : "68f04dee-11a9-4d69-84fd-1096a4264492",
                  "typeNavn" : "Arbeidsmarkedsoppfølging gruppe",
                  "typeKode" : "GRUPPE_AMO",
                  "deltagelseFraOgMed" : "2025-01-01",
                  "deltagelseTilOgMed" : "2025-03-31",
                  "deltakelseStatus" : "Deltar",
                  "deltakelseProsent" : 100.0,
                  "antallDagerPerUke" : 5.0,
                  "kilde" : "Komet",
                  "deltakelseProsentFraGjennomforing" : false
                } ],
                "periode" : {
                  "fraOgMed" : "2025-01-01",
                  "tilOgMed" : "2025-03-31"
                },
                "ytelser" : [ ],
                "tiltakspengevedtakFraArena" : [ ]
              },
              "attesteringer" : [ {
                "endretAv" : "B12345",
                "status" : "GODKJENT",
                "begrunnelse" : null,
                "endretTidspunkt" : "2025-01-01T01:02:07.456789"
              } ],
              "virkningsperiode" : {
                "fraOgMed" : "2025-01-01",
                "tilOgMed" : "2025-03-31"
              },
              "fritekstTilVedtaksbrev" : "nyBehandlingUnderBeslutning()",
              "begrunnelseVilkårsvurdering" : "nyBehandlingUnderBeslutning()",
              "avbrutt" : null,
              "sistEndret" : "2025-01-01T01:02:05.456789",
              "iverksattTidspunkt" : "2025-01-01T01:02:08.456789",
              "ventestatus" : null,
              "utbetaling" : null,
              "avslagsgrunner" : [ "DeltarIkkePåArbeidsmarkedstiltak" ],
              "resultat" : "AVSLAG",
              "søknad" : {
                "id" : "soknad_01K8QWMR32C2X5Y5T4N945BF9V",
                "journalpostId" : "journalpostId",
                "tiltak" : {
                  "id" : "06872f2f-5ca4-453a-8d41-8e91e1f777a3",
                  "fraOgMed" : "2025-01-01",
                  "tilOgMed" : "2025-03-31",
                  "typeKode" : "GRUPPEAMO",
                  "typeNavn" : "Gruppe AMO"
                },
                "barnetillegg" : [ ],
                "opprettet" : "2022-01-01T12:00:00",
                "tidsstempelHosOss" : "2022-01-01T12:00:00",
                "kvp" : null,
                "intro" : null,
                "institusjon" : null,
                "etterlønn" : false,
                "gjenlevendepensjon" : null,
                "alderspensjon" : null,
                "sykepenger" : null,
                "supplerendeStønadAlder" : null,
                "supplerendeStønadFlyktning" : null,
                "jobbsjansen" : null,
                "trygdOgPensjon" : null,
                "antallVedlegg" : 0,
                "avbrutt" : null,
                "kanInnvilges" : true
              },
              "automatiskSaksbehandlet" : false,
              "manueltBehandlesGrunner" : [ ],
              "type" : "SØKNADSBEHANDLING"
            }
        """.trimIndent()

        behandlingJson.shouldEqualJson(expectedJson)
    }

    @Test
    fun `Rammebehandling DTO fra søknadsbehandling uten valgt resultat`() {
        val behandling = nyOpprettetSøknadsbehandling(
            clock = clock.copy(),
            id = behandlingId,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            søknad = nyInnvilgbarSøknad(
                id = søknadId,
                søknadstiltak = søknadstiltak(
                    deltakelseFom = virkningsperiode.fraOgMed,
                    deltakelseTom = virkningsperiode.tilOgMed,
                    id = søknadTiltakId,
                ),
            ),
            hentSaksopplysninger = { _, _, _, _, _ ->
                saksopplysninger(
                    fom = virkningsperiode.fraOgMed,
                    tom = virkningsperiode.tilOgMed,
                    tiltaksdeltagelse = tiltaksdeltagelse(
                        fom = virkningsperiode.fraOgMed,
                        tom = virkningsperiode.tilOgMed,
                        eksternTiltaksdeltagelseId = eksternTiltaksdeltagelseId,
                        eksternTiltaksgjennomføringsId = eksternTiltaksgjennomføringsId,
                    ),
                )
            },
        )

        val behandlingJson = serialize(
            behandling.tilSøknadsbehandlingDTO(
                utbetalingsstatus = null,
                beregninger = beregninger,
                rammevedtakId = vedtakId,
            ),
        )

        @Language("JSON")
        val expectedJson = """
            {
              "id" : "beh_01K8R3V8S9X8KGR8HDXXDXN9P3",
              "status" : "UNDER_BEHANDLING",
              "sakId" : "sak_01K8QWMR1KZZB728K0F4RQG184",
              "saksnummer" : "202510291001",
              "rammevedtakId" : "vedtak_01J94XH6CKY0SZ5FBEE6YZG8S6",
              "saksbehandler" : "Z12345",
              "beslutter" : null,
              "saksopplysninger" : {
                "fødselsdato" : "2001-01-01",
                "tiltaksdeltagelse" : [ {
                  "eksternDeltagelseId" : "f02e50df-d2ee-47f6-9afa-db66bd842bfd",
                  "gjennomføringId" : "68f04dee-11a9-4d69-84fd-1096a4264492",
                  "typeNavn" : "Arbeidsmarkedsoppfølging gruppe",
                  "typeKode" : "GRUPPE_AMO",
                  "deltagelseFraOgMed" : "2025-01-01",
                  "deltagelseTilOgMed" : "2025-03-31",
                  "deltakelseStatus" : "Deltar",
                  "deltakelseProsent" : 100.0,
                  "antallDagerPerUke" : 5.0,
                  "kilde" : "Komet",
                  "deltakelseProsentFraGjennomforing" : false
                } ],
                "periode" : {
                  "fraOgMed" : "2025-01-01",
                  "tilOgMed" : "2025-03-31"
                },
                "ytelser" : [ ],
                "tiltakspengevedtakFraArena" : [ ]
              },
              "attesteringer" : [ ],
              "virkningsperiode" : null,
              "fritekstTilVedtaksbrev" : null,
              "begrunnelseVilkårsvurdering" : null,
              "avbrutt" : null,
              "sistEndret" : "2025-01-01T01:02:04.456789",
              "iverksattTidspunkt" : null,
              "ventestatus" : null,
              "utbetaling" : null,
              "resultat" : "IKKE_VALGT",
              "søknad" : {
                "id" : "soknad_01K8QWMR32C2X5Y5T4N945BF9V",
                "journalpostId" : "journalpostId",
                "tiltak" : {
                  "id" : "06872f2f-5ca4-453a-8d41-8e91e1f777a3",
                  "fraOgMed" : "2025-01-01",
                  "tilOgMed" : "2025-03-31",
                  "typeKode" : "GRUPPEAMO",
                  "typeNavn" : "Gruppe AMO"
                },
                "barnetillegg" : [ ],
                "opprettet" : "2022-01-01T12:00:00",
                "tidsstempelHosOss" : "2022-01-01T12:00:00",
                "kvp" : null,
                "intro" : null,
                "institusjon" : null,
                "etterlønn" : false,
                "gjenlevendepensjon" : null,
                "alderspensjon" : null,
                "sykepenger" : null,
                "supplerendeStønadAlder" : null,
                "supplerendeStønadFlyktning" : null,
                "jobbsjansen" : null,
                "trygdOgPensjon" : null,
                "antallVedlegg" : 0,
                "avbrutt" : null,
                "kanInnvilges" : true
              },
              "automatiskSaksbehandlet" : false,
              "manueltBehandlesGrunner" : [ ],
              "type" : "SØKNADSBEHANDLING"
            }
        """.trimIndent()

        behandlingJson.shouldEqualJson(expectedJson)
    }

    @Test
    fun `Rammebehandling DTO fra revurdering med innvilgelse`() {
        val behandling = nyVedtattRevurderingInnvilgelse(
            clock = clock.copy(),
            id = behandlingId,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            virkningsperiode = virkningsperiode,
            saksopplysninger = saksopplysninger(
                fom = virkningsperiode.fraOgMed,
                tom = virkningsperiode.tilOgMed,
                tiltaksdeltagelse = tiltaksdeltagelse(
                    fom = virkningsperiode.fraOgMed,
                    tom = virkningsperiode.tilOgMed,
                    eksternTiltaksdeltagelseId = eksternTiltaksdeltagelseId,
                    eksternTiltaksgjennomføringsId = eksternTiltaksgjennomføringsId,
                ),
            ),
        )

        val behandlingJson = serialize(
            behandling.tilRevurderingDTO(
                utbetalingsstatus = null,
                beregninger = beregninger,
                rammevedtakId = vedtakId,
            ),
        )

        @Language("JSON")
        val expectedJson = """
            {
              "id": "beh_01K8R3V8S9X8KGR8HDXXDXN9P3",
              "status": "VEDTATT",
              "sakId": "sak_01K8QWMR1KZZB728K0F4RQG184",
              "saksnummer": "202510291001",
              "rammevedtakId": "vedtak_01J94XH6CKY0SZ5FBEE6YZG8S6",
              "saksbehandler": "Z12345",
              "beslutter": "B12345",
              "saksopplysninger": {
                "fødselsdato": "2001-01-01",
                "tiltaksdeltagelse": [
                  {
                    "eksternDeltagelseId": "f02e50df-d2ee-47f6-9afa-db66bd842bfd",
                    "gjennomføringId": "68f04dee-11a9-4d69-84fd-1096a4264492",
                    "typeNavn": "Arbeidsmarkedsoppfølging gruppe",
                    "typeKode": "GRUPPE_AMO",
                    "deltagelseFraOgMed": "2025-01-01",
                    "deltagelseTilOgMed": "2025-03-31",
                    "deltakelseStatus": "Deltar",
                    "deltakelseProsent": 100.0,
                    "antallDagerPerUke": 5.0,
                    "kilde": "Komet",
                    "deltakelseProsentFraGjennomforing": false
                  }
                ],
                "periode": {
                  "fraOgMed": "2025-01-01",
                  "tilOgMed": "2025-03-31"
                },
                "ytelser": [],
                "tiltakspengevedtakFraArena": []
              },
              "attesteringer": [
                {
                  "endretAv": "B12345",
                  "status": "GODKJENT",
                  "begrunnelse": null,
                  "endretTidspunkt": "2025-01-01T01:02:04.456789"
                }
              ],
              "virkningsperiode": {
                "fraOgMed": "2025-01-01",
                "tilOgMed": "2025-03-31"
              },
              "fritekstTilVedtaksbrev": "nyRevurderingKlarTilBeslutning()",
              "begrunnelseVilkårsvurdering": "nyRevurderingKlarTilBeslutning()",
              "avbrutt": null,
              "sistEndret": "2025-01-01T01:02:06.456789",
              "iverksattTidspunkt": "2025-01-01T01:02:08.456789",
              "ventestatus": null,
              "utbetaling": null,
              "innvilgelsesperiode": {
                "fraOgMed": "2025-01-01",
                "tilOgMed": "2025-03-31"
              },
              "valgteTiltaksdeltakelser": [
                {
                  "eksternDeltagelseId": "f02e50df-d2ee-47f6-9afa-db66bd842bfd",
                  "periode": {
                    "fraOgMed": "2025-01-01",
                    "tilOgMed": "2025-03-31"
                  }
                }
              ],
              "barnetillegg": {
                "perioder": [
                  {
                    "antallBarn": 0,
                    "periode": {
                      "fraOgMed": "2025-01-01",
                      "tilOgMed": "2025-03-31"
                    }
                  }
                ],
                "begrunnelse": null
              },
              "antallDagerPerMeldeperiode": [
                {
                  "periode": {
                    "fraOgMed": "2025-01-01",
                    "tilOgMed": "2025-03-31"
                  },
                  "antallDagerPerMeldeperiode": 10
                }
              ],
              "resultat": "REVURDERING_INNVILGELSE",
              "type": "REVURDERING"
            }
        """.trimIndent()

        behandlingJson.shouldEqualJson(expectedJson)
    }

    @Test
    fun `Rammebehandling DTO fra revurdering med stans`() {
        val behandling = nyVedtattRevurderingStans(
            clock = clock.copy(),
            id = behandlingId,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            virkningsperiode = virkningsperiode,
            stansFraOgMed = virkningsperiode.fraOgMed.plusDays(7),
            stansTilOgMed = virkningsperiode.tilOgMed,
            førsteDagSomGirRett = virkningsperiode.fraOgMed,
            sisteDagSomGirRett = virkningsperiode.tilOgMed,
            saksopplysninger = saksopplysninger(
                fom = virkningsperiode.fraOgMed,
                tom = virkningsperiode.tilOgMed,
                tiltaksdeltagelse = tiltaksdeltagelse(
                    fom = virkningsperiode.fraOgMed,
                    tom = virkningsperiode.tilOgMed,
                    eksternTiltaksdeltagelseId = eksternTiltaksdeltagelseId,
                    eksternTiltaksgjennomføringsId = eksternTiltaksgjennomføringsId,
                ),
            ),
        )

        val behandlingJson = serialize(
            behandling.tilRevurderingDTO(
                utbetalingsstatus = null,
                beregninger = beregninger,
                rammevedtakId = vedtakId,
            ),
        )

        @Language("JSON")
        val expectedJson = """
            {
              "id": "beh_01K8R3V8S9X8KGR8HDXXDXN9P3",
              "status": "VEDTATT",
              "sakId": "sak_01K8QWMR1KZZB728K0F4RQG184",
              "saksnummer": "202510291001",
              "rammevedtakId": "vedtak_01J94XH6CKY0SZ5FBEE6YZG8S6",
              "saksbehandler": "Z12345",
              "beslutter": "B12345",
              "saksopplysninger": {
                "fødselsdato": "2001-01-01",
                "tiltaksdeltagelse": [
                  {
                    "eksternDeltagelseId": "f02e50df-d2ee-47f6-9afa-db66bd842bfd",
                    "gjennomføringId": "68f04dee-11a9-4d69-84fd-1096a4264492",
                    "typeNavn": "Arbeidsmarkedsoppfølging gruppe",
                    "typeKode": "GRUPPE_AMO",
                    "deltagelseFraOgMed": "2025-01-01",
                    "deltagelseTilOgMed": "2025-03-31",
                    "deltakelseStatus": "Deltar",
                    "deltakelseProsent": 100.0,
                    "antallDagerPerUke": 5.0,
                    "kilde": "Komet",
                    "deltakelseProsentFraGjennomforing": false
                  }
                ],
                "periode": {
                  "fraOgMed": "2025-01-01",
                  "tilOgMed": "2025-03-31"
                },
                "ytelser": [],
                "tiltakspengevedtakFraArena": []
              },
              "attesteringer": [
                {
                  "endretAv": "B12345",
                  "status": "GODKJENT",
                  "begrunnelse": null,
                  "endretTidspunkt": "2025-01-01T01:02:04.456789"
                }
              ],
              "virkningsperiode": {
                "fraOgMed": "2025-01-08",
                "tilOgMed": "2025-03-31"
              },
              "fritekstTilVedtaksbrev": "nyRevurderingKlarTilBeslutning()",
              "begrunnelseVilkårsvurdering": "nyRevurderingKlarTilBeslutning()",
              "avbrutt": null,
              "sistEndret": "2025-01-01T01:02:06.456789",
              "iverksattTidspunkt": "2025-01-01T01:02:07.456789",
              "ventestatus": null,
              "utbetaling": null,
              "valgtHjemmelHarIkkeRettighet": [
                "DeltarIkkePåArbeidsmarkedstiltak"
              ],
              "harValgtStansFraFørsteDagSomGirRett": false,
              "harValgtStansTilSisteDagSomGirRett": false,
              "resultat": "STANS",
              "type": "REVURDERING"
            }
        """.trimIndent()

        behandlingJson.shouldEqualJson(expectedJson)
    }

    @Test
    fun `Rammebehandling DTO fra revurdering med omgjøring`() {
        val nyClock = clock.copy()

        val behandling = nyVedtattSøknadsbehandling(
            clock = nyClock,
            id = behandlingId,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            resultat = SøknadsbehandlingType.INNVILGELSE,
            virkningsperiode = virkningsperiode,
            søknad = nyInnvilgbarSøknad(
                id = søknadId,
                søknadstiltak = søknadstiltak(
                    deltakelseFom = virkningsperiode.fraOgMed,
                    deltakelseTom = virkningsperiode.tilOgMed,
                    id = søknadTiltakId,
                ),
            ),
            saksopplysninger = saksopplysninger(
                fom = virkningsperiode.fraOgMed,
                tom = virkningsperiode.tilOgMed,
                tiltaksdeltagelse = tiltaksdeltagelse(
                    fom = virkningsperiode.fraOgMed,
                    tom = virkningsperiode.tilOgMed,
                    eksternTiltaksdeltagelseId = eksternTiltaksdeltagelseId,
                    eksternTiltaksgjennomføringsId = eksternTiltaksgjennomføringsId,
                ),
            ),
        )

        val omgjøringId = BehandlingId.fromString("beh_01K8R3V8S9X8KGR8HDXXDXN9P4")
        val omgjøringVedtakId = VedtakId.fromString("vedtak_01J94XH6CKY0SZ5FBEE6YZG8S7")

        val omgjøring = nyOpprettetRevurderingOmgjøring(
            clock = nyClock,
            id = omgjøringId,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            virkningsperiode = virkningsperiode.plusFraOgMed(1),
            omgjørRammevedtak = nyRammevedtakInnvilgelse(
                id = vedtakId,
                sakId = sakId,
                periode = behandling.virkningsperiode!!,
                fnr = fnr,
                behandling = behandling,
            ),
            hentSaksopplysninger = {
                saksopplysninger(
                    fom = virkningsperiode.fraOgMed,
                    tom = virkningsperiode.tilOgMed,
                    tiltaksdeltagelse = tiltaksdeltagelse(
                        fom = virkningsperiode.fraOgMed,
                        tom = virkningsperiode.tilOgMed,
                        eksternTiltaksdeltagelseId = eksternTiltaksdeltagelseId,
                        eksternTiltaksgjennomføringsId = eksternTiltaksgjennomføringsId,
                    ),
                )
            },
        )

        val behandlingJson = serialize(
            omgjøring.tilRevurderingDTO(
                utbetalingsstatus = null,
                beregninger = beregninger,
                rammevedtakId = omgjøringVedtakId,
            ),
        )

        @Language("JSON")
        val expectedJson = """
            {
              "id": "beh_01K8R3V8S9X8KGR8HDXXDXN9P4",
              "status": "UNDER_BEHANDLING",
              "sakId": "sak_01K8QWMR1KZZB728K0F4RQG184",
              "saksnummer": "202510291001",
              "rammevedtakId": "vedtak_01J94XH6CKY0SZ5FBEE6YZG8S7",
              "saksbehandler": "Z12345",
              "beslutter": null,
              "saksopplysninger": {
                "fødselsdato": "2001-01-01",
                "tiltaksdeltagelse": [
                  {
                    "eksternDeltagelseId": "f02e50df-d2ee-47f6-9afa-db66bd842bfd",
                    "gjennomføringId": "68f04dee-11a9-4d69-84fd-1096a4264492",
                    "typeNavn": "Arbeidsmarkedsoppfølging gruppe",
                    "typeKode": "GRUPPE_AMO",
                    "deltagelseFraOgMed": "2025-01-01",
                    "deltagelseTilOgMed": "2025-03-31",
                    "deltakelseStatus": "Deltar",
                    "deltakelseProsent": 100.0,
                    "antallDagerPerUke": 5.0,
                    "kilde": "Komet",
                    "deltakelseProsentFraGjennomforing": false
                  }
                ],
                "periode": {
                  "fraOgMed": "2025-01-01",
                  "tilOgMed": "2025-03-31"
                },
                "ytelser": [],
                "tiltakspengevedtakFraArena": []
              },
              "attesteringer": [],
              "virkningsperiode": {
                "fraOgMed": "2025-01-01",
                "tilOgMed": "2025-03-31"
              },
              "fritekstTilVedtaksbrev": null,
              "begrunnelseVilkårsvurdering": null,
              "avbrutt": null,
              "sistEndret": "2025-01-01T01:02:09.456789",
              "iverksattTidspunkt": null,
              "ventestatus": null,
              "utbetaling": null,
              "innvilgelsesperiode": {
                "fraOgMed": "2025-01-01",
                "tilOgMed": "2025-03-31"
              },
              "valgteTiltaksdeltakelser": [
                {
                  "eksternDeltagelseId": "f02e50df-d2ee-47f6-9afa-db66bd842bfd",
                  "periode": {
                    "fraOgMed": "2025-01-01",
                    "tilOgMed": "2025-03-31"
                  }
                }
              ],
              "barnetillegg": {
                "perioder": [
                  {
                    "antallBarn": 0,
                    "periode": {
                      "fraOgMed": "2025-01-01",
                      "tilOgMed": "2025-03-31"
                    }
                  }
                ],
                "begrunnelse": null
              },
              "antallDagerPerMeldeperiode": [
                {
                  "periode": {
                    "fraOgMed": "2025-01-01",
                    "tilOgMed": "2025-03-31"
                  },
                  "antallDagerPerMeldeperiode": 10
                }
              ],
              "omgjørVedtak": "vedtak_01J94XH6CKY0SZ5FBEE6YZG8S6",
              "resultat": "OMGJØRING",
              "type": "REVURDERING"
            }
        """.trimIndent()

        behandlingJson.shouldEqualJson(expectedJson)
    }
}
