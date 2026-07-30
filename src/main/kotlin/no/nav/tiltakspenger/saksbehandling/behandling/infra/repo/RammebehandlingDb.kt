package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import arrow.core.toNonEmptySetOrNull
import kotliquery.Row
import kotliquery.Session
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingUtbetaling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingstype
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Omgjøringsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Rammebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.RevurderingsresultatType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Søknadsbehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.SøknadsbehandlingsresultatType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltakelser
import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.attesteringer.toAttesteringer
import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.attesteringer.toDbJson
import no.nav.tiltakspenger.saksbehandling.beregning.infra.repo.tilBeregningFraRammebehandling
import no.nav.tiltakspenger.saksbehandling.beregning.infra.repo.tilBeregningerDbJsonString
import no.nav.tiltakspenger.saksbehandling.beregning.infra.repo.tilDbJson
import no.nav.tiltakspenger.saksbehandling.beregning.infra.repo.tilRammebehandlingUtbetalingskontroll
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringer
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.felles.Ventestatus
import no.nav.tiltakspenger.saksbehandling.infra.repo.booleanOrNull
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.toAvbrutt
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.toDbJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.toVentestatus
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.infra.repo.KlagebehandlingPostgresRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.MeldeperiodePostgresRepo
import no.nav.tiltakspenger.saksbehandling.omgjøring.infra.repo.toDbJson
import no.nav.tiltakspenger.saksbehandling.omgjøring.infra.repo.toOmgjørRammevedtak
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.søknad.infra.repo.SøknadDAO
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.toAutomatiskOpprettetRevurderingGrunn
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.toDbJson
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.TiltaksdeltakerPostgresRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo.toDbJson
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo.toSimuleringFraDbJson

/**
 * Oversettelse mellom databaseraden i `behandling` og domenemodellen [no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling].
 *
 * Ligger for seg selv fordi mappingen er omfangsrik og har et annet ansvar enn spørringene i [RammebehandlingPostgresRepo].
 */
