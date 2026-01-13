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


  @Override
  public URI getRegisteredURI(MCRBase obj) throws URISyntaxException {
    if (!(obj instanceof MCRObject)) {
      throw new IllegalArgumentException("Object must be of type MCRObject");
    }
    MCRMODSWrapper mods = new MCRMODSWrapper((MCRObject) obj);
    String urlPrefix = Optional.ofNullable(
            mods.getElement("mods:relatedItem[@type='host']/mods:location/mods:url"))
        .map(Element::getTextNormalize)
        .orElseThrow(() -> new IllegalArgumentException(
            "Parent MODS metadata does not contain required URL element"));

    String extractedNumber = Integer.valueOf(obj.getId().getNumberAsInteger()).toString();
    return new URI(urlPrefix).resolve(extractedNumber);
  }

}
