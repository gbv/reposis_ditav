package de.gbv.reposis.ditav;

import java.io.IOException;
import java.io.InputStream;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.pi.MCRGenericPIGenerator;
import org.mycore.pi.MCRPIGenerator;
import org.mycore.pi.doi.MCRDigitalObjectIdentifier;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@MCRTestConfiguration(
    properties = {
        @MCRTestProperty(key = "MCR.Metadata.Type.mods", string = "true"),
        @MCRTestProperty(key = "MCR.Metadata.Type.test", string = "true"),
        @MCRTestProperty(key = "MCR.PI.Generator.Ditav", classNameOf = MCRGenericPIGenerator.class),
        @MCRTestProperty(key = "MCR.PI.Generator.Ditav.GeneralPattern", string = "10.58137/$1-$2"),
        @MCRTestProperty(key = "MCR.PI.Generator.Ditav.Type", string = "doi"),
        @MCRTestProperty(
            key = "MCR.PI.Generator.Ditav.XPath.1",
            string = "/mycoreobject/metadata/def.modsContainer/modsContainer/mods:mods/mods:identifier[@type='qedid']|/mycoreobject/metadata/def.modsContainer/modsContainer/mods:mods/mods:relatedItem[@type='host']/mods:identifier[@type='qedid']"
        ),
        @MCRTestProperty(
            key = "MCR.PI.Generator.Ditav.XPath.2",
            string = "string(number(substring-after(substring-after(/mycoreobject/@ID, '_'), '_')))"
        )
    }
)
public class DitavDOIGeneratorTest {

  @Test
  public void testDOI() throws IOException, JDOMException, MCRPersistentIdentifierException {
    String generatorPropertyKey = "MCR.PI.Generator.Ditav";

    try (InputStream is = DitavDOIGeneratorTest.class.getClassLoader()
        .getResourceAsStream("objects/ditav_mods_00000004.xml")) {
      MCRPIGenerator<MCRDigitalObjectIdentifier> gen
          = MCRConfiguration2.getInstanceOfOrThrow(MCRPIGenerator.class, generatorPropertyKey);

      Document doc = new SAXBuilder().build(is);
      MCRObject mcrObject = new MCRObject(doc);
      MCRDigitalObjectIdentifier doi = gen.generate(mcrObject, null);
      Assertions.assertEquals("10.58137/002-2025-4", doi.asString(), "DOI should match");
    }

    try (InputStream is = DitavDOIGeneratorTest.class.getClassLoader()
        .getResourceAsStream("objects/ditav_mods_00000005.xml")) {
      MCRPIGenerator<MCRDigitalObjectIdentifier> gen
          = MCRConfiguration2.getInstanceOfOrThrow(MCRPIGenerator.class, generatorPropertyKey);

      Document doc = new SAXBuilder().build(is);
      MCRObject mcrObject = new MCRObject(doc);
      MCRDigitalObjectIdentifier doi = gen.generate(mcrObject, null);
      Assertions.assertEquals( "10.58137/002-2025-5", doi.asString(), "DOI should match");
    }

    try (InputStream is = DitavDOIGeneratorTest.class.getClassLoader()
        .getResourceAsStream("objects/ditav_mods_00000001.xml")) {
      MCRPIGenerator<MCRDigitalObjectIdentifier> gen
          = MCRConfiguration2.getInstanceOfOrThrow(MCRPIGenerator.class, generatorPropertyKey);

      Document doc = new SAXBuilder().build(is);
      MCRObject mcrObject = new MCRObject(doc);
      MCRDigitalObjectIdentifier doi = gen.generate(mcrObject, null);
      Assertions.assertEquals( "10.58137/002-2025-1", doi.asString(), "DOI should match");
    }

  }

}
