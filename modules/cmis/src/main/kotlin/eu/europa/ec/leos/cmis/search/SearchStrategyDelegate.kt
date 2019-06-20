/*
 * Copyright 2018 European Commission
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *     https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */
package eu.europa.ec.leos.cmis.search

import org.apache.chemistry.opencmis.client.api.Session
import kotlin.reflect.KProperty

internal class SearchStrategyDelegate(
        private val cmisSession: Session
) {
    private val strategy by lazy(this::init)

    operator fun getValue(thisRef: Any?, property: KProperty<*>) = strategy

    private fun init() = when (cmisSession.repositoryInfo.capabilities.isAllVersionsSearchableSupported) {
        true -> SearchStrategyDiscoveryServices(cmisSession)
        else -> SearchStrategyNavigationServices(cmisSession)
    }
}
