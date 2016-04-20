/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.security.servlet.logout;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.tika.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import ddf.action.Action;
import ddf.security.SecurityConstants;
import ddf.security.common.SecurityTokenHolder;
import ddf.security.http.SessionFactory;
import ddf.security.service.SecurityManager;
import ddf.security.service.SecurityServiceException;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;

public class TestLogoutService {

    private static SessionFactory sessionFactory;

    private static SecurityManager sm;

    @BeforeClass
    public static void initialize() {

        Map<String, SecurityToken> realmTokenMap = new HashMap<>();
        realmTokenMap.put("karaf", new SecurityToken());
        realmTokenMap.put("ldap", new SecurityToken());

        sessionFactory = mock(SessionFactory.class);
        HttpSession httpSession = mock(HttpSession.class);
        SecurityTokenHolder securityTokenHolder = mock(SecurityTokenHolder.class);
        sm = mock(SecurityManager.class);

        when(sessionFactory.getOrCreateSession(null)).thenReturn(httpSession);
        when(httpSession.getAttribute(SecurityConstants.SAML_ASSERTION)).thenReturn(
                securityTokenHolder);
        when(securityTokenHolder.getRealmTokenMap()).thenReturn(realmTokenMap);
    }

    @Test
    public void testLogout() throws IOException, ParseException, SecurityServiceException {
        KarafLogoutAction karafLogoutActionProvider = new KarafLogoutAction();
        LdapLogoutAction ldapLogoutActionProvider = new LdapLogoutAction();
        List<Action> karafLogoutActions = karafLogoutActionProvider.getActions(null);
        List<Action> ldapLogoutActions = ldapLogoutActionProvider.getActions(null);

        LogoutService logoutService = new LogoutService();
        logoutService.setHttpSessionFactory(sessionFactory);
        logoutService.setSecurityManager(sm);
        logoutService.setLogoutActionProviders(Arrays.asList(karafLogoutActionProvider,
                ldapLogoutActionProvider));

        String responseMessage =
                IOUtils.toString((ByteArrayInputStream) logoutService.getActionProviders(null)
                        .getEntity());

        JSONArray actionProperties = (JSONArray) new JSONParser().parse(responseMessage);
        assertEquals(2, actionProperties.size());
        JSONObject karafActionProperty = ((JSONObject) actionProperties.get(0));

        assertEquals(karafActionProperty.get("description"),
                karafLogoutActions.get(0)
                        .getDescription());
        assertEquals(karafActionProperty.get("realm"),
                karafLogoutActions.get(0)
                        .getId()
                        .substring(karafLogoutActions.get(0)
                                .getId()
                                .lastIndexOf(".") + 1));
        assertEquals(karafActionProperty.get("title"),
                karafLogoutActions.get(0)
                        .getTitle());
        assertEquals(karafActionProperty.get("url"),
                karafLogoutActions.get(0)
                        .getUrl()
                        .toString());

        JSONObject ldapActionProperty = ((JSONObject) actionProperties.get(1));

        assertEquals(ldapActionProperty.get("description"),
                ldapLogoutActions.get(0)
                        .getDescription());
        assertEquals(ldapActionProperty.get("realm"),
                ldapLogoutActions.get(0)
                        .getId()
                        .substring(ldapLogoutActions.get(0)
                                .getId()
                                .lastIndexOf(".") + 1));
        assertEquals(ldapActionProperty.get("title"),
                ldapLogoutActions.get(0)
                        .getTitle());
        assertEquals(ldapActionProperty.get("url"),
                ldapLogoutActions.get(0)
                        .getUrl()
                        .toString());
    }
}