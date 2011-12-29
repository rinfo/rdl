package se.lagrummet.rinfo.main.storage


class SchemaReportException extends Exception {

    def report

    SchemaReportException(report) {
        this.report = report
    }

}
