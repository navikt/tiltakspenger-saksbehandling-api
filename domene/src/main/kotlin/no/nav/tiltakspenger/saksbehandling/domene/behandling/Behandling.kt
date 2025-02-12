package no.nav.tiltakspenger.saksbehandling.domene.behandling

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.felles.OppgaveId
import no.nav.tiltakspenger.felles.exceptions.StøtterIkkeUtfallException
import no.nav.tiltakspenger.felles.nå
import no.nav.tiltakspenger.felles.sikkerlogg
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandlingsstatus.KLAR_TIL_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandlingsstatus.KLAR_TIL_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandlingsstatus.UNDER_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandlingsstatus.UNDER_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandlingsstatus.VEDTATT
import no.nav.tiltakspenger.saksbehandling.domene.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.domene.stønadsdager.Stønadsdager
import no.nav.tiltakspenger.saksbehandling.domene.stønadsdager.tilStønadsdagerRegisterSaksopplysning
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.Tiltaksdeltagelse
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.AvklartUtfallForPeriode
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.SamletUtfall
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.UtfallForPeriode
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.Vilkårssett
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.toAvklartUtfallForPeriode
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Revurdering: https://jira.adeo.no/browse/BEGREP-1559
 *
 * Unikt for søknadssbehandling: Søknad? (I følge begrepskatalogen kan en endringssøknad føre til en revurdering (må den da være nullable?), men gitt at det er et helt nytt uavhengig tiltak, er det da en førstegangssøknad, søknad eller endringssøknad?)
 * Unikt for Revurdering:
 *
 * @param saksbehandler Vil bli satt på en behandling ved opprettelse, men i noen tilfeller kan saksbehandler ta seg selv av behandlingen igjen.
 * @param beslutter Vil bli satt når behandlingen avsluttes (iverksett eller avbrytes) eller underkjennes.
 * @param søknad Påkrevd for [Behandlingstype.FØRSTEGANGSBEHANDLING]. Kan være null for [Behandlingstype.REVURDERING]. Må vurdere på sikt om en endringssøknad (samme tiltak) er en ny førstegangssøknad eller en revurdering. Og om en ny søknad (nytt tiltak) er en førstegangssøknad, søknad eller en revurdering.
 */
