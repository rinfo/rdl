FROM tomcat:7.0.59-jre7
MAINTAINER Domstolsverket RINFO <rinfo@lagrummet.se>

ENV CATALINA_HOME /usr/local/tomcat
ENV PATH $CATALINA_HOME/bin:$PATH
WORKDIR $CATALINA_HOME

RUN rm -rf $CATALINA_HOME/webapps/*
ADD rinfo-main.war $CATALINA_HOME/webapps/ROOT.war
ADD server.xml $CATALINA_HOME/conf/server.xml
#ADD rinfo-main.properties /etc/rinfo/rinfo-main.properties

EXPOSE 8080
CMD ["catalina.sh", "run"]


