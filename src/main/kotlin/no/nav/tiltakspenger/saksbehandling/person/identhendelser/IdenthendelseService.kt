package no.nav.tiltakspenger.saksbehandling.person.identhendelser

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.person.pdl.aktor.v2.Aktor
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SakRepo
import no.nav.tiltakspenger.saksbehandling.person.identhendelser.repo.IdenthendelseDb
import no.nav.tiltakspenger.saksbehandling.person.identhendelser.repo.IdenthendelseRepository
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.util.UUID

class IdenthendelseService(
    private val sakRepo: SakRepo,
    private val identhendelseRepository: IdenthendelseRepository,
) {
    private val log = KotlinLogging.logger { }

    fun behandleIdenthendelse(aktor: Aktor) {
        try {
            val personidenter = aktor.identifikatorer.map { it.toPersonident() }
            val gjeldendeIdent = finnGjeldendeIdent(personidenter)
            val nyttFnr = Fnr.tryFromString(gjeldendeIdent.ident)
            if (nyttFnr == null) {
                Sikkerlogg.error { "Ny ident er ikke gyldig fnr: ${gjeldendeIdent.ident}" }
                throw IllegalArgumentException("Ny ident må være gyldig fnr")
            }

            val identOgSakMap = mutableMapOf<Fnr, Sak>()
            personidenter.filter { it.historisk && it.identtype != Identtype.AKTORID }.forEach {
                val fnr = Fnr.tryFromString(it.ident) ?: return@forEach
                val saker = sakRepo.hentForFnr(fnr)
                if (saker.saker.isNotEmpty()) {
                    val sak = saker.saker.single()
                    identOgSakMap[fnr] = sak
                }
            }

            val harSakForNyttFnr = sakRepo.hentForFnr(nyttFnr).saker.isNotEmpty()
            if (harSakForNyttFnr) {
                log.warn { "Fant sak for nytt fnr" }
            }

            if (identOgSakMap.entries.size > 1 || (harSakForNyttFnr && identOgSakMap.entries.isNotEmpty())) {
                log.error { "Fant flere saker knyttet til samme nye fnr, kan ikke behandle identendringen. Se sikkerlogg for mer informasjon" }
                Sikkerlogg.error { "Fant flere saker knyttet til samme nye fnr, kan ikke behandle identendringen: $personidenter" }
                throw IllegalStateException("Fant flere saker knyttet til samme nye fnr, kan ikke behandle identendringen")
            }

            identOgSakMap.entries.forEach {
                val fnr = it.key
                val sak = it.value
                val identhendelseDb = IdenthendelseDb(
                    id = UUID.randomUUID(),
                    gammeltFnr = fnr,
                    nyttFnr = nyttFnr,
                    personidenter = personidenter,
                    sakId = sak.id,
                    produsertHendelse = null,
                    oppdatertDatabase = null,
                )
                identhendelseRepository.lagre(identhendelseDb)
                log.info { "Lagret identhendelse med id ${identhendelseDb.id}" }
            }
        } catch (e: Exception) {
            log.error(e) { "Noe gikk galt ved håndtering av identhendelse" }
            throw e
        }
    }

    private fun finnGjeldendeIdent(personidenter: List<Personident>): Personident {
        val gjeldendeIdent = personidenter.firstOrNull {
            !it.historisk && it.identtype == Identtype.FOLKEREGISTERIDENT
        } ?: personidenter.firstOrNull {
            !it.historisk && it.identtype == Identtype.NPID
        }
        if (gjeldendeIdent == null) {
            log.error { "Kan ikke behandle identhendelse uten gjeldende ident, se sikkerlogg for mer informasjon" }
            Sikkerlogg.error { "Kan ikke behandle identhendelse uten gjeldende ident: $personidenter" }
            throw IllegalArgumentException("Kan ikke behandle identhendelse uten gjeldende ident")
        }
        return gjeldendeIdent
    }
}
