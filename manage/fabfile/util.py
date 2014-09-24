from fabric.api import *
from fabric.contrib.files import exists
import sys
import time


from os import path as p


venv = lambda: dict(env, **sys._getframe(1).f_locals)


def fullpath(dirpath):
    return p.abspath(p.normpath(dirpath))


def mkdirpath(path):
    if not exists(path):
        run("mkdir -p %s" % path)


def slashed(path):
    return path if path.endswith('/') else path+'/'


def cygpath(path):
    return local("cygpath %s" % path)


@task
def msg_sleep(sleep_time, msg=""):
    print "Pause in {0} second(s) for {1}!".format(sleep_time, msg)
    time.sleep(sleep_time)


def verify_url_content(url, string_exists_in_content, sleep_time=15, max_retry=3):
    retry_count = 1
    resp_http = ""
    while retry_count < max_retry:
        resp_http = local("curl %(url)s" % vars(), capture=True)
        if not string_exists_in_content:
            return True
        if not string_exists_in_content in resp_http:
            print "Could not find '%(string_exists_in_content)s' in response! Failed! Retry %(retry_count)s " \
                  "of %(max_retry)s." % vars()
            retry_count += 1
            time.sleep(sleep_time)
            continue
        return True
    print "#########################################################################################"
    print resp_http
    print "#########################################################################################"
    return False


def test_url(report, name, class_name, url, content):
    try:
        if verify_url_content(url, content):
            report.add_test_success(name, class_name)
        else:
            report.add_test_failure(name, class_name, "ping failure",
                                    "Unable to verify '%s' contains '%s'" % (url, content))
    except:
        e = sys.exc_info()[0]
        print e
        report.add_test_failure(name, class_name, "ping error", "Unable to verify '%s' contains '%s' because Unknonw "
                                                                "error. See log for details." % (url, content))


class PrintXml:
    xml_file = ""

    def __init__(self, file_name):
        self.xml_file = open(file_name, 'w')
        self.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")

    def write(self, text):
        self.xml_file.write(text)
        self.xml_file.write("\n")

    def close(self):
        self.xml_file.close()


class JUnitReportItem:
    name = ""
    class_name = ""
    failure_type = ""
    failure_description = ""

    def __init__(self, name, class_name, failure_type="", failure_description=""):
        self.name = name
        self.class_name = class_name
        self.failure_type = failure_type
        self.failure_description = failure_description

    def write(self, output):
        if not self.failure_type:
            output.write("  <testcase classname=\"%s\" name=\"%s\"/>" % (self.class_name, self.name))
        else:
            output.write("  <testcase classname=\"%s\" name=\"%s\">" % (self.class_name, self.name))
            output.write("    <failure type=\"%s\">%s</failure>" % (self.failure_type, self.failure_description))
            output.write("  </testcase>")


class JUnitReport:
    """ Creates a junit xml report """

    items = list()

    def add_test_success(self, name, class_name):
        self.items.append(JUnitReportItem(name, class_name))

    def add_test_failure(self, name, class_name, failure_type, failure_description):
        self.items.append(JUnitReportItem(name, class_name, failure_type, failure_description))

    def empty(self):
        return len(self.items) == 0

    def create_report(self, file_name):
        if self.empty():
            return
        print_xml = PrintXml(file_name)
        print_xml.write("<testsuite tests=\"%i\">" % (len(self.items)))
        for item in self.items:
            item.write(print_xml)
        print_xml.write("</testsuite>")
        print_xml.close()


#<testsuite tests="3">
#    <testcase classname="foo" name="ASuccessfulTest"/>
#    <testcase classname="foo" name="AnotherSuccessfulTest"/>
#    <testcase classname="foo" name="AFailingTest">
#        <failure type="NotEnoughFoo"> details about failure </failure>
#    </testcase>
#</testsuite>