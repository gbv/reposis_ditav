package de.gbv.reposis.ditav;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.mir.authorization.MIRStrategy;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;

public class DitavExtendedMIRStrategy extends MIRStrategy {

  private static final String ASSET_ID_PREFIX = "cms:asset:";
  private static final String PAGE_ID_PREFIX = "cms:page:";
  private static final String MAPPING_PROP_PREFIX = "DITAV.CMS.Rights.DirectoryRoleMapping.";
  private final Map<String, String> directoryRoleMap;


  private static final Logger LOGGER = LogManager.getLogger();

  public DitavExtendedMIRStrategy() {
    LOGGER.info("Initializing DitavExtendedMIRStrategy with directory-role mappings from configuration");
    directoryRoleMap = MCRConfiguration2.getSubPropertiesMap(MAPPING_PROP_PREFIX);
  }



  @Override
  public boolean checkPermission(String id, String permission) {
    if (id.startsWith("cms:")) {
      return checkCMSPermission(id, permission);
    }

    return super.checkPermission(id, permission);
  }

  public boolean checkCMSPermission(String id, String permission) {
    if (MCRUserManager.getCurrentUser().isUserInRole("admin")) {
      return true;
    }

    if (id == null) {
      return false;
    }

    String path;
    if (id.startsWith(ASSET_ID_PREFIX)) {
      path = id.substring(ASSET_ID_PREFIX.length());
    } else if (id.startsWith(PAGE_ID_PREFIX)) {
      path = id.substring(PAGE_ID_PREFIX.length());
    } else {
      return false;
    }

    if(permission.equals("read")) {
      return true;
    }

    // convert path to replace all non [a-zA-Z0-9] with _
    String pseudoPath = path.replaceAll("[^a-zA-Z0-9]", "_");

    for (Entry<String, String> entry : directoryRoleMap.entrySet()) {
      String directory = entry.getKey();
      if (!pseudoPath.startsWith(directory)) {
        continue;
      }

      String rolesStr = entry.getValue();
      List<String> roles = List.of(rolesStr.split(";"));
      for (String role : roles) {

        // since we are using rest api, we dont want to put all institutes in the token property
        // so we check for role directly
        String userID = MCRUserManager.getCurrentUser().getUserID();
        MCRUser user = MCRUserManager.getUser(userID);
        if (user != null && user.isUserInRole(role)) {
          LOGGER.info("User {} has role {} for path {}, granting permission {}",
              MCRUserManager.getCurrentUser().getUserName(), role, path, permission);
          return true;
        }
      }

    }

    return false;
  }


}
