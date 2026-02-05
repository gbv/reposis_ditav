package de.gbv.reposis.ditav;


import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.JDOMException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.test.MyCoReTest;


@MyCoReTest
@MCRTestConfiguration(
    properties = {
        @MCRTestProperty(key = "MCR.Metadata.Type.mods", string = "true"),
        @MCRTestProperty(key = "MCR.Metadata.Type.test", string = "true"),
    }
)
public class DitavFrontEndRedirectingDOIServiceTest {

  private static final Logger LOGGER = LogManager.getLogger();

  public static final String TEST_NUMBER_PART_1 = "1234";
  public static final String TEST_OBJECT_ID_1 = "ditav_test_0000" + TEST_NUMBER_PART_1;

  public static final String TEST_NUMBER_PART_2 = "4321";
  public static final String TEST_OBJECT_ID_2 = "ditav_test_0000" + TEST_NUMBER_PART_2;
  public static final String TEST_URL_PREFIX = "https://qed.perspectivia.net/";
  public static final String SUB_PATH = "documents/";

  private MCRObject object1;
  private MCRObject object2;
  private DitavFrontEndRedirectingDOIService service;



  @Test
  public void getRegisteredURI() throws URISyntaxException, IOException, JDOMException {

    service = new DitavFrontEndRedirectingDOIService();
    HashMap<String, String> subPath = new HashMap<>();
    subPath.put("TeiPageSubPath", SUB_PATH);
    service.setProperties(subPath);

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

    URI registeredURI1 = service.getRegisteredURI(object1);

    Assertions.assertEquals(TEST_URL_PREFIX + SUB_PATH + TEST_NUMBER_PART_1,
        registeredURI1.toString());
    LOGGER.info("Registered URI 1: {}", registeredURI1);

    URI registeredURI2 = service.getRegisteredURI(object2);
    // the edition need to point to the base URL only, without sub path and number
    Assertions.assertEquals(TEST_URL_PREFIX, registeredURI2.toString());
    LOGGER.info("Registered URI 2: {}", registeredURI2);
  }


}