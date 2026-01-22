package de.gbv.reposis.ditav;


import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;


public class DitavFrontEndRedirectingDOIServiceTest extends MCRTestCase {

  private static final Logger LOGGER = LogManager.getLogger();

  public static final String TEST_NUMBER_PART_1 = "1234";
  public static final String TEST_OBJECT_ID_1 = "ditav_test_0000" + TEST_NUMBER_PART_1;

  public static final String TEST_NUMBER_PART_2 = "4321";
  public static final String TEST_OBJECT_ID_2 = "ditav_test_0000" + TEST_NUMBER_PART_2;
  public static final String TEST_URL_PREFIX = "https://qed.perspectivia.net/documents/";

  private MCRObject object1;
  private MCRObject object2;
  private DitavFrontEndRedirectingDOIService service;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    service = new DitavFrontEndRedirectingDOIService();

    try (InputStream is = DitavFrontEndRedirectingDOIServiceTest.class.getClassLoader()
        .getResourceAsStream("objects/ditav_mods_00000004.xml")) {
      object1 = new MCRObject(new org.jdom2.input.SAXBuilder().build(is));
    }

    object1.setId(MCRObjectID.getInstance(TEST_OBJECT_ID_1));


    try (InputStream is = DitavFrontEndRedirectingDOIServiceTest.class.getClassLoader()
        .getResourceAsStream("objects/ditav_mods_00000001.xml")) {
      object2 = new MCRObject(new org.jdom2.input.SAXBuilder().build(is));
    }

    object2.setId(MCRObjectID.getInstance(TEST_OBJECT_ID_2));
  }

  @Test
  public void getRegisteredURI() throws URISyntaxException {
    URI registeredURI1 = service.getRegisteredURI(object1);
    Assert.assertEquals(TEST_URL_PREFIX + TEST_NUMBER_PART_1, registeredURI1.toString());
    LOGGER.info("Registered URI 1: {}", registeredURI1);

    URI registeredURI2 = service.getRegisteredURI(object2);
    Assert.assertEquals(TEST_URL_PREFIX + TEST_NUMBER_PART_2, registeredURI2.toString());
    LOGGER.info("Registered URI 2: {}", registeredURI2);
  }
  @Override
  protected Map<String, String> getTestProperties() {
    Map<String, String> properties = super.getTestProperties();
    properties.put("MCR.Metadata.Type.mods", "true");
    return properties;
  }


}