package de.gbv.reposis.ditav;

import jakarta.servlet.http.HttpServletRequest;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.mir.viewer.MIRViewerConfigurationStrategy;
import org.mycore.viewer.configuration.MCRViewerConfiguration;

public class DitavViewerConfigurationStrategy extends MIRViewerConfigurationStrategy {

    @Override
    public MCRViewerConfiguration get(HttpServletRequest request) {
        MCRViewerConfiguration defaultConfig = super.get(request);

        defaultConfig.setProperty("text.showOnStart", "transcription");
        defaultConfig.setProperty("chapter.showOnStart", false);
        defaultConfig.setProperty("actionGroupEnabled", true);

        if(request.getParameter("frame") != null && request.getParameter("frame").equals("true")) {
            defaultConfig.addScript(MCRFrontendUtil.getBaseURL() + "assets/bootstrap/js/bootstrap.min.js", false);
        }

        return defaultConfig;
    }
}