data class Behandling(
    val id: BehandlingId,
    val sakId: SakId,
    val saksnummer: Saksnummer,
    val fnr: Fnr,
    val vurderingsperiode: Periode,
    val søknad: Søknad?,
    val saksbehandler: String?,
    val sendtTilBeslutning: LocalDateTime?,
    val beslutter: String?,
    // TODO John + Anders: Gjør denne nullable og sjekk i init at vi enten har enten Vilkårssett eller Saksopplysninger. Dette for å støtte gammel og ny behandling.
    val vilkårssett: Vilkårssett,
    val saksopplysninger: Saksopplysninger?,
    // TODO John og Anders: Slett denne når vi har fjernet den gamle vilkårsvurderingen.
    val stønadsdager: Stønadsdager,
    val status: Behandlingsstatus,
    val attesteringer: List<Attestering>,
    val opprettet: LocalDateTime,
    val iverksattTidspunkt: LocalDateTime?,
    val sendtTilDatadeling: LocalDateTime?,
    val sistEndret: LocalDateTime,
    val behandlingstype: Behandlingstype,
    val oppgaveId: OppgaveId?,
    val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev? = null,
    val begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering? = null,
) {
    val erVedtatt: Boolean = status == VEDTATT
    val maksDagerMedTiltakspengerForPeriode: Int = stønadsdager.registerSaksopplysning.antallDager

    val tiltaksnavn = vilkårssett.tiltakDeltagelseVilkår.registerSaksopplysning.tiltaksnavn
    val tiltakstype: TiltakstypeSomGirRett = vilkårssett.tiltakDeltagelseVilkår.registerSaksopplysning.tiltakstype
    val tiltaksid: String = vilkårssett.tiltakDeltagelseVilkår.registerSaksopplysning.eksternDeltagelseId
    val gjennomføringId: String? = vilkårssett.tiltakDeltagelseVilkår.registerSaksopplysning.gjennomføringId
    val samletUtfall = vilkårssett.samletUtfall

    val utfallsperioder: Periodisering<UtfallForPeriode> get() = vilkårssett.utfallsperioder()
    val avklarteUtfallsperioder: Periodisering<AvklartUtfallForPeriode> get() = utfallsperioder.toAvklartUtfallForPeriode()

    val erFørstegangsbehandling: Boolean = behandlingstype == Behandlingstype.FØRSTEGANGSBEHANDLING
    val erRevurdering: Boolean = behandlingstype == Behandlingstype.REVURDERING

    val erHelePeriodenIkkeOppfylt: Boolean = samletUtfall == SamletUtfall.IKKE_OPPFYLT

    companion object {
        private val logger = mu.KotlinLogging.logger { }

        /**
         * Gammel måte og opprette førstegangsbehandling. Denne skal slettes når vi har ny flyt.
         */
        fun opprettDeprecatedFørstegangsbehandling(
            sakId: SakId,
            saksnummer: Saksnummer,
            fnr: Fnr,
            søknad: Søknad,
            fødselsdato: LocalDate,
            saksbehandler: Saksbehandler,
            registrerteTiltak: List<Tiltaksdeltagelse>,
        ): Either<KanIkkeOppretteBehandling, Behandling> {
            val vurderingsperiode = søknad.vurderingsperiode()
            if (søknad.barnetillegg.isNotEmpty()) {
                return KanIkkeOppretteBehandling.StøtterIkkeBarnetillegg.left()
            }
            val tiltak =
                registrerteTiltak.find {
                    it.eksternDeltagelseId == søknad.tiltak.id &&
                        it.deltakelsesperiode.overlapperMed(vurderingsperiode)
                }

            if (tiltak == null) {
                return KanIkkeOppretteBehandling.FantIkkeTiltak.left()
            }

            // TODO post-mvp B og H: Fjern denne når vi begynner å implementere delvis innvilgelse og/eller avslag
            val vilkårssett = Either.catch {
                Vilkårssett.opprett(
                    søknad = søknad,
                    fødselsdato = fødselsdato,
                    tiltaksdeltagelse = tiltak,
                    vurderingsperiode = vurderingsperiode,
                )
            }.getOrElse {
                if (it is StøtterIkkeUtfallException) {
                    logger.error(RuntimeException("Trigger stacktrace for enklere debugging")) { "Støtter kun innvilgelse. Se sikkerlogg for mer info." }
                    sikkerlogg.error(it) { "Støtter kun innvilgelse." }
                    return KanIkkeOppretteBehandling.StøtterKunInnvilgelse.left()
                } else {
                    throw it
                }
            }

            val opprettet = nå()
            return Behandling(
                id = BehandlingId.random(),
                saksnummer = saksnummer,
                sakId = sakId,
                fnr = fnr,
                søknad = søknad,
                vurderingsperiode = vurderingsperiode,
                vilkårssett = vilkårssett,
                // Dette er gammel flyt for opprettelse av førstegangsbehandling og da vil saksopplysninger være null. Ved ny flyt vil ha saksopplysninger, mens vilkårssett vil være null. Hele denne funksjonen skal slettes når vi har ny flyt.
                saksopplysninger = null,
                stønadsdager = Stønadsdager(
                    vurderingsperiode = vurderingsperiode,
                    tiltak.tilStønadsdagerRegisterSaksopplysning(),
                ),
                saksbehandler = saksbehandler.navIdent,
                sendtTilBeslutning = null,
                beslutter = null,
                status = UNDER_BEHANDLING,
                attesteringer = emptyList(),
                opprettet = opprettet,
                iverksattTidspunkt = null,
                sendtTilDatadeling = null,
                sistEndret = opprettet,
                behandlingstype = Behandlingstype.FØRSTEGANGSBEHANDLING,
                oppgaveId = søknad.oppgaveId,
            ).right()
        }

        fun opprettRevurdering(
            sakId: SakId,
            saksnummer: Saksnummer,
            fnr: Fnr,
            saksbehandler: Saksbehandler,
            periode: Periode,
            vilkårssett: Vilkårssett,
            stønadsdager: Stønadsdager,
        ): Behandling {
            val opprettet = nå()
            return Behandling(
                id = BehandlingId.random(),
                sakId = sakId,
                saksnummer = saksnummer,
                fnr = fnr,
                vurderingsperiode = periode,
                søknad = null,
                saksbehandler = saksbehandler.navIdent,
                sendtTilBeslutning = null,
                beslutter = null,
                vilkårssett = vilkårssett,
                // TODO John + Anders: Vurder om vi skal ta med oss saksopplysninger fra førstegangsbehandling eller hente de på nytt. Kan gjøres i flere iterasjoner.
                saksopplysninger = null,
                stønadsdager = stønadsdager,
                status = UNDER_BEHANDLING,
                attesteringer = emptyList(),
                opprettet = opprettet,
                iverksattTidspunkt = null,
                sendtTilDatadeling = null,
                sistEndret = opprettet,
                behandlingstype = Behandlingstype.REVURDERING,
                oppgaveId = null, // her kan man på sikt lagre oppgaveId hvis man oppretter oppgave for revurdering
            )
        }
    }

    /**
     * Saksbehandler/beslutter tar eller overtar behandlingen.
     */
    fun taBehandling(saksbehandler: Saksbehandler): Behandling =
        when (this.status) {
            KLAR_TIL_BEHANDLING, UNDER_BEHANDLING -> {
                check(saksbehandler.erSaksbehandler()) {
                    "Saksbehandler må ha rolle saksbehandler. Utøvende saksbehandler: $saksbehandler"
                }
                this.copy(saksbehandler = saksbehandler.navIdent, status = UNDER_BEHANDLING).let {
                    // Dersom utøvende saksbehandler er beslutter, fjern beslutter fra behandlingen.
                    if (it.saksbehandler == it.beslutter) it.copy(beslutter = null) else it
                }
            }

            KLAR_TIL_BESLUTNING, UNDER_BESLUTNING -> {
                check(saksbehandler.navIdent != this.saksbehandler) {
                    "Beslutter ($saksbehandler) kan ikke være den samme som saksbehandleren (${this.saksbehandler}"
                }
                check(saksbehandler.erBeslutter()) {
                    "Saksbehandler må ha beslutterrolle. Utøvende saksbehandler: $saksbehandler"
                }
                this.copy(beslutter = saksbehandler.navIdent, status = UNDER_BESLUTNING)
            }

            VEDTATT -> {
                throw IllegalArgumentException(
                    "Kan ikke ta behandling når behandlingen er VEDTATT. Behandlingsstatus: ${this.status}. Utøvende saksbehandler: $saksbehandler. Saksbehandler på behandling: ${this.saksbehandler}",
                )
            }
        }

    fun tilBeslutning(saksbehandler: Saksbehandler): Behandling {
        if (behandlingstype == Behandlingstype.FØRSTEGANGSBEHANDLING && vilkårssett.samletUtfall != SamletUtfall.OPPFYLT) {
            throw IllegalStateException("Kan ikke sende en behandling til beslutning som ikke er innvilget i MVP")
        }
        if (behandlingstype == Behandlingstype.REVURDERING && vilkårssett.samletUtfall != SamletUtfall.IKKE_OPPFYLT) {
            throw IllegalStateException("Kan ikke sende en revurdering til beslutning som er (delvis)innvilget i MVP")
        }
        check(status == UNDER_BEHANDLING) {
            "Behandlingen må være under behandling, det innebærer også at en saksbehandler må ta saken før den kan sendes til beslutter. Behandlingsstatus: ${this.status}. Utøvende saksbehandler: $saksbehandler. Saksbehandler på behandling: ${this.saksbehandler}"
        }

        check(saksbehandler.navIdent == this.saksbehandler) { "Det er ikke lov å sende en annen sin behandling til beslutter" }
        check(samletUtfall != SamletUtfall.UAVKLART) { "Kan ikke sende en UAVKLART behandling til beslutter" }
        return this.copy(
            status = if (beslutter == null) KLAR_TIL_BESLUTNING else UNDER_BESLUTNING,
            sendtTilBeslutning = nå(),
        )
    }

    fun iverksett(
        utøvendeBeslutter: Saksbehandler,
        attestering: Attestering,
    ): Behandling {
        if (this.behandlingstype == Behandlingstype.FØRSTEGANGSBEHANDLING && vilkårssett.samletUtfall != SamletUtfall.OPPFYLT) {
            throw IllegalStateException("Kan ikke iverksette en førstegangsbehandling som ikke er innvilget i MVP")
        }
        if (this.behandlingstype == Behandlingstype.REVURDERING && vilkårssett.samletUtfall != SamletUtfall.IKKE_OPPFYLT) {
            throw IllegalStateException("Kan ikke iverksette en revurdering som ikke er 'IKKE_OPPFYLT' i MVP")
        }
        return when (status) {
            UNDER_BESLUTNING -> {
                check(utøvendeBeslutter.erBeslutter()) { "utøvende saksbehandler må være beslutter" }
                check(this.beslutter == utøvendeBeslutter.navIdent) { "Kan ikke iverksette en behandling man ikke er beslutter på" }
                check(!this.attesteringer.any { it.isGodkjent() }) {
                    "Behandlingen er allerede godkjent"
                }
                this.copy(
                    status = VEDTATT,
                    attesteringer = attesteringer + attestering,
                    iverksattTidspunkt = nå(),
                )
            }

            KLAR_TIL_BEHANDLING, UNDER_BEHANDLING, KLAR_TIL_BESLUTNING, VEDTATT -> throw IllegalStateException(
                "Må ha status UNDER_BESLUTNING for å iverksette. Behandlingsstatus: $status",
            )
        }
    }

    fun sendTilbake(
        utøvendeBeslutter: Saksbehandler,
        attestering: Attestering,
    ): Behandling {
        return when (status) {
            UNDER_BESLUTNING -> {
                check(
                    utøvendeBeslutter.erBeslutter(),
                ) { "utøvende saksbehandler må være beslutter" }
                check(this.beslutter == utøvendeBeslutter.navIdent) {
                    "Kun beslutter som har saken kan sende tilbake"
                }
                check(!this.attesteringer.any { it.isGodkjent() }) {
                    "Behandlingen er allerede godkjent"
                }
                this.copy(status = UNDER_BEHANDLING, attesteringer = attesteringer + attestering)
            }

            KLAR_TIL_BEHANDLING, UNDER_BEHANDLING, KLAR_TIL_BESLUTNING, VEDTATT -> throw IllegalStateException(
                "Må ha status UNDER_BESLUTNING for å sende tilbake. Behandlingsstatus: $status",
            )
        }
    }

    /**
     * Krymper [vurderingsperiode], [vilkårssett] og [stønadsdager] til [nyPeriode].
     * Endrer ikke [Søknad].
     */
    fun krymp(nyPeriode: Periode): Behandling {
        if (vurderingsperiode == nyPeriode) return this
        require(vurderingsperiode.inneholderHele(nyPeriode)) { "Ny periode ($nyPeriode) må være innenfor vedtakets periode ($vurderingsperiode)" }
        return this.copy(
            vurderingsperiode = nyPeriode,
            vilkårssett = vilkårssett.krymp(nyPeriode),
            stønadsdager = stønadsdager.krymp(nyPeriode),
        )
    }

    init {
        require(vilkårssett.vurderingsperiode == vurderingsperiode) {
            "Vilkårssettets periode (${vilkårssett.vurderingsperiode} må være lik vurderingsperioden $vurderingsperiode"
        }
        require(stønadsdager.vurderingsperiode == vurderingsperiode) {
            "Stønadsdagers periode (${stønadsdager.vurderingsperiode} må være lik vurderingsperioden $vurderingsperiode"
        }
        if (beslutter != null && saksbehandler != null) {
            require(beslutter != saksbehandler) { "Saksbehandler og beslutter kan ikke være samme person" }
        }
        when (behandlingstype) {
            Behandlingstype.FØRSTEGANGSBEHANDLING -> {
                requireNotNull(søknad) { "Søknad må være satt for førstegangsbehandling" }
                require(søknad.barnetillegg.isEmpty()) {
                    "Barnetillegg er ikke støttet i MVP 1"
                }
            }

            Behandlingstype.REVURDERING -> {
                require(søknad == null) { "Søknad kan ikke være satt for revurdering" }
            }
        }

        when (status) {
            KLAR_TIL_BEHANDLING -> {
                require(saksbehandler == null) {
                    "Behandlingen kan ikke være tilknyttet en saksbehandler når statusen er KLAR_TIL_BEHANDLING"
                }
                // Selvom beslutter har underkjent, må vi kunne ta hen av behandlingen.
                require(iverksattTidspunkt == null)
            }

            UNDER_BEHANDLING -> {
                requireNotNull(saksbehandler) {
                    "Behandlingen må være tilknyttet en saksbehandler når status er UNDER_BEHANDLING"
                }
                // Selvom beslutter har underkjent, må vi kunne ta hen av behandlingen.
                require(iverksattTidspunkt == null)
            }

            KLAR_TIL_BESLUTNING -> {
                // Vi kan ikke ta saksbehandler av behandlingen før den underkjennes.
                requireNotNull(saksbehandler) { "Behandlingen må ha saksbehandler når status er KLAR_TIL_BESLUTNING" }
                require(beslutter == null) {
                    "Behandlingen kan ikke være tilknyttet en beslutter når status er KLAR_TIL_BESLUTNING"
                }
                require(vilkårssett.samletUtfall != SamletUtfall.UAVKLART) {
                    "Behandlingen kan ikke være KLAR_TIL_BESLUTNING når samlet utfall er UAVKLART"
                }
                require(iverksattTidspunkt == null)
            }

            UNDER_BESLUTNING -> {
                // Vi kan ikke ta saksbehandler av behandlingen før den underkjennes.
                requireNotNull(saksbehandler) { "Behandlingen må ha saksbehandler når status er UNDER_BESLUTNING" }
                requireNotNull(beslutter) { "Behandlingen må tilknyttet en beslutter når status er UNDER_BESLUTNING" }
                require(
                    vilkårssett.samletUtfall != SamletUtfall.UAVKLART,
                ) { "Behandlingen kan ikke være UNDER_BESLUTNING når samlet utfall er UAVKLART" }
                require(iverksattTidspunkt == null)
            }

            VEDTATT -> {
                // Det er viktig at vi ikke tar saksbehandler og beslutter av behandlingen når status er VEDTATT.
                requireNotNull(beslutter) { "Behandlingen må ha beslutter når status er VEDTATT" }
                requireNotNull(saksbehandler) { "Behandlingen må ha saksbehandler når status er VEDTATT" }
                require(
                    vilkårssett.samletUtfall != SamletUtfall.UAVKLART,
                ) { "Vi støtter ikke samlet utfall: UAVKLART på dette tidspunktet." }
                requireNotNull(iverksattTidspunkt)
                requireNotNull(sendtTilBeslutning)
            }
        }
    }
}