internal fun Row.toBehandling(session: Session): Rammebehandling {
    val behandlingstype = string("behandlingstype").toBehandlingstype()
    val id = RammebehandlingId.fromString(string("id"))
    val sakId = SakId.fromString(string("sak_id"))
    val status = string("status").toBehandlingsstatus()
    val saksbehandler = stringOrNull("saksbehandler")
    val beslutter = stringOrNull("beslutter")
    val attesteringer = string("attesteringer").toAttesteringer()
    val fnr = Fnr.fromString(string("fnr"))
    val saksnummer = Saksnummer(string("saksnummer"))
    val sendtTilBeslutning = localDateTimeOrNull("sendt_til_beslutning")
    val opprettet = localDateTime("opprettet")
    val iverksattTidspunkt = localDateTimeOrNull("iverksatt_tidspunkt")
    val sistEndret = localDateTime("sist_endret")
    val avbrutt = stringOrNull("avbrutt")?.toAvbrutt()
    val ventestatus = stringOrNull("ventestatus")?.toVentestatus() ?: Ventestatus()
    val venterTil = localDateTimeOrNull("venter_til")
    val sendtTilDatadeling = localDateTimeOrNull("sendt_til_datadeling")
    val fritekstTilVedtaksbrev = stringOrNull("fritekst_vedtaksbrev")?.let { FritekstTilVedtaksbrev.create(it) }
    val begrunnelseVilkårsvurdering = stringOrNull("begrunnelse_vilkårsvurdering")?.let {
        Begrunnelse.create(it)
    }

    val saksopplysninger = saksopplysningerMedOppdatertEksternDeltakselseId(
        saksopplysninger = string("saksopplysninger").toSaksopplysninger(),
        session = session,
    )

    // TODO: Rename virkningsperiode_fra_og_med -> vedtaksperiode_fra_og_med og virkningsperiode_til_og_med -> vedtaksperiode_til_og_med
    val vedtaksperiodeFraOgMed = localDateOrNull("virkningsperiode_fra_og_med")
    val vedtaksperiodeTilOgMed = localDateOrNull("virkningsperiode_til_og_med")

    if ((vedtaksperiodeFraOgMed == null).xor(vedtaksperiodeTilOgMed == null)) {
        throw IllegalStateException("Både fra og med og til og med for vedtaksperiode må være satt, eller ingen av dem")
    }
    val vedtaksperiode =
        vedtaksperiodeFraOgMed?.let { Periode(vedtaksperiodeFraOgMed, vedtaksperiodeTilOgMed!!) }
    val søknadId = stringOrNull("soknad_id")?.let { SøknadId.fromString(it) }
    val omgjørRammevedtak = stringOrNull("omgjør_rammevedtak").toOmgjørRammevedtak()

    val innvilgelsesperioder = stringOrNull("innvilgelsesperioder")?.tilInnvilgelsesperioder()

    val meldeperiodekjeder by lazy {
        MeldeperiodePostgresRepo.hentMeldeperiodekjederForSakId(
            sakId = sakId,
            session = session,
        )
    }

    val utbetaling = stringOrNull("beregning")?.let {
        BehandlingUtbetaling(
            beregning = it.tilBeregningFraRammebehandling(id),
            navkontor = Navkontor(
                kontornummer = string("navkontor"),
                kontornavn = stringOrNull("navkontor_navn"),
            ),
            simulering = stringOrNull("simulering")?.toSimuleringFraDbJson(meldeperiodekjeder),
        )
    }

    val utbetalingskontroll = stringOrNull("utbetalingskontroll")
        ?.tilRammebehandlingUtbetalingskontroll(id, meldeperiodekjeder)

    when (behandlingstype) {
        Behandlingstype.SØKNADSBEHANDLING -> {
            val automatiskSaksbehandlet = boolean("automatisk_saksbehandlet")
            val manueltBehandlesGrunner =
                stringOrNull("manuelt_behandles_grunner")?.toManueltBehandlesGrunner() ?: emptyList()
            val resultatType = stringOrNull("resultat")?.tilSøknadsbehandlingResultatType()

            val resultat = when (resultatType) {
                SøknadsbehandlingsresultatType.INNVILGELSE -> Søknadsbehandlingsresultat.Innvilgelse(
                    barnetillegg = string("barnetillegg").toBarnetillegg(),
                    innvilgelsesperioder = innvilgelsesperioder!!,
                    omgjørRammevedtak = omgjørRammevedtak,
                )

                SøknadsbehandlingsresultatType.AVSLAG -> Søknadsbehandlingsresultat.Avslag(
                    avslagsgrunner = string("avslagsgrunner").toAvslagsgrunnlag(),
                    avslagsperiode = vedtaksperiode,
                )

                null -> null
            }

            return Søknadsbehandling(
                id = id,
                status = status,
                opprettet = opprettet,
                sistEndret = sistEndret,
                iverksattTidspunkt = iverksattTidspunkt,
                sendtTilDatadeling = sendtTilDatadeling,
                sakId = sakId,
                saksnummer = saksnummer,
                fnr = fnr,
                saksopplysninger = saksopplysninger,
                søknad = søknadId?.let { SøknadDAO.hentForSøknadId(it, session) }
                    ?: throw IllegalStateException("Fant ikke søknad for søknadsbehandling, behandlingsid $id"),
                saksbehandler = saksbehandler,
                sendtTilBeslutning = sendtTilBeslutning,
                beslutter = beslutter,
                attesteringer = Attesteringer(attesteringer),
                fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
                begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering,
                avbrutt = avbrutt,
                ventestatus = ventestatus,
                venterTil = venterTil,
                resultat = resultat,
                automatiskSaksbehandlet = automatiskSaksbehandlet,
                manueltBehandlesGrunner = manueltBehandlesGrunner,
                klagebehandling = stringOrNull("klagebehandling_id")?.let {
                    KlagebehandlingPostgresRepo.hentOrNull(KlagebehandlingId.fromString(it), session)
                },
                utbetaling = utbetaling,
                utbetalingskontroll = utbetalingskontroll,
                skalSendeVedtaksbrev = boolean("skal_sende_vedtaksbrev"),
            )
        }

        Behandlingstype.REVURDERING -> {
            val resultatType = string("resultat").tilRevurderingResultatType()

            val resultat = when (resultatType) {
                RevurderingsresultatType.STANS -> Revurderingsresultat.Stans(
                    valgtHjemmel = stringOrNull("valgt_hjemmel_har_ikke_rettighet")
                        ?.tilHjemmelForStans()
                        ?.toNonEmptySetOrNull(),
                    harValgtStansFraFørsteDagSomGirRett = booleanOrNull("har_valgt_stans_fra_første_dag_som_gir_rett"),
                    stansperiode = vedtaksperiode,
                    omgjørRammevedtak = omgjørRammevedtak,
                )

                RevurderingsresultatType.INNVILGELSE -> Revurderingsresultat.Innvilgelse(
                    barnetillegg = stringOrNull("barnetillegg")?.toBarnetillegg(),
                    innvilgelsesperioder = innvilgelsesperioder,
                    omgjørRammevedtak = omgjørRammevedtak,
                )

                RevurderingsresultatType.OMGJØRING_INNVILGELSE -> {
                    Omgjøringsresultat.OmgjøringInnvilgelse(
                        vedtaksperiode = vedtaksperiode!!,
                        innvilgelsesperioder = innvilgelsesperioder,
                        barnetillegg = stringOrNull("barnetillegg")?.toBarnetillegg(),
                        omgjørRammevedtak = omgjørRammevedtak,
                    )
                }

                RevurderingsresultatType.OMGJØRING_OPPHØR -> Omgjøringsresultat.OmgjøringOpphør(
                    vedtaksperiode = vedtaksperiode!!,
                    omgjørRammevedtak = omgjørRammevedtak,
                    valgteHjemler = string("valgt_hjemmel_har_ikke_rettighet")
                        .tilHjemmelForOpphør(),
                )

                RevurderingsresultatType.OMGJØRING_IKKE_VALGT -> Omgjøringsresultat.OmgjøringIkkeValgt(
                    omgjørRammevedtak = omgjørRammevedtak,
                )
            }

            return Revurdering(
                id = id,
                status = status,
                opprettet = opprettet,
                sistEndret = sistEndret,
                iverksattTidspunkt = iverksattTidspunkt,
                sendtTilDatadeling = sendtTilDatadeling,
                sakId = sakId,
                saksnummer = saksnummer,
                fnr = fnr,
                saksopplysninger = saksopplysninger,
                saksbehandler = saksbehandler,
                sendtTilBeslutning = sendtTilBeslutning,
                beslutter = beslutter,
                attesteringer = Attesteringer(attesteringer),
                fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
                begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering,
                avbrutt = avbrutt,
                ventestatus = ventestatus,
                venterTil = venterTil,
                resultat = resultat,
                klagebehandling = stringOrNull("klagebehandling_id")?.let {
                    KlagebehandlingPostgresRepo.hentOrNull(KlagebehandlingId.fromString(it), session)
                },
                utbetaling = utbetaling,
                utbetalingskontroll = utbetalingskontroll,
                automatiskOpprettetGrunn = stringOrNull("automatisk_opprettet_grunn")?.toAutomatiskOpprettetRevurderingGrunn(),
                skalSendeVedtaksbrev = boolean("skal_sende_vedtaksbrev"),
            )
        }
    }
}

