package se.lagrummet.rinfo.service.dataview

import org.antlr.stringtemplate.StringTemplate
import org.antlr.stringtemplate.StringTemplateGroup


class TemplateUtil {

    StringTemplateGroup templates

    TemplateUtil(StringTemplateGroup templates) {
        this.templates = templates
    }

    String runTemplate(String templatePath, Map data) {
        StringTemplate st = templates.getInstanceOf(templatePath)
        if (data != null) {
            data.each { key, value ->
                st.setAttribute(key, value)
            }
        }
        return st.toString()
    }

}

