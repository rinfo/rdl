import ConfigParser
from fabric.api import *
from fabric.contrib.files import exists
from fabric.decorators import task
from fabric.operations import put, run
from fabric.state import env
import sys
import time
import os
import errno
from functools import wraps

from os import path as p
from os.path import expanduser


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


PASSWORD_FILE_NAME = 'password.cfg'
PASSWORD_FILE_STANDARD_PASSWORD_PARAM_NAME = 'standard.password'
PASSWORD_FILE_FTP_USERNAME_PARAM_NAME = 'ftp.username'
PASSWORD_FILE_FTP_PASSWORD_PARAM_NAME = 'ftp.password'
PASSWORD_FILE_DB_USERNAME_PARAM_NAME = 'db.username'
PASSWORD_FILE_DB_PASSWORD_PARAM_NAME = 'db.password'
PASSWORD_FILE_ADMIN_USERNAME_PARAM_NAME = 'admin.username'
PASSWORD_FILE_ADMIN_PASSWORD_PARAM_NAME = 'admin.password'
PASSWORD_FILE_PARAMS = (PASSWORD_FILE_STANDARD_PASSWORD_PARAM_NAME,
                        PASSWORD_FILE_FTP_USERNAME_PARAM_NAME,
                        PASSWORD_FILE_FTP_PASSWORD_PARAM_NAME,
                        PASSWORD_FILE_ADMIN_USERNAME_PARAM_NAME,
                        PASSWORD_FILE_ADMIN_PASSWORD_PARAM_NAME,
                        PASSWORD_FILE_DB_USERNAME_PARAM_NAME,
                        PASSWORD_FILE_DB_PASSWORD_PARAM_NAME)

def get_password_file_name_and_path():
    return "%s/%s" % (expanduser("~"), PASSWORD_FILE_NAME)

def get_password_config():
    password_file_name_ = get_password_file_name_and_path()
    try:
        config = ConfigParser.RawConfigParser()
        config.read(password_file_name_)
        return config
    except:
        config = ConfigParser.RawConfigParser()
        return config


@task
def create_or_update_password_store():
    """ Creates a password file to be used by the program """
    print "To create password store '%s'. Please enter passwords " % PASSWORD_FILE_NAME
    standard_password = raw_input("Standard password: ")
    ftp_username = raw_input("ftp username: ")
    ftp_password = raw_input("ftp password: ")
    db_username = raw_input("db username: ")
    db_password = raw_input("db password: ")
    admin_username = raw_input("admin username: ")
    admin_password = raw_input("admin password: ")
    config = get_password_config()
    if not config.has_section(env.target):
        config.add_section(env.target)
    config.set(env.target, PASSWORD_FILE_STANDARD_PASSWORD_PARAM_NAME, standard_password)
    config.set(env.target, PASSWORD_FILE_FTP_USERNAME_PARAM_NAME, ftp_username)
    config.set(env.target, PASSWORD_FILE_FTP_PASSWORD_PARAM_NAME, ftp_password)
    config.set(env.target, PASSWORD_FILE_DB_USERNAME_PARAM_NAME, db_username)
    config.set(env.target, PASSWORD_FILE_DB_PASSWORD_PARAM_NAME, db_password)
    config.set(env.target, PASSWORD_FILE_ADMIN_USERNAME_PARAM_NAME, admin_username)
    config.set(env.target, PASSWORD_FILE_ADMIN_PASSWORD_PARAM_NAME, admin_password)
    password_file_name_ = get_password_file_name_and_path()
    with open(password_file_name_, 'wb') as configfile:
        config.write(configfile)
        print "Written to file %s" % password_file_name_


@task
def get_value_from_password_store(name, default_value=''):
    if not name in PASSWORD_FILE_PARAMS:
        print "Param name '%s' not accepted here"
        return default_value
    try:
        value = get_password_config().get(env.target, name)
        if not value:
            return default_value
        return value
    except:
        return default_value


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
        make_sure_directory_exists(file_name)
        print_xml = PrintXml(file_name)
        print_xml.write("<testsuite tests=\"%i\">" % (len(self.items)))
        for item in self.items:
            item.write(print_xml)
        print_xml.write("</testsuite>")
        print_xml.close()


def make_sure_directory_exists(file_name_and_path):
    try:
        os.makedirs(os.path.abspath(os.path.dirname(file_name_and_path)))
    except OSError as exc:
        if exc.errno != errno.EEXIST:
            raise
    return file_name_and_path

def exit_on_error(func):
    @wraps(func)
    def wrapper(*args, **kwargs):
        try:
            return func(*args, **kwargs)
        except Exception as e:
            print 'Got some exception, exiting with 1 to make it rain in jenkins..'
            print e
            sys.exit(1)
        raise
    return wrapper

#<testsuite tests="3">
#    <testcase classname="foo" name="ASuccessfulTest"/>
#    <testcase classname="foo" name="AnotherSuccessfulTest"/>
#    <testcase classname="foo" name="AFailingTest">
#        <failure type="NotEnoughFoo"> details about failure </failure>
#    </testcase>
#</testsuite>


@task
def install_public_key(id_rsa_pub_filename='id_rsa.pub'):
    mkdirpath('/home/%s/.ssh' % env.user)
    put('%s/.ssh/%s' % (expanduser('~'), id_rsa_pub_filename), '/home/%s/.' % env.user)
    run('cat %s >> .ssh/authorized_keys' % id_rsa_pub_filename)
    run('rm %s' % id_rsa_pub_filename)


def role_is_active(role):
    return env.host in env.roledefs[role]
