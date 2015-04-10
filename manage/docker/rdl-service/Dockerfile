FROM tomcat:7.0.59-jre7
MAINTAINER Domstolsverket RINFO <rinfo@lagrummet.se>

ENV CATALINA_HOME /usr/local/tomcat
ENV PATH $CATALINA_HOME/bin:$PATH
WORKDIR $CATALINA_HOME

RUN rm -rf $CATALINA_HOME/webapps/*
ADD rinfo-service.war $CATALINA_HOME/webapps/ROOT.war
ADD server.xml $CATALINA_HOME/conf/server.xml
#ADD rinfo-service.properties /etc/rinfo/rinfo-service.properties

EXPOSE 8080
CMD ["catalina.sh", "run"]


