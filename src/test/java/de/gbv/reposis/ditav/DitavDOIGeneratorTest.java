package de.gbv.reposis.ditav;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.pi.MCRGenericPIGenerator;
import org.mycore.pi.MCRPIGenerator;
import org.mycore.pi.doi.MCRDigitalObjectIdentifier;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

public class DitavDOIGeneratorTest extends MCRTestCase {

  @Test
  public void testDOI() throws IOException, JDOMException, MCRPersistentIdentifierException {
    String generatorPropertyKey = "MCR.PI.Generator.Ditav";

    try (InputStream is = DitavDOIGeneratorTest.class.getClassLoader()
        .getResourceAsStream("objects/ditav_mods_00000004.xml")) {
      MCRPIGenerator<MCRDigitalObjectIdentifier> gen1
          = MCRConfiguration2.getInstanceOfOrThrow(MCRPIGenerator.class, generatorPropertyKey+ "1");

      Document doc = new SAXBuilder().build(is);
      MCRObject mcrObject = new MCRObject(doc);
      MCRDigitalObjectIdentifier doi = gen1.generate(mcrObject, null);
      Assert.assertEquals("DOI should match", "10.58137/002-2025-4", doi.asString());
    }

    try (InputStream is = DitavDOIGeneratorTest.class.getClassLoader()
        .getResourceAsStream("objects/ditav_mods_00000005.xml")) {
      MCRPIGenerator<MCRDigitalObjectIdentifier> gen2
          = MCRConfiguration2.getInstanceOfOrThrow(MCRPIGenerator.class, generatorPropertyKey + "2");

      Document doc = new SAXBuilder().build(is);
      MCRObject mcrObject = new MCRObject(doc);
      MCRDigitalObjectIdentifier doi = gen2.generate(mcrObject, null);
      Assert.assertEquals("DOI should match", "10.58137/002-2025-5", doi.asString());
    }

  }


  @Override
  protected Map<String, String> getTestProperties() {
    Map<String, String> properties = super.getTestProperties();
    properties.put("MCR.Metadata.Type.mods", "true");
    properties.put("MCR.PI.Generator.Ditav1", MCRGenericPIGenerator.class.getName());
    properties.put("MCR.PI.Generator.Ditav1.GeneralPattern", "10.58137/$1-$2-$3");
    properties.put("MCR.PI.Generator.Ditav1.Type", "doi");
    properties.put("MCR.PI.Generator.Ditav1.XPath.1",
        "/mycoreobject/metadata/def.modsContainer/modsContainer/mods:mods/mods:relatedItem[@type='host']/mods:identifier[@type='internalId']");
    properties.put("MCR.PI.Generator.Ditav1.XPath.2",
        "/mycoreobject/metadata/def.modsContainer/modsContainer/mods:mods/mods:relatedItem[@type='host']/mods:originInfo/mods:dateIssued[@encoding='w3cdtf']");
    properties.put("MCR.PI.Generator.Ditav1.XPath.3",
        "string(number(substring-after(substring-after(/mycoreobject/@ID, '_'), '_')))");

    properties.put("MCR.PI.Generator.Ditav2", MCRGenericPIGenerator.class.getName());
    properties.put("MCR.PI.Generator.Ditav2.GeneralPattern", "10.58137/$1-$2");
    properties.put("MCR.PI.Generator.Ditav2.Type", "doi");
    properties.put("MCR.PI.Generator.Ditav2.XPath.1",
        "/mycoreobject/metadata/def.modsContainer/modsContainer/mods:mods/mods:relatedItem[@type='host']/mods:identifier[@type='internalId']");
    properties.put("MCR.PI.Generator.Ditav2.XPath.2",
        "string(number(substring-after(substring-after(/mycoreobject/@ID, '_'), '_')))");


    return properties;
  }
}
