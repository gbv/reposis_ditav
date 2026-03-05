package de.gbv.reposis.ditav;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.transform.JDOMResult;
import org.jdom2.transform.JDOMSource;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.junit.jupiter.api.Test;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class DitavIndexingTest {

    private static final Namespace XSL_NS = Namespace.getNamespace("xsl",
        "http://www.w3.org/1999/XSL/Transform");

    @Test
    public void testFacetIndexing() throws IOException, JDOMException, TransformerException {
        // Load XSL and remove xsl:import and xsl:apply-imports
        Document xslDoc;
        try (InputStream is = DitavIndexingTest.class.getClassLoader()
            .getResourceAsStream("xsl/ditav-solr.xsl")) {
            xslDoc = new SAXBuilder().build(is);
        }

        Element xslRoot = xslDoc.getRootElement();
        xslRoot.removeChild("import", XSL_NS);

        XPathExpression<Element> applyImportsXP = XPathFactory.instance()
            .compile("//xsl:apply-imports", Filters.element(), null, XSL_NS);
        applyImportsXP.evaluate(xslDoc).forEach(Element::detach);

        // Add wrapper template: <xsl:template match="/"><doc><xsl:apply-templates select="mycoreobject"/></doc></xsl:template>
        Element applyTemplates = new Element("apply-templates", XSL_NS);
        applyTemplates.setAttribute("select", "mycoreobject");
        Element docWrapper = new Element("doc");
        docWrapper.addContent(applyTemplates);
        Element wrapperTemplate = new Element("template", XSL_NS);
        wrapperTemplate.setAttribute("match", "/");
        wrapperTemplate.addContent(docWrapper);
        xslRoot.addContent(wrapperTemplate);

        // Load MCRObject document
        Document mcrObjectDoc;
        try (InputStream is = DitavIndexingTest.class.getClassLoader()
            .getResourceAsStream("objects/lod_mods_00014883.xml")) {
            mcrObjectDoc = new SAXBuilder().build(is);
        }

        // Transform
        Transformer transformer = TransformerFactory.newInstance()
            .newTransformer(new JDOMSource(xslDoc));
        JDOMResult result = new JDOMResult();
        transformer.transform(new JDOMSource(mcrObjectDoc), result);

        // Assert fields
        Document resultDoc = result.getDocument();
        assertNotNull(resultDoc, "Result document must not be null");
        List<Element> fields = resultDoc.getRootElement().getChildren("field");

        assertEquals("sp_ags_estado_6610_0004",
            getFieldValue(fields, "mods.identifier.type.intern"));
        assertEquals("Letter from Golitsyn to Grimaldo",
            getFieldValue(fields, "ditav.mods.title.lang.en"));
        assertEquals("Golitsyn, Sergei Dmitrievich",
            getFieldValue(fields, "ditav.mods.author.facet"));
        assertEquals("Grimaldo, Marquis of, José",
            getFieldValue(fields, "ditav.mods.recipient.facet"));
    }

    private String getFieldValue(List<Element> fields, String name) {
        return fields.stream()
            .filter(f -> name.equals(f.getAttributeValue("name")))
            .map(Element::getTextTrim)
            .findFirst()
            .orElse(null);
    }
}
