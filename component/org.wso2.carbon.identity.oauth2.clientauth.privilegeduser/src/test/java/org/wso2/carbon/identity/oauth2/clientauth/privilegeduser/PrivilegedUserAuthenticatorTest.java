/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.oauth2.clientauth.privilegeduser;

import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.common.testng.WithCarbonHome;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.oauth2.bean.OAuthClientAuthnContext;
import org.wso2.carbon.identity.oauth2.clientauth.privilegeduser.internal.PrivilegedUserAuthenticatorServiceHolder;
import org.wso2.carbon.identity.oauth2.util.OAuth2Util;
import org.wso2.carbon.user.api.AuthorizationManager;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.util.UserCoreUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@PrepareForTest({
        HttpServletRequest.class,
        OAuth2Util.class,
        IdentityTenantUtil.class,
        PrivilegedUserAuthenticatorServiceHolder.class,
        UserCoreUtil.class,
})
@WithCarbonHome
public class PrivilegedUserAuthenticatorTest extends PowerMockTestCase {

    private PrivilegedUserAuthenticator privilegedUserAuthenticator = new PrivilegedUserAuthenticator();
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String USERNAME_VALUE = "user1";
    private static final String PASSWORD_VALUE = "password1";
    private static final String CLIENT_ID = "KrVLov4Bl3natUksF2HmWsdw684a";
    private static final String REVOKE_ENDPOINT = "/oauth2/revoke";

    @Mock
    private RealmService realmService;
    @Mock
    private UserRealm userRealm;
    @Mock
    private UserStoreManager userStoreManager;
    @Mock
    private PrivilegedUserAuthenticatorServiceHolder privilegedUserAuthenticatorServiceHolder;
    @Mock
    private RealmConfiguration mockedRealmConfiguration;
    @Mock
    private AuthorizationManager authorizationManager;

    @DataProvider(name = "testCanAuthenticateData")
    public Object[][] testCanAuthenticateData() {

        return new Object[][]{

                // Correct request parameters.
                {"username", "password", new HashMap<String, List>(), true},

                // Fault  request parameter name.
                {"user","password", new HashMap<String, List>(), false},

                // Fault  request parameter names.
                {"username","pass", new HashMap<String, List>(), false},

                // Fault  request parameter names.
                {"user","pass", new HashMap<String, List>(), false},

                // No credential parameter
                { null, null, new HashMap<String, List>(), false},

        };
    }

    @Test(dataProvider = "testCanAuthenticateData")
    public void testCanAuthenticate(String userNameParam, String passwordParam,
                                    HashMap<String, List> bodyContent, boolean
            canHandle) throws Exception {

        HttpServletRequest httpServletRequest = PowerMockito.mock(HttpServletRequest.class);
        List<String> userNameCredentials = new ArrayList<>();
        userNameCredentials.add(USERNAME_VALUE);
        List<String> passwordCredentials = new ArrayList<>();
        passwordCredentials.add(PASSWORD_VALUE);
        bodyContent.put(userNameParam, userNameCredentials);
        bodyContent.put(passwordParam, passwordCredentials);
        when(httpServletRequest.getRequestURI()).thenReturn(REVOKE_ENDPOINT);
        assertEquals(privilegedUserAuthenticator.canAuthenticate(httpServletRequest, bodyContent, new
                OAuthClientAuthnContext()), canHandle, "Expected can authenticate evaluation not received");
    }

    @Test
    public void testGetName() throws Exception {

        assertEquals("PrivilegedUserAuthenticator", privilegedUserAuthenticator.getName(),
                "PrivilegedUserAuthenticator name has changed.");
    }

    @Test
    public void testGetClientId() throws Exception {

        Map<String, List> bodyContent = new HashMap<>();
        List<String> clientIDContent = new ArrayList<>();
        clientIDContent.add(CLIENT_ID);
        bodyContent.put("client_id", clientIDContent);
        HttpServletRequest httpServletRequest = PowerMockito.mock(HttpServletRequest.class);
        String clientId = privilegedUserAuthenticator.getClientId(httpServletRequest, bodyContent,
                new OAuthClientAuthnContext());
        assertEquals(clientId, "KrVLov4Bl3natUksF2HmWsdw684a", "The expected client id is not found.");
    }


    @Test()
    public void testAuthenticateClient() throws Exception {

        OAuthClientAuthnContext oAuthClientAuthnContextObj =  buildOAuthClientAuthnContext(CLIENT_ID);
        HttpServletRequest httpServletRequest = PowerMockito.mock(HttpServletRequest.class);

        mockStatic(IdentityTenantUtil.class);
        when(IdentityTenantUtil.getTenantIdOfUser(anyString())).thenReturn(-1234);

        mockStatic(PrivilegedUserAuthenticatorServiceHolder.class);
        when(PrivilegedUserAuthenticatorServiceHolder.getInstance()).thenReturn(privilegedUserAuthenticatorServiceHolder);

        mockStatic(UserCoreUtil.class);

        HashMap<String, List> bodyContent = new HashMap<>();
        List<String> userNameCredentials = new ArrayList<>();
        userNameCredentials.add(USERNAME_VALUE);
        List<String> passwordCredentials = new ArrayList<>();
        passwordCredentials.add(PASSWORD_VALUE);
        bodyContent.put(USERNAME, userNameCredentials);
        bodyContent.put(PASSWORD, passwordCredentials);

        when(privilegedUserAuthenticatorServiceHolder.getRealmService()).thenReturn(realmService);
        when(realmService.getTenantUserRealm(anyInt())).thenReturn(userRealm);
        when(userRealm.getUserStoreManager()).thenReturn(userStoreManager);
        when(userStoreManager.authenticate(anyString(), any())).thenReturn(true);

        when(userRealm.getRealmConfiguration()).thenReturn(mockedRealmConfiguration);
        when(UserCoreUtil.getDomainName(mockedRealmConfiguration)).thenReturn("PRIMARY");

        when(userRealm.getAuthorizationManager()).thenReturn(authorizationManager);
        when(authorizationManager.isUserAuthorized(anyString(), anyString(), anyString())).thenReturn(true);

        assertTrue(privilegedUserAuthenticator.authenticateClient(httpServletRequest, bodyContent,
                oAuthClientAuthnContextObj), "Expected client authentication result was not " +
                "received");
    }


    private OAuthClientAuthnContext buildOAuthClientAuthnContext(String clientId) {

        OAuthClientAuthnContext oAuthClientAuthnContext = new OAuthClientAuthnContext();
        oAuthClientAuthnContext.setClientId(clientId);
        return oAuthClientAuthnContext;
    }
}
