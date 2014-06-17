package se.lagrummet.rinfo.base.rdf.checker

import org.openrdf.repository.RepositoryConnection
import org.openrdf.repository.RepositoryResult
import org.openrdf.model.Statement
import org.openrdf.model.Resource
import org.openrdf.model.Value
import org.openrdf.model.URI

import spock.lang.*

class ReportReaderSpec extends Specification {

    private static final String ERROR_MESSAGE = "Värdet '[1]' matchar inte datatyp för egenskap: [2]"
    private static final String ERROR_IMPLICATED_1 = "9999-99-99"
    private static final String ERROR_IMPLICATED_2 = "http://rinfo.lagrummet.se/ns/2008/11/rinfo/publ#ikrafttradandedatum"
    private static final String WARNING_MESSAGE = "Saknar obligatoriskt värde för egenskap: [1]"
    private static final String WARNING_IMPLICATED = "http://rinfo.lagrummet.se/ns/2008/11/rinfo/publ#bemyndigande"

    def "replace implicated in message"() {
        setup:
        def reportReader = new ReportReader([])
        def list = [ERROR_IMPLICATED_1, ERROR_IMPLICATED_2]

        when:
        def result = reportReader.replaceImplicatedInMessage(ERROR_MESSAGE, list)

        then:
        result.equals("Värdet '"+ ERROR_IMPLICATED_1 + "' matchar inte datatyp för egenskap: " + ERROR_IMPLICATED_2)
    }

    def "get error message"() {
        setup:
        def reportReader = new ReportReader(mockedErrorStatements())
        def statement = reportReader.getStatementsForErrorType(ReportReader.ERROR).get(0)
        def subject = statement.getSubject().stringValue()

        when:
        def message = reportReader.getValueForSubjectAndPredicate(subject, ReportReader.MESSAGE)

        then:
        message.equals(ERROR_MESSAGE)
    }

    def "get warning message"() {
        setup:
        def reportReader = new ReportReader(mockedWarningStatements())
        def statement = reportReader.getStatementsForErrorType(ReportReader.WARNING).get(0)
        def subject = statement.getSubject().stringValue()

        when:
        def message = reportReader.getValueForSubjectAndPredicate(subject, ReportReader.MESSAGE)

        then:
        message.equals(WARNING_MESSAGE)
    }

    def "get all errors and warnings"() {
        setup:
        def warnings = mockedWarningStatements()
        def errors = mockedErrorStatements()
        errors.addAll(warnings)
        def reportReader = new ReportReader(errors)

        when:
        def result = reportReader.getErrorsAndWarnings()

        then:
        result.equals("ERRORS = [Värdet '9999-99-99' matchar inte datatyp för egenskap: ikrafttradandedatum], " +
                      "WARNINGS = [Saknar obligatoriskt värde för egenskap: bemyndigande]")
    }

    List<Statement> mockedErrorStatements() {
        def statements = new ArrayList<Statement>();
        statements.add(mockStatement("node18hk3uno4x250", ReportReader.TYPE, ReportReader.ERROR))
        statements.add(mockStatement("node18hk3uno4x250", ReportReader.MESSAGE, ERROR_MESSAGE))
        statements.add(mockStatement("node18hk3uno4x250", ReportReader.IMPLICATED, "node18hk3uno4x251"))
        statements.add(mockStatement("node18hk3uno4x251", ReportReader.FIRST, ERROR_IMPLICATED_1))
        statements.add(mockStatement("node18hk3uno4x251", ReportReader.REST, "node18hk3uno4x252"))
        statements.add(mockStatement("node18hk3uno4x252", ReportReader.FIRST, ERROR_IMPLICATED_2))
        statements.add(mockStatement("node18hk3uno4x252", ReportReader.REST, ReportReader.NIL))
        return statements
    }

    List<Statement> mockedWarningStatements() {
        def statements = new ArrayList<Statement>();
        statements.add(mockStatement("node18hk3uno4x253", ReportReader.TYPE, ReportReader.WARNING))
        statements.add(mockStatement("node18hk3uno4x253", ReportReader.MESSAGE, WARNING_MESSAGE))
        statements.add(mockStatement("node18hk3uno4x253", ReportReader.IMPLICATED, "node18hk3uno4x254"))
        statements.add(mockStatement("node18hk3uno4x254", ReportReader.FIRST, WARNING_IMPLICATED))
        statements.add(mockStatement("node18hk3uno4x254", ReportReader.REST, ReportReader.NIL))
        return statements
    }

    Statement mockStatement(String subject, String predicate, String object) {
        Resource subj = Mock()
        subj.stringValue() >> subject
        Value obj = Mock()
        obj.stringValue() >> object
        URI pred = Mock()
        pred.stringValue() >> predicate
        pred.toString() >> predicate

        Statement statement = Mock()
        statement.getObject() >> obj
        statement.getSubject() >> subj
        statement.getPredicate() >> pred
        return statement
    }
}