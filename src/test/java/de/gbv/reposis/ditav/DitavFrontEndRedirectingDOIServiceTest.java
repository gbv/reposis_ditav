package de.gbv.reposis.ditav;


import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;


public class DitavFrontEndRedirectingDOIServiceTest extends MCRTestCase {

  public static final String TEST_NUMBER_PART = "1234";
  public static final String TEST_OBJECT_ID = "ditav_test_0000" + TEST_NUMBER_PART;
  public static final String TEST_URL_PREFIX = "https://qed.perspectivia.net/documents/";

  private MCRObject object;
  private DitavFrontEndRedirectingDOIService service;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    service = new DitavFrontEndRedirectingDOIService();

    try (InputStream is = DitavFrontEndRedirectingDOIServiceTest.class.getClassLoader()
        .getResourceAsStream("objects/ditav_mods_00000004.xml")) {
      object = new MCRObject(new org.jdom2.input.SAXBuilder().build(is));
    }

    object.setId(MCRObjectID.getInstance(TEST_OBJECT_ID));
  }

  @Test
  public void getRegisteredURI() throws URISyntaxException {
    URI registeredURI = service.getRegisteredURI(object);
    Assert.assertEquals(TEST_URL_PREFIX + TEST_NUMBER_PART, registeredURI.toString());
  }
  @Override
  protected Map<String, String> getTestProperties() {
    Map<String, String> properties = super.getTestProperties();
    properties.put("MCR.Metadata.Type.mods", "true");
    return properties;
  }


}