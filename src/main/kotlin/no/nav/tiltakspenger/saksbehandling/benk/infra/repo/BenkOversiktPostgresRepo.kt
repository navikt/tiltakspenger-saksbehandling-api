package no.nav.tiltakspenger.saksbehandling.benk.infra.repo

import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.attesteringer.toAttesteringer
import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.toBehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.toBehandlingstype
import no.nav.tiltakspenger.saksbehandling.behandling.ports.BenkOversiktRepo
import no.nav.tiltakspenger.saksbehandling.benk.BehandlingEllerSøknadForSaksoversikt
import no.nav.tiltakspenger.saksbehandling.benk.BenkBehandlingstype
import no.nav.tiltakspenger.saksbehandling.benk.toBenkBehandlingstype
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer

class BenkOversiktPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : BenkOversiktRepo {

    override fun hentÅpneBehandlinger(sessionContext: SessionContext?): List<BehandlingEllerSøknadForSaksoversikt> {
        return sessionFactory.withSession(sessionContext) { session ->
            session
                .run(
                    queryOf(
                        //language=SQL
                        """
                            select behandling.id as behandling_id,
                              sak.fnr,
                              behandling.opprettet,
                              behandling.virkningsperiode_fra_og_med,
                              behandling.virkningsperiode_til_og_med,
                              behandling.status,
                              sak.saksnummer,
                              behandling.saksbehandler,
                              behandling.beslutter,
                              behandling.attesteringer,
                              behandling.sak_id,
                              behandling.behandlingstype,
                              søknad.tidsstempel_hos_oss as kravtidspunkt
                            from behandling
                            left join sak on sak.id = behandling.sak_id
                            left join søknad on behandling.id = søknad.behandling_id
                            where behandling.status != 'VEDTATT' and behandling.status != 'AVBRUTT'
                            order by sak.saksnummer, behandling.id
                        """.trimIndent(),
                    ).map { row ->
                        val id = row.string("behandling_id").let { BehandlingId.fromString(it) }
                        val virkningsperiodeFraOgMed = row.localDateOrNull("virkningsperiode_fra_og_med")
                        val periode = virkningsperiodeFraOgMed?.let {
                            Periode(
                                fraOgMed = it,
                                tilOgMed = row.localDate("virkningsperiode_til_og_med"),
                            )
                        }

                        val kravtidspunkt = row.localDateTimeOrNull("kravtidspunkt")
                        val beslutter = row.stringOrNull("beslutter")
                        val saksbehandler = row.stringOrNull("saksbehandler")
                        val behandlingstype = row.string("behandlingstype").toBehandlingstype().toBenkBehandlingstype()
                        val status =
                            BehandlingEllerSøknadForSaksoversikt.Status.Behandling(
                                row.string("status").toBehandlingsstatus(),
                            )
                        val attesteringer = row.string("attesteringer").toAttesteringer()
                        BehandlingEllerSøknadForSaksoversikt(
                            periode = periode,
                            status = status,
                            underkjent = attesteringer.any { attestering -> attestering.isUnderkjent() },
                            kravtidspunkt = kravtidspunkt,
                            behandlingstype = behandlingstype,
                            fnr = Fnr.fromString(row.string("fnr")),
                            saksnummer = Saksnummer(
                                row.string(
                                    "saksnummer",
                                ),
                            ),
                            id = id,
                            saksbehandler = saksbehandler,
                            beslutter = beslutter,
                            sakId = SakId.fromString(row.string("sak_id")),
                            opprettet = row.localDateTime("opprettet"),
                        )
                    }.asList,
                )
        }
    }

    override fun hentÅpneSøknader(sessionContext: SessionContext?): List<BehandlingEllerSøknadForSaksoversikt> =
        sessionFactory.withSession(sessionContext) { session ->
            session
                .run(
                    queryOf(
                        //language=SQL
                        """
                        select søknad.id as søknad_id,
                          søknad.fnr,
                          søknad.opprettet,
                          søknad.behandling_id,
                          søknad.sak_id,
                          søknad.tidsstempel_hos_oss as kravtidspunkt,
                          sak.saksnummer,
                          søknad.avbrutt
                        from søknad join sak on søknad.sak_id = sak.id
                        where søknad.behandling_id is null and søknad.avbrutt is null
                        order by søknad.id
                        """.trimIndent(),
                    ).map { row ->
                        val id = row.string("søknad_id").let { SøknadId.fromString(row.string("søknad_id")) }
                        val opprettet = row.localDateTime("opprettet")
                        val kravtidspunkt = row.localDateTime("kravtidspunkt")
                        val behandlingstype = BenkBehandlingstype.SØKNAD
                        val status = BehandlingEllerSøknadForSaksoversikt.Status.Søknad

                        BehandlingEllerSøknadForSaksoversikt(
                            periode = null,
                            status = status,
                            underkjent = false,
                            kravtidspunkt = kravtidspunkt,
                            behandlingstype = behandlingstype,
                            fnr = Fnr.fromString(row.string("fnr")),
                            saksnummer = Saksnummer(
                                row.string(
                                    "saksnummer",
                                ),
                            ),
                            id = id,
                            saksbehandler = null,
                            beslutter = null,
                            sakId = SakId.fromString(row.string("sak_id")),
                            opprettet = opprettet,
                        )
                    }.asList,
                )
        }
}