internal fun saksopplysningerMedOppdatertEksternDeltakselseId(
    saksopplysninger: Saksopplysninger,
    session: Session,
): Saksopplysninger {
    val oppdaterteTiltaksdeltakelser = saksopplysninger.tiltaksdeltakelser.value.map {
        val oppdatertEksternId =
            TiltaksdeltakerPostgresRepo.hentEksternId(internId = it.internDeltakelseId, session = session)
        it.copy(eksternDeltakelseId = oppdatertEksternId)
    }
    return saksopplysninger.copy(tiltaksdeltakelser = Tiltaksdeltakelser(oppdaterteTiltaksdeltakelser))
}

internal fun Rammebehandling.tilDbParams(): Map<String, Any?> {
    val søknadId = when (this) {
        is Søknadsbehandling -> this.søknad.id.toString()
        is Revurdering -> null
    }
    val automatiskSaksbehandlet = when (this) {
        is Søknadsbehandling -> this.automatiskSaksbehandlet
        is Revurdering -> false
    }

    val manueltBehandlesGrunner = when (this) {
        is Søknadsbehandling -> this.manueltBehandlesGrunner
        is Revurdering -> null
    }
    return mapOf(
        "id" to this.id.toString(),
        "status" to this.status.toDb(),
        "sist_endret" to this.sistEndret,
        "iverksatt_tidspunkt" to this.iverksattTidspunkt,
        "sendt_til_datadeling" to this.sendtTilDatadeling,
        "oppgave_id" to null,
        "virkningsperiode_fra_og_med" to this.vedtaksperiode?.fraOgMed,
        "virkningsperiode_til_og_med" to this.vedtaksperiode?.tilOgMed,
        "saksbehandler" to this.saksbehandler,
        "beslutter" to this.beslutter,
        "attesteringer" to this.attesteringer.toDbJson(),
        "sendt_til_beslutning" to this.sendtTilBeslutning,
        "fritekst_vedtaksbrev" to this.fritekstTilVedtaksbrev?.verdi,
        "begrunnelse_vilkarsvurdering" to this.begrunnelseVilkårsvurdering?.verdi,
        "saksopplysninger" to this.saksopplysninger.toDbJson(),
        "avbrutt" to this.avbrutt?.toDbJson(),
        "ventestatus" to this.ventestatus.toDbJson(),
        "venter_til" to this.venterTil,
        "resultat" to this.resultat?.toDb(),
        "opprettet" to this.opprettet,
        "sak_id" to this.sakId.toString(),
        "behandlingstype" to this.behandlingstype.toDbValue(),
        "soknad_id" to søknadId,
        "automatisk_saksbehandlet" to automatiskSaksbehandlet,
        "manuelt_behandles_grunner" to manueltBehandlesGrunner?.toDbJson(),
        "beregning" to this.utbetaling?.beregning?.tilBeregningerDbJsonString(),
        "simulering" to this.utbetaling?.simulering?.toDbJson(),
        "utbetalingskontroll" to this.utbetalingskontroll?.tilDbJson(),
        "navkontor" to this.utbetaling?.navkontor?.kontornummer,
        "navkontor_navn" to this.utbetaling?.navkontor?.kontornavn,
        "klagebehandling_id" to this.klagebehandling?.id?.toString(),
        "automatisk_opprettet_grunn" to when (this) {
            is Revurdering -> this.automatiskOpprettetGrunn?.toDbJson()
            is Søknadsbehandling -> null
        },
        "skal_sende_vedtaksbrev" to this.skalSendeVedtaksbrev,

        *this.resultat.tilDbParams(),
    )
}

