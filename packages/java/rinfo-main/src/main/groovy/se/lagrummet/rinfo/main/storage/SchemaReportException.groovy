package se.lagrummet.rinfo.main.storage

import se.lagrummet.rinfo.base.rdf.checker.Report

class SchemaReportException extends Exception {

    Report report

    SchemaReportException(Report report, String cause) {
        super(cause)
        this.report = report
    }

}
