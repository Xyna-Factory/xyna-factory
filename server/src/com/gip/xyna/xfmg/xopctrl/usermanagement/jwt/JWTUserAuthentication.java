package com.gip.xyna.xfmg.xopctrl.usermanagement.jwt;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.exceptions.XFMG_UserAuthenticationFailedException;
import com.gip.xyna.xfmg.xopctrl.usermanagement.AuthenticationResult;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Domain;
import com.gip.xyna.xfmg.xopctrl.usermanagement.DomainName;
import com.gip.xyna.xfmg.xopctrl.usermanagement.OrderBackedUserAuthentication;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement;
import com.gip.xyna.xprc.XynaOrder;
import com.gip.xyna.xprc.XynaOrderCreationParameter;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;

public class JWTUserAuthentication extends OrderBackedUserAuthentication {

    public static final String ORDER_CONTEXT_KEY_JWT_TOKEN = "xfmg.xopctrl.jwt.token";
    private static final Logger logger = CentralFactoryLogging.getLogger(JWTUserAuthentication.class);

    private final JWTDomainSpecificData domainSpecificData;
    private final String domainName;

    public JWTUserAuthentication(Domain domain) {
        super(domain);
        this.domainSpecificData = (JWTDomainSpecificData) domain.getDomainSpecificData();
        this.domainName = domain.getName();
    }

    public JWTUserAuthentication(JWTDomainSpecificData domainSpecificData) {
        super(null);
        this.domainSpecificData = domainSpecificData;
        this.domainName = null;
    }


    @Override
    public XynaOrder generateAuthOrder(String username, String password, Domain domain, int retry) {
        DomainName domainName = new DomainName(domain.getName());
        DestinationKey dk = new DestinationKey(domainSpecificData.getAssociatedOrdertype(), domainSpecificData.getRuntimeContext());
        XynaOrderCreationParameter xocp = new XynaOrderCreationParameter(dk, domainName);
        XynaOrderServerExtension xo = new XynaOrderServerExtension(xocp);
        xo.setNewOrderContext();
        xo.getOrderContext().set(ORDER_CONTEXT_KEY_JWT_TOKEN, password);
        return xo;
    }


    @Override
    public Role handleResponse(AuthenticationResult result) throws XFMG_UserAuthenticationFailedException {
        if (result != null && result.wasSuccesfull()) {
            if (logger.isDebugEnabled()) {
                logger.debug("JWT authentication succeeded for user '" + username + "' in domain '" + domainName + "'.");
            }
            try {
                return XynaFactory.getPortalInstance().getXynaMultiChannelPortalPortal()
                    .getRole(result.getRole(), UserManagement.PREDEFINED_LOCALDOMAIN_NAME);
            } catch (Exception e) {
                logger.warn("JWT authentication returned role '" + result.getRole() + "' which could not be resolved.", e);
                throw new XFMG_UserAuthenticationFailedException(username, e);
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("JWT authentication failed for user '" + username + "' in domain '" + domainName + "'.");
        }
        return null;
    }


    @Override
    public Role handleError(Throwable exception) throws XFMG_UserAuthenticationFailedException {
        logger.warn("JWT authentication order execution failed for user '" + username + "'.", exception);
        return null;
    }


    public String getConfiguredDefaultRole() {
        String defaultRole = domainSpecificData.getDefaultRole().orElse(null);
        if (defaultRole == null) {
            return null;
        }
        String trimmed = defaultRole.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }





}