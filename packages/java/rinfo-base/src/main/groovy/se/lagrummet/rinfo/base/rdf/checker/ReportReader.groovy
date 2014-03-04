package se.lagrummet.rinfo.base.rdf.checker

import org.openrdf.model.Statement

class ReportReader {

    protected static final String WARNING = "http://purl.org/net/schemarama#Warning"
    protected static final String ERROR = "http://purl.org/net/schemarama#Error"
    protected static final String TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"
    protected static final String MESSAGE = "http://purl.org/net/schemarama#message"
    protected static final String IMPLICATED = "http://purl.org/net/schemarama#implicated"
    protected static final String FIRST = "http://www.w3.org/1999/02/22-rdf-syntax-ns#first"
    protected static final String REST = "http://www.w3.org/1999/02/22-rdf-syntax-ns#rest"
    protected static final String NIL = "http://www.w3.org/1999/02/22-rdf-syntax-ns#nil"

    List<Statement> allStatements

    ReportReader(List<Statement> allStatements) {
        this.allStatements = allStatements
    }

    List<Statement> getStatementsForErrorType(String errorType) {
        return getStatementsForValue(errorType)
    }

    List<Statement> getStatementsForValue(String value) {
        def result = new ArrayList<Statement>();
        def it = allStatements.iterator();

        while (it.hasNext()) {
            Statement statement = it.next();
            if (value.equalsIgnoreCase(statement.getObject().stringValue())) {
                result.add(statement)
            }
        }

        return result
    }

    String getErrorsAndWarnings() {
        def errors = getErrorDescriptions()
        def warnings = getWarningDescriptions()
        warnings = errors ? ", " + warnings : warnings
        return errors + warnings
    }

    String getErrorDescriptions() {
        def descriptions = getDescriptions(getStatementsForErrorType(ERROR))
        if (descriptions) {
            return "ERRORS = [" + descriptions + "]"
        } else {
            return ""
        }
    }

    String getWarningDescriptions() {
        def descriptions = getDescriptions(getStatementsForErrorType(WARNING))
        if (descriptions) {
            return "WARNINGS = [" + descriptions + "]"
        } else {
            return ""
        }
    }

    String getDescriptions(final List<Statement> result) {
        def descriptions = ""

        def it = result.iterator();
            while (it.hasNext()) {
                Statement st = it.next();
                def subject = st.getSubject().stringValue()
                def message = getValueForSubjectAndPredicate(subject, MESSAGE)

                if (message) {
                    def implicated = getValueForSubjectAndPredicate(subject, IMPLICATED)
                    def allImplicated = getAllImplicatedForSubject(implicated)
                    def implicatedInMessage = replaceImplicatedInMessage(message, allImplicated)
                    implicatedInMessage = implicatedInMessage.trim()
                    descriptions += it.hasNext() ? implicatedInMessage + ", " : implicatedInMessage
                }
            }

        return descriptions
    }


    String getValueForSubjectAndPredicate(String subject, String predicate) {
        def it = allStatements.iterator()

        if (!subject || !predicate) {
            return null
        }

        while (it.hasNext()) {
            Statement statement = it.next();
            if (subject.equalsIgnoreCase(statement.getSubject().stringValue()) &&
                predicate.equalsIgnoreCase(statement.getPredicate().stringValue())) {
                return statement.getObject().stringValue()
            }
        }

        return null
    }

    String getFirstForSubject(String subject) {
        def first = getValueForSubjectAndPredicate(subject, FIRST)
        return removeURIBaseFromValue(first)
    }


    String replaceImplicatedInMessage(String message, List<String> implicatedList) {
        for (int i = 0; i < implicatedList.size(); i++) {
            def pos = i + 1
            message = message.replace("["+ pos +"]", implicatedList.get(i))
        }
        return message
    }

    List<String> getAllImplicatedForSubject(String subject) {
        def list = new ArrayList<String>();

        if(!subject) {
            return list
        }

        def first = getFirstForSubject(subject)
        if (first) {
            list.add(first)
            def rest = getValueForSubjectAndPredicate(subject, REST)
            if (rest && !rest.equalsIgnoreCase(NIL)) {
                list.addAll(getAllImplicatedForSubject(rest))
                return list
            }
        }

        return list
    }

    String removeURIBaseFromValue(String value) {
        value = value.replace("http://rinfo.lagrummet.se/ns/2008/11/rinfo/publ#","")
        return value.replace("http://purl.org/dc/terms/","")
    }

    void logAll() {
        println(">>>>>>>>>>>>>logAll()>>>>>>>>>>>>>>> :")
        logStatements(allStatements)
    }

    void logStatements(List<Statement> result) {
        def it = result.iterator();
        while (it.hasNext()) {
            Statement st = it.next();
            String statements = st.getSubject().stringValue()
            statements += ", " + st.getPredicate().stringValue()
            statements += ", " + st.getObject().stringValue()
            println(statements)
        }
    }
}