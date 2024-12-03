package com.atlassian.ctt.integrations.jql

import io.atlassian.migration.sanitisation.Sanitisation
import io.atlassian.migration.sanitisation.sanitiser.extraction.CustomFieldIdentifierExtractionService
import io.atlassian.migration.sanitisation.sanitiser.extraction.ExtractionService
import io.atlassian.migration.sanitisation.sanitiser.extraction.GenericNumericalIdentifierExtractionService
import io.atlassian.migration.sanitisation.sanitiser.extraction.UserIdentifierExtractionService
import io.atlassian.migration.sanitisation.sanitiser.parser.JQLParserWrapper
import io.atlassian.migration.sanitisation.sanitiser.parser.visitor.JQLClauseIdentifier
import io.atlassian.migration.sanitisation.sanitiser.sanitisation.CustomFieldIdentifierSanitisationService
import io.atlassian.migration.sanitisation.sanitiser.sanitisation.GeneralNumericalIdentifierSanitisationService
import io.atlassian.migration.sanitisation.sanitiser.sanitisation.SanitisationService
import io.atlassian.migration.sanitisation.sanitiser.sanitisation.UserIdentifierSanitisationService
import io.atlassian.migration.sanitisation.stub.StubUserHandlerMigrationService

typealias IdentifierSet = io.atlassian.migration.sanitisation.sanitiser.model.IdentifierSet
typealias IdentifierType = io.atlassian.migration.sanitisation.sanitiser.model.IdentifierType
typealias ParseCancellationException = org.antlr.v4.runtime.misc.ParseCancellationException

class JQLSanitisationLibrary {
    val sanitiser: Sanitisation
        get() =
            Sanitisation(
                extractors = getExtractors(),
                jqlParserWrapper = JQLParserWrapper(),
                sanitisers = getSanitisers(),
            )

    /**
     * Utility function to return a list of extractor services.
     */
    private fun getExtractors(): List<ExtractionService> =
        listOf(
            CustomFieldIdentifierExtractionService(),
            GenericNumericalIdentifierExtractionService(JQLClauseIdentifier()),
            UserIdentifierExtractionService(StubUserHandlerMigrationService()),
        )

    /**
     * Utility function to return a list of sanitiser services.
     */
    private fun getSanitisers(): List<SanitisationService> =
        listOf(
            CustomFieldIdentifierSanitisationService(),
            GeneralNumericalIdentifierSanitisationService(JQLClauseIdentifier()),
            UserIdentifierSanitisationService(StubUserHandlerMigrationService()),
        )
}
