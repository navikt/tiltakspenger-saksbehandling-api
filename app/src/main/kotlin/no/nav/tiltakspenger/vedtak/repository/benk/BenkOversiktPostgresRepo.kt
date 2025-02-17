package no.nav.tiltakspenger.vedtak.repository.benk

import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.domene.benk.BehandlingEllerSøknadForSaksoversikt
import no.nav.tiltakspenger.saksbehandling.domene.benk.BenkBehandlingstype
import no.nav.tiltakspenger.saksbehandling.domene.benk.toBenkBehandlingstype
import no.nav.tiltakspenger.saksbehandling.domene.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.ports.SaksoversiktRepo
import no.nav.tiltakspenger.vedtak.repository.behandling.attesteringer.toAttesteringer
import no.nav.tiltakspenger.vedtak.repository.behandling.toBehandlingsstatus
import no.nav.tiltakspenger.vedtak.repository.behandling.toBehandlingstype

class BenkOversiktPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : SaksoversiktRepo {
    override fun hentAlleBehandlinger(sessionContext: SessionContext?): List<BehandlingEllerSøknadForSaksoversikt> =
        sessionFactory.withSession(sessionContext) { session ->
            session
                .run(
                    queryOf(
                        //language=SQL
                        """
                        select behandling.id as behandling_id,
                          sak.ident,
                          behandling.opprettet,
                          behandling.fra_og_med,
                          behandling.til_og_med,
                          behandling.status,
                          sak.saksnummer,
                          behandling.saksbehandler,
                          behandling.beslutter,
                          behandling.attesteringer,
                          behandling.sak_id,
                          behandling.behandlingstype,
                          søknad.tidsstempel_hos_oss as kravtidspunkt,
                          behandling.saksopplysninger
                        from behandling
                        left join sak on sak.id = behandling.sak_id
                        left join søknad on behandling.id = søknad.behandling_id
                        where behandling.status != 'VEDTATT'
                        order by sak.saksnummer, behandling.id
                        """.trimIndent(),
                    ).map { row ->
                        val id = row.string("behandling_id").let { BehandlingId.fromString(it) }
                        val periode =
                            Periode(
                                fraOgMed = row.localDate("fra_og_med"),
                                tilOgMed = row.localDate("til_og_med"),
                            )
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
                            fnr = Fnr.fromString(row.string("ident")),
                            saksnummer = row.stringOrNull("saksnummer")?.let { Saksnummer(it) },
                            id = id,
                            saksbehandler = saksbehandler,
                            beslutter = beslutter,
                            sakId = row.stringOrNull("sak_id")?.let { SakId.fromString(it) },
                            erDeprecatedBehandling = row.stringOrNull("saksopplysninger") == null,
                        )
                    }.asList,
                )
        }

    override fun hentAlleSøknader(sessionContext: SessionContext?): List<BehandlingEllerSøknadForSaksoversikt> =
        sessionFactory.withSession(sessionContext) { session ->
            session
                .run(
                    queryOf(
                        //language=SQL
                        """
                        select søknad.id as søknad_id,
                          søknad.ident,
                          søknad.opprettet,
                          søknad.behandling_id,
                          søknad.sak_id
                        from søknad
                        where søknad.behandling_id is null
                        order by søknad.id
                        """.trimIndent(),
                    ).map { row ->
                        val id = row.string("søknad_id").let { SøknadId.fromString(row.string("søknad_id")) }
                        val opprettet = row.localDateTime("opprettet")
                        val behandlingstype = BenkBehandlingstype.SØKNAD
                        val status = BehandlingEllerSøknadForSaksoversikt.Status.Søknad
                        BehandlingEllerSøknadForSaksoversikt(
                            periode = null,
                            status = status,
                            underkjent = false,
                            kravtidspunkt = opprettet,
                            behandlingstype = behandlingstype,
                            fnr = Fnr.fromString(row.string("ident")),
                            saksnummer = null,
                            id = id,
                            saksbehandler = null,
                            beslutter = null,
                            sakId = row.stringOrNull("sak_id")?.let { SakId.fromString(it) },
                        )
                    }.asList,
                )
        }
}