internal fun Rammebehandlingsresultat?.tilDbParams(): Array<Pair<String, Any?>> = when (this) {
    is Søknadsbehandlingsresultat.Avslag -> arrayOf(
        "avslagsgrunner" to this.avslagsgrunner.toDb(),
        "omgjoer_rammevedtak" to null,
    )

    is Søknadsbehandlingsresultat.Innvilgelse -> arrayOf(
        "innvilgelsesperioder" to this.innvilgelsesperioder.tilInnvilgelsesperioderDbJson(),
        "barnetillegg" to this.barnetillegg.toDbJson(),
        "omgjoer_rammevedtak" to this.omgjørRammevedtak.toDbJson(),
    )

    is Omgjøringsresultat.OmgjøringInnvilgelse -> arrayOf(
        "innvilgelsesperioder" to this.innvilgelsesperioder?.tilInnvilgelsesperioderDbJson(),
        "barnetillegg" to this.barnetillegg?.toDbJson(),
        "omgjoer_rammevedtak" to this.omgjørRammevedtak.toDbJson(),
    )

    is Revurderingsresultat.Innvilgelse -> arrayOf(
        "innvilgelsesperioder" to this.innvilgelsesperioder?.tilInnvilgelsesperioderDbJson(),
        "barnetillegg" to this.barnetillegg?.toDbJson(),
        "omgjoer_rammevedtak" to this.omgjørRammevedtak.toDbJson(),
    )

    is Revurderingsresultat.Stans -> arrayOf(
        "valgt_hjemmel_har_ikke_rettighet" to this.valgtHjemmel.toHjemmelForStansDbJson(),
        "har_valgt_stans_fra_forste_dag_som_gir_rett" to this.harValgtStansFraFørsteDagSomGirRett,
        "omgjoer_rammevedtak" to this.omgjørRammevedtak.toDbJson(),
    )

    is Omgjøringsresultat.OmgjøringIkkeValgt -> arrayOf(
        "omgjoer_rammevedtak" to this.omgjørRammevedtak.toDbJson(),
    )

    is Omgjøringsresultat.OmgjøringOpphør -> arrayOf(
        "omgjoer_rammevedtak" to this.omgjørRammevedtak.toDbJson(),
        "valgt_hjemmel_har_ikke_rettighet" to this.valgteHjemler.toHjemmelForOpphørDbJson(),
    )

    null -> emptyArray()
}
