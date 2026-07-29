package no.nav.tiltakspenger.saksbehandling.common

import org.junit.jupiter.api.parallel.ResourceAccessMode
import org.junit.jupiter.api.parallel.ResourceLock

/**
 * Markerer en test som kjører med `runIsolated = true`, dvs. med eksklusiv tilgang til det isolerte
 * databaseskjemaet (som trunkeres før testen).
 *
 * Gir to garantier:
 * 1. Runneren (JUnit) serialiserer alle tester med denne annotasjonen når parallellkjøring er aktivert, via [ResourceLock] med READ_WRITE på en felles ressurs.
 * 2. [no.nav.tiltakspenger.saksbehandling.infra.repo.TestDatabaseManager] håndhever det samme i koden med en JVM-global lås, uavhengig av runner-konfigurasjon.
 *
 * Konvensjonen (alle `runIsolated = true`-tester skal ha annotasjonen) håndheves av
 * [IsolatedDatabaseTestKonvensjonTest].
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ResourceLock(ISOLATED_DATABASE_RESOURCE, mode = ResourceAccessMode.READ_WRITE)
annotation class IsolatedDatabaseTest

const val ISOLATED_DATABASE_RESOURCE = "tiltakspenger.isolated-database"
