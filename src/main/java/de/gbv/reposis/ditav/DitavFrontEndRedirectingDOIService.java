package de.gbv.reposis.ditav;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import org.jdom2.Element;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.mods.MCRMODSWrapper;
import org.mycore.pi.doi.MCRDOIService;

public class DitavFrontEndRedirectingDOIService extends MCRDOIService {


  public static final String SUB_PATH_PROPERTY = "TeiPageSubPath";

  @Override
  public URI getRegisteredURI(MCRBase obj) throws URISyntaxException {
    if (!(obj instanceof MCRObject)) {
      throw new IllegalArgumentException("Object must be of type MCRObject");
    }
    MCRMODSWrapper mods = new MCRMODSWrapper((MCRObject) obj);

    Element genreElement = mods.getElement("mods:genre[@type='intern']");
    boolean isEdition = false;
    if (genreElement != null) {
      String valueURI = genreElement.getAttributeValue("valueURI");
      if (valueURI != null) {
        isEdition = valueURI.endsWith("#edition");
      }
    }

    String teiPageSubPath = getProperties().get(SUB_PATH_PROPERTY);

    String xPath = "mods:location/mods:url";
    if (!isEdition) {
      xPath = "mods:relatedItem[@type='host']/" + xPath;
    }

    String urlPrefix = Optional.ofNullable(
            mods.getElement(xPath))
        .map(Element::getTextNormalize)
        .orElseThrow(() -> new IllegalArgumentException(
            "Parent MODS metadata does not contain required URL element"));

    String extractedNumber = Integer.valueOf(obj.getId().getNumberAsInteger()).toString();
    URI uri = new URI(urlPrefix);

    // the teiPageSubPath is only appended for the tei files in the edition
    if (!isEdition && teiPageSubPath != null && !teiPageSubPath.isEmpty()) {
      return uri.resolve(teiPageSubPath).resolve(extractedNumber);
    }

    return uri;
  }

}
